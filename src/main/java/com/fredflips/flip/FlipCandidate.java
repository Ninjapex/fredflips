package com.fredflips.flip;

import lombok.Value;

/**
 * One item's flip, with every number already worked out.
 */
@Value
public class FlipCandidate
{
	int itemId;
	String name;
	boolean members;
	/**
	 * GE buy limit per 4 hours, or 0 if unknown.
	 */
	int buyLimit;

	/**
	 * Price to put your buy offer in at.
	 */
	long buyPrice;
	/**
	 * Price to put your sell offer in at.
	 */
	long sellPrice;
	/**
	 * GE tax per item, paid when it sells.
	 */
	long tax;
	/**
	 * Profit per item after tax: sellPrice - tax - buyPrice.
	 */
	long margin;
	double roiPercent;

	/**
	 * Suggested quantity, limited by buy limit, cash stack and trade volume.
	 */
	long quantity;
	/**
	 * margin * quantity.
	 */
	long potentialProfit;

	/**
	 * Instant-sells in the last hour - these fill your buy offer.
	 */
	long sellersPerHour;
	/**
	 * Instant-buys in the last hour - these fill your sell offer.
	 */
	long buyersPerHour;
	/**
	 * Items traded on both sides in the last 24 hours, or 0 if unknown.
	 */
	long dailyVolume;

	/**
	 * Minutes since someone last traded at roughly the buy price (an instant-sell).
	 */
	long buyPriceAgeMinutes;
	/**
	 * Minutes since someone last traded at roughly the sell price (an instant-buy).
	 */
	long sellPriceAgeMinutes;

	/**
	 * 0-100, see {@link FlipScore}.
	 */
	int confidence;
	Tier tier;
	Signal signal;

	public long getSlowerSidePerHour()
	{
		return Math.min(sellersPerHour, buyersPerHour);
	}
}
