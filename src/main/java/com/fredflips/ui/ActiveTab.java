package com.fredflips.ui;

import com.fredflips.flip.GeTax;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferHealth;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.TradeTracker;
import com.fredflips.wiki.LatestPrice;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;

/**
 * Your 8 GE slots and the items you've bought but not sold yet.
 */
class ActiveTab extends JPanel
{
	private final JPanel content = Ui.stack();

	ActiveTab()
	{
		super(new BorderLayout());
		setOpaque(false);
		content.add(Ui.note("Log in and open the Grand Exchange to see your offers."));
		add(content, BorderLayout.CENTER);
	}

	void show(Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		Map<Integer, LatestPrice> market, Set<Integer> taxExempt, int staleMinutes, boolean tracking, long now)
	{
		content.removeAll();
		if (market == null)
		{
			market = Collections.emptyMap();
		}

		content.add(Ui.heading("GE slots"));
		if (!tracking)
		{
			Ui.addSpaced(content, Ui.note("Turn on \"Track my trades\" in the settings to see your GE slots here."));
		}
		else
		{
			int used = 0;
			for (int slot = 0; slot < TradeTracker.SLOT_COUNT; slot++)
			{
				OfferSnapshot offer = slots.get(slot);
				if (offer != null)
				{
					used++;
					Ui.addSpaced(content, slotCard(slot, offer, market.get(offer.getItemId()), staleMinutes, now));
				}
			}
			Ui.addSpaced(content, Ui.muted(used + " of " + TradeTracker.SLOT_COUNT + " slots in use"));
		}

		content.add(Ui.heading("Holding (bought, not sold)"));
		if (holdings.isEmpty())
		{
			Ui.addSpaced(content, Ui.note("Nothing. Items you buy show here until you sell them."));
		}
		for (FlipLedger.OpenPosition position : holdings)
		{
			Ui.addSpaced(content, holdingCard(position, market.get(position.getItemId()), taxExempt, now));
		}

		content.revalidate();
		content.repaint();
	}

	private static JPanel slotCard(int slot, OfferSnapshot offer, LatestPrice market, int staleMinutes, long now)
	{
		JPanel card = Ui.card();
		card.add(Ui.row("Slot " + (slot + 1) + " - " + (offer.isBuy() ? "BUY" : "SELL"), offer.getName(), Color.WHITE));

		int percent = offer.getTotalQuantity() == 0 ? 0 : offer.getQuantitySold() * 100 / offer.getTotalQuantity();
		Color statusColor = offer.getStatus().isPending() ? Color.WHITE : Ui.GOOD;
		card.add(Ui.row(offer.getStatus().getDisplayName(),
			Ui.gp(offer.getQuantitySold()) + " / " + Ui.gp(offer.getTotalQuantity()) + " (" + percent + "%)", statusColor));

		String marketText = market == null || market.getLow() == null || market.getHigh() == null
			? "-"
			: Ui.gp(market.getLow()) + " - " + Ui.gp(market.getHigh());
		card.add(Ui.row("Your price " + Ui.gp(offer.getPrice()), "Market " + marketText, Ui.MUTED));

		if (offer.getStatus().isPending())
		{
			OfferHealth health = OfferHealth.check(offer, market);
			card.add(Ui.label(health.getDescription(), color(health.getLevel())));

			long waitingSince = offer.getLastFillAt() > 0 ? offer.getLastFillAt() : offer.getPlacedAt();
			long waited = now - waitingSince;
			String waitText = offer.getLastFillAt() > 0
				? "Last fill " + Ui.duration(waited) + " ago"
				: "Nothing filled yet - waiting " + Ui.duration(waited);
			card.add(Ui.label(waitText, waited >= staleMinutes * 60_000L ? Ui.WARN : Ui.MUTED));
		}
		else
		{
			card.add(Ui.muted("Finished " + Ui.duration(now - Math.max(offer.getLastFillAt(), offer.getPlacedAt())) + " ago"));
		}
		return card;
	}

	private static JPanel holdingCard(FlipLedger.OpenPosition position, LatestPrice market, Set<Integer> taxExempt, long now)
	{
		JPanel card = Ui.card();
		card.add(Ui.row(position.getName(), "x" + Ui.gp(position.getQuantity()), Color.WHITE));
		card.add(Ui.row("Avg cost", Ui.gp(position.getAverageCost()) + " gp"));
		if (market != null && market.getHigh() != null)
		{
			long sellAt = market.getHigh() - 1;
			long afterTax = sellAt - GeTax.perItem(sellAt, taxExempt.contains(position.getItemId()));
			long unrealised = (afterTax - position.getAverageCost()) * position.getQuantity();
			card.add(Ui.row("Sell now at ~" + Ui.gp(sellAt), Ui.signedShortGp(unrealised) + " after tax", Ui.profitColor(unrealised)));
		}
		card.add(Ui.muted("Held " + Ui.duration(now - position.getBoughtAt())));
		return card;
	}

	private static Color color(OfferHealth.Level level)
	{
		switch (level)
		{
			case GOOD:
				return Ui.GOOD;
			case BAD:
				return Ui.BAD;
			default:
				return Ui.WARN;
		}
	}
}
