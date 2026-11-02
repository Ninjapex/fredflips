package com.fredflips.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * Small shared helpers so every tab looks the same.
 */
final class Ui
{
	static final Color GOOD = new Color(0x00C27A);
	static final Color WARN = new Color(0xFF9F40);
	static final Color BAD = new Color(0xE85050);
	static final Color MUTED = ColorScheme.LIGHT_GRAY_COLOR;
	// Sidebar width minus our padding and the scrollbar.
	private static final int TEXT_WIDTH = 175;

	private Ui()
	{
	}

	static JLabel label(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(color);
		return label;
	}

	static JLabel muted(String text)
	{
		return label(text, MUTED);
	}

	/**
	 * Wrapping text for explanations and empty states.
	 */
	static JLabel note(String text)
	{
		return label(wrap(text), MUTED);
	}

	/**
	 * Wrapping text in the warning colour.
	 */
	static JLabel warning(String text)
	{
		return label(wrap(text), WARN);
	}

	/**
	 * Swing measures an HTML label's height as if it were one long line, so wrapped text gets clipped.
	 * Giving it a fixed width makes it wrap and measure its height correctly.
	 */
	static String wrap(String text)
	{
		return "<html><body style='width:" + TEXT_WIDTH + "px'>" + text + "</body></html>";
	}

	static JLabel heading(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(Color.WHITE);
		label.setBorder(new EmptyBorder(8, 0, 4, 0));
		return label;
	}

	/**
	 * A "Label ........ value" line.
	 */
	static JPanel row(String name, String value, Color valueColor)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.add(muted(name), BorderLayout.WEST);
		row.add(label(value, valueColor), BorderLayout.EAST);
		return row;
	}

	static JPanel row(String name, String value)
	{
		return row(name, value, Color.WHITE);
	}

	/**
	 * A dark card holding rows, used for every list entry and section.
	 */
	static JPanel card()
	{
		JPanel card = new JPanel(new GridLayout(0, 1, 0, 1));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(5, 6, 5, 6));
		return card;
	}

	/**
	 * Stacks components vertically with a small gap, full width.
	 */
	static JPanel stack()
	{
		JPanel stack = new JPanel();
		stack.setLayout(new javax.swing.BoxLayout(stack, javax.swing.BoxLayout.Y_AXIS));
		stack.setOpaque(false);
		return stack;
	}

	static void addSpaced(JPanel stack, JComponent component)
	{
		component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
		stack.add(component);
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
		gap.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		stack.add(gap);
	}

	static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount);
	}

	static String shortGp(long amount)
	{
		return (amount < 0 ? "-" : "") + QuantityFormatter.quantityToStackSize(Math.abs(amount));
	}

	static String signedShortGp(long amount)
	{
		return (amount > 0 ? "+" : "") + shortGp(amount);
	}

	static String percent(Double value)
	{
		return value == null ? "-" : String.format("%+.1f%%", value);
	}

	static Color profitColor(long amount)
	{
		return amount > 0 ? GOOD : amount < 0 ? BAD : Color.WHITE;
	}

	static Color changeColor(Double value)
	{
		return value == null ? Color.WHITE : value > 0 ? GOOD : value < 0 ? BAD : Color.WHITE;
	}

	/**
	 * "45s", "12m", "3h 5m", "2d 4h".
	 */
	static String duration(long millis)
	{
		long minutes = Math.max(0, millis) / 60_000;
		if (minutes < 1)
		{
			return Math.max(0, millis) / 1000 + "s";
		}
		if (minutes < 60)
		{
			return minutes + "m";
		}
		long hours = minutes / 60;
		if (hours < 24)
		{
			return hours + "h " + minutes % 60 + "m";
		}
		return hours / 24 + "d " + hours % 24 + "h";
	}
}
