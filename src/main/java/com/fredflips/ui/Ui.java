package com.fredflips.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.QuantityFormatter;

/**
 * Shared look for every tab: clean Dialog fonts, a dark palette with green/red money colours, and helpers.
 */
final class Ui
{
	static final Font TITLE = new Font("Dialog", Font.BOLD, 14);
	static final Font BOLD = new Font("Dialog", Font.BOLD, 12);
	static final Font SMALL = new Font("Dialog", Font.PLAIN, 11);
	static final Font SMALL_BOLD = new Font("Dialog", Font.BOLD, 11);

	static final Color GOOD = new Color(0x00C27A);
	static final Color BUY = new Color(0xFF7070);
	static final Color BAD = new Color(0xE85050);
	static final Color WARN = new Color(0xE8A838);
	static final Color ORANGE = new Color(0xFF981F);
	static final Color SIGNAL = new Color(0xFFE07A);
	static final Color MUTED = new Color(0x888888);
	static final Color FAINT = new Color(0x666666);

	static final Color ROW_BG = ColorScheme.DARK_GRAY_COLOR;
	static final Color ROW_ALT_BG = new Color(0x272727);
	static final Color HOVER_BG = new Color(0x3A3A3A);
	static final Color FIELD_BG = new Color(0x1E1E1E);

	// Sidebar width minus our padding and the scrollbar.
	private static final int TEXT_WIDTH = 175;

	private Ui()
	{
	}

	static JLabel label(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(SMALL);
		label.setForeground(color);
		return label;
	}

	static JLabel bold(String text, Color color)
	{
		JLabel label = new JLabel(text);
		label.setFont(BOLD);
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
		label.setFont(BOLD);
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
	 * Left and right labels on one line, e.g. "Buy: 2.77B ........ 3m".
	 */
	static JPanel split(JComponent left, JComponent right)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		row.add(left, BorderLayout.CENTER);
		if (right != null)
		{
			row.add(right, BorderLayout.EAST);
		}
		return row;
	}

	/**
	 * A dark card holding rows, used for sections and simple list entries.
	 */
	static JPanel card()
	{
		JPanel card = new JPanel(new GridLayout(0, 1, 0, 2));
		card.setBackground(ROW_ALT_BG);
		card.setBorder(new EmptyBorder(8, 10, 8, 10));
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
		addTight(stack, component);
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
		gap.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		stack.add(gap);
	}

	/**
	 * Full width, no gap after - for list rows that sit flush like a table.
	 */
	static void addTight(JPanel stack, JComponent component)
	{
		component.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
		stack.add(component);
	}

	static String gp(long amount)
	{
		return QuantityFormatter.formatNumber(amount);
	}

	/**
	 * Compact gp: 950, 106.1K, 4.7M, 2.77B.
	 */
	static String shortGp(long amount)
	{
		long abs = Math.abs(amount);
		String sign = amount < 0 ? "-" : "";
		if (abs < 1_000)
		{
			return sign + abs;
		}
		if (abs < 1_000_000)
		{
			return sign + trim(String.format("%.1f", abs / 1_000.0)) + "K";
		}
		if (abs < 1_000_000_000)
		{
			return sign + trim(String.format("%.2f", abs / 1_000_000.0)) + "M";
		}
		return sign + trim(String.format("%.2f", abs / 1_000_000_000.0)) + "B";
	}

	static String signedShortGp(long amount)
	{
		return (amount > 0 ? "+" : "") + shortGp(amount);
	}

	private static String trim(String number)
	{
		return number.contains(".") ? number.replaceAll("0+$", "").replaceAll("\\.$", "") : number;
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
	 * Green 70+, amber 40+, red below.
	 */
	static Color scoreColor(int score)
	{
		return score >= 70 ? GOOD : score >= 40 ? WARN : BAD;
	}

	/**
	 * "now" under a minute, otherwise "12m", "3h", "2d".
	 */
	static String age(long minutes)
	{
		if (minutes < 1)
		{
			return "now";
		}
		if (minutes < 60)
		{
			return minutes + "m";
		}
		if (minutes < 60 * 24)
		{
			return minutes / 60 + "h";
		}
		return minutes / (60 * 24) + "d";
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
