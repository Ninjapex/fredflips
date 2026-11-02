package com.fredflips.ui;

import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.Trade;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Completed flips, your stats and the raw trade log.
 */
class DoneTab extends JPanel
{
	private static final int MAX_COMPLETED_SHOWN = 100;
	private static final int MAX_LOG_SHOWN = 50;

	private final PanelActions actions;
	private final JPanel content = Ui.stack();

	DoneTab(PanelActions actions)
	{
		super(new BorderLayout());
		setOpaque(false);
		this.actions = actions;
		content.add(Ui.note("Your completed flips will show here."));
		add(content, BorderLayout.CENTER);
	}

	void show(FlipLedger ledger, List<Trade> trades, long now)
	{
		content.removeAll();

		content.add(Ui.heading("Completed flips"));
		if (ledger.getCompleted().isEmpty())
		{
			Ui.addSpaced(content, Ui.note("None yet. A flip completes when you sell something you bought while FredFlips was running."));
		}
		int shown = 0;
		for (FlipLedger.CompletedFlip flip : ledger.getCompleted())
		{
			if (shown++ >= MAX_COMPLETED_SHOWN)
			{
				break;
			}
			JPanel card = Ui.card();
			card.add(Ui.row(flip.getName(), Ui.signedShortGp(flip.getProfit()), Ui.profitColor(flip.getProfit())));
			double roi = flip.getCost() == 0 ? 0 : flip.getProfit() * 100.0 / flip.getCost();
			card.add(Ui.row("x" + Ui.gp(flip.getQuantity()) + "  tax " + Ui.shortGp(flip.getTax()),
				String.format("%+.1f%%  %s ago", roi, Ui.duration(now - flip.getTime())), Ui.MUTED));
			Ui.addSpaced(content, card);
		}

		content.add(Ui.heading("Trade log"));
		if (trades.isEmpty())
		{
			Ui.addSpaced(content, Ui.note("Every buy and sell fill is recorded here."));
		}
		for (int i = trades.size() - 1, count = 0; i >= 0 && count < MAX_LOG_SHOWN; i--, count++)
		{
			Trade trade = trades.get(i);
			JPanel line = Ui.card();
			line.add(Ui.row((trade.isBuy() ? "BUY " : "SELL ") + Ui.gp(trade.getQuantity()) + " x " + trade.getName(),
				Ui.duration(now - trade.getTime()) + " ago", trade.isBuy() ? Ui.MUTED : Ui.GOOD));
			line.add(Ui.muted("@ " + Ui.gp(trade.getPriceEach()) + " = " + Ui.gp(trade.getGpTotal()) + " gp"));
			Ui.addSpaced(content, line);
		}

		JButton clear = new JButton("Clear trade log");
		clear.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Delete your whole FredFlips trade log for this account? This can't be undone.",
				"Clear trade log", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.YES_OPTION)
			{
				actions.clearTradeLog();
			}
		});
		Ui.addSpaced(content, clear);

		content.revalidate();
		content.repaint();
	}
}
