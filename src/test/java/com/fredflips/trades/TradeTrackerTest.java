package com.fredflips.trades;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TradeTrackerTest
{
	private static OfferSnapshot buy(int sold, long spent, OfferStatus status)
	{
		return new OfferSnapshot(4151, "Abyssal whip", true, 1_500_000, 10, sold, spent, status, 0, 0);
	}

	@Test
	public void recordsOnlyTheNewFillsEachUpdate()
	{
		TradeTracker tracker = new TradeTracker();
		assertTrue(tracker.onSlotChanged(0, buy(0, 0, OfferStatus.BUYING), 1000).isEmpty());

		List<Trade> first = tracker.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.BUYING), 2000);
		assertEquals(1, first.size());
		assertEquals(3, first.get(0).getQuantity());
		assertEquals(4_500_000, first.get(0).getGpTotal());

		List<Trade> second = tracker.onSlotChanged(0, buy(10, 15_000_000, OfferStatus.BOUGHT), 3000);
		assertEquals(7, second.get(0).getQuantity());
		assertEquals(10_500_000, second.get(0).getGpTotal());
	}

	@Test
	public void repeatedUpdateWithNoChangeRecordsNothing()
	{
		TradeTracker tracker = new TradeTracker();
		tracker.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.BUYING), 1000);
		// The game replays every slot on login; that must not count the same fills twice.
		assertTrue(tracker.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.BUYING), 2000).isEmpty());
	}

	@Test
	public void fillsWhileLoggedOutAreCountedOnceFromSavedState()
	{
		TradeTracker before = new TradeTracker();
		before.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.BUYING), 1000);

		TradeTracker after = new TradeTracker();
		after.restore(new java.util.HashMap<>(before.getSlots()));
		List<Trade> trades = after.onSlotChanged(0, buy(8, 12_000_000, OfferStatus.BUYING), 5000);

		assertEquals(5, trades.get(0).getQuantity());
	}

	@Test
	public void newOfferInSameSlotStartsFromZero()
	{
		TradeTracker tracker = new TradeTracker();
		tracker.onSlotChanged(0, buy(10, 15_000_000, OfferStatus.BOUGHT), 1000);
		// Collected on mobile (no EMPTY seen), then an identical new offer partly filled.
		List<Trade> trades = tracker.onSlotChanged(0, buy(2, 3_000_000, OfferStatus.BUYING), 2000);
		assertEquals(2, trades.get(0).getQuantity());
		assertEquals(2000, tracker.getSlots().get(0).getPlacedAt());
	}

	@Test
	public void cancellingKeepsFillsAndRecordsNothingNew()
	{
		TradeTracker tracker = new TradeTracker();
		tracker.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.BUYING), 1000);
		assertTrue(tracker.onSlotChanged(0, buy(3, 4_500_000, OfferStatus.CANCELLED_BUY), 2000).isEmpty());
	}

	@Test
	public void tracksPlacedAndLastFillTimes()
	{
		TradeTracker tracker = new TradeTracker();
		tracker.onSlotChanged(0, buy(0, 0, OfferStatus.BUYING), 1000);
		tracker.onSlotChanged(0, buy(1, 1_500_000, OfferStatus.BUYING), 5000);
		tracker.onSlotChanged(0, buy(1, 1_500_000, OfferStatus.BUYING), 9000);

		OfferSnapshot slot = tracker.getSlots().get(0);
		assertEquals(1000, slot.getPlacedAt());
		assertEquals(5000, slot.getLastFillAt());
	}

	@Test
	public void emptiedSlotIsForgotten()
	{
		TradeTracker tracker = new TradeTracker();
		tracker.onSlotChanged(2, buy(10, 15_000_000, OfferStatus.BOUGHT), 1000);
		tracker.onSlotEmptied(2);
		assertTrue(tracker.getSlots().isEmpty());
	}
}
