package com.fredflips.flip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MoverType
{
	DUMPS("Dumps", "Latest sell price well below the daily average. Could be a bargain, or the start of a crash"),
	SPIKES("Spikes", "Latest buy price well above the daily average. A chance to sell if you already hold it"),
	HIGH_ALCH("High alch", "Profit from buying an item and casting High Level Alchemy, after nature rune cost");

	private final String displayName;
	private final String description;

	@Override
	public String toString()
	{
		return displayName;
	}
}
