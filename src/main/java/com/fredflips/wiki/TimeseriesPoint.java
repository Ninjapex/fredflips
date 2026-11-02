package com.fredflips.wiki;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One bucket from the wiki's /timeseries endpoint (5 minutes, 1 hour, 6 hours or 24 hours long).
 * Prices are null when nothing traded on that side during the bucket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeseriesPoint
{
	/**
	 * Unix epoch seconds at the start of the bucket.
	 */
	private long timestamp;
	private Long avgHighPrice;
	private Long avgLowPrice;
	private long highPriceVolume;
	private long lowPriceVolume;
}
