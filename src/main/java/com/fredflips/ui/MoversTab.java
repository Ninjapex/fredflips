package com.fredflips.ui;

import com.fredflips.flip.Mover;
import com.fredflips.flip.MoverType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Dumps, spikes and high alch profits.
 */
class MoversTab extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JComboBox<MoverType> typeBox = new JComboBox<>(MoverType.values());
	private final JLabel description = Ui.note(MoverType.DUMPS.getDescription());
	private final JPanel list = Ui.stack();

	MoversTab(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;

		typeBox.addActionListener(e ->
		{
			MoverType type = (MoverType) typeBox.getSelectedItem();
			description.setText(Ui.wrap(type.getDescription()));
			actions.changeMoverType(type);
		});

		JPanel top = Ui.stack();
		Ui.addSpaced(top, typeBox);
		Ui.addSpaced(top, description);
		add(top, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
	}

	MoverType getType()
	{
		return (MoverType) typeBox.getSelectedItem();
	}

	void showMovers(List<Mover> movers, MoverType type)
	{
		list.removeAll();
		if (movers.isEmpty())
		{
			list.add(Ui.note("Nothing right now. Prices refresh every minute."));
		}
		for (Mover mover : movers)
		{
			Ui.addSpaced(list, row(mover, type));
		}
		list.revalidate();
		list.repaint();
	}

	private JPanel row(Mover mover, MoverType type)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		itemManager.getImage(mover.getItemId()).addTo(icon);
		row.add(icon, BorderLayout.WEST);

		JPanel text = Ui.card();
		text.setOpaque(false);
		text.setBorder(null);

		JPanel title = new JPanel(new BorderLayout());
		title.setOpaque(false);
		title.add(Ui.label(mover.getName(), Color.WHITE), BorderLayout.CENTER);
		if (type == MoverType.HIGH_ALCH)
		{
			title.add(Ui.label("+" + Ui.gp(mover.getProfit()) + " ea", Ui.GOOD), BorderLayout.EAST);
			text.add(title);
			text.add(Ui.muted("Buy " + Ui.gp(mover.getPrice()) + "  Alch " + Ui.gp(mover.getReferencePrice())));
			text.add(Ui.muted("Limit " + (mover.getBuyLimit() > 0 ? Ui.gp(mover.getBuyLimit()) : "?")
				+ "  Vol " + Ui.shortGp(mover.getVolumePerHour()) + "/h"));
		}
		else
		{
			title.add(Ui.label(Ui.percent(mover.getChangePercent()), Ui.changeColor(mover.getChangePercent())), BorderLayout.EAST);
			text.add(title);
			text.add(Ui.muted("Now " + Ui.gp(mover.getPrice()) + "  Day avg " + Ui.gp(mover.getReferencePrice())));
			text.add(Ui.muted("Vol " + Ui.shortGp(mover.getVolumePerHour()) + "/h"));
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
				row.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		return row;
	}
}
