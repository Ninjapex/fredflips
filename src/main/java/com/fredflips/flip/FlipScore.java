package com.fredflips.flip;

import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.LatestPrice;

/**
 * Confidence score and signal for a flip. These are our own rules of thumb, not predictions:
 * they reward the things that make a flip safer and penalise the things that make it riskier.
 * <p>
 * Confidence (0-100) is a weighted mix of:
 * <ul>
 *     <li>30% liquidity - trades per hour on the slower side (1,000+/hr scores full marks)</li>
 *     <li>25% stability - whether the margin also existed on average over the last hour and day</li>
 *     <li>20% trend - how far the last hour's price has drifted from the daily average</li>
 *     <li>15% freshness - how recently both prices were seen</li>
 *     <li>10% balance - whether buyers and sellers trade in similar numbers</li>
 * </ul>
 */
public final class FlipScore
{
	static final double LIQUIDITY_WEIGHT = 0.30;
	static final double STABILITY_WEIGHT = 0.25;
	static final double TREND_WEIGHT = 0.20;
	static final double FRESHNESS_WEIGHT = 0.15;
	static final double BALANCE_WEIGHT = 0.10;

	// Volume that earns full liquidity marks, on a log scale: 10/hr = 1/3, 100/hr = 2/3.
	private static final double FULL_LIQUIDITY_PER_HOUR = 1000;
	// Prices older than this earn no freshness marks.
	private static final double STALE_MINUTES = 30;
	// Trend drift that still earns full marks, and drift that earns none.
	private static final double CALM_DRIFT_PERCENT = 1;
	private static final double WILD_DRIFT_PERCENT = 10;

	// How far the latest trade must stray from the daily average to count as a spike or dump.
	static final double SPIKE_PERCENT = 5;
	// How far the hourly average must stray from the daily average to count as rising or falling.
	static final double TREND_PERCENT = 1.5;

	private FlipScore()
	{
	}

	/**
	 * @param margin           our (pessimistic) margin per item after tax
	 * @param taxExempt        whether the item pays GE tax
	 * @param day              24h averages, or null if unavailable
	 * @param maxAgeMinutes    age of the older of the two latest prices
	 */
	public static int confidence(long margin, boolean taxExempt, long sellersPerHour, long buyersPerHour,
		long maxAgeMinutes, AveragePrice hour, AveragePrice day)
	{
		long slower = Math.min(sellersPerHour, buyersPerHour);
		long faster = Math.max(sellersPerHour, buyersPerHour);

		double liquidity = clamp(Math.log10(Math.max(1, slower)) / Math.log10(FULL_LIQUIDITY_PER_HOUR));
		double freshness = clamp(1 - maxAgeMinutes / STALE_MINUTES);
		double balance = faster == 0 ? 0 : (double) slower / faster;

		double hourStability = marginRatio(averageMargin(hour, taxExempt), margin);
		double dayStability = day == null ? 0.5 : marginRatio(averageMargin(day, taxExempt), margin);
		double stability = (hourStability + dayStability) / 2;

		Double drift = driftPercent(hour, day);
		double trend = drift == null
			? 0.5
			: clamp(1 - (Math.abs(drift) - CALM_DRIFT_PERCENT) / (WILD_DRIFT_PERCENT - CALM_DRIFT_PERCENT));

		double score = LIQUIDITY_WEIGHT * liquidity
			+ STABILITY_WEIGHT * stability
			+ TREND_WEIGHT * trend
			+ FRESHNESS_WEIGHT * freshness
			+ BALANCE_WEIGHT * balance;
		return (int) Math.round(score * 100);
	}

	public static Signal signal(LatestPrice latest, AveragePrice hour, AveragePrice day)
	{
		if (day == null || day.getAvgHighPrice() == null || day.getAvgLowPrice() == null)
		{
			return Signal.UNKNOWN;
		}
		if (latest.getHigh() != null && percentAbove(latest.getHigh(), day.getAvgHighPrice()) >= SPIKE_PERCENT)
		{
			return Signal.SPIKING;
		}
		if (latest.getLow() != null && percentAbove(latest.getLow(), day.getAvgLowPrice()) <= -SPIKE_PERCENT)
		{
			return Signal.DUMPING;
		}
		Double drift = driftPercent(hour, day);
		if (drift == null)
		{
			return Signal.UNKNOWN;
		}
		if (drift >= TREND_PERCENT)
		{
			return Signal.RISING;
		}
		if (drift <= -TREND_PERCENT)
		{
			return Signal.FALLING;
		}
		return Signal.STABLE;
	}

	/**
	 * Percent change from the daily mid price to the hourly mid price, or null without data.
	 */
	static Double driftPercent(AveragePrice hour, AveragePrice day)
	{
		Double hourMid = mid(hour);
		Double dayMid = mid(day);
		if (hourMid == null || dayMid == null || dayMid == 0)
		{
			return null;
		}
		return (hourMid - dayMid) * 100 / dayMid;
	}

	private static Double mid(AveragePrice average)
	{
		if (average == null || average.getAvgHighPrice() == null || average.getAvgLowPrice() == null)
		{
			return null;
		}
		return (average.getAvgHighPrice() + average.getAvgLowPrice()) / 2.0;
	}

	/**
	 * The margin you'd have made trading at the window's average prices, or null without data.
	 */
	private static Long averageMargin(AveragePrice average, boolean taxExempt)
	{
		if (average == null || average.getAvgHighPrice() == null || average.getAvgLowPrice() == null)
		{
			return null;
		}
		long sell = average.getAvgHighPrice();
		return sell - GeTax.perItem(sell, taxExempt) - average.getAvgLowPrice();
	}

	/**
	 * 1 when the average margin is at least as big as ours, sliding to 0 as it shrinks to nothing.
	 */
	private static double marginRatio(Long averageMargin, long margin)
	{
		if (averageMargin == null || margin <= 0)
		{
			return 0;
		}
		return clamp((double) averageMargin / margin);
	}

	private static double percentAbove(long price, long reference)
	{
		return reference == 0 ? 0 : (price - reference) * 100.0 / reference;
	}

	private static double clamp(double value)
	{
		return Math.max(0, Math.min(1, value));
	}
}
