package com.fredflips.wiki;

import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Everything fetched from the wiki in one refresh.
 */
@Value
public class MarketSnapshot
{
	List<ItemMapping> items;
	Map<Integer, LatestPrice> latest;
	Map<Integer, AveragePrice> fiveMinute;
	Map<Integer, AveragePrice> oneHour;
	Map<Integer, AveragePrice> oneDay;
	/**
	 * Unix epoch seconds when this snapshot was fetched.
	 */
	long fetchedAt;
}
