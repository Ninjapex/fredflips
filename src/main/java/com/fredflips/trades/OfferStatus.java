package com.fredflips.trades;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Plugin-side copy of the game's offer states, so saved data doesn't depend on RuneLite's enum.
 */
@Getter
@RequiredArgsConstructor
public enum OfferStatus
{
	BUYING("Buying", true),
	BOUGHT("Bought - collect", false),
	SELLING("Selling", true),
	SOLD("Sold - collect", false),
	CANCELLED_BUY("Buy cancelled", false),
	CANCELLED_SELL("Sell cancelled", false);

	private final String displayName;
	/**
	 * Still waiting on the market (as opposed to finished and waiting for you to collect).
	 */
	private final boolean pending;
}
