package com.fredflips.money;

import java.util.List;
import lombok.Value;

/**
 * Live GP/hr for one money maker.
 */
@Value
public class MoneyResult
{
	MoneyMethod method;

	/**
	 * Worst case: inputs bought instantly, outputs sold instantly, after GE tax.
	 */
	long instantGpPerHour;
	/**
	 * Best case: inputs bought and outputs sold with patient offers, after GE tax.
	 */
	long offersGpPerHour;
	/**
	 * What the inputs cost per hour when bought instantly.
	 */
	long costPerHour;
	long taxPerHour;

	/**
	 * Share of an hour's inputs your buy limits allow (1 = not limited).
	 * For example 0.5 means you can only buy enough for 30 minutes per hour on average.
	 */
	double buyLimitShare;
	/**
	 * Name of the input whose buy limit is the tightest, or null when nothing limits you.
	 */
	String limitingInput;
	/**
	 * Outputs whose hourly market volume is smaller than what you'd make in an hour.
	 */
	List<String> thinOutputs;
	/**
	 * Share of an hour's outputs the market trades in an hour (1 = it can take them all).
	 */
	double marketShare;
	/**
	 * True when any price used is older than the max price age setting.
	 */
	boolean stalePrices;

	/**
	 * Instant GP/hr scaled down when buy limits or market volume can't keep up with a full hour.
	 * Losses are never scaled down.
	 */
	public long getRealisticGpPerHour()
	{
		if (instantGpPerHour <= 0)
		{
			return instantGpPerHour;
		}
		return Math.round(instantGpPerHour * Math.min(1, Math.min(buyLimitShare, marketShare)));
	}
}
