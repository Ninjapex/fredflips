package com.fredflips.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JTextField;

/**
 * A dark text box that shows grey hint text while it's empty.
 */
class SearchField extends JTextField
{
	private final String hint;

	SearchField(String hint)
	{
		this.hint = hint;
		setFont(Ui.SMALL);
		setForeground(java.awt.Color.WHITE);
		setCaretColor(java.awt.Color.WHITE);
		setBackground(Ui.FIELD_BG);
		setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Ui.HOVER_BG),
			BorderFactory.createEmptyBorder(6, 8, 6, 8)));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		if (getText().isEmpty())
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(Ui.FAINT);
			g.setFont(getFont());
			int baseline = (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
			g.drawString(hint, getInsets().left, baseline);
			g.dispose();
		}
	}
}
