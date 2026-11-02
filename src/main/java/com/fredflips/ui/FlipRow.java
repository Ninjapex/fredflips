package com.fredflips.ui;

import com.fredflips.flip.FlipCandidate;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;

/**
 * One flip in the list:
 * <pre>
 * [icon] Name                     84
 *        Buy:  2.77B              now
 *        Sell: 2.79B               3m
 *        +16.57M                 ~19m
 *        Tier A - Stable
 * </pre>
 * Click it to open the item in the Item tab.
 */
class FlipRow extends JPanel
{
	private static final long MINUTES_PER_HOUR = 60;

	FlipRow(FlipCandidate flip, ItemManager itemManager, PanelActions actions, boolean alternate)
	{
		Color background = alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG;
		setLayout(new BorderLayout(8, 0));
		setBorder(new EmptyBorder(8, 10, 8, 10));
		setBackground(background);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(tooltip(flip));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(flip.getItemId()).addTo(icon);
		add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
		text.setOpaque(false);

		JLabel name = Ui.bold(flip.getName(), Color.WHITE);
		JLabel score = Ui.bold(String.valueOf(flip.getConfidence()), Ui.scoreColor(flip.getConfidence()));
		score.setToolTipText("Confidence 0-100 (tier " + flip.getTier() + ")");
		text.add(Ui.split(name, score));

		text.add(Ui.split(
			Ui.label("<html><b>Buy:</b>&nbsp; " + Ui.shortGp(flip.getBuyPrice()) + "</html>", Ui.BUY),
			Ui.label(Ui.age(flip.getBuyPriceAgeMinutes()), Ui.MUTED)));
		text.add(Ui.split(
			Ui.label("<html><b>Sell:</b>&nbsp; " + Ui.shortGp(flip.getSellPrice()) + "</html>", Ui.GOOD),
			Ui.label(Ui.age(flip.getSellPriceAgeMinutes()), Ui.MUTED)));

		JLabel eta = Ui.label("~" + Ui.age(estimatedMinutes(flip)), Ui.MUTED);
		eta.setToolTipText("Rough time to buy and sell the suggested quantity, from the last hour's volume");
		text.add(Ui.split(Ui.label(Ui.signedShortGp(flip.getPotentialProfit()), Ui.profitColor(flip.getPotentialProfit())), eta));

		JLabel signal = Ui.label("Tier " + flip.getTier() + " - " + flip.getSignal().getDisplayName(), Ui.SIGNAL);
		signal.setForeground(flip.getSignal().getColor());
		text.add(signal);

		add(text, BorderLayout.CENTER);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openItem(flip.getItemId());
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				setBackground(Ui.HOVER_BG);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(background);
			}
		});
	}

	/**
	 * Time to buy the suggested quantity (filled by sellers) plus time to sell it (filled by buyers).
	 */
	static long estimatedMinutes(FlipCandidate flip)
	{
		long quantity = Math.max(1, flip.getQuantity());
		long buy = quantity * MINUTES_PER_HOUR / Math.max(1, flip.getSellersPerHour());
		long sell = quantity * MINUTES_PER_HOUR / Math.max(1, flip.getBuyersPerHour());
		return buy + sell;
	}

	private static String tooltip(FlipCandidate flip)
	{
		return "<html>"
			+ "<b>" + flip.getName() + "</b><br>"
			+ "Buy at: <font color='#FF7070'>" + Ui.gp(flip.getBuyPrice()) + " gp</font> (traded " + Ui.age(flip.getBuyPriceAgeMinutes()) + ")<br>"
			+ "Sell at: <font color='#00C27A'>" + Ui.gp(flip.getSellPrice()) + " gp</font> (traded " + Ui.age(flip.getSellPriceAgeMinutes()) + ")<br>"
			+ "GE tax: " + Ui.gp(flip.getTax()) + " gp each<br>"
			+ "Margin after tax: " + Ui.gp(flip.getMargin()) + " gp each (" + String.format("%.2f", flip.getRoiPercent()) + "%)<br>"
			+ "Buy limit: " + (flip.getBuyLimit() > 0 ? Ui.gp(flip.getBuyLimit()) + " per 4h" : "unknown") + "<br>"
			+ "Sellers last hour: " + Ui.gp(flip.getSellersPerHour())
			+ " &nbsp;Buyers last hour: " + Ui.gp(flip.getBuyersPerHour()) + "<br>"
			+ "Suggested qty: " + Ui.gp(flip.getQuantity())
			+ " = " + Ui.gp(flip.getPotentialProfit()) + " gp profit<br>"
			+ "Confidence: " + flip.getConfidence() + " (tier " + flip.getTier() + ")<br>"
			+ "Signal: " + flip.getSignal().getDescription() + "<br>"
			+ "<font color='#666666'>Click for chart, indicators and volume</font>"
			+ "</html>";
	}
}
