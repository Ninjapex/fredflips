package com.fredflips.trades;

import com.fredflips.flip.GeTax;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * Pairs your sells with your earlier buys (first in, first out) to work out real profit after tax.
 * Sells of items you bought before tracking started are left out of the profit figures.
 */
@Value
public class FlipLedger
{
	List<CompletedFlip> completed;
	List<OpenPosition> open;
	long totalProfit;
	long profitSince;
	long taxPaid;
	int wins;
	int losses;

	@Value
	public static class CompletedFlip
	{
		int itemId;
		String name;
		int quantity;
		long cost;
		/**
		 * What the buyers paid, before tax.
		 */
		long revenue;
		long tax;
		/**
		 * Epoch millis of the sell.
		 */
		long time;

		public long getProfit()
		{
			return revenue - tax - cost;
		}
	}

	@Value
	public static class OpenPosition
	{
		int itemId;
		String name;
		int quantity;
		long totalCost;
		/**
		 * Epoch millis of the oldest unsold buy.
		 */
		long boughtAt;

		public long getAverageCost()
		{
			return quantity == 0 ? 0 : totalCost / quantity;
		}
	}

	/**
	 * @param trades     every recorded trade, in any order
	 * @param taxExempt  item IDs that pay no GE tax
	 * @param sinceMillis profit after this time is also reported separately (e.g. the start of today)
	 */
	public static FlipLedger build(List<Trade> trades, Set<Integer> taxExempt, long sinceMillis)
	{
		List<Trade> ordered = new ArrayList<>(trades);
		ordered.sort(Comparator.comparingLong(Trade::getTime));

		Map<Integer, Deque<Lot>> lotsByItem = new HashMap<>();
		Map<Integer, String> names = new HashMap<>();
		List<CompletedFlip> completed = new ArrayList<>();

		for (Trade trade : ordered)
		{
			names.put(trade.getItemId(), trade.getName());
			Deque<Lot> lots = lotsByItem.computeIfAbsent(trade.getItemId(), id -> new ArrayDeque<>());
			if (trade.isBuy())
			{
				lots.addLast(new Lot(trade.getQuantity(), trade.getGpTotal(), trade.getTime()));
				continue;
			}

			int toMatch = trade.getQuantity();
			long cost = 0;
			int matched = 0;
			while (toMatch > 0 && !lots.isEmpty())
			{
				Lot lot = lots.peekFirst();
				int take = Math.min(toMatch, lot.quantity);
				cost += lot.takeCost(take);
				matched += take;
				toMatch -= take;
				if (lot.quantity == 0)
				{
					lots.removeFirst();
				}
			}
			if (matched == 0)
			{
				continue;
			}

			long priceEach = trade.getPriceEach();
			long revenue = trade.getGpTotal() * matched / trade.getQuantity();
			long tax = GeTax.perItem(priceEach, taxExempt.contains(trade.getItemId())) * matched;
			completed.add(new CompletedFlip(trade.getItemId(), trade.getName(), matched, cost, revenue, tax, trade.getTime()));
		}

		List<OpenPosition> open = new ArrayList<>();
		for (Map.Entry<Integer, Deque<Lot>> entry : lotsByItem.entrySet())
		{
			int quantity = 0;
			long cost = 0;
			long oldest = Long.MAX_VALUE;
			for (Lot lot : entry.getValue())
			{
				quantity += lot.quantity;
				cost += lot.remainingCost;
				oldest = Math.min(oldest, lot.time);
			}
			if (quantity > 0)
			{
				open.add(new OpenPosition(entry.getKey(), names.get(entry.getKey()), quantity, cost, oldest));
			}
		}
		open.sort(Comparator.comparingLong(OpenPosition::getBoughtAt));

		long total = 0;
		long since = 0;
		long tax = 0;
		int wins = 0;
		int losses = 0;
		for (CompletedFlip flip : completed)
		{
			total += flip.getProfit();
			tax += flip.getTax();
			if (flip.getTime() >= sinceMillis)
			{
				since += flip.getProfit();
			}
			if (flip.getProfit() > 0)
			{
				wins++;
			}
			else if (flip.getProfit() < 0)
			{
				losses++;
			}
		}
		completed.sort(Comparator.comparingLong(CompletedFlip::getTime).reversed());
		return new FlipLedger(completed, open, total, since, tax, wins, losses);
	}

	/**
	 * Some bought items that haven't been matched to a sell yet. Cost is tracked as a running
	 * total so splitting a lot never loses or invents a coin to rounding.
	 */
	private static class Lot
	{
		int quantity;
		long remainingCost;
		final long time;

		Lot(int quantity, long cost, long time)
		{
			this.quantity = quantity;
			this.remainingCost = cost;
			this.time = time;
		}

		long takeCost(int take)
		{
			long cost = take == quantity ? remainingCost : remainingCost * take / quantity;
			quantity -= take;
			remainingCost -= cost;
			return cost;
		}
	}
}
