package com.fredflips.flip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Minimum potential profit per flip for the flip list.
 */
@Getter
@RequiredArgsConstructor
public enum ProfitFloor
{
	ANY(0, "Any profit"),
	K100(100_000, "100K+"),
	K500(500_000, "500K+"),
	M1(1_000_000, "1M+");

	private final long minimum;
	private final String displayName;

	@Override
	public String toString()
	{
		return displayName;
	}
}
