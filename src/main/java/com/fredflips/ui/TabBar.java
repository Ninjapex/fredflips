package com.fredflips.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Tab buttons in rows of three, so six tabs fit the narrow sidebar.
 */
class TabBar extends JPanel
{
	private static final int COLUMNS = 3;

	private final Map<String, JLabel> tabs = new LinkedHashMap<>();
	private final Map<String, Boolean> visible = new LinkedHashMap<>();
	private final Consumer<String> onSelect;
	private String selected;

	TabBar(Consumer<String> onSelect)
	{
		super(new GridLayout(0, COLUMNS, 2, 2));
		this.onSelect = onSelect;
	}

	void addTab(String name)
	{
		JLabel tab = new JLabel(name, SwingConstants.CENTER);
		tab.setFont(FontManager.getRunescapeSmallFont());
		tab.setOpaque(true);
		tab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tab.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				select(name);
			}
		});
		tabs.put(name, tab);
		visible.put(name, true);
		rebuild();
	}

	void setTabVisible(String name, boolean show)
	{
		visible.put(name, show);
		rebuild();
		if (!show && name.equals(selected))
		{
			select(tabs.keySet().iterator().next());
		}
	}

	void select(String name)
	{
		selected = name;
		for (Map.Entry<String, JLabel> entry : tabs.entrySet())
		{
			style(entry.getValue(), entry.getKey().equals(name));
		}
		onSelect.accept(name);
	}

	String getSelected()
	{
		return selected;
	}

	// GridLayout leaves gaps for hidden components, so only visible tabs are added.
	private void rebuild()
	{
		removeAll();
		for (Map.Entry<String, JLabel> entry : tabs.entrySet())
		{
			if (visible.get(entry.getKey()))
			{
				add(entry.getValue());
				style(entry.getValue(), entry.getKey().equals(selected));
			}
		}
		revalidate();
		repaint();
	}

	private static void style(JLabel tab, boolean active)
	{
		tab.setBackground(active ? ColorScheme.DARK_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
		tab.setForeground(active ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
		tab.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 2, 0, active ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR),
			BorderFactory.createEmptyBorder(5, 2, 5, 2)));
	}
}
