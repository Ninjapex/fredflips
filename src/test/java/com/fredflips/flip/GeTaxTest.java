package com.fredflips.flip;

import com.fredflips.wiki.ItemMapping;
import java.util.Arrays;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class GeTaxTest
{
	@Test
	public void itemsUnder50gpPayNoTax()
	{
		assertEquals(0, GeTax.perItem(1, false));
		assertEquals(0, GeTax.perItem(49, false));
	}

	@Test
	public void taxIsTwoPercentRoundedDown()
	{
		// Examples straight from the wiki: 50 -> 1, 99 -> 1, 100 -> 2
		assertEquals(1, GeTax.perItem(50, false));
		assertEquals(1, GeTax.perItem(99, false));
		assertEquals(2, GeTax.perItem(100, false));
		assertEquals(20_000, GeTax.perItem(1_000_000, false));
		assertEquals(20_000, GeTax.perItem(1_000_049, false));
	}

	@Test
	public void taxIsCappedAt5mPerItem()
	{
		assertEquals(5_000_000, GeTax.perItem(250_000_000, false));
		assertEquals(5_000_000, GeTax.perItem(1_500_000_000L, false));
		assertEquals(4_999_999, GeTax.perItem(249_999_999, false));
	}

	@Test
	public void exemptItemsPayNoTax()
	{
		assertEquals(0, GeTax.perItem(10_000_000, true));
	}

	@Test
	public void exemptNamesResolveToIds()
	{
		Set<Integer> ids = GeTax.exemptIds(Arrays.asList(
			new ItemMapping(13190, "Old school bond", false, 1),
			new ItemMapping(3008, "Energy potion(4)", false, 2000),
			new ItemMapping(4151, "Abyssal whip", true, 70)));

		assertEquals(2, ids.size());
		assertTrue(ids.contains(13190));
		assertTrue(ids.contains(3008));
	}
}
