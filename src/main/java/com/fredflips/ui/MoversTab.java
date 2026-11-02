package com.fredflips.ui;

import com.fredflips.flip.Mover;
import com.fredflips.flip.MoverType;
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
 * Dumps, spikes and high alch lists, shown inside the Other tab.
 */
class MoversTab extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JLabel description = Ui.note(MoverType.DUMPS.getDescription());
	private final JPanel list = Ui.stack();
	private MoverType type = MoverType.DUMPS;

	MoversTab(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		add(description, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
	}

	MoverType getType()
	{
		return type;
	}

	void setType(MoverType newType)
	{
		type = newType;
		description.setText(Ui.wrap(newType.getDescription()));
		list.removeAll();
		list.add(Ui.note("Loading..."));
		list.revalidate();
		actions.changeMoverType(newType);
	}

	void showMovers(List<Mover> movers, MoverType forType)
	{
		if (forType != type)
		{
			return;
		}
		list.removeAll();
		if (movers.isEmpty())
		{
			list.add(Ui.note("Nothing right now. Prices refresh every minute."));
		}
		boolean alternate = false;
		for (Mover mover : movers)
		{
			Ui.addTight(list, row(mover, alternate));
			alternate = !alternate;
		}
		list.revalidate();
		list.repaint();
	}

	private JPanel row(Mover mover, boolean alternate)
	{
		Color background = alternate ? Ui.ROW_ALT_BG : Ui.ROW_BG;
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(background);
		row.setBorder(new EmptyBorder(8, 10, 8, 10));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(32, 32));
		icon.setVerticalAlignment(SwingConstants.TOP);
		itemManager.getImage(mover.getItemId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
		text.setOpaque(false);
		if (type == MoverType.HIGH_ALCH)
		{
			text.add(Ui.split(Ui.bold(mover.getName(), Color.WHITE), Ui.bold("+" + Ui.shortGp(mover.getProfit()), Ui.GOOD)));
			text.add(Ui.split(Ui.label("<html><b>Buy:</b>&nbsp; " + Ui.shortGp(mover.getPrice()) + "</html>", Ui.BUY), null));
			text.add(Ui.split(Ui.label("<html><b>Alch:</b>&nbsp; " + Ui.shortGp(mover.getReferencePrice()) + "</html>", Ui.GOOD), null));
			text.add(Ui.split(Ui.muted("Limit " + (mover.getBuyLimit() > 0 ? Ui.gp(mover.getBuyLimit()) : "?")),
				Ui.muted(Ui.shortGp(mover.getVolumePerHour()) + "/hr")));
		}
		else if (type == MoverType.VOLATILE)
		{
			text.add(Ui.split(Ui.bold(mover.getName(), Color.WHITE), Ui.bold(String.format("%.1f%% gap", mover.getChangePercent()), Ui.WARN)));
			text.add(Ui.split(Ui.label("<html><b>Buy:</b>&nbsp; " + Ui.shortGp(mover.getPrice()) + "</html>", Ui.BUY), null));
			text.add(Ui.split(Ui.label("<html><b>Sell:</b>&nbsp; " + Ui.shortGp(mover.getReferencePrice()) + "</html>", Ui.GOOD),
				Ui.muted(Ui.shortGp(mover.getVolumePerHour()) + "/hr")));
		}
		else
		{
			text.add(Ui.split(Ui.bold(mover.getName(), Color.WHITE),
				Ui.bold(Ui.percent(mover.getChangePercent()), Ui.changeColor(mover.getChangePercent()))));
			text.add(Ui.split(Ui.label("<html><b>" + (type == MoverType.DIPS ? "Last hour" : "Now") + ":</b>&nbsp; "
				+ Ui.shortGp(mover.getPrice()) + "</html>", type == MoverType.SPIKES ? Ui.GOOD : Ui.BUY), null));
			text.add(Ui.split(Ui.label("Day average " + Ui.shortGp(mover.getReferencePrice()), Ui.MUTED),
				Ui.muted(Ui.shortGp(mover.getVolumePerHour()) + "/hr")));
		}
		row.add(text, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				actions.openItem(mover.getItemId());
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
