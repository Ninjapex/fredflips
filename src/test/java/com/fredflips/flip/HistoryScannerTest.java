package com.fredflips.flip;

import com.fredflips.wiki.TimeseriesPoint;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class HistoryScannerTest
{
	@Test
	public void allTimeLow()
	{
		List<TimeseriesPoint> year = new ArrayList<>();
		for (int day = 0; day < 365; day++)
		{
			long price = 1_000 + (day % 50) * 10;
			year.add(new TimeseriesPoint(day * 86_400L, price + 20, price, 10, 10));
		}
		// Lowest average sell price in the year is 1,000
		HistoryScanner.Result atl = HistoryScanner.allTimeLow(1, "x", year, 1_020, 1_100);
		assertNotNull(atl);
		assertEquals(2.0, atl.getPercent(), 1e-9);
		assertEquals(1_000, atl.getYearLow());
		assertNull(HistoryScanner.allTimeLow(1, "x", year, 1_100, 1_150));
	}

	@Test
	public void timeOfDayFindsTheDailyPattern()
	{
		// Every day: cheap at 05:00 (980), dear at 20:00 (1,060), 1,000 the rest of the time.
		List<TimeseriesPoint> hourly = new ArrayList<>();
		long start = LocalDate.of(2026, 10, 1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
		for (int hour = 0; hour < 24 * 14; hour++)
		{
			int hourOfDay = hour % 24;
			long low = hourOfDay == 5 ? 980 : 1_000;
			long high = hourOfDay == 20 ? 1_060 : 1_010;
			hourly.add(new TimeseriesPoint(start + hour * 3_600L, high, low, 10, 10));
		}

		HistoryScanner.Result result = HistoryScanner.timeOfDay(1, "x", hourly, false, ZoneOffset.UTC);

		assertNotNull(result);
		assertEquals(5, result.getBuyHour());
		assertEquals(20, result.getSellHour());
		// Buy 981, sell 1,059 minus 21 tax
		assertEquals(1_059 - 21 - 981, result.getProfitEach());
		assertEquals(100.0, result.getPercent(), 1e-9);
	}

	@Test
	public void trendingPricesAreNotAPattern()
	{
		// Steady climb of 2% a day: always "cheaper this morning than tonight", but it's a trend.
		List<TimeseriesPoint> hourly = new ArrayList<>();
		for (int hour = 0; hour < 24 * 14; hour++)
		{
			long price = Math.round(1_000 * Math.pow(1.02, hour / 24.0));
			hourly.add(new TimeseriesPoint(hour * 3_600L, price + 5, price, 10, 10));
		}
		assertNull(HistoryScanner.timeOfDay(1, "x", hourly, false, ZoneOffset.UTC));
	}

	@Test
	public void flatPricesHaveNoArbitrage()
	{
		List<TimeseriesPoint> hourly = new ArrayList<>();
		for (int hour = 0; hour < 24 * 14; hour++)
		{
			hourly.add(new TimeseriesPoint(hour * 3_600L, 1_005L, 1_000L, 10, 10));
		}
		assertNull(HistoryScanner.timeOfDay(1, "x", hourly, false, ZoneOffset.UTC));
	}
}
