package com.fredflips.flip;

import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortOrder
{
	POTENTIAL_PROFIT("Potential profit", Comparator.comparingLong(FlipCandidate::getPotentialProfit)),
	CONFIDENCE("Confidence", Comparator.comparingInt(FlipCandidate::getConfidence)
		.thenComparingLong(FlipCandidate::getPotentialProfit)),
	MARGIN("Margin per item", Comparator.comparingLong(FlipCandidate::getMargin)),
	ROI("ROI %", Comparator.comparingDouble(FlipCandidate::getRoiPercent)),
	VOLUME("Trade volume", Comparator.comparingLong(FlipCandidate::getSlowerSidePerHour));

	private final String displayName;
	// Ascending; callers reverse it so the best flip comes first.
	private final Comparator<FlipCandidate> comparator;

	@Override
	public String toString()
	{
		return displayName;
	}
}
