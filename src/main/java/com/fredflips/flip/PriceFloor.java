package com.fredflips.flip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Minimum buy price for the flip list.
 */
@Getter
@RequiredArgsConstructor
public enum PriceFloor
{
	ANY(0, "Any price"),
	K1(1_000, "1K+"),
	K10(10_000, "10K+"),
	K100(100_000, "100K+"),
	M1(1_000_000, "1M+"),
	M10(10_000_000, "10M+");

	private final long minimum;
	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
