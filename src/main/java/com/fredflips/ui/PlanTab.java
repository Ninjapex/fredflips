package com.fredflips.ui;

import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.FlipPlan;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.Trade;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;

/**
 * Plan mode: a fixed set of flips that splits your cash across free GE slots, with live
 * "Bought 120 / 700" and "Sold 0 / 700" progress from your real GE fills.
 * The plan only changes when you press Build plan, so it doesn't reshuffle mid-flip.
 */
class PlanTab extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final SearchField cashField = new SearchField("Your GP, e.g. 50m");
	private final JPanel content = Ui.stack();

	PlanTab(ItemManager itemManager, PanelActions actions, long cashStack)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;

		if (cashStack > 0)
		{
			cashField.setText(Ui.shortGp(cashStack).toLowerCase(Locale.ROOT));
		}
		cashField.setToolTipText("How much GP to flip with. Accepts 50m, 1.5b, 200k or a plain number");
		cashField.addActionListener(e -> build());

		JButton build = new JButton("Build plan");
		build.setFont(Ui.SMALL_BOLD);
		build.setForeground(Color.BLACK);
		build.setBackground(Ui.ORANGE);
		build.setFocusPainted(false);
		build.addActionListener(e -> build());

		JPanel cashRow = new JPanel(new BorderLayout(6, 0));
		cashRow.setOpaque(false);
		cashRow.add(cashField, BorderLayout.CENTER);
		cashRow.add(build, BorderLayout.EAST);

		JPanel top = Ui.stack();
		Ui.addSpaced(top, Ui.muted("Cash stack"));
		Ui.addSpaced(top, cashRow);
		Ui.addSpaced(top, Ui.note("Picks the safest profitable flips (tier B+, Stable or Rising) and gives each free"
			+ " GE slot an equal share of your GP."));
		add(top, BorderLayout.NORTH);
		add(content, BorderLayout.CENTER);
	}

	private void build()
	{
		Long cash = parseGp(cashField.getText());
		if (cash != null && cash > 0)
		{
			actions.setCashStack(cash);
		}
		actions.buildPlan();
	}

	/**
	 * "50m", "1.5b", "200k", "12,500,000" -> gp, or null if it isn't a number.
	 */
	static Long parseGp(String text)
	{
		String value = text.trim().toLowerCase(Locale.ROOT).replace(",", "").replace("gp", "").trim();
		if (value.isEmpty())
		{
			return null;
		}
		long multiplier = 1;
		char unit = value.charAt(value.length() - 1);
		if (unit == 'k' || unit == 'm' || unit == 'b')
		{
			multiplier = unit == 'k' ? 1_000L : unit == 'm' ? 1_000_000L : 1_000_000_000L;
			value = value.substring(0, value.length() - 1).trim();
		}
		try
		{
			return Math.round(Double.parseDouble(value) * multiplier);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	void show(FlipPlan plan, String message, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		List<Trade> trades)
	{
		content.removeAll();
		if (message != null)
		{
			Ui.addSpaced(content, Ui.warning(message));
		}
		if (plan != null)
		{
			JPanel summary = Ui.card();
			summary.add(Ui.row("Cash stack", Ui.shortGp(plan.getCapital())));
			summary.add(Ui.row("Planned spend", Ui.shortGp(plan.getCapitalUsed())));
			summary.add(Ui.row("Expected profit", Ui.signedShortGp(plan.getExpectedProfit()), Ui.GOOD));
			summary.add(Ui.row("Slots", plan.getEntries().size() + " of " + plan.getSlots() + " free"));
			Ui.addSpaced(content, summary);

			boolean alternate = false;
			for (FlipPlan.Entry entry : plan.getEntries())
			{
				Ui.addTight(content, entryRow(entry, plan.getCreatedAt(), slots, holdings, trades, alternate));
				alternate = !alternate;
			}
			Ui.addSpaced(content, new JPanel());
			Ui.addSpaced(content, Ui.note("Find on GE fills in the GE search: click it, then Buy on an empty slot."
				+ " The GE price and quantity boxes show this plan's numbers to click."));
			Ui.addSpaced(content, Ui.note("Expected profit assumes every offer fills at the planned prices."));
		}
		content.revalidate();
		content.repaint();
	}

	private JPanel entryRow(FlipPlan.Entry entry, long planCreatedAt, Map<Integer, OfferSnapshot> slots,
		List<FlipLedger.OpenPosition> holdings, List<Trade> trades, boolean alternate)
	{
		FlipCandidate flip = entry.getFlip();
		long planned = entry.getQuantity();
		long bought = 0;
		long sold = 0;
		for (Trade trade : trades)
		{
			if (trade.getItemId() == flip.getItemId() && trade.getTime() >= planCreatedAt)
			{
				if (trade.isBuy())
				{
					bought += trade.getQuantity();
				}
				else
				{
					sold += trade.getQuantity();
				}
			}
		}

		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(flip.getItemId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
		text.setOpaque(false);
		String[] status = status(flip, slots, holdings);
		text.add(Ui.split(Ui.bold(flip.getName(), Color.WHITE), badge(status[2])));
		text.add(Ui.split(Ui.label("<html><b>Buy at:</b>&nbsp; " + Ui.gp(flip.getBuyPrice()) + "</html>", Ui.BUY),
			Ui.label(Ui.gp(planned) + " \u00B7 " + Ui.shortGp(entry.getCost()), Ui.MUTED)));
		text.add(Ui.split(Ui.label("<html><b>Sell at:</b>&nbsp; " + Ui.gp(flip.getSellPrice()) + "</html>", Ui.GOOD),
			Ui.label("+" + Ui.gp(flip.getMargin()) + " ea", Ui.GOOD)));

		// One bar: buying until the full amount is bought, then selling.
		boolean selling = sold > 0 || bought >= planned;
		long done = selling ? sold : bought;
		long total = selling ? Math.max(planned, bought) : planned;
		text.add(new ProgressBar(done, total, PLAN_PURPLE, Ui.gp(Math.min(done, total)) + " / " + Ui.gp(total) + (selling ? " sold" : " bought")));
		text.add(Ui.label(status[0], status[1].equals("done") ? Ui.GOOD : status[1].equals("busy") ? Ui.SIGNAL : Ui.MUTED));

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		JButton swap = new JButton("\u21BB Swap");
		swap.setFont(Ui.SMALL);
		swap.setToolTipText("Replace this item with the next-best flip");
		swap.addActionListener(e -> actions.swapPlanEntry(flip.getItemId()));
		JButton find = new JButton("Find on GE");
		find.setFont(Ui.SMALL);
		find.setToolTipText("Fills in the GE item search: click this, then Buy on an empty GE slot");
		find.addActionListener(e -> actions.findOnGe(flip.getItemId()));
		buttons.add(swap);
		buttons.add(find);
		text.add(buttons);

		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private static final Color PLAN_PURPLE = new Color(0x8E44AD);

	/**
	 * A small coloured label like "Selling", or nothing when the item hasn't been started.
	 */
	private static JLabel badge(String text)
	{
		JLabel badge = new JLabel(text == null ? "" : text);
		if (text != null)
		{
			badge.setFont(Ui.SMALL_BOLD);
			badge.setOpaque(true);
			badge.setBackground(new Color(0x4A3A12));
			badge.setForeground(Ui.ORANGE);
			badge.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
		}
		return badge;
	}

	/**
	 * Where this item is right now: being bought, held, being sold, or not started.
	 */
	private static String[] status(FlipCandidate flip, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings)
	{
		for (Map.Entry<Integer, OfferSnapshot> slot : slots.entrySet())
		{
			OfferSnapshot offer = slot.getValue();
			if (offer.getItemId() != flip.getItemId())
			{
				continue;
			}
			String where = " in slot " + (slot.getKey() + 1);
			switch (offer.getStatus())
			{
				case BUYING:
					return new String[]{"Buying at " + Ui.gp(offer.getPrice()) + where, "busy", "Buying"};
				case BOUGHT:
					return new String[]{"Bought - collect, then sell at " + Ui.gp(flip.getSellPrice()), "busy", "Bought"};
				case SELLING:
					return new String[]{"Selling at " + Ui.gp(offer.getPrice()) + where, "busy", "Selling"};
				case SOLD:
					return new String[]{"Sold - collect your coins", "done", "Done"};
				default:
					break;
			}
		}
		for (FlipLedger.OpenPosition position : holdings)
		{
			if (position.getItemId() == flip.getItemId())
			{
				return new String[]{"Holding " + Ui.gp(position.getQuantity()) + " - sell at " + Ui.gp(flip.getSellPrice()), "busy", "Holding"};
			}
		}
		return new String[]{"Not started - buy at " + Ui.gp(flip.getBuyPrice()), "todo", null};
	}
}
