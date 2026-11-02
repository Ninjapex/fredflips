package com.fredflips.insights;

import com.fredflips.wiki.TimeseriesPoint;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * Indicators and volume for one item, worked out from its hourly price history.
 * Any value can be null when the history is too short.
 */
@Value
public class ItemInsights
{
	private static final int HOURS_PER_DAY = 24;
	private static final int HOURS_PER_WEEK = 168;
	private static final int RSI_PERIOD = 14;

	Double change1h;
	Double change24h;
	Double change7d;

	Double rsi;
	Double macd;
	Double macdSignal;
	Double macdHistogram;
	Double sma24;
	/**
	 * Latest mid price compared with its 24 hour simple moving average.
	 */
	Double priceVsSma24Percent;

	Long low24h;
	Long high24h;
	Long low7d;
	Long high7d;

	/**
	 * Instant-buys (people buying) and instant-sells (people selling) over the last 24 hours.
	 */
	long buyers24h;
	long sellers24h;
	/**
	 * Average items traded per day over the whole history the wiki returned.
	 */
	long averageDailyVolume;

	public long getVolume24h()
	{
		return buyers24h + sellers24h;
	}

	/**
	 * @param hourly the wiki's 1h timeseries, oldest first
	 */
	public static ItemInsights fromHourly(List<TimeseriesPoint> hourly)
	{
		double[] mids = midPrices(hourly);

		double[] macd = Indicators.macd(mids);
		Double sma24 = Indicators.sma(mids, HOURS_PER_DAY);
		Double priceVsSma = sma24 == null || mids.length == 0 || sma24 == 0
			? null
			: (mids[mids.length - 1] - sma24) * 100 / sma24;

		List<TimeseriesPoint> lastDay = lastN(hourly, HOURS_PER_DAY);
		List<TimeseriesPoint> lastWeek = lastN(hourly, HOURS_PER_WEEK);

		long buyers24h = 0;
		long sellers24h = 0;
		for (TimeseriesPoint point : lastDay)
		{
			buyers24h += point.getHighPriceVolume();
			sellers24h += point.getLowPriceVolume();
		}

		long totalVolume = 0;
		for (TimeseriesPoint point : hourly)
		{
			totalVolume += point.getHighPriceVolume() + point.getLowPriceVolume();
		}
		long averageDailyVolume = hourly.isEmpty() ? 0 : totalVolume * HOURS_PER_DAY / hourly.size();

		return new ItemInsights(
			Indicators.percentChange(mids, 1),
			Indicators.percentChange(mids, HOURS_PER_DAY),
			Indicators.percentChange(mids, HOURS_PER_WEEK),
			Indicators.rsi(mids, RSI_PERIOD),
			macd == null ? null : macd[0],
			macd == null ? null : macd[1],
			macd == null ? null : macd[2],
			sma24,
			priceVsSma,
			lowest(lastDay), highest(lastDay),
			lowest(lastWeek), highest(lastWeek),
			buyers24h, sellers24h, averageDailyVolume);
	}

	public String rsiLabel()
	{
		if (rsi == null)
		{
			return "Not enough data";
		}
		if (rsi < 30)
		{
			return "Oversold";
		}
		if (rsi > 70)
		{
			return "Overbought";
		}
		return "Neutral";
	}

	public String macdLabel()
	{
		if (macdHistogram == null)
		{
			return "Not enough data";
		}
		return macdHistogram >= 0 ? "Bullish momentum" : "Bearish momentum";
	}

	/**
	 * Mid price of each bucket. Buckets with no trades on either side repeat the previous price,
	 * and leading empty buckets are dropped.
	 */
	static double[] midPrices(List<TimeseriesPoint> points)
	{
		List<Double> mids = new ArrayList<>();
		Double previous = null;
		for (TimeseriesPoint point : points)
		{
			Double mid = mid(point);
			if (mid == null)
			{
				mid = previous;
			}
			if (mid != null)
			{
				mids.add(mid);
				previous = mid;
			}
		}
		double[] result = new double[mids.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = mids.get(i);
		}
		return result;
	}

	static Double mid(TimeseriesPoint point)
	{
		Long high = point.getAvgHighPrice();
		Long low = point.getAvgLowPrice();
		if (high != null && low != null)
		{
			return (high + low) / 2.0;
		}
		if (high != null)
		{
			return high.doubleValue();
		}
		return low == null ? null : low.doubleValue();
	}

	private static List<TimeseriesPoint> lastN(List<TimeseriesPoint> points, int n)
	{
		return points.subList(Math.max(0, points.size() - n), points.size());
	}

	private static Long lowest(List<TimeseriesPoint> points)
	{
		Long lowest = null;
		for (TimeseriesPoint point : points)
		{
			if (point.getAvgLowPrice() != null && (lowest == null || point.getAvgLowPrice() < lowest))
			{
				lowest = point.getAvgLowPrice();
			}
		}
		return lowest;
	}

	private static Long highest(List<TimeseriesPoint> points)
	{
		Long highest = null;
		for (TimeseriesPoint point : points)
		{
			if (point.getAvgHighPrice() != null && (highest == null || point.getAvgHighPrice() > highest))
			{
				highest = point.getAvgHighPrice();
			}
		}
		return highest;
	}
}
