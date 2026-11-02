package com.fredflips.ui;

import com.fredflips.flip.DecantScanner;
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
 * Potion decanting opportunities, shown inside the Other tab.
 */
class DecantView extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JPanel list = Ui.stack();

	DecantView(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		add(Ui.note("Buy the dose size that's cheapest per dose, decant it for free with Bob Barter at the GE,"
			+ " then sell the dose size worth most per dose. Profit is after tax."), BorderLayout.NORTH);
		list.add(Ui.note("Loading..."));
		add(list, BorderLayout.CENTER);
	}

	void show(List<DecantScanner.Option> options)
	{
		list.removeAll();
		if (options.isEmpty())
		{
			list.add(Ui.note("No profitable decants right now."));
		}
		boolean alternate = false;
		for (DecantScanner.Option option : options)
		{
			Ui.addTight(list, row(option, alternate));
			alternate = !alternate;
		}
		list.revalidate();
		list.repaint();
	}

	private JPanel row(DecantScanner.Option option, boolean alternate)
	{
		Color background = alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG;
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(background);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(option.getSellItemId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		text.add(Ui.split(Ui.bold(option.getPotion(), Color.WHITE), Ui.bold(String.format("%.1f%%", option.getRoiPercent()), Ui.GOOD)));
		text.add(Ui.split(Ui.label("<html><b>Buy (" + option.getBuyDoses() + "):</b>&nbsp; " + Ui.gp(option.getBuyPrice()) + "</html>", Ui.BUY), null));
		text.add(Ui.split(Ui.label("<html><b>Sell (" + option.getSellDoses() + "):</b>&nbsp; " + Ui.gp(option.getSellPrice()) + "</html>", Ui.GOOD),
			Ui.muted(Ui.shortGp(option.getVolumePerHour()) + "/hr")));
		text.add(Ui.label("+" + Ui.gp(option.getProfitPerPotion()) + " per (" + option.getSellDoses() + ") sold", Ui.GOOD));
		row.add(text, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openItem(option.getSellItemId());
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
