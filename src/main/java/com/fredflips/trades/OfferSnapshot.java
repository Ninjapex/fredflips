package com.fredflips.trades;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What one GE slot looked like the last time the game told us about it.
 * Saved per account so fills that happen while logged out are counted on the next login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferSnapshot
{
	private int itemId;
	private String name;
	private boolean buy;
	private long price;
	private int totalQuantity;
	private int quantitySold;
	/**
	 * Total gp spent (buys) or received before tax (sells) so far.
	 */
	private long spent;
	private OfferStatus status;
	/**
	 * Epoch millis when we first saw this offer. Usually when it was placed; for offers placed on
	 * another device, when this plugin first saw it.
	 */
	private long placedAt;
	/**
	 * Epoch millis of the most recent fill, or 0 if nothing has filled yet.
	 */
	private long lastFillAt;

	/**
	 * Same item, side, price and size - so it's the same offer, not a new one in the same slot.
	 */
	boolean isSameOfferAs(OfferSnapshot other)
	{
		return other != null
			&& itemId == other.itemId
			&& buy == other.buy
			&& price == other.price
			&& totalQuantity == other.totalQuantity
			&& quantitySold >= other.quantitySold
			&& spent >= other.spent;
	}
}
