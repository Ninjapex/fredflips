package com.fredflips;

import com.fredflips.flip.GeTax;
import com.fredflips.trades.FlipLedger;
import com.fredflips.wiki.LatestPrice;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * Hover an item in your inventory that FredFlips saw you buy to see what you paid and what it's worth now.
 */
class InventoryCostOverlay extends Overlay
{
	private final Client client;
	private final FredFlipsPlugin plugin;
	private final FredFlipsConfig config;
	private final ItemManager itemManager;
	private final TooltipManager tooltipManager;

	@Inject
	InventoryCostOverlay(Client client, FredFlipsPlugin plugin, FredFlipsConfig config, ItemManager itemManager,
		TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.itemManager = itemManager;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.inventoryCostTooltip() || client.isMenuOpen())
		{
			return null;
		}
		Map<Integer, FlipLedger.OpenPosition> holdings = plugin.getHoldingsView();
		Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
		Point mouse = client.getMouseCanvasPosition();
		if (holdings.isEmpty() || inventory == null || inventory.isHidden() || mouse == null)
		{
			return null;
		}
		Widget[] slots = inventory.getDynamicChildren();
		if (slots == null)
		{
			return null;
		}
		for (Widget slot : slots)
		{
			if (slot == null || slot.getItemId() <= 0 || !slot.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				continue;
			}
			// Notes and placeholders share the real item's prices.
			int itemId = itemManager.canonicalize(slot.getItemId());
			FlipLedger.OpenPosition position = holdings.get(itemId);
			if (position != null)
			{
				tooltipManager.add(new Tooltip(text(position, plugin.marketPrice(itemId))));
			}
			break;
		}
		return null;
	}

	private String text(FlipLedger.OpenPosition position, LatestPrice market)
	{
		StringBuilder text = new StringBuilder("FredFlips: paid ")
			.append(QuantityFormatter.formatNumber(position.getAverageCost())).append(" gp each");
		if (market != null && market.getHigh() != null)
		{
			long sellAt = market.getHigh() - 1;
			long afterTax = sellAt - GeTax.perItem(sellAt, plugin.isTaxExempt(position.getItemId()));
			long profit = (afterTax - position.getAverageCost()) * position.getQuantity();
			text.append("</br>Sell now ~").append(QuantityFormatter.formatNumber(sellAt))
				.append(" = ").append(profit >= 0 ? "<col=00c27a>+" : "<col=e85050>")
				.append(QuantityFormatter.quantityToStackSize(profit)).append("</col> after tax");
		}
		return text.toString();
	}
}
