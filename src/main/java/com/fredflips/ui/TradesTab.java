package com.fredflips.ui;

import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.Trade;
import com.fredflips.wiki.LatestPrice;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Your trading: a profit and performance summary for the chosen period, then either your live GE slots (Active),
 * completed flips (Recent) or profit added up by item (Margin).
 */
class TradesTab extends JPanel
{
	private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

	@Getter
	@RequiredArgsConstructor
	enum Period
	{
		TODAY("Today"),
		WEEK("Week"),
		MONTH("Month"),
		ALL("All time");

		private final String displayName;

		long startMillis(long now)
		{
			switch (this)
			{
				case TODAY:
					return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
				case WEEK:
					return now - 7 * DAY_MILLIS;
				case MONTH:
					return now - 30 * DAY_MILLIS;
				default:
					return 0;
			}
		}

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	private final ActiveTab active;
	private final DoneTab recent;
	private final JPanel margin = Ui.stack();
	private final JPanel stats = Ui.stack();
	private final JPanel holder = new JPanel(new BorderLayout());
	private final JComboBox<Period> periodBox = new JComboBox<>(Period.values());
	private FlipLedger ledger;

	TradesTab(ActiveTab active, DoneTab recent)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.active = active;
		this.recent = recent;
		holder.setOpaque(false);
		margin.add(Ui.note("Profit by item will show here once you complete a flip."));

		periodBox.setFont(Ui.SMALL);
		periodBox.addActionListener(e -> renderStats());
		Pills views = new Pills(new String[]{"Active", "Recent", "Margin"}, 0, this::showView);

		JPanel top = Ui.stack();
		Ui.addSpaced(top, Ui.split(views, periodBox));
		top.add(stats);
		add(top, BorderLayout.NORTH);
		add(holder, BorderLayout.CENTER);
		showView(0);
	}

	private void showView(int index)
	{
		JComponent content = index == 0 ? active : index == 1 ? recent : margin;
		holder.removeAll();
		holder.add(content, BorderLayout.CENTER);
		holder.revalidate();
		holder.repaint();
	}

	void showActive(Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		Map<Integer, LatestPrice> market, Set<Integer> taxExempt, int staleMinutes, boolean tracking, long now)
	{
		active.show(slots, holdings, market, taxExempt, staleMinutes, tracking, now);
	}

	void showDone(FlipLedger ledger, List<Trade> trades, long now)
	{
		this.ledger = ledger;
		recent.show(ledger, trades, now);
		renderStats();
		showMargins(ledger);
	}

	private void renderStats()
	{
		stats.removeAll();
		if (ledger == null)
		{
			return;
		}
		Period period = (Period) periodBox.getSelectedItem();
		FlipLedger.Stats s = ledger.statsSince(period.startMillis(System.currentTimeMillis()));

		JPanel profit = Ui.card();
		profit.add(Ui.label("Profit · " + period.getDisplayName(), Ui.ORANGE));
		profit.add(Ui.row("Total", Ui.signedShortGp(s.getTotalProfit()) + " gp", Ui.profitColor(s.getTotalProfit())));
		profit.add(Ui.row("Best", s.getBest() == null ? "-"
			: s.getBest().getName() + " " + Ui.signedShortGp(s.getBest().getProfit()), s.getBest() == null ? Color.WHITE : Ui.GOOD));
		if (s.getWorst() != null && s.getWorst().getProfit() < 0)
		{
			profit.add(Ui.row("Worst", s.getWorst().getName() + " " + Ui.signedShortGp(s.getWorst().getProfit()), Ui.BAD));
		}
		profit.add(Ui.label("Performance", Ui.ORANGE));
		profit.add(Ui.row("Flips", String.valueOf(s.getFlips())));
		profit.add(Ui.row("Win rate", s.getWinRatePercent() + "% (" + s.getWins() + "W / " + s.getLosses() + "L)"));
		profit.add(Ui.row("Avg ROI", String.format("%+.1f%%", s.getAverageRoiPercent()), Ui.profitColor(s.getTotalProfit())));
		profit.add(Ui.row("GE tax paid", Ui.gp(s.getTaxPaid()) + " gp"));
		profit.add(Ui.row("Invested", Ui.gp(s.getInvested()) + " gp"));
		Ui.addSpaced(stats, profit);
		stats.revalidate();
		stats.repaint();
	}

	/**
	 * Completed flips added up per item, best total profit first.
	 */
	private void showMargins(FlipLedger ledger)
	{
		Map<Integer, long[]> totals = new LinkedHashMap<>();
		Map<Integer, String> names = new LinkedHashMap<>();
		for (FlipLedger.CompletedFlip flip : ledger.getCompleted())
		{
			long[] sum = totals.computeIfAbsent(flip.getItemId(), id -> new long[3]);
			sum[0] += flip.getProfit();
			sum[1] += flip.getCost();
			sum[2] += flip.getQuantity();
			names.put(flip.getItemId(), flip.getName());
		}
		List<Map.Entry<Integer, long[]>> rows = new ArrayList<>(totals.entrySet());
		rows.sort(Comparator.comparingLong((Map.Entry<Integer, long[]> e) -> e.getValue()[0]).reversed());

		margin.removeAll();
		if (rows.isEmpty())
		{
			margin.add(Ui.note("Profit by item will show here once you complete a flip."));
		}
		for (Map.Entry<Integer, long[]> row : rows)
		{
			long[] sum = row.getValue();
			double roi = sum[1] == 0 ? 0 : sum[0] * 100.0 / sum[1];
			JPanel card = Ui.card();
			card.add(Ui.split(Ui.bold(names.get(row.getKey()), Color.WHITE), Ui.bold(Ui.signedShortGp(sum[0]), Ui.profitColor(sum[0]))));
			card.add(Ui.split(Ui.muted("x" + Ui.gp(sum[2]) + " flipped"), Ui.label(String.format("%+.1f%% ROI", roi), Ui.profitColor(sum[0]))));
			Ui.addSpaced(margin, card);
		}
		margin.revalidate();
		margin.repaint();
	}
}
