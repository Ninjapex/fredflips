package com.fredflips.ui;

import com.fredflips.flip.ItemSets;
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
 * Item set combine/split opportunities, shown inside the Other tab.
 */
class ItemSetView extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JPanel list = Ui.stack();

	ItemSetView(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		add(Ui.note("Combine: buy the pieces, make the set at a GE clerk (free), sell the set. Split: the other way"
			+ " round. Profit is after tax."), BorderLayout.NORTH);
		list.add(Ui.note("Loading..."));
		add(list, BorderLayout.CENTER);
	}

	void show(List<ItemSets.Option> options)
	{
		list.removeAll();
		if (options.isEmpty())
		{
			list.add(Ui.note("No profitable sets right now."));
		}
		boolean alternate = false;
		for (ItemSets.Option option : options)
		{
			Ui.addTight(list, row(option, alternate));
			alternate = !alternate;
		}
		list.revalidate();
		list.repaint();
	}

	private JPanel row(ItemSets.Option option, boolean alternate)
	{
		Color background = alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG;
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(background);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(option.getSetId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		text.add(Ui.split(Ui.bold(option.getSetName(), Color.WHITE), Ui.bold(String.format("%.1f%%", option.getRoiPercent()), Ui.GOOD)));
		String buyWhat = option.isCombine() ? option.getPieceCount() + " pieces" : "the set";
		String sellWhat = option.isCombine() ? "the set" : "the pieces";
		text.add(Ui.split(Ui.label("<html><b>Buy " + buyWhat + ":</b>&nbsp; " + Ui.shortGp(option.getCost()) + "</html>", Ui.BUY), null));
		text.add(Ui.split(Ui.label("<html><b>Sell " + sellWhat + ":</b>&nbsp; " + Ui.shortGp(option.getRevenue()) + "</html>", Ui.GOOD), null));
		text.add(Ui.split(Ui.label(option.isCombine() ? "Combine" : "Split", Ui.SIGNAL), Ui.label(Ui.signedShortGp(option.getProfit()), Ui.GOOD)));
		row.add(text, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openItem(option.getSetId());
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
