package com.fredflips.trades;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One fill on the GE: some quantity of an item bought or sold in a single update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trade
{
	/**
	 * Epoch millis.
	 */
	private long time;
	private int itemId;
	private String name;
	private boolean buy;
	private int quantity;
	/**
	 * Total gp paid (buys) or received before tax (sells) for this fill.
	 */
	private long gpTotal;

	public long getPriceEach()
	{
		return quantity == 0 ? 0 : gpTotal / quantity;
	}
}
