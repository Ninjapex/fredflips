package com.fredflips.insights;

/**
 * Standard technical indicators over a series of prices, oldest first.
 * Each method returns null when there isn't enough data.
 */
public final class Indicators
{
	private Indicators()
	{
	}

	/**
	 * Simple moving average of the last {@code period} values.
	 */
	public static Double sma(double[] values, int period)
	{
		if (values.length < period || period <= 0)
		{
			return null;
		}
		double sum = 0;
		for (int i = values.length - period; i < values.length; i++)
		{
			sum += values[i];
		}
		return sum / period;
	}

	/**
	 * Exponential moving average for every point, seeded with the simple average of the first
	 * {@code period} values. Entries before the seed are NaN.
	 */
	public static double[] emaSeries(double[] values, int period)
	{
		double[] ema = new double[values.length];
		java.util.Arrays.fill(ema, Double.NaN);
		if (values.length < period || period <= 0)
		{
			return ema;
		}
		double k = 2.0 / (period + 1);
		double seed = 0;
		for (int i = 0; i < period; i++)
		{
			seed += values[i];
		}
		ema[period - 1] = seed / period;
		for (int i = period; i < values.length; i++)
		{
			ema[i] = values[i] * k + ema[i - 1] * (1 - k);
		}
		return ema;
	}

	/**
	 * Relative Strength Index using Wilder's smoothing. Under 30 is usually read as oversold,
	 * over 70 as overbought.
	 */
	public static Double rsi(double[] values, int period)
	{
		if (values.length <= period || period <= 0)
		{
			return null;
		}
		double gain = 0;
		double loss = 0;
		for (int i = 1; i <= period; i++)
		{
			double change = values[i] - values[i - 1];
			gain += Math.max(change, 0);
			loss += Math.max(-change, 0);
		}
		gain /= period;
		loss /= period;
		for (int i = period + 1; i < values.length; i++)
		{
			double change = values[i] - values[i - 1];
			gain = (gain * (period - 1) + Math.max(change, 0)) / period;
			loss = (loss * (period - 1) + Math.max(-change, 0)) / period;
		}
		if (loss == 0)
		{
			return gain == 0 ? 50.0 : 100.0;
		}
		return 100 - 100 / (1 + gain / loss);
	}

	/**
	 * MACD (12, 26, 9): returns {macd, signal, histogram} for the latest point.
	 * A positive histogram means short-term momentum is above the longer trend.
	 */
	public static double[] macd(double[] values)
	{
		int fast = 12;
		int slow = 26;
		int signalPeriod = 9;
		if (values.length < slow + signalPeriod)
		{
			return null;
		}
		double[] fastEma = emaSeries(values, fast);
		double[] slowEma = emaSeries(values, slow);
		double[] macdLine = new double[values.length - (slow - 1)];
		for (int i = slow - 1; i < values.length; i++)
		{
			macdLine[i - (slow - 1)] = fastEma[i] - slowEma[i];
		}
		double[] signal = emaSeries(macdLine, signalPeriod);
		double macd = macdLine[macdLine.length - 1];
		double signalValue = signal[signal.length - 1];
		return new double[]{macd, signalValue, macd - signalValue};
	}

	/**
	 * Percent change from {@code periodsBack} points ago to the latest point.
	 */
	public static Double percentChange(double[] values, int periodsBack)
	{
		if (values.length <= periodsBack || periodsBack <= 0)
		{
			return null;
		}
		double then = values[values.length - 1 - periodsBack];
		return then == 0 ? null : (values[values.length - 1] - then) * 100 / then;
	}
}
