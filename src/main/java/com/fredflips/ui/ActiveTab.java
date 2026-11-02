package com.fredflips.ui;

import com.fredflips.flip.GeTax;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferHealth;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.TradeTracker;
import com.fredflips.wiki.LatestPrice;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;

/**
 * Your GE slots as cards tinted by how competitive the price is, then items you've bought but not sold.
 */
class ActiveTab extends JPanel
{
	private static final Color GOOD_TINT = new Color(0x1C3A2E);
	private static final Color MIDDLE_TINT = new Color(0x333333);
	private static final Color BAD_TINT = new Color(0x3D2323);

	private final ItemManager itemManager;
	private final JPanel content = Ui.stack();

	ActiveTab(ItemManager itemManager)
	{
		super(new BorderLayout());
		setOpaque(false);
		this.itemManager = itemManager;
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
			if (used == 0)
			{
				Ui.addSpaced(content, Ui.note("No GE offers. Log in and open the Grand Exchange to see them here."));
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

	private JPanel slotCard(int slot, OfferSnapshot offer, LatestPrice market, int staleMinutes, long now)
	{
		OfferHealth health = OfferHealth.check(offer, market);
		long waitingSince = offer.getLastFillAt() > 0 ? offer.getLastFillAt() : offer.getPlacedAt();
		boolean stale = offer.getStatus().isPending() && now - waitingSince >= staleMinutes * 60_000L;
		Color tint = !offer.getStatus().isPending() ? GOOD_TINT
			: health.getLevel() == OfferHealth.Level.BAD ? BAD_TINT
			: health.getLevel() == OfferHealth.Level.WARN || stale ? MIDDLE_TINT
			: GOOD_TINT;

		JPanel card = new JPanel(new BorderLayout(8, 0));
		card.setBackground(tint);
		card.setBorder(new EmptyBorder(8, 10, 8, 10));

		JPanel left = new JPanel(new BorderLayout());
		left.setOpaque(false);
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(offer.getItemId()).addTo(icon);
		left.add(icon, BorderLayout.NORTH);
		JLabel side = Ui.label(offer.isBuy() ? "Buy" : "Sell", offer.isBuy() ? Ui.BUY : Ui.GOOD);
		side.setHorizontalAlignment(SwingConstants.CENTER);
		left.add(side, BorderLayout.SOUTH);
		card.add(left, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
		text.setOpaque(false);
		text.add(Ui.split(Ui.bold(offer.getName(), Color.WHITE), Ui.muted("Slot " + (slot + 1))));
		String marketText = market == null || market.getLow() == null || market.getHigh() == null
			? "no market data"
			: Ui.shortGp(market.getLow()) + " - " + Ui.shortGp(market.getHigh());
		text.add(Ui.split(Ui.label("@ " + Ui.gp(offer.getPrice()), offer.isBuy() ? Ui.BUY : Ui.GOOD), Ui.muted(marketText)));
		text.add(new ProgressBar(offer.getQuantitySold(), offer.getTotalQuantity(), offer.isBuy() ? new Color(0x8A3B3B) : new Color(0x1F7A55),
			Ui.gp(offer.getQuantitySold()) + " / " + Ui.gp(offer.getTotalQuantity())));

		String when;
		if (!offer.getStatus().isPending())
		{
			when = offer.getStatus().getDisplayName();
		}
		else if (offer.getLastFillAt() > 0)
		{
			when = (offer.isBuy() ? "Last buy " : "Last sale ") + Ui.duration(now - offer.getLastFillAt());
		}
		else
		{
			when = "Waiting " + Ui.duration(now - offer.getPlacedAt());
		}
		JLabel verdict = Ui.label(offer.getStatus().isPending() ? health.getDescription() : "Collect it", color(health.getLevel(), offer));
		verdict.setToolTipText(health.getDescription());
		text.add(Ui.split(verdict, Ui.label(when, stale ? Ui.WARN : Ui.MUTED)));
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	private static JPanel holdingCard(FlipLedger.OpenPosition position, LatestPrice market, Set<Integer> taxExempt, long now)
	{
		JPanel card = Ui.card();
		card.add(Ui.split(Ui.bold(position.getName(), Color.WHITE), Ui.muted("x" + Ui.gp(position.getQuantity()))));
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

	private static Color color(OfferHealth.Level level, OfferSnapshot offer)
	{
		if (!offer.getStatus().isPending())
		{
			return Ui.GOOD;
		}
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
