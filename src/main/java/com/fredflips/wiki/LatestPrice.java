package com.fredflips.wiki;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry from the wiki's /latest endpoint.
 * <p>
 * {@code high} is the price of the most recent instant-buy (what you can sell at).
 * {@code low} is the price of the most recent instant-sell (what you can buy at).
 * Times are unix epoch seconds. Any field can be null if that side never traded.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LatestPrice
{
	private Long high;
	private Long highTime;
	private Long low;
	private Long lowTime;
}
