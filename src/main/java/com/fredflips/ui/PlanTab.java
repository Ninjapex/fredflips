package com.fredflips.ui;

import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.FlipPlan;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Plan mode: a fixed set of flips that splits your cash across free GE slots, with live progress.
 * The plan only changes when you press Build plan, so it doesn't reshuffle mid-flip.
 */
class PlanTab extends JPanel
{
	private final JPanel content = Ui.stack();
	private final PanelActions actions;

	PlanTab(PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.actions = actions;

		JPanel top = Ui.stack();
		Ui.addSpaced(top, Ui.note("Plan mode picks the safest profitable flips (tier B+, Stable or Rising) and gives each"
			+ " free GE slot an equal share of your Cash stack. Press Build plan again any time to start over."));
		JButton build = new JButton("Build plan");
		build.addActionListener(e -> actions.buildPlan());
		Ui.addSpaced(top, build);
		add(top, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	void show(FlipPlan plan, String message, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings)
	{
		content.removeAll();
		if (message != null)
		{
			Ui.addSpaced(content, Ui.warning(message));
		}
		if (plan != null)
		{
			JPanel summary = Ui.card();
			summary.add(Ui.row("Cash stack", Ui.gp(plan.getCapital()) + " gp"));
			summary.add(Ui.row("Planned spend", Ui.gp(plan.getCapitalUsed()) + " gp"));
			summary.add(Ui.row("Expected profit", Ui.gp(plan.getExpectedProfit()) + " gp", Ui.GOOD));
			summary.add(Ui.row("Slots", plan.getEntries().size() + " of " + plan.getSlots() + " free"));
			Ui.addSpaced(content, summary);

			int step = 1;
			for (FlipPlan.Entry entry : plan.getEntries())
			{
				Ui.addSpaced(content, entryCard(step++, entry, slots, holdings));
			}
			Ui.addSpaced(content, Ui.note("Find on GE fills in the GE search for you: click it, then Buy on an empty"
				+ " slot. The GE quantity and price boxes will show this plan's numbers to click."));
			Ui.addSpaced(content, Ui.note("Expected profit assumes every offer fills at the planned prices. It's an"
				+ " estimate: check Active for live progress and whether your prices are still competitive."));
		}
		content.revalidate();
		content.repaint();
	}

	private JPanel entryCard(int step, FlipPlan.Entry entry, Map<Integer, OfferSnapshot> slots,
		List<FlipLedger.OpenPosition> holdings)
	{
		FlipCandidate flip = entry.getFlip();
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(net.runelite.client.ui.ColorScheme.DARKER_GRAY_COLOR);
		JPanel card = Ui.card();
		wrapper.add(card, BorderLayout.CENTER);
		JButton find = new JButton("Find on GE");
		find.setToolTipText("Fills in the GE item search: click this, then Buy on an empty GE slot");
		find.addActionListener(e -> actions.findOnGe(flip.getItemId()));
		wrapper.add(find, BorderLayout.SOUTH);
		card.add(Ui.row(step + ". " + flip.getName(), "Tier " + flip.getTier() + " " + flip.getConfidence() + "%", flip.getTier().getColor()));
		card.add(Ui.row("Buy " + Ui.gp(entry.getQuantity()) + " at", Ui.gp(flip.getBuyPrice())));
		card.add(Ui.row("Sell at", Ui.gp(flip.getSellPrice())));
		card.add(Ui.row("Cost " + Ui.shortGp(entry.getCost()), "Profit " + Ui.signedShortGp(entry.getExpectedProfit()), Ui.GOOD));
		String[] progress = progress(flip, slots, holdings);
		card.add(Ui.label(progress[0], progress[1].equals("done") ? Ui.GOOD : progress[1].equals("busy") ? Color.WHITE : Ui.MUTED));
		return wrapper;
	}

	/**
	 * Where this item is right now: being bought, held, being sold, or not started.
	 */
	private static String[] progress(FlipCandidate flip, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings)
	{
		for (OfferSnapshot offer : slots.values())
		{
			if (offer.getItemId() != flip.getItemId())
			{
				continue;
			}
			String filled = Ui.gp(offer.getQuantitySold()) + "/" + Ui.gp(offer.getTotalQuantity());
			switch (offer.getStatus())
			{
				case BUYING:
					return new String[]{"Buying " + filled + " at " + Ui.gp(offer.getPrice()), "busy"};
				case BOUGHT:
					return new String[]{"Bought - collect, then sell at " + Ui.gp(flip.getSellPrice()), "busy"};
				case SELLING:
					return new String[]{"Selling " + filled + " at " + Ui.gp(offer.getPrice()), "busy"};
				case SOLD:
					return new String[]{"Sold - collect your coins", "done"};
				default:
					break;
			}
		}
		for (FlipLedger.OpenPosition position : holdings)
		{
			if (position.getItemId() == flip.getItemId())
			{
				return new String[]{"Holding " + Ui.gp(position.getQuantity()) + " - sell at " + Ui.gp(flip.getSellPrice()), "busy"};
			}
		}
		return new String[]{"Not started", "todo"};
	}
}
