package com.fredflips.flip;

import com.fredflips.wiki.TimeseriesPoint;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Lists that need an item's price history: all-time lows and time-of-day arbitrage.
 */
public final class HistoryScanner
{
	// "At an all-time low" means within this much of the year's lowest price.
	static final double ATL_MARGIN_PERCENT = 3;
	// Need this many days of hourly data before trusting a time-of-day pattern.
	static final int MIN_ARBITRAGE_DAYS = 10;
	// The pattern must have paid after tax on at least this share of days.
	static final double MIN_HIT_PERCENT = 70;
	// Daily swings bigger than this are usually a trend (a new item climbing, a crash) rather than a daily pattern.
	static final double MAX_ARBITRAGE_ROI_PERCENT = 10;
	private static final int HOURS = 24;

	@Value
	public static class Result
	{
		int itemId;
		String name;
		long buyPrice;
		long sellPrice;
		/**
		 * Profit per item after tax (arbitrage), or 0 for ATL.
		 */
		long profitEach;
		/**
		 * Arbitrage: share of days the pattern paid. ATL: how far above the year's low the price is now.
		 */
		double percent;
		/**
		 * Arbitrage: hour of day to buy and sell (local time). ATL: -1.
		 */
		int buyHour;
		int sellHour;
		/**
		 * ATL: the year's lowest and highest average prices.
		 */
		long yearLow;
		long yearHigh;
	}

	// A daily pattern needs a roughly flat price: if the first few days and the last few days differ by more
	// than this, "buy early, sell late" is just riding a trend that won't repeat.
	static final double MAX_TREND_PERCENT = 5;
	private static final int TREND_DAYS = 3;

	private HistoryScanner()
	{
	}

	/**
	 * Compares the average price of the first and last few days.
	 */
	static boolean isTrending(Map<Long, Long[]> pricesByDay)
	{
		java.util.List<Long> days = new java.util.ArrayList<>(pricesByDay.keySet());
		java.util.Collections.sort(days);
		if (days.size() < TREND_DAYS * 2)
		{
			return true;
		}
		double first = averageOf(pricesByDay, days.subList(0, TREND_DAYS));
		double last = averageOf(pricesByDay, days.subList(days.size() - TREND_DAYS, days.size()));
		return first <= 0 || Math.abs(last - first) * 100 / first > MAX_TREND_PERCENT;
	}

	private static double averageOf(Map<Long, Long[]> pricesByDay, java.util.List<Long> days)
	{
		double sum = 0;
		int count = 0;
		for (Long day : days)
		{
			for (Long price : pricesByDay.get(day))
			{
				if (price != null)
				{
					sum += price;
					count++;
				}
			}
		}
		return count == 0 ? 0 : sum / count;
	}

	/**
	 * @param daily  the wiki's 24h timeseries, oldest first
	 * @param latestLow the current instant-sell price
	 * @return a result if the price is within {@link #ATL_MARGIN_PERCENT} of the year's low, else null
	 */
	public static Result allTimeLow(int itemId, String name, List<TimeseriesPoint> daily, long latestLow, long latestHigh)
	{
		Long low = null;
		Long high = null;
		for (TimeseriesPoint point : daily)
		{
			if (point.getAvgLowPrice() != null && (low == null || point.getAvgLowPrice() < low))
			{
				low = point.getAvgLowPrice();
			}
			if (point.getAvgHighPrice() != null && (high == null || point.getAvgHighPrice() > high))
			{
				high = point.getAvgHighPrice();
			}
		}
		if (low == null || low <= 0 || high == null || daily.size() < 30)
		{
			return null;
		}
		double above = (latestLow - low) * 100.0 / low;
		if (above > ATL_MARGIN_PERCENT)
		{
			return null;
		}
		return new Result(itemId, name, latestLow, latestHigh, 0, above, -1, -1, low, high);
	}

