package com.fredflips;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.QuantityFormatter;

/**
 * A "+123K gp" (or "-4K gp") that floats up and fades out above the GE when a sale completes a flip.
 */
class GpDropOverlay extends Overlay
{
	// How far the text rises over its lifetime, in pixels.
	private static final int RISE = 40;

	private final Client client;
	private final FredFlipsConfig config;
	private final ConcurrentLinkedQueue<Drop> drops = new ConcurrentLinkedQueue<>();

	private static final class Drop
	{
		final long profit;
		final long startedAt;

		Drop(long profit, long startedAt)
		{
			this.profit = profit;
			this.startedAt = startedAt;
		}
	}

	@Inject
	GpDropOverlay(Client client, FredFlipsConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	void addDrop(long profit)
	{
		if (config.gpDrop() && profit != 0)
		{
			drops.add(new Drop(profit, System.currentTimeMillis()));
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (drops.isEmpty())
		{
			return null;
		}
		long now = System.currentTimeMillis();
		long duration = Math.max(100, config.gpDropMillis());
		Rectangle anchor = anchor();
		graphics.setFont(new Font("Dialog", Font.BOLD, config.gpDropFontSize()));

		int stack = 0;
		for (Iterator<Drop> it = drops.iterator(); it.hasNext(); )
		{
			Drop drop = it.next();
			double progress = (now - drop.startedAt) / (double) duration;
			if (progress >= 1)
			{
				it.remove();
				continue;
			}
			String text = (drop.profit > 0 ? "+" : "-") + QuantityFormatter.quantityToStackSize(Math.abs(drop.profit)) + " gp";
			int width = graphics.getFontMetrics().stringWidth(text);
			int x = anchor.x + anchor.width / 2 - width / 2 + config.gpDropOffsetX();
			int y = anchor.y - (int) (RISE * progress) - stack * graphics.getFontMetrics().getHeight() + config.gpDropOffsetY();

			Composite before = graphics.getComposite();
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - progress)));
			graphics.setColor(Color.BLACK);
			graphics.drawString(text, x + 1, y + 1);
			graphics.setColor(drop.profit > 0 ? config.gpDropProfitColour() : config.gpDropLossColour());
			graphics.drawString(text, x, y);
			graphics.setComposite(before);
			stack++;
		}
		return null;
	}

	/**
	 * Just above the GE window when it's open, otherwise the middle of the game screen.
	 */
	private Rectangle anchor()
	{
		Widget ge = client.getWidget(InterfaceID.GeOffers.INDEX);
		if (ge != null && !ge.isHidden())
		{
			Rectangle bounds = ge.getBounds();
			return new Rectangle(bounds.x, bounds.y, bounds.width, 0);
		}
		return new Rectangle(client.getViewportXOffset(), client.getViewportYOffset() + client.getViewportHeight() / 3,
			client.getViewportWidth(), 0);
	}
}
