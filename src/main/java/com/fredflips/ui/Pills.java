package com.fredflips.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A row of small rounded toggle buttons where one is selected, e.g. Active / Recent / Margin.
 */
class Pills extends JPanel
{
	private final List<JLabel> pills = new ArrayList<>();
	private final Consumer<Integer> onSelect;
	private int selected;

	Pills(String[] names, int initial, Consumer<Integer> onSelect)
	{
		super(new FlowLayout(FlowLayout.LEFT, 4, 0));
		setOpaque(false);
		this.onSelect = onSelect;
		this.selected = initial;
		for (int i = 0; i < names.length; i++)
		{
			int index = i;
			JLabel pill = new JLabel(names[i]);
			pill.setFont(Ui.SMALL_BOLD);
			pill.setOpaque(true);
			pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			pill.setBorder(BorderFactory.createEmptyBorder(3, 9, 3, 9));
			pill.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					select(index);
				}
			});
			pills.add(pill);
			add(pill);
		}
		restyle();
	}

	int getSelected()
	{
		return selected;
	}

	void select(int index)
	{
		selected = index;
		restyle();
		onSelect.accept(index);
	}

	private void restyle()
	{
		for (int i = 0; i < pills.size(); i++)
		{
			boolean active = i == selected;
			pills.get(i).setBackground(active ? Ui.ORANGE : Ui.HOVER_BG);
			pills.get(i).setForeground(active ? Color.BLACK : new Color(0xAAAAAA));
		}
	}
}
