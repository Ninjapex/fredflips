package com.fredflips.insights;

import com.fredflips.flip.GeTax;
import com.fredflips.wiki.TimeseriesPoint;
import java.util.ArrayList;
import java.util.List;
import lombok.Value;

/**
 * The longer view of an item from its daily prices (up to a year) and hourly prices (about two weeks):
 * 90-day range, moving averages, Bollinger position, liquidity and a rough projection.
 * Any value can be null when the history is too short.
 */
@Value
public class LongTermInsights
{
	private static final int DAYS_90 = 90;
	private static final int BOLLINGER_PERIOD = 20;
	private static final double BOLLINGER_WIDTH = 2;

	Long low90;
	Long high90;
	/**
	 * Where today's price sits in the 90-day range: 0% at the low, 100% at the high.
	 */
	Double position90Percent;
	/**
	 * How far today's price is below the 90-day high.
	 */
	Double drawdownPercent;
	Integer daysSinceLow90;
	Integer daysSinceHigh90;

	Double ma7;
	Double ma30;
	Double change30dPercent;

	/**
	 * Where the latest hourly price sits in its 20-hour Bollinger band: 0% at the bottom, 100% at the top.
	 */
	Double bollingerPercent;
	/**
	 * Hours in the last 24 when buying at the average low and selling at the average high lost money after tax.
	 */
	Integer marginInvertedHours;

	/**
	 * Likely price range (one standard deviation, about 68% of outcomes) in 30 and 90 days,
	 * from how much the price usually moves day to day. Not a prediction.
	 */
	Long projection30Low;
	Long projection30High;
	Long projection90Low;
	Long projection90High;

	public String maCrossLabel()
	{
		if (ma7 == null || ma30 == null)
		{
			return "Not enough data";
		}
		return ma7 >= ma30 ? "Bullish (7d above 30d)" : "Bearish (7d below 30d)";
	}

	/**
	 * @param daily      the wiki's 24h timeseries, oldest first
	 * @param hourly     the wiki's 1h timeseries, oldest first
	 * @param price      today's price to compare against the history
	 * @param taxExempt  whether the item pays GE tax
	 */
	public static LongTermInsights of(List<TimeseriesPoint> daily, List<TimeseriesPoint> hourly, long price, boolean taxExempt)
	{
		double[] dailyMids = ItemInsights.midPrices(daily);
		List<TimeseriesPoint> last90 = daily.subList(Math.max(0, daily.size() - DAYS_90), daily.size());

		Long low = null;
		Long high = null;
		int lowIndex = -1;
		int highIndex = -1;
		for (int i = 0; i < last90.size(); i++)
		{
			TimeseriesPoint point = last90.get(i);
			if (point.getAvgLowPrice() != null && (low == null || point.getAvgLowPrice() < low))
			{
				low = point.getAvgLowPrice();
				lowIndex = i;
			}
			if (point.getAvgHighPrice() != null && (high == null || point.getAvgHighPrice() > high))
			{
				high = point.getAvgHighPrice();
				highIndex = i;
			}
		}
		Double position = low == null || high == null || high <= low ? null : clampPercent((price - low) * 100.0 / (high - low));
		Double drawdown = high == null || high == 0 ? null : Math.max(0, (high - price) * 100.0 / high);

		double[] hourlyMids = ItemInsights.midPrices(hourly);
		Double ma7 = Indicators.sma(dailyMids, 7);
		Double ma30 = Indicators.sma(dailyMids, 30);
		double[] spread30 = projection(dailyMids, price, 30);
		double[] spread90 = projection(dailyMids, price, 90);

		return new LongTermInsights(
			low, high, position, drawdown,
			lowIndex < 0 ? null : last90.size() - 1 - lowIndex,
			highIndex < 0 ? null : last90.size() - 1 - highIndex,
			ma7, ma30, Indicators.percentChange(dailyMids, 30),
			bollinger(hourlyMids), invertedHours(hourly, taxExempt),
			spread30 == null ? null : Math.round(spread30[0]), spread30 == null ? null : Math.round(spread30[1]),
			spread90 == null ? null : Math.round(spread90[0]), spread90 == null ? null : Math.round(spread90[1]));
	}

	static Double bollinger(double[] values)
	{
		if (values.length < BOLLINGER_PERIOD)
		{
			return null;
		}
		double mean = Indicators.sma(values, BOLLINGER_PERIOD);
		double variance = 0;
		for (int i = values.length - BOLLINGER_PERIOD; i < values.length; i++)
		{
			variance += Math.pow(values[i] - mean, 2);
		}
		double deviation = Math.sqrt(variance / BOLLINGER_PERIOD);
		if (deviation == 0)
		{
			return 50.0;
		}
		double lower = mean - BOLLINGER_WIDTH * deviation;
		double upper = mean + BOLLINGER_WIDTH * deviation;
		return clampPercent((values[values.length - 1] - lower) * 100 / (upper - lower));
	}

	static Integer invertedHours(List<TimeseriesPoint> hourly, boolean taxExempt)
	{
		List<TimeseriesPoint> lastDay = hourly.subList(Math.max(0, hourly.size() - 24), hourly.size());
		if (lastDay.isEmpty())
		{
			return null;
		}
		int inverted = 0;
		for (TimeseriesPoint point : lastDay)
		{
			if (point.getAvgHighPrice() != null && point.getAvgLowPrice() != null)
			{
				long sell = point.getAvgHighPrice();
				if (sell - GeTax.perItem(sell, taxExempt) <= point.getAvgLowPrice())
				{
					inverted++;
				}
			}
		}
		return inverted;
	}

	/**
	 * Price +/- one standard deviation of daily returns, scaled to the number of days.
	 */
	static double[] projection(double[] dailyMids, long price, int days)
	{
		List<Double> returns = new ArrayList<>();
		for (int i = 1; i < dailyMids.length; i++)
		{
			if (dailyMids[i - 1] > 0 && dailyMids[i] > 0)
			{
				returns.add(Math.log(dailyMids[i] / dailyMids[i - 1]));
			}
		}
		if (returns.size() < 30)
		{
			return null;
		}
		double mean = 0;
		for (double r : returns)
		{
			mean += r;
		}
		mean /= returns.size();
		double variance = 0;
		for (double r : returns)
		{
			variance += Math.pow(r - mean, 2);
		}
		double move = Math.sqrt(variance / (returns.size() - 1)) * Math.sqrt(days);
		return new double[]{price * Math.exp(-move), price * Math.exp(move)};
	}

	private static double clampPercent(double value)
	{
		return Math.max(0, Math.min(100, value));
	}
}
