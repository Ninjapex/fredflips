package com.fredflips.trades;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the game's GE slot updates into individual trades.
 * <p>
 * The game only tells us each slot's running total ("37 of 100 bought for 3,700gp"), so we remember
 * the last total per slot and record the difference. Slot memory is saved per account, so fills that
 * happen while you're logged out show up as one trade when you log back in.
 * <p>
 * This class has no RuneLite dependencies so it can be unit tested.
 */
public class TradeTracker
{
	public static final int SLOT_COUNT = 8;

	private final Map<Integer, OfferSnapshot> slots = new HashMap<>();

	public Map<Integer, OfferSnapshot> getSlots()
	{
		return Collections.unmodifiableMap(slots);
	}

	public void restore(Map<Integer, OfferSnapshot> saved)
	{
		slots.clear();
		if (saved != null)
		{
			slots.putAll(saved);
		}
	}

	/**
	 * A slot was emptied (offer collected or never used).
	 */
	public void onSlotEmptied(int slot)
	{
		slots.remove(slot);
	}

	/**
	 * A slot changed. {@code current} must have everything except placedAt and lastFillAt filled in.
	 *
	 * @return the new fill, if any
	 */
	public List<Trade> onSlotChanged(int slot, OfferSnapshot current, long now)
	{
		OfferSnapshot previous = slots.get(slot);
		boolean sameOffer = current.isSameOfferAs(previous);

		int alreadyCounted = sameOffer ? previous.getQuantitySold() : 0;
		long alreadySpent = sameOffer ? previous.getSpent() : 0;
		int newQuantity = current.getQuantitySold() - alreadyCounted;
		long newSpent = current.getSpent() - alreadySpent;

		current.setPlacedAt(sameOffer ? previous.getPlacedAt() : now);
		current.setLastFillAt(sameOffer ? previous.getLastFillAt() : 0);
		if (newQuantity > 0)
		{
			current.setLastFillAt(now);
		}
		slots.put(slot, current);

		if (newQuantity <= 0 || newSpent <= 0)
		{
			return Collections.emptyList();
		}
		return Collections.singletonList(new Trade(now, current.getItemId(), current.getName(), current.isBuy(), newQuantity, newSpent));
	}
}
