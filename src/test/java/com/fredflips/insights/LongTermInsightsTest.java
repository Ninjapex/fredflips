package com.fredflips.insights;

import com.fredflips.wiki.TimeseriesPoint;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class LongTermInsightsTest
{
	private static final double EPSILON = 1e-6;

	@Test
	public void ninetyDayRangePositionAndDrawdown()
	{
		List<TimeseriesPoint> daily = new ArrayList<>();
		for (int day = 0; day < 120; day++)
		{
			// Low of 800 on day 60, high of 1,200 on day 100, 1,000 otherwise.
			long low = day == 60 ? 800 : 1_000;
			long high = day == 100 ? 1_200 : 1_010;
			daily.add(new TimeseriesPoint(day * 86_400L, high, low, 10, 10));
		}
		LongTermInsights insights = LongTermInsights.of(daily, new ArrayList<>(), 1_000, false);

		assertEquals(Long.valueOf(800), insights.getLow90());
		assertEquals(Long.valueOf(1_200), insights.getHigh90());
		assertEquals(50.0, insights.getPosition90Percent(), EPSILON);
		assertEquals(100.0 * 200 / 1_200, insights.getDrawdownPercent(), EPSILON);
		assertEquals(Integer.valueOf(59), insights.getDaysSinceLow90());
		assertEquals(Integer.valueOf(19), insights.getDaysSinceHigh90());
	}

	@Test
	public void bollingerAtTheMeanIsHalfway()
	{
		double[] values = new double[20];
		for (int i = 0; i < 20; i++)
		{
			values[i] = i % 2 == 0 ? 90 : 110;
		}
		values[19] = 100;
		double percent = LongTermInsights.bollinger(values);
		assertTrue("was " + percent, percent > 45 && percent < 55);
		assertNull(LongTermInsights.bollinger(new double[5]));
	}

	@Test
	public void invertedHoursCountLossMakingHours()
	{
		List<TimeseriesPoint> hourly = new ArrayList<>();
		for (int hour = 0; hour < 24; hour++)
		{
			// 6 hours where the 2% tax eats the whole spread
			long high = hour < 6 ? 1_010 : 1_100;
			hourly.add(new TimeseriesPoint(hour * 3_600L, high, 1_000L, 10, 10));
		}
		assertEquals(Integer.valueOf(6), LongTermInsights.invertedHours(hourly, false));
	}

	@Test
	public void projectionWidensWithTime()
	{
		double[] mids = new double[100];
		for (int i = 0; i < mids.length; i++)
		{
			mids[i] = 1_000 * (1 + 0.02 * Math.sin(i));
		}
		double[] month = LongTermInsights.projection(mids, 1_000, 30);
		double[] quarter = LongTermInsights.projection(mids, 1_000, 90);
		assertTrue(month[0] < 1_000 && month[1] > 1_000);
		assertTrue(quarter[0] < month[0] && quarter[1] > month[1]);
	}
}
