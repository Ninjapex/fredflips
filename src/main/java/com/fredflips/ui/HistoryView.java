package com.fredflips.ui;

import com.fredflips.flip.HistoryScanner;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;

/**
 * All-time lows and time-of-day arbitrage, shown inside the Other tab. Both need price history,
 * so they cover the most-traded items and take a few seconds to load the first time.
 */
class HistoryView extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JLabel description = Ui.note("");
	private final JPanel list = Ui.stack();
	private boolean arbitrage;

	HistoryView(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		add(description, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
	}

	void load(boolean arbitrage)
	{
		this.arbitrage = arbitrage;
		description.setText(Ui.wrap(arbitrage
			? "Items that are reliably cheaper at one time of day than another, from the last two weeks of hourly"
			+ " prices (your local time). Hit rate is how many days the pattern paid after tax."
			: "Items trading within 3% of their lowest price in the past year. Cheap now, but low prices can go lower."));
		list.removeAll();
		list.add(Ui.note("Loading price history for the most-traded items..."));
		list.revalidate();
		list.repaint();
		actions.loadHistory(arbitrage);
	}

	void show(List<HistoryScanner.Result> results, boolean forArbitrage)
	{
		if (forArbitrage != arbitrage)
		{
			return;
		}
		list.removeAll();
		if (results.isEmpty())
		{
			list.add(Ui.note(arbitrage ? "No reliable time-of-day patterns right now." : "Nothing at an all-time low right now."));
		}
		boolean alternate = false;
		for (HistoryScanner.Result result : results)
		{
			Ui.addTight(list, row(result, alternate));
			alternate = !alternate;
		}
		list.revalidate();
		list.repaint();
	}

	private JPanel row(HistoryScanner.Result result, boolean alternate)
	{
		Color background = alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG;
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(background);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(result.getItemId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		if (arbitrage)
		{
			text.add(Ui.split(Ui.bold(result.getName(), Color.WHITE),
				Ui.bold(Math.round(result.getPercent()) + "% hit", Ui.scoreColor((int) Math.round(result.getPercent())))));
			text.add(Ui.split(Ui.label("<html><b>Buy:</b>&nbsp; " + Ui.shortGp(result.getBuyPrice()) + "</html>", Ui.BUY), null));
			text.add(Ui.split(Ui.label("<html><b>Sell:</b>&nbsp; " + Ui.shortGp(result.getSellPrice()) + "</html>", Ui.GOOD),
				Ui.label("+" + Ui.shortGp(result.getProfitEach()) + " ea", Ui.GOOD)));
			String nextDay = result.getSellHour() < result.getBuyHour() ? " next day" : "";
			text.add(Ui.label(String.format("Buy %02d:00 → sell %02d:00%s", result.getBuyHour(), result.getSellHour(), nextDay), Ui.SIGNAL));
		}
		else
		{
			String vsLow = result.getPercent() <= 0 ? "New low" : String.format("+%.1f%% vs low", result.getPercent());
			text.add(Ui.split(Ui.bold(result.getName(), Color.WHITE), Ui.bold(vsLow, Ui.GOOD)));
			text.add(Ui.split(Ui.label("<html><b>Buy:</b>&nbsp; " + Ui.shortGp(result.getBuyPrice()) + "</html>", Ui.BUY), null));
			text.add(Ui.split(Ui.label("<html><b>Sell:</b>&nbsp; " + Ui.shortGp(result.getSellPrice()) + "</html>", Ui.GOOD), null));
			text.add(Ui.label("Year: " + Ui.shortGp(result.getYearLow()) + " - " + Ui.shortGp(result.getYearHigh()), Ui.MUTED));
		}
		row.add(text, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openItem(result.getItemId());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(Ui.HOVER_BG);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(background);
			}
		});
		return row;
	}
}
