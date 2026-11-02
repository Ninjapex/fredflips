package com.fredflips.flip;

import lombok.Value;

/**
 * One row in the Movers tab.
 */
@Value
public class Mover
{
	int itemId;
	String name;
	/**
	 * The current price that stands out (dumps/spikes), or the price to buy at (high alch).
	 */
	long price;
	/**
	 * What it's compared against: the daily average (dumps/spikes) or the alch value (high alch).
	 */
	long referencePrice;
	/**
	 * Percent difference from the reference price.
	 */
	double changePercent;
	/**
	 * Profit per item for high alch; 0 for dumps and spikes.
	 */
	long profit;
	/**
	 * Items traded on both sides in the last hour.
	 */
	long volumePerHour;
	int buyLimit;
}
