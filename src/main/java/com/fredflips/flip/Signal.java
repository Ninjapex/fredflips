package com.fredflips.flip;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Which way an item's price is heading right now, judged from the latest trades
 * against its hourly and daily averages.
 */
@Getter
@RequiredArgsConstructor
public enum Signal
{
	STABLE("Stable", "Price is close to its daily average", new Color(0x00C27A)),
	RISING("Rising", "Last hour is above the daily average", new Color(0x4AA3FF)),
	FALLING("Falling", "Last hour is below the daily average - your sell price may drop", new Color(0xFF9F40)),
	SPIKING("Spiking", "Latest buy is well above the daily average - may not last", new Color(0xC77DFF)),
	DUMPING("Dumping", "Latest sell is well below the daily average - could keep falling", new Color(0xE85050)),
	UNKNOWN("No data", "Not enough daily data to judge", new Color(0xA0A0A0));

	private final String displayName;
	private final String description;
	private final Color color;

	@Override
	public String toString()
	{
		return displayName;
	}
}
