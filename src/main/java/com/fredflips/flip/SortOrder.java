package com.fredflips.flip;

import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SortOrder
{
	CONFIDENCE("Confidence", Comparator.comparingInt(FlipCandidate::getConfidence)
		.thenComparingLong(FlipCandidate::getPotentialProfit).reversed()),
	GP_PER_HOUR("GP / hour", Comparator.comparingLong(FlipCandidate::getGpPerHour).reversed()),
	POTENTIAL_PROFIT("Potential profit", Comparator.comparingLong(FlipCandidate::getPotentialProfit).reversed()),
	MARGIN("Margin per item", Comparator.comparingLong(FlipCandidate::getMargin).reversed()),
	ROI("ROI %", Comparator.comparingDouble(FlipCandidate::getRoiPercent).reversed()),
	DAILY_VOLUME("Daily volume", Comparator.comparingLong(FlipCandidate::getDailyVolume).reversed()),
	HOURLY_VOLUME("Hourly volume", Comparator.comparingLong(FlipCandidate::getSlowerSidePerHour).reversed()),
	BUY_PRICE_LOW("Buy price (low to high)", Comparator.comparingLong(FlipCandidate::getBuyPrice)),
	BUY_PRICE_HIGH("Buy price (high to low)", Comparator.comparingLong(FlipCandidate::getBuyPrice).reversed()),
	SELL_PRICE_LOW("Sell price (low to high)", Comparator.comparingLong(FlipCandidate::getSellPrice)),
	SELL_PRICE_HIGH("Sell price (high to low)", Comparator.comparingLong(FlipCandidate::getSellPrice).reversed());

	private final String displayName;
	// Best first.
	private final Comparator<FlipCandidate> comparator;

	@Override
	public String toString()
	{
		return displayName;
	}
}
