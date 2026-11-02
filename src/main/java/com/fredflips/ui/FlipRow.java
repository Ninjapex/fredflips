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
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/**
 * One flip in the list. Click it to open the item in the Item tab.
 */
class FlipRow extends JPanel
{
	FlipRow(FlipCandidate flip, ItemManager itemManager, PanelActions actions)
	{
		setLayout(new BorderLayout(6, 0));
		setBorder(new EmptyBorder(5, 5, 5, 5));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(tooltip(flip));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		itemManager.getImage(flip.getItemId()).addTo(icon);
		add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(4, 1));
		text.setOpaque(false);

		JPanel titleLine = new JPanel(new BorderLayout());
		titleLine.setOpaque(false);
		titleLine.add(Ui.label(flip.getName(), Color.WHITE), BorderLayout.CENTER);
		titleLine.add(Ui.label("+" + Ui.shortGp(flip.getPotentialProfit()), Ui.GOOD), BorderLayout.EAST);
		text.add(titleLine);

		text.add(Ui.muted("Buy " + Ui.gp(flip.getBuyPrice()) + "  Sell " + Ui.gp(flip.getSellPrice())));
		text.add(Ui.muted(String.format("Margin %s (%.1f%%)  x%s",
			Ui.gp(flip.getMargin()), flip.getRoiPercent(), Ui.gp(flip.getQuantity()))));

		JPanel scoreLine = new JPanel(new BorderLayout());
		scoreLine.setOpaque(false);
		scoreLine.add(Ui.label("Tier " + flip.getTier() + " - " + flip.getConfidence() + "% conf.",
			flip.getTier().getColor()), BorderLayout.WEST);
		scoreLine.add(Ui.label(flip.getSignal().getDisplayName(), flip.getSignal().getColor()), BorderLayout.EAST);
		text.add(scoreLine);

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
				setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
	}

	private static String tooltip(FlipCandidate flip)
	{
		return "<html>"
			+ "<b>" + flip.getName() + "</b><br>"
			+ "Buy at: " + Ui.gp(flip.getBuyPrice()) + " gp (last trade " + flip.getBuyPriceAgeMinutes() + "m ago)<br>"
			+ "Sell at: " + Ui.gp(flip.getSellPrice()) + " gp (last trade " + flip.getSellPriceAgeMinutes() + "m ago)<br>"
			+ "GE tax: " + Ui.gp(flip.getTax()) + " gp each<br>"
			+ "Margin after tax: " + Ui.gp(flip.getMargin()) + " gp each<br>"
			+ "Buy limit: " + (flip.getBuyLimit() > 0 ? Ui.gp(flip.getBuyLimit()) + " per 4h" : "unknown") + "<br>"
			+ "Sellers last hour: " + Ui.gp(flip.getSellersPerHour())
			+ " &nbsp;Buyers last hour: " + Ui.gp(flip.getBuyersPerHour()) + "<br>"
			+ "Suggested qty: " + Ui.gp(flip.getQuantity())
			+ " = " + Ui.gp(flip.getPotentialProfit()) + " gp profit<br>"
			+ "Signal: " + flip.getSignal().getDescription() + "<br>"
			+ "<i>Click for chart, indicators and volume</i>"
			+ "</html>";
	}
}
