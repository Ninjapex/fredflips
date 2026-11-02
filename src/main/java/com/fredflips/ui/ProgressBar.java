package com.fredflips.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A bar showing how much of something is done, optionally with text like "8,000 / 13,000" on it.
 */
class ProgressBar extends JComponent
{
	private static final int THIN = 5;
	private static final int LABELLED = 15;

	private final double fraction;
	private final Color color;
	private final String text;

	ProgressBar(long done, long total, Color color)
	{
		this(done, total, color, null);
	}

	ProgressBar(long done, long total, Color color, String text)
	{
		this.fraction = total <= 0 ? 0 : Math.min(1, (double) done / total);
		this.color = color;
		this.text = text;
		setPreferredSize(new Dimension(100, text == null ? THIN : LABELLED));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setColor(Ui.FIELD_BG);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(color);
		g.fillRect(0, 0, (int) Math.round(getWidth() * fraction), getHeight());
		if (text != null)
		{
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(Ui.SMALL_BOLD);
			FontMetrics metrics = g.getFontMetrics();
			int x = (getWidth() - metrics.stringWidth(text)) / 2;
			int y = (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
			g.setColor(Color.BLACK);
			g.drawString(text, x + 1, y + 1);
			g.setColor(Color.WHITE);
			g.drawString(text, x, y);
		}
		g.dispose();
	}
}
