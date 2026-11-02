package com.fredflips.trades;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FlipLedgerTest
{
	private static Trade buy(long time, int qty, long total)
	{
		return new Trade(time, 4151, "Abyssal whip", true, qty, total);
	}

	private static Trade sell(long time, int qty, long total)
	{
		return new Trade(time, 4151, "Abyssal whip", false, qty, total);
	}

	@Test
	public void profitSubtractsCostAndTax()
	{
		FlipLedger ledger = FlipLedger.build(Arrays.asList(
			buy(1, 10, 14_500_000),
			sell(2, 10, 15_000_000)), Collections.emptySet(), 0);

		FlipLedger.CompletedFlip flip = ledger.getCompleted().get(0);
		assertEquals(10, flip.getQuantity());
		// Tax: 2% of 1,500,000 = 30,000 each
		assertEquals(300_000, flip.getTax());
		assertEquals(15_000_000 - 300_000 - 14_500_000, flip.getProfit());
		assertEquals(200_000, ledger.getTotalProfit());
		assertEquals(1, ledger.getWins());
		assertTrue(ledger.getOpen().isEmpty());
	}

	@Test
	public void matchesOldestBuysFirst()
	{
		FlipLedger ledger = FlipLedger.build(Arrays.asList(
			buy(1, 5, 5_000),       // 1,000 each
			buy(2, 5, 6_000),       // 1,200 each
			sell(3, 6, 7_800)),     // 1,300 each, tax 26 each
			Collections.emptySet(), 0);

		FlipLedger.CompletedFlip flip = ledger.getCompleted().get(0);
		// 5 at 1,000 + 1 at 1,200
		assertEquals(6_200, flip.getCost());
		assertEquals(7_800 - 156 - 6_200, flip.getProfit());

		FlipLedger.OpenPosition open = ledger.getOpen().get(0);
		assertEquals(4, open.getQuantity());
		assertEquals(4_800, open.getTotalCost());
	}

	@Test
	public void splittingLotsNeverLosesACoin()
	{
		// 3 items for 1,000 total (333.33 each), sold one at a time.
		List<Trade> trades = Arrays.asList(
			buy(1, 3, 1_000),
			sell(2, 1, 40),
			sell(3, 1, 40),
			sell(4, 1, 40));
		FlipLedger ledger = FlipLedger.build(trades, Collections.emptySet(), 0);

		long totalCost = 0;
		for (FlipLedger.CompletedFlip flip : ledger.getCompleted())
		{
			totalCost += flip.getCost();
		}
		assertEquals(1_000, totalCost);
	}

	@Test
	public void sellsWithoutTrackedBuysAreIgnored()
	{
		FlipLedger ledger = FlipLedger.build(Collections.singletonList(sell(1, 10, 15_000_000)), Collections.emptySet(), 0);
		assertTrue(ledger.getCompleted().isEmpty());
		assertEquals(0, ledger.getTotalProfit());
	}

	@Test
	public void taxExemptItemsPayNoTax()
	{
		FlipLedger ledger = FlipLedger.build(Arrays.asList(
			buy(1, 1, 10_000_000),
			sell(2, 1, 10_100_000)), Collections.singleton(4151), 0);
		assertEquals(0, ledger.getTaxPaid());
		assertEquals(100_000, ledger.getTotalProfit());
	}

	@Test
	public void lossesAreCounted()
	{
		FlipLedger ledger = FlipLedger.build(Arrays.asList(
			buy(1, 1, 1_000_000),
			sell(2, 1, 990_000)), Collections.emptySet(), 0);
		assertEquals(1, ledger.getLosses());
		assertEquals(990_000 - 19_800 - 1_000_000, ledger.getTotalProfit());
	}

	@Test
	public void profitSinceOnlyCountsRecentSells()
	{
		FlipLedger ledger = FlipLedger.build(Arrays.asList(
			buy(1, 2, 2_000),
			sell(5, 1, 1_100),
			sell(20, 1, 1_100)), Collections.emptySet(), 10);
		assertEquals(1_100 - 22 - 1_000, ledger.getProfitSince());
	}
}
