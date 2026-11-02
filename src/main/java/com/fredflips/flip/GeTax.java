package com.fredflips.flip;

import com.fredflips.wiki.ItemMapping;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Grand Exchange tax ("convenience fee") rules, from
 * https://oldschool.runescape.wiki/w/Grand_Exchange#Convenience_fee_and_item_sink
 * <p>
 * The seller pays 2% of the sale price, rounded down, capped at 5m per item.
 * Some items are exempt.
 */
public final class GeTax
{
	public static final long MAX_TAX_PER_ITEM = 5_000_000L;

	// 2% rounded down is the same as dividing by 50 with integer division.
	private static final long PRICE_PER_TAX_COIN = 50L;

	/**
	 * Exempt items, by their exact wiki /mapping name. Names are used instead of IDs so the
	 * list can be checked against the wiki page by eye. Update this when Jagex changes the list.
	 */
	static final Set<String> EXEMPT_NAMES = new HashSet<>(Arrays.asList(
		"Old school bond",
		"Energy potion(1)", "Energy potion(2)", "Energy potion(3)", "Energy potion(4)",
		// Low level combat consumables
		"Bronze arrow", "Bronze dart", "Iron arrow", "Iron dart", "Mind rune", "Steel arrow", "Steel dart",
		// Low level food
		"Bass", "Bread", "Cake", "Cooked chicken", "Cooked meat", "Herring", "Lobster", "Mackerel",
		"Meat pie", "Pike", "Salmon", "Shrimps", "Tuna",
		// Teleport items
		"Ardougne teleport (tablet)", "Camelot teleport (tablet)", "Civitas illa fortis teleport",
		"Falador teleport (tablet)", "Games necklace(8)", "Kourend castle teleport (tablet)",
		"Lumbridge teleport (tablet)", "Ring of dueling(8)", "Teleport to house (tablet)",
		"Varrock teleport (tablet)",
		// Tools
		"Chisel", "Gardening trowel", "Glassblowing pipe", "Hammer", "Needle", "Pestle and mortar",
		"Rake", "Saw", "Secateurs", "Seed dibber", "Shears", "Spade", "Watering can"
	));

	private GeTax()
	{
	}

	/**
	 * Tax taken from the seller when one item sells at {@code sellPrice}.
	 */
	public static long perItem(long sellPrice, boolean exempt)
	{
		if (exempt || sellPrice <= 0)
		{
			return 0;
		}
		return Math.min(sellPrice / PRICE_PER_TAX_COIN, MAX_TAX_PER_ITEM);
	}

	/**
	 * Turns {@link #EXEMPT_NAMES} into item IDs using the wiki's mapping data.
	 */
	public static Set<Integer> exemptIds(Collection<ItemMapping> items)
	{
		Set<Integer> ids = new HashSet<>();
		for (ItemMapping item : items)
		{
			if (EXEMPT_NAMES.contains(item.getName()))
			{
				ids.add(item.getId());
			}
		}
		return ids;
	}
}
