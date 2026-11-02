package com.fredflips.insights;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Chart time ranges, and the wiki timestep that gives enough points for each.
 */
@Getter
@RequiredArgsConstructor
public enum ChartPeriod
{
	DAY("24h", "5m", 288),
	WEEK("7d", "1h", 168),
	MONTH("30d", "6h", 120);

	private final String displayName;
	private final String timestep;
	/**
	 * How many of the newest points cover this period.
	 */
	private final int points;

	@Override
	public String toString()
	{
		return displayName;
	}
}
