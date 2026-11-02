package com.fredflips;

import com.fredflips.ge.GeSuggestion;
import com.fredflips.trades.OfferHealth;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.wiki.LatestPrice;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

/**
 * Colours each active GE offer box by whether its price is still competitive, shows how long it has been
 * listed, and pops up a details panel when you hover over it.
 */
class GeSlotOverlay extends Overlay
{
	private static final Font TIMER_FONT = new Font("Dialog", Font.BOLD, 10);
	private static final Font COUNTER_FONT = new Font("Dialog", Font.BOLD, 10);
	// The progress bar sits along the bottom of each offer box.
	private static final int BAR_BASELINE_FROM_BOTTOM = 9;
	private static final int PANEL_WIDTH = 170;

	private final Client client;
	private final FredFlipsPlugin plugin;
	private final FredFlipsConfig config;
	private final PanelComponent panel = new PanelComponent();

	@Inject
	GeSlotOverlay(Client client, FredFlipsPlugin plugin, FredFlipsConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		panel.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.geSlotHighlights() || !config.trackTrades())
		{
			return null;
		}
		Widget firstSlot = client.getWidget(InterfaceID.GeOffers.INDEX_0);
		Widget setup = client.getWidget(InterfaceID.GeOffers.SETUP);
		// Only on the main GE screen with the 8 boxes, not while setting up an offer.
		if (firstSlot == null || firstSlot.isHidden() || (setup != null && !setup.isHidden()))
		{
			return null;
		}

		Map<Integer, OfferSnapshot> slots = plugin.getSlotView();
		long now = System.currentTimeMillis();
		Point mouse = client.getMouseCanvasPosition();
		Rectangle hovered = null;
		OfferSnapshot hoveredOffer = null;
		Color hoveredColor = null;

		for (int slot = 0; slot < 8; slot++)
		{
			OfferSnapshot offer = slots.get(slot);
			Widget box = client.getWidget(InterfaceID.GeOffers.INDEX_0 + slot);
			if (offer == null || box == null || box.isHidden())
			{
				continue;
			}
			Rectangle bounds = box.getBounds();
			Color color = colorFor(offer, plugin.marketPrice(offer.getItemId()), now);

			graphics.setStroke(new BasicStroke(2));
			graphics.setColor(color);
			graphics.drawRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 3, 6, 6);

			long listed = now - offer.getPlacedAt();
			String timer = clock(listed, config.geTimerCompact());
			graphics.setFont(TIMER_FONT);
			int textWidth = graphics.getFontMetrics().stringWidth(timer);
			drawShadowed(graphics, timer, bounds.x + bounds.width - textWidth - 6, bounds.y + 14, timerColor(offer, color, listed));

			if (config.geFillCounter())
			{
				graphics.setFont(COUNTER_FONT);
				String counter = offer.getQuantitySold() + "/" + offer.getTotalQuantity();
				int baseline = bounds.y + bounds.height - BAR_BASELINE_FROM_BOTTOM;
				drawShadowed(graphics, counter, bounds.x + 8, baseline, Color.WHITE);
				if (offer.getLastFillAt() > 0 && offer.getStatus().isPending())
				{
					String since = shortDuration(now - offer.getLastFillAt());
					int sinceWidth = graphics.getFontMetrics().stringWidth(since);
					drawShadowed(graphics, since, bounds.x + bounds.width - sinceWidth - 8, baseline, Color.LIGHT_GRAY);
				}
			}

