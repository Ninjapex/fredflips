package com.fredflips.money;

import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MoneyMakerCalculatorTest
{
	private static final long NOW = 1_800_000_000L;
	private static final int RING = 1637;
	private static final int COSMIC = 564;
	private static final int RECOIL = 2550;

	// Enchanting sapphire rings: 1,600 casts an hour.
	private static final MoneyMethod ENCHANT = new MoneyMethod("Enchanting sapphire rings", "Money making guide/Enchanting sapphire rings",
		"Skilling", true, Collections.singletonList(new MoneyMethod.Requirement("Magic", 7)), 1600.0,
		Arrays.asList(new MoneyMethod.Ingredient(RING, "Sapphire ring", 1600), new MoneyMethod.Ingredient(COSMIC, "Cosmic rune", 1600)),
		Collections.singletonList(new MoneyMethod.Ingredient(RECOIL, "Ring of recoil", 1600)));

	private static Map<Integer, ItemMapping> items(int ringLimit)
	{
		Map<Integer, ItemMapping> items = new HashMap<>();
		items.put(RING, new ItemMapping(RING, "Sapphire ring", false, ringLimit));
		items.put(COSMIC, new ItemMapping(COSMIC, "Cosmic rune", false, 25_000));
		items.put(RECOIL, new ItemMapping(RECOIL, "Ring of recoil", true, 10_000));
		return items;
	}

	private static MarketSnapshot snapshot(long recoilVolume)
	{
		Map<Integer, LatestPrice> latest = new HashMap<>();
		// high = instant-buy price, low = instant-sell price
		latest.put(RING, new LatestPrice(900L, NOW - 60, 880L, NOW - 60));
		latest.put(COSMIC, new LatestPrice(110L, NOW - 60, 105L, NOW - 60));
		latest.put(RECOIL, new LatestPrice(1_300L, NOW - 60, 1_250L, NOW - 60));
		Map<Integer, AveragePrice> hour = new HashMap<>();
		hour.put(RECOIL, new AveragePrice(1_300L, recoilVolume / 2, 1_250L, recoilVolume / 2));
		hour.put(RING, new AveragePrice(900L, 5_000, 880L, 5_000));
		hour.put(COSMIC, new AveragePrice(110L, 50_000, 105L, 50_000));
		return new MarketSnapshot(Collections.emptyList(), latest, new HashMap<>(), hour, new HashMap<>(), NOW);
	}

	@Test
	public void instantGpPerHourUsesWorstPricesAndTax()
	{
		MoneyResult result = MoneyMakerCalculator.calculate(ENCHANT, snapshot(10_000), items(10_000), Collections.emptySet(), 15);

		// Cost: 1,600 x (900 + 110) = 1,616,000
		assertEquals(1_616_000, result.getCostPerHour());
		// Sell recoils instantly at 1,250, tax 25 each: 1,600 x 1,225 = 1,960,000
		assertEquals(1_960_000 - 1_616_000, result.getInstantGpPerHour());
		assertEquals(1_600 * 25, result.getTaxPerHour());
		// With offers: buy at 880 + 105, sell at 1,300 (tax 26): 1,600 x (1,274 - 985)
		assertEquals(1_600 * (1_274 - 985), result.getOffersGpPerHour());
		assertFalse(result.isStalePrices());
	}

	@Test
	public void buyLimitCapsHourlyProfit()
	{
		// Ring limit 2,000 per 4 hours = 500 an hour, but the method uses 1,600 an hour.
		MoneyResult result = MoneyMakerCalculator.calculate(ENCHANT, snapshot(10_000), items(2_000), Collections.emptySet(), 15);

		assertEquals("Sapphire ring", result.getLimitingInput());
		assertEquals(500.0 / 1_600, result.getBuyLimitShare(), 1e-9);
		assertEquals(Math.round(result.getInstantGpPerHour() * 500.0 / 1_600), result.getRealisticGpPerHour());
	}

	@Test
	public void flagsOutputsTheMarketCantAbsorb()
	{
		MoneyResult result = MoneyMakerCalculator.calculate(ENCHANT, snapshot(400), items(10_000), Collections.emptySet(), 15);
		assertEquals(Collections.singletonList("Ring of recoil"), result.getThinOutputs());
		// The market trades 400 an hour, a quarter of what you'd make
		assertEquals(0.25, result.getMarketShare(), 1e-9);
		assertEquals(Math.round(result.getInstantGpPerHour() * 0.25), result.getRealisticGpPerHour());
	}

	@Test
	public void lossesAreNeverScaledDown()
	{
		MarketSnapshot snapshot = snapshot(400);
		snapshot.getLatest().put(RECOIL, new LatestPrice(900L, NOW - 60, 850L, NOW - 60));
		MoneyResult result = MoneyMakerCalculator.calculate(ENCHANT, snapshot, items(10_000), Collections.emptySet(), 15);
		assertTrue(result.getInstantGpPerHour() < 0);
		assertEquals(result.getInstantGpPerHour(), result.getRealisticGpPerHour());
	}

	@Test
	public void processingMeansBoughtMaterialsMatter()
	{
		MoneyResult enchanting = MoneyMakerCalculator.calculate(ENCHANT, snapshot(10_000), items(10_000), Collections.emptySet(), 15);
		assertTrue(MoneyMakers.isProcessing(enchanting));
		MoneyMethod gathering = new MoneyMethod("Hunting", "p", "Skilling", true, null, null,
			Collections.singletonList(new MoneyMethod.Ingredient(COSMIC, "Cosmic rune", 1)),
			Collections.singletonList(new MoneyMethod.Ingredient(RECOIL, "Ring of recoil", 1000)));
		assertFalse(MoneyMakers.isProcessing(
			MoneyMakerCalculator.calculate(gathering, snapshot(10_000), items(10_000), Collections.emptySet(), 15)));
	}

	@Test
	public void missingPriceGivesNoResult()
	{
		MarketSnapshot snapshot = snapshot(10_000);
		snapshot.getLatest().remove(COSMIC);
		snapshot.getOneHour().remove(COSMIC);
		assertNull(MoneyMakerCalculator.calculate(ENCHANT, snapshot, items(10_000), Collections.emptySet(), 15));
	}

	@Test
	public void coinsCountAsGp()
	{
		MoneyMethod tan = new MoneyMethod("Tanning", "p", "Processing", true, null, null,
			Arrays.asList(new MoneyMethod.Ingredient(RING, "Sapphire ring", 10), new MoneyMethod.Ingredient(995, "Coins", 200)),
			Collections.singletonList(new MoneyMethod.Ingredient(RECOIL, "Ring of recoil", 10)));
		MoneyResult result = MoneyMakerCalculator.calculate(tan, snapshot(10_000), items(10_000), Collections.emptySet(), 15);
		// 10 x 1,225 - 10 x 900 - 200
		assertEquals(12_250 - 9_000 - 200, result.getInstantGpPerHour());
	}

	@Test
	public void levelFilter()
	{
		Map<String, Integer> levels = new HashMap<>();
		levels.put("magic", 6);
		assertFalse(MoneyMakers.meetsLevels(ENCHANT, levels));
		levels.put("magic", 7);
		assertTrue(MoneyMakers.meetsLevels(ENCHANT, levels));
	}

	@Test
	public void bundledListLoadsAndHasCorePerHourAmounts()
	{
		List<MoneyMethod> methods = new MoneyMakers(new Gson()).getMethods();
		assertTrue(methods.size() > 100);
		MoneyMethod sapphire = methods.stream().filter(m -> m.getName().equals("Enchanting sapphire rings")).findFirst().orElse(null);
		assertEquals(1600.0, sapphire.getActionsPerHour(), 0);
		assertEquals(1600.0, sapphire.getInputsPerHour().get(0).getQuantity(), 0);
	}
}
