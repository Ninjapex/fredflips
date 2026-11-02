package com.fredflips.flip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoverType
{
	DUMPS("Dumps", "Latest sell price well below the daily average. Could be a bargain, or the start of a crash"),
	SPIKES("Spikes", "Latest buy price well above the daily average. A chance to sell if you already hold it"),
	DIPS("Dips", "Last hour's average sell price 2%+ under today's average - a steadier drop than a single dump trade"),
	VOLATILE("Volatile", "Busy items with a wide gap between buy and sell prices. Bigger margins, bigger swings"),
	HIGH_ALCH("High alch", "Profit from buying an item and casting High Level Alchemy, after nature rune cost");

	private final String displayName;
	private final String description;

	@Override
	public String toString()
	{
		return displayName;
	}
}