	/**
	 * Finds the cheapest hour of the day to buy and the dearest later hour (up to a day later) to sell,
	 * from the item's average hourly prices, and how often that worked day by day.
	 *
	 * @param hourly the wiki's 1h timeseries, oldest first
	 * @return the best pattern if it's profitable after tax, else null
	 */
	public static Result timeOfDay(int itemId, String name, List<TimeseriesPoint> hourly, boolean taxExempt, ZoneId zone)
	{
		double[] lowSum = new double[HOURS];
		int[] lowCount = new int[HOURS];
		double[] highSum = new double[HOURS];
		int[] highCount = new int[HOURS];
		// Prices by calendar day and hour, for the hit rate.
		Map<Long, Long[]> lowByDay = new HashMap<>();
		Map<Long, Long[]> highByDay = new HashMap<>();

		for (TimeseriesPoint point : hourly)
		{
			ZonedDateTime time = Instant.ofEpochSecond(point.getTimestamp()).atZone(zone);
			int hour = time.getHour();
			long day = time.toLocalDate().toEpochDay();
			if (point.getAvgLowPrice() != null)
			{
				lowSum[hour] += point.getAvgLowPrice();
				lowCount[hour]++;
				lowByDay.computeIfAbsent(day, d -> new Long[HOURS])[hour] = point.getAvgLowPrice();
			}
			if (point.getAvgHighPrice() != null)
			{
				highSum[hour] += point.getAvgHighPrice();
				highCount[hour]++;
				highByDay.computeIfAbsent(day, d -> new Long[HOURS])[hour] = point.getAvgHighPrice();
			}
		}
		if (lowByDay.size() < MIN_ARBITRAGE_DAYS || isTrending(lowByDay))
		{
			return null;
		}

		int bestBuy = -1;
		int bestSell = -1;
		long bestProfit = 0;
		for (int buy = 0; buy < HOURS; buy++)
		{
			if (lowCount[buy] < MIN_ARBITRAGE_DAYS)
			{
				continue;
			}
			long buyPrice = Math.round(lowSum[buy] / lowCount[buy]) + 1;
			for (int wait = 1; wait < HOURS; wait++)
			{
				int sell = (buy + wait) % HOURS;
				if (highCount[sell] < MIN_ARBITRAGE_DAYS)
				{
					continue;
				}
				long sellPrice = Math.round(highSum[sell] / highCount[sell]) - 1;
				long profit = sellPrice - GeTax.perItem(sellPrice, taxExempt) - buyPrice;
				if (profit > bestProfit)
				{
					bestProfit = profit;
					bestBuy = buy;
					bestSell = sell;
				}
			}
		}
		if (bestBuy < 0)
		{
			return null;
		}

		// Hit rate: days where buying at the buy hour and selling at the sell hour (same or next day) paid after tax.
		int tried = 0;
		int paid = 0;
		for (Map.Entry<Long, Long[]> day : lowByDay.entrySet())
		{
			Long buyPrice = day.getValue()[bestBuy];
			Long[] sellDay = highByDay.get(bestSell > bestBuy ? day.getKey() : day.getKey() + 1);
			Long sellPrice = sellDay == null ? null : sellDay[bestSell];
			if (buyPrice == null || sellPrice == null)
			{
				continue;
			}
			tried++;
			if (sellPrice - GeTax.perItem(sellPrice, taxExempt) > buyPrice)
			{
				paid++;
			}
		}
		long buyPrice = Math.round(lowSum[bestBuy] / lowCount[bestBuy]) + 1;
		long sellPrice = Math.round(highSum[bestSell] / highCount[bestSell]) - 1;
		double hitPercent = tried == 0 ? 0 : paid * 100.0 / tried;
		if (tried < MIN_ARBITRAGE_DAYS || hitPercent < MIN_HIT_PERCENT || bestProfit * 100.0 / buyPrice > MAX_ARBITRAGE_ROI_PERCENT)
		{
			return null;
		}
		return new Result(itemId, name, buyPrice, sellPrice, bestProfit, hitPercent, bestBuy, bestSell, 0, 0);
	}
}
