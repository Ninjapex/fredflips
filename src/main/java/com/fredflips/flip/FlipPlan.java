package com.fredflips.flip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Value;

/**
 * Plan mode: spreads your cash across your free GE slots using the safest profitable flips.
 * <p>
 * Rules:
 * <ul>
 *     <li>Only flips at or above the minimum confidence, with a Stable or Rising signal.
 *     Falling, spiking and dumping items are left out.</li>
 *     <li>Each slot gets an equal share of your cash, so one bad item can't sink the whole stack.</li>
 *     <li>Quantity is capped by buy limit and one hour of the slower side's trading, as in the flip list.</li>
 *     <li>The flips that make the most profit with their share win the slots.</li>
 * </ul>
 */
@Value
public class FlipPlan
{
	private static final Set<Signal> SAFE_SIGNALS = EnumSet.of(Signal.STABLE, Signal.RISING);

	List<Entry> entries;
	long capital;
	int slots;
	/**
	 * Epoch millis when the plan was built. Fills after this count towards its progress.
	 */
	long createdAt;
	/**
	 * The next-best flips that didn't make the plan, best first, used by {@link #swap}.
	 */
	List<Entry> alternatives;

	@Value
	public static class Entry
	{
		FlipCandidate flip;
		long quantity;

		public long getCost()
		{
			return flip.getBuyPrice() * quantity;
		}

		public long getExpectedProfit()
		{
			return flip.getMargin() * quantity;
		}
	}

	public long getCapitalUsed()
	{
		long used = 0;
		for (Entry entry : entries)
		{
			used += entry.getCost();
		}
		return used;
	}

	public long getExpectedProfit()
	{
		long profit = 0;
		for (Entry entry : entries)
		{
			profit += entry.getExpectedProfit();
		}
		return profit;
	}

	/**
	 * @param candidates every flip that passed the normal filters, any order
	 */
	public static FlipPlan build(List<FlipCandidate> candidates, long capital, int freeSlots, int minConfidence)
	{
		List<Entry> options = new ArrayList<>();
		if (capital > 0 && freeSlots > 0)
		{
			long share = capital / freeSlots;
			for (FlipCandidate flip : candidates)
			{
				if (flip.getConfidence() < minConfidence || !SAFE_SIGNALS.contains(flip.getSignal()) || flip.getMargin() <= 0)
				{
					continue;
				}
				long quantity = FlipFinder.suggestedQuantity(flip.getBuyLimit(), share, flip.getBuyPrice(), flip.getSlowerSidePerHour());
				if (quantity >= 1)
				{
					options.add(new Entry(flip, quantity));
				}
			}
		}

		options.sort(Comparator.comparingLong(Entry::getExpectedProfit).reversed());
		int count = Math.min(freeSlots, options.size());
		List<Entry> chosen = new ArrayList<>(options.subList(0, count));
		List<Entry> spare = new ArrayList<>(options.subList(count, options.size()));
		return new FlipPlan(chosen, capital, freeSlots, System.currentTimeMillis(), spare);
	}

	/**
	 * Replaces one item with the best flip not already in the plan. Everything else, including progress
	 * (which counts fills since the plan was built), stays as it is. Returns this plan if there's nothing to swap in.
	 */
	public FlipPlan swap(int itemId)
	{
		if (alternatives.isEmpty())
		{
			return this;
		}
		List<Entry> updated = new ArrayList<>();
		List<Entry> spare = new ArrayList<>(alternatives);
		for (Entry entry : entries)
		{
			if (entry.getFlip().getItemId() == itemId && !spare.isEmpty())
			{
				updated.add(spare.remove(0));
			}
			else
			{
				updated.add(entry);
			}
		}
		return new FlipPlan(updated, capital, slots, createdAt, spare);
	}
}
