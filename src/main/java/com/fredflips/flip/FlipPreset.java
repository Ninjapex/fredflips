package com.fredflips.flip;

import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Quick ways to narrow the flip list. Each is a simple rule over the flip's numbers.
 */
@Getter
@RequiredArgsConstructor
public enum FlipPreset
{
	ALL("All flips", "Everything that passes your safety filters", false, flip -> true),
	STARTER("Starter", "Cheap, busy items with a solid confidence score - a gentle place to start",
		false, flip -> flip.getBuyPrice() <= 100_000 && flip.getConfidence() >= 60 && flip.getSlowerSidePerHour() >= 100),
	HIGH_MARGIN("High margin", "At least 2% back after tax",
		false, flip -> flip.getRoiPercent() >= 2),
	F2P("F2P only", "Free-to-play items only",
		false, flip -> !flip.isMembers()),
	PRICE_DIP("Price dip", "Latest sell price 2%+ under today's average. Can keep falling",
		true, flip -> flip.getDipPercent() <= -2),
	STABLE("Stable", "Price is steady against its daily average",
		false, flip -> flip.getSignal() == Signal.STABLE && Math.abs(flip.getDriftPercent()) < 1),
	HIGH_VOLUME("High volume", "1,000+ trades an hour on the slower side",
		false, flip -> flip.getSlowerSidePerHour() >= 1_000),
	VOLUME_SPIKE("Volume spike", "Trading at twice its usual hourly rate or more",
		false, flip -> flip.getVolumeSurge() >= 2),
	OVERSOLD("Oversold", "Last hour 1.5%+ under the daily average. Can keep falling",
		true, flip -> flip.getDriftPercent() <= -1.5),
	MOMENTUM("Momentum", "Last hour 1%+ over the daily average, on normal or higher volume",
		true, flip -> flip.getDriftPercent() >= 1 && flip.getVolumeSurge() >= 1),
	LOW_VOLATILITY("Low volatility", "Tight spread and barely moving",
		false, flip -> flip.getSpreadPercent() <= 1 && Math.abs(flip.getDriftPercent()) <= 0.5),
	BULK_MARGIN("Bulk margin", "Small margins on big quantities (1,000+ per flip)",
		false, flip -> flip.getQuantity() >= 1_000);

	private final String displayName;
	private final String description;
	/**
	 * Presets that are about moving prices show Falling/Spiking/Dumping items even when those are hidden.
	 */
	private final boolean showsRiskySignals;
	private final Predicate<FlipCandidate> rule;

	@Override
	public String toString()
	{
		return displayName;
	}
}