			if (mouse != null && bounds.contains(mouse.getX(), mouse.getY()))
			{
				hovered = bounds;
				hoveredOffer = offer;
				hoveredColor = color;
			}
		}

		if (hovered != null && config.geSlotHoverPanel())
		{
			drawHoverPanel(graphics, hovered, hoveredOffer, hoveredColor, now);
		}
		return null;
	}

	/**
	 * The timer shows the offer's colour while it's fresh, then the borderline and off-market colours as it ages.
	 */
	private Color timerColor(OfferSnapshot offer, Color offerColor, long listedMillis)
	{
		if (!offer.getStatus().isPending())
		{
			return offerColor;
		}
		long minutes = listedMillis / 60_000;
		if (minutes >= config.geTimerOffMarketMinutes())
		{
			return config.geColourOffMarket();
		}
		if (minutes >= config.geTimerBorderlineMinutes())
		{
			return config.geColourBorderline();
		}
		return offerColor;
	}

	private static void drawShadowed(Graphics2D graphics, String text, int x, int y, Color color)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x + 1, y + 1);
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	/**
	 * "00:25:20", or "00:25" when compact.
	 */
	static String clock(long millis, boolean compact)
	{
		long seconds = Math.max(0, millis) / 1000;
		long hours = seconds / 3600;
		long minutes = seconds / 60 % 60;
		return compact
			? String.format("%02d:%02d", hours, minutes)
			: String.format("%02d:%02d:%02d", hours, minutes, seconds % 60);
	}

	private Color colorFor(OfferSnapshot offer, LatestPrice market, long now)
	{
		Color good = config.geColourCompetitive();
		if (!offer.getStatus().isPending())
		{
			// Finished: waiting for you to collect.
			return good;
		}
		OfferHealth.Level level = OfferHealth.check(offer, market).getLevel();
		long waitingSince = offer.getLastFillAt() > 0 ? offer.getLastFillAt() : offer.getPlacedAt();
		boolean stale = now - waitingSince >= config.staleOfferMinutes() * 60_000L;
		if (level == OfferHealth.Level.BAD)
		{
			return config.geColourOffMarket();
		}
		if (level == OfferHealth.Level.WARN || stale)
		{
			return config.geColourBorderline();
		}
		return good;
	}

	private void drawHoverPanel(Graphics2D graphics, Rectangle box, OfferSnapshot offer, Color color, long now)
	{
		LatestPrice market = plugin.marketPrice(offer.getItemId());
		GeSuggestion suggestion = plugin.suggestionFor(offer.getItemId(), !offer.isBuy());

		panel.getChildren().clear();
		panel.getChildren().add(TitleComponent.builder().text(offer.getName()).color(Color.WHITE).build());
		panel.getChildren().add(line(offer.getStatus().getDisplayName(),
			gp(offer.getQuantitySold()) + " / " + gp(offer.getTotalQuantity()), Color.WHITE));
		panel.getChildren().add(line("Your price", gp(offer.getPrice()), Color.WHITE));
		if (market != null && market.getLow() != null && market.getHigh() != null)
		{
			panel.getChildren().add(line("Market", gp(market.getLow()) + " - " + gp(market.getHigh()), Color.LIGHT_GRAY));
		}
		if (suggestion != null && suggestion.getPrice() != null)
		{
			panel.getChildren().add(line("FredFlips " + (offer.isBuy() ? "buy" : "sell"), gp(suggestion.getPrice()), new Color(0xFF981F)));
		}
		if (offer.getStatus().isPending())
		{
			panel.getChildren().add(LineComponent.builder().left(OfferHealth.check(offer, market).getDescription()).leftColor(color).build());
			String wait = offer.getLastFillAt() > 0
				? "Last fill " + shortDuration(now - offer.getLastFillAt()) + " ago"
				: "Nothing filled for " + shortDuration(now - offer.getPlacedAt());
			panel.getChildren().add(LineComponent.builder().left(wait).leftColor(Color.LIGHT_GRAY).build());
		}
		else
		{
			panel.getChildren().add(LineComponent.builder().left("Finished - collect it").leftColor(config.geColourCompetitive()).build());
		}

		// Beside the box, flipped to the left if it would go off the edge of the game screen.
		int x = box.x + box.width + 4;
		if (x + PANEL_WIDTH > client.getCanvasWidth())
		{
			x = Math.max(0, box.x - PANEL_WIDTH - 4);
		}
		graphics.translate(x, box.y);
		panel.render(graphics);
		graphics.translate(-x, -box.y);
	}

	private static LineComponent line(String left, String right, Color rightColor)
	{
		return LineComponent.builder().left(left).leftColor(Color.LIGHT_GRAY).right(right).rightColor(rightColor).build();
	}

	private static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount);
	}

	/**
	 * "45s", "12m", "3h05".
	 */
	static String shortDuration(long millis)
	{
		long seconds = Math.max(0, millis) / 1000;
		if (seconds < 60)
		{
			return seconds + "s";
		}
		long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m";
		}
		return String.format("%dh%02d", minutes / 60, minutes % 60);
	}
}
