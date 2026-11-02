package com.fredflips.insights;

import com.fredflips.wiki.TimeseriesPoint;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class IndicatorsTest
{
	private static final double EPSILON = 1e-6;

	@Test
	public void simpleMovingAverage()
	{
		assertEquals(4.0, Indicators.sma(new double[]{1, 2, 3, 4, 5}, 3), EPSILON);
		assertNull(Indicators.sma(new double[]{1, 2}, 3));
	}

	@Test
	public void exponentialMovingAverageSeedsWithSimpleAverage()
	{
		double[] ema = Indicators.emaSeries(new double[]{2, 4, 6, 8}, 3);
		assertTrue(Double.isNaN(ema[1]));
		assertEquals(4.0, ema[2], EPSILON);
		// k = 0.5: 8 * 0.5 + 4 * 0.5
		assertEquals(6.0, ema[3], EPSILON);
	}

	@Test
	public void rsiExtremes()
	{
		double[] rising = new double[20];
		double[] falling = new double[20];
		for (int i = 0; i < 20; i++)
		{
			rising[i] = 100 + i;
			falling[i] = 100 - i;
		}
		assertEquals(100.0, Indicators.rsi(rising, 14), EPSILON);
		assertEquals(0.0, Indicators.rsi(falling, 14), EPSILON);
		assertNull(Indicators.rsi(new double[]{1, 2, 3}, 14));
	}

	@Test
	public void rsiMatchesHandCalculation()
	{
		// Period 2: changes +2, -1, then +3.
		// Seed: gain (2+0)/2 = 1, loss (0+1)/2 = 0.5. Next: gain (1*1+3)/2 = 2, loss (0.5*1+0)/2 = 0.25.
		// RS = 8, RSI = 100 - 100/9
		assertEquals(100 - 100.0 / 9, Indicators.rsi(new double[]{10, 12, 11, 14}, 2), EPSILON);
	}

	@Test
	public void macdOfFlatSeriesIsZero()
	{
		double[] flat = new double[60];
		java.util.Arrays.fill(flat, 500);
		assertArrayEquals(new double[]{0, 0, 0}, Indicators.macd(flat), EPSILON);
		assertNull(Indicators.macd(new double[30]));
	}

	@Test
	public void macdIsPositiveInAnUptrend()
	{
		double[] rising = new double[60];
		for (int i = 0; i < rising.length; i++)
		{
			rising[i] = 100 + i * i;
		}
		assertTrue(Indicators.macd(rising)[0] > 0);
	}

	@Test
	public void percentChange()
	{
		assertEquals(50.0, Indicators.percentChange(new double[]{100, 120, 150}, 2), EPSILON);
		assertNull(Indicators.percentChange(new double[]{100}, 1));
	}

	@Test
	public void insightsFillGapsAndSumVolume()
	{
		List<TimeseriesPoint> points = new ArrayList<>();
		for (int i = 0; i < 30; i++)
		{
			points.add(new TimeseriesPoint(i * 3600L, 110L, 90L, 5, 3));
		}
		// An hour where nothing traded repeats the previous price.
		points.add(new TimeseriesPoint(30 * 3600L, null, null, 0, 0));

		ItemInsights insights = ItemInsights.fromHourly(points);

		assertEquals(0.0, insights.getChange1h(), EPSILON);
		assertEquals(0.0, insights.getChange24h(), EPSILON);
		assertEquals(Long.valueOf(90), insights.getLow24h());
		assertEquals(Long.valueOf(110), insights.getHigh24h());
		// Last 24 hours: 23 full hours plus the empty one
		assertEquals(23 * 5, insights.getBuyers24h());
		assertEquals(23 * 3, insights.getSellers24h());
		assertNull(insights.getChange7d());
	}
}
