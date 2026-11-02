package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class FlipFinderTest
{
	private static final long NOW = 1_800_000_000L;
	private static final int WHIP = 4151;
	private static final ItemMapping WHIP_ITEM = new ItemMapping(WHIP, "Abyssal whip", true, 70);
	private static final FredFlipsConfig DEFAULTS = new FredFlipsConfig()
	{
	};

	@Test
	public void marginSubtractsTaxAndOneGpUndercutEachSide()
	{
		FlipCandidate flip = evaluate(
			latest(1_500_000, 1_450_000, 60),
			average(1_500_000, 100, 1_450_000, 100),
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS);

		assertEquals(1_450_001, flip.getBuyPrice());
		assertEquals(1_499_999, flip.getSellPrice());
		assertEquals(29_999, flip.getTax());
		// 1,499,999 - 29,999 - 1,450,001
		assertEquals(19_999, flip.getMargin());
		// Whip limit is 70, both sides trade 500/hr, no cash cap -> 70
		assertEquals(70, flip.getQuantity());
		assertEquals(19_999L * 70, flip.getPotentialProfit());
	}

	@Test
	public void singleSpikeTradeCannotInflateTheMargin()
	{
		// Someone instant-bought one whip for 2m, but the recent average is 1.5m.
		// The sell price must use the lower average, not the spike.
		FlipCandidate flip = evaluate(
			latest(2_000_000, 1_450_000, 60),
			average(1_500_000, 100, 1_450_000, 100),
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS);

		assertEquals(1_499_999, flip.getSellPrice());
	}

	@Test
	public void singleDumpTradeCannotLowerTheBuyPrice()
	{
		// Someone panic-sold one whip for 1m. We can't expect to buy at that price again.
		FlipCandidate flip = evaluate(
			latest(1_500_000, 1_000_000, 60),
			average(1_500_000, 100, 1_450_000, 100),
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS);

		assertEquals(1_450_001, flip.getBuyPrice());
	}

	@Test
	public void spikeLastingFiveMinutesCannotInflateTheMargin()
	{
		// Real case seen on live data: 8 buys at 40k in the last 5 minutes pushed the
		// 5 minute average up too, but the item normally sells around 29k.
		FlipCandidate flip = evaluate(
			latest(40_004, 23_139, 60),
			average(40_004, 8, 23_713, 3),
			average(29_213, 12, 25_095, 10),
			DEFAULTS);

		assertEquals(25_096, flip.getBuyPrice());
		assertEquals(29_212, flip.getSellPrice());
	}

	@Test
	public void spikeThatErasesTheRealMarginIsHidden()
	{
		// Real case: Forgotten brew(4) spiked to 2,550 but averaged 2,181 over the hour,
		// while the last instant-sell was 2,266. Buying at 2,267 would lose money.
		assertNull(evaluate(
			latest(2_550, 2_266, 60),
			new AveragePrice(2_550L, 11, null, 0),
			average(2_181, 2_036, 2_105, 695),
			DEFAULTS));
	}

	@Test
	public void fallsBackToHourAverageWhenNothingTradedInFiveMinutes()
	{
		FlipCandidate flip = evaluate(
			latest(2_000_000, 1_000_000, 60),
			null,
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS);

		assertEquals(1_450_001, flip.getBuyPrice());
		assertEquals(1_499_999, flip.getSellPrice());
	}

	@Test
	public void unprofitableAfterTaxIsHidden()
	{
		// 1% spread is less than the 2% tax
		assertNull(evaluate(
			latest(1_010_000, 1_000_000, 60),
			average(1_010_000, 100, 1_000_000, 100),
			average(1_010_000, 500, 1_000_000, 500),
			DEFAULTS));
	}

	@Test
	public void invertedPricesAreHidden()
	{
		assertNull(evaluate(
			latest(1_000_000, 1_100_000, 60),
			average(1_000_000, 100, 1_100_000, 100),
			average(1_000_000, 500, 1_100_000, 500),
			DEFAULTS));
	}

	@Test
	public void stalePricesAreHidden()
	{
		// Last trade 20 minutes ago, default max age is 15
		assertNull(evaluate(
			latest(1_500_000, 1_450_000, 20 * 60),
			average(1_500_000, 100, 1_450_000, 100),
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS));
	}

	@Test
	public void thinMarketsAreHidden()
	{
		// Plenty of buyers but only 3 sellers in the last hour
		assertNull(evaluate(
			latest(1_500_000, 1_450_000, 60),
			average(1_500_000, 100, 1_450_000, 1),
			average(1_500_000, 500, 1_450_000, 3),
			DEFAULTS));
	}

	@Test
	public void missingPricesAreHidden()
	{
		assertNull(evaluate(
			new LatestPrice(1_500_000L, NOW - 60, null, null),
			null,
			average(1_500_000, 500, 1_450_000, 500),
			DEFAULTS));
	}

	@Test
	public void spikingItemsAreHiddenByDefault()
	{
		ItemMapping item = new ItemMapping(1119, "Iron platebody", false, 125);
		// Daily average ~55 but it's trading at 114 now: a pump that could reverse.
		MarketSnapshot snapshot = new MarketSnapshot(
			Collections.singletonList(item),
			Collections.singletonMap(1119, latest(140, 113, 60)),
			new HashMap<>(),
			Collections.singletonMap(1119, average(140, 500, 113, 500)),
			Collections.singletonMap(1119, average(56, 5000, 54, 5000)),
			NOW);

		assertNull(FlipFinder.evaluate(item, snapshot, false, DEFAULTS));

		FredFlipsConfig showRisky = new FredFlipsConfig()
		{
			@Override
			public boolean hideRiskySignals()
			{
				return false;
			}
		};
		FlipCandidate flip = FlipFinder.evaluate(item, snapshot, false, showRisky);
		assertEquals(Signal.SPIKING, flip.getSignal());
	}

	@Test
	public void membersItemsCanBeHidden()
	{
		FredFlipsConfig f2pOnly = new FredFlipsConfig()
		{
			@Override
			public boolean membersItems()
			{
				return false;
			}
		};

		assertNull(evaluate(
			latest(1_500_000, 1_450_000, 60),
			average(1_500_000, 100, 1_450_000, 100),
			average(1_500_000, 500, 1_450_000, 500),
			f2pOnly));
	}

	@Test
	public void quantityRespectsLimitCashAndVolume()
	{
		// Limited by slower side's hourly volume
		assertEquals(40, FlipFinder.suggestedQuantity(70, 0, 1_000, 40));
		// Limited by buy limit
		assertEquals(70, FlipFinder.suggestedQuantity(70, 0, 1_000, 500));
		// Limited by cash: 100k / 1,001 = 99
		assertEquals(99, FlipFinder.suggestedQuantity(0, 100_000, 1_001, 500));
		// Can't afford even one
		assertEquals(0, FlipFinder.suggestedQuantity(70, 500, 1_001, 500));
	}

	@Test
	public void taxExemptItemsKeepTheWholeSpread()
	{
		ItemMapping bond = new ItemMapping(13190, "Old school bond", false, 100);
		MarketSnapshot snapshot = snapshot(bond,
			latest(10_000_000, 9_900_000, 60),
			null,
			average(10_000_000, 50, 9_900_000, 50));

		List<FlipCandidate> flips = FlipFinder.find(snapshot, DEFAULTS);

		assertEquals(1, flips.size());
		assertEquals(0, flips.get(0).getTax());
		assertEquals(9_999_999 - 9_900_001, flips.get(0).getMargin());
	}

	private static FlipCandidate evaluate(LatestPrice latest, AveragePrice fiveMinute, AveragePrice hour, FredFlipsConfig config)
	{
		return FlipFinder.evaluate(WHIP_ITEM, snapshot(WHIP_ITEM, latest, fiveMinute, hour), false, config);
	}

	private static MarketSnapshot snapshot(ItemMapping item, LatestPrice latest, AveragePrice fiveMinute, AveragePrice hour)
	{
		Map<Integer, AveragePrice> fiveMinuteMap = new HashMap<>();
		if (fiveMinute != null)
		{
			fiveMinuteMap.put(item.getId(), fiveMinute);
		}
		return new MarketSnapshot(
			Collections.singletonList(item),
			Collections.singletonMap(item.getId(), latest),
			fiveMinuteMap,
			Collections.singletonMap(item.getId(), hour),
			Collections.emptyMap(),
			NOW);
	}

	private static LatestPrice latest(long high, long low, long secondsAgo)
	{
		return new LatestPrice(high, NOW - secondsAgo, low, NOW - secondsAgo);
	}

	private static AveragePrice average(long avgHigh, long highVolume, long avgLow, long lowVolume)
	{
		return new AveragePrice(avgHigh, highVolume, avgLow, lowVolume);
	}
}
