package com.fredflips.flip;

import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.LatestPrice;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FlipScoreTest
{
	private static final AveragePrice HOUR = new AveragePrice(1_050L, 1_000, 1_000L, 1_000);
	private static final AveragePrice DAY = new AveragePrice(1_050L, 24_000, 1_000L, 24_000);

	@Test
	public void idealFlipScoresFullMarks()
	{
		// Margin 50 - 21 tax - 0 = 29 at average prices; ours is the same; perfectly liquid, fresh, flat.
		int score = FlipScore.confidence(29, false, 1_000, 1_000, 0, HOUR, DAY);
		assertEquals(100, score);
		assertEquals(Tier.S, Tier.fromConfidence(score));
	}

	@Test
	public void thinStaleMarketsScoreLow()
	{
		int score = FlipScore.confidence(29, false, 2, 40, 25, HOUR, DAY);
		assertTrue("score was " + score, score < 55);
		assertEquals(Tier.C, Tier.fromConfidence(score));
	}

	@Test
	public void marginThatOnlyExistsRightNowScoresLower()
	{
		int real = FlipScore.confidence(29, false, 1_000, 1_000, 0, HOUR, DAY);
		// Our margin claims 200 but the averages only ever showed 29.
		int suspicious = FlipScore.confidence(200, false, 1_000, 1_000, 0, HOUR, DAY);
		assertTrue(suspicious < real);
	}

	@Test
	public void tiers()
	{
		assertEquals(Tier.S, Tier.fromConfidence(85));
		assertEquals(Tier.A, Tier.fromConfidence(84));
		assertEquals(Tier.A, Tier.fromConfidence(70));
		assertEquals(Tier.B, Tier.fromConfidence(55));
		assertEquals(Tier.C, Tier.fromConfidence(54));
	}

	@Test
	public void signals()
	{
		LatestPrice normal = new LatestPrice(1_050L, 0L, 1_000L, 0L);
		assertEquals(Signal.STABLE, FlipScore.signal(normal, HOUR, DAY));

		LatestPrice spike = new LatestPrice(1_200L, 0L, 1_000L, 0L);
		assertEquals(Signal.SPIKING, FlipScore.signal(spike, HOUR, DAY));

		LatestPrice dump = new LatestPrice(1_050L, 0L, 900L, 0L);
		assertEquals(Signal.DUMPING, FlipScore.signal(dump, HOUR, DAY));

		AveragePrice hourUp = new AveragePrice(1_080L, 1_000, 1_030L, 1_000);
		assertEquals(Signal.RISING, FlipScore.signal(normal, hourUp, DAY));

		AveragePrice hourDown = new AveragePrice(1_020L, 1_000, 970L, 1_000);
		assertEquals(Signal.FALLING, FlipScore.signal(normal, hourDown, DAY));

		assertEquals(Signal.UNKNOWN, FlipScore.signal(normal, HOUR, null));
	}

	@Test
	public void planSkipsRiskySignalsAndSplitsCash()
	{
		FlipCandidate safe = candidate(1, 100, 1_000, 80, Signal.STABLE);
		FlipCandidate falling = candidate(2, 500, 1_000, 90, Signal.FALLING);
		FlipCandidate lowConfidence = candidate(3, 500, 1_000, 40, Signal.STABLE);

		FlipPlan plan = FlipPlan.build(Arrays.asList(safe, falling, lowConfidence), 1_000_000, 4, 55);

		assertEquals(1, plan.getEntries().size());
		assertEquals(1, plan.getEntries().get(0).getFlip().getItemId());
		// Share per slot: 250,000 / 1,000 each = 250, under the 1,000 limit and volume
		assertEquals(250, plan.getEntries().get(0).getQuantity());
		assertEquals(25_000, plan.getExpectedProfit());
	}

	@Test
	public void planUsesAtMostTheFreeSlots()
	{
		FlipPlan plan = FlipPlan.build(Arrays.asList(
			candidate(1, 100, 1_000, 80, Signal.STABLE),
			candidate(2, 90, 1_000, 80, Signal.RISING),
			candidate(3, 80, 1_000, 80, Signal.STABLE)), 3_000_000, 2, 55);
		assertEquals(2, plan.getEntries().size());
		assertTrue(plan.getCapitalUsed() <= 3_000_000);
	}

	@Test
	public void swapReplacesOneItemWithTheNextBest()
	{
		FlipPlan plan = FlipPlan.build(Arrays.asList(
			candidate(1, 100, 1_000, 80, Signal.STABLE),
			candidate(2, 90, 1_000, 80, Signal.STABLE),
			candidate(3, 80, 1_000, 80, Signal.STABLE)), 3_000_000, 2, 55);
		assertEquals(2, plan.getEntries().size());

		FlipPlan swapped = plan.swap(1);
		assertEquals(3, swapped.getEntries().get(0).getFlip().getItemId());
		assertEquals(2, swapped.getEntries().get(1).getFlip().getItemId());
		assertEquals(plan.getCreatedAt(), swapped.getCreatedAt());
		// Nothing left to swap in
		assertEquals(swapped, swapped.swap(2));
	}

	@Test
	public void planNeedsCash()
	{
		assertTrue(FlipPlan.build(Collections.singletonList(candidate(1, 100, 1_000, 80, Signal.STABLE)), 0, 8, 55)
			.getEntries().isEmpty());
	}

	private static FlipCandidate candidate(int id, long margin, long buyPrice, int confidence, Signal signal)
	{
		return new FlipCandidate(id, "item" + id, false, 1_000, buyPrice, buyPrice + margin + 30, 30, margin,
			margin * 100.0 / buyPrice, 1_000, margin * 1_000, 5_000, 5_000, 100_000, 0, 0,
			confidence, Tier.fromConfidence(confidence), signal, 0, 0, 1);
	}
}
