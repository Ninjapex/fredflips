package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FlipFilterTest
{
	private static FlipCandidate flip(long buyPrice, long margin, long tax, boolean members, int confidence,
		long quantity, long slowerPerHour, Signal signal, double drift, double dip, double surge)
	{
		return new FlipCandidate(1, "item", members, 1_000, buyPrice, buyPrice + margin + tax, tax, margin,
			margin * 100.0 / buyPrice, quantity, margin * quantity, slowerPerHour, slowerPerHour, slowerPerHour * 24, 0, 0,
			confidence, Tier.fromConfidence(confidence), signal, drift, dip, surge);
	}

	private static FredFlipsConfig config(FlipPreset preset, PriceFloor price, ProfitFloor profit,
		boolean f2p, boolean taxFree, boolean affordable, int cash)
	{
		return new FredFlipsConfig()
		{
			@Override public FlipPreset flipPreset() { return preset; }
			@Override public PriceFloor flipMinBuyPrice() { return price; }
			@Override public ProfitFloor flipMinProfit() { return profit; }
			@Override public boolean flipF2pOnly() { return f2p; }
			@Override public boolean flipTaxFreeOnly() { return taxFree; }
			@Override public boolean flipCapitalFilter() { return affordable; }
			@Override public int cashStack() { return cash; }
		};
	}

	private static final FlipCandidate CHEAP_BUSY = flip(500, 20, 10, false, 75, 2_000, 5_000, Signal.STABLE, 0.2, 0, 1);
	private static final FlipCandidate EXPENSIVE = flip(5_000_000, 90_000, 100_000, true, 50, 8, 20, Signal.RISING, 2, 0, 1.5);

	@Test
	public void presets()
	{
		assertTrue(FlipPreset.STARTER.getRule().test(CHEAP_BUSY));
		assertFalse(FlipPreset.STARTER.getRule().test(EXPENSIVE));
		assertTrue(FlipPreset.F2P.getRule().test(CHEAP_BUSY));
		assertFalse(FlipPreset.F2P.getRule().test(EXPENSIVE));
		assertTrue(FlipPreset.HIGH_VOLUME.getRule().test(CHEAP_BUSY));
		assertTrue(FlipPreset.BULK_MARGIN.getRule().test(CHEAP_BUSY));
		assertTrue(FlipPreset.STABLE.getRule().test(CHEAP_BUSY));
		assertTrue(FlipPreset.MOMENTUM.getRule().test(EXPENSIVE));
		assertTrue(FlipPreset.PRICE_DIP.getRule().test(flip(500, 20, 10, false, 75, 10, 50, Signal.DUMPING, -1, -3, 1)));
		assertTrue(FlipPreset.VOLUME_SPIKE.getRule().test(flip(500, 20, 10, false, 75, 10, 50, Signal.STABLE, 0, 0, 2.5)));
	}

	@Test
	public void listFilters()
	{
		FredFlipsConfig none = config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.ANY, false, false, false, 0);
		assertTrue(FlipFinder.passesListFilters(EXPENSIVE, none));

		assertFalse(FlipFinder.passesListFilters(CHEAP_BUSY, config(FlipPreset.ALL, PriceFloor.K1, ProfitFloor.ANY, false, false, false, 0)));
		assertFalse(FlipFinder.passesListFilters(CHEAP_BUSY, config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.K100, false, false, false, 0)));
		assertFalse(FlipFinder.passesListFilters(EXPENSIVE, config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.ANY, true, false, false, 0)));
		assertFalse(FlipFinder.passesListFilters(CHEAP_BUSY, config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.ANY, false, true, false, 0)));
		// Can't afford a 5m item with 1m
		assertFalse(FlipFinder.passesListFilters(EXPENSIVE, config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.ANY, false, false, true, 1_000_000)));
		assertTrue(FlipFinder.passesListFilters(CHEAP_BUSY, config(FlipPreset.ALL, PriceFloor.ANY, ProfitFloor.ANY, false, false, true, 1_000_000)));
	}

	@Test
	public void gpPerHourUsesEstimatedFlipTime()
	{
		// 2,000 items at 5,000/hr each side: 24 min to buy + 24 min to sell = 48 min
		assertEquals(48, CHEAP_BUSY.getEstimatedMinutes());
		assertEquals(40_000L * 60 / 48, CHEAP_BUSY.getGpPerHour());
	}
}
