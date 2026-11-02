package com.fredflips.wiki;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry from the wiki's /5m or /1h endpoints: average prices and trade counts over that window.
 * <p>
 * {@code highPriceVolume} counts instant-buys (people buying - this is what fills your sell offers).
 * {@code lowPriceVolume} counts instant-sells (people selling - this is what fills your buy offers).
 * The average prices are null when that side had no trades in the window.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AveragePrice
{
	private Long avgHighPrice;
	private long highPriceVolume;
	private Long avgLowPrice;
	private long lowPriceVolume;
}
