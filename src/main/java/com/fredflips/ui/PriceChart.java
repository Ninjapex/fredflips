package com.fredflips.ui;

import com.fredflips.wiki.TimeseriesPoint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.QuantityFormatter;

/**
 * Line chart of the average instant-buy (sell side) and instant-sell (buy side) prices.
 */
class PriceChart extends JComponent
{
	static final Color SELL_LINE = new Color(0x00C27A);
	static final Color BUY_LINE = new Color(0xFF9F40);
	private static final int HEIGHT = 130;
	private static final int PAD = 4;

	private List<TimeseriesPoint> points = Collections.emptyList();

	PriceChart()
	{
		setPreferredSize(new Dimension(200, HEIGHT));
	}

	void setPoints(List<TimeseriesPoint> points)
	{
		this.points = points;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ColorScheme.DARKER_GRAY_COLOR);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setFont(FontManager.getRunescapeSmallFont());

		long min = Long.MAX_VALUE;
		long max = Long.MIN_VALUE;
		for (TimeseriesPoint point : points)
		{
			for (Long price : new Long[]{point.getAvgHighPrice(), point.getAvgLowPrice()})
			{
				if (price != null)
				{
					min = Math.min(min, price);
					max = Math.max(max, price);
				}
			}
		}
		if (points.size() < 2 || min == Long.MAX_VALUE)
		{
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g.drawString(points.isEmpty() ? "Loading chart..." : "Not enough trades to chart", PAD, getHeight() / 2);
			g.dispose();
			return;
		}
		if (max == min)
		{
			max = min + 1;
		}

		drawLine(g, TimeseriesPoint::getAvgHighPrice, SELL_LINE, min, max);
		drawLine(g, TimeseriesPoint::getAvgLowPrice, BUY_LINE, min, max);

		g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
		g.drawString(QuantityFormatter.formatNumber(max), PAD, PAD + 10);
		g.drawString(QuantityFormatter.formatNumber(min), PAD, getHeight() - PAD);
		g.dispose();
	}

	private void drawLine(Graphics2D g, Function<TimeseriesPoint, Long> price, Color color, long min, long max)
	{
		g.setColor(color);
		g.setStroke(new BasicStroke(1.5f));
		int width = getWidth() - PAD * 2;
		int height = getHeight() - PAD * 2 - 12;
		Integer lastX = null;
		Integer lastY = null;
		for (int i = 0; i < points.size(); i++)
		{
			Long value = price.apply(points.get(i));
			if (value == null)
			{
				continue;
			}
			int x = PAD + (int) ((long) width * i / (points.size() - 1));
			int y = PAD + 12 + (int) (height - (double) (value - min) / (max - min) * height);
			if (lastX != null)
			{
				g.drawLine(lastX, lastY, x, y);
			}
			lastX = x;
			lastY = y;
		}
	}
}
