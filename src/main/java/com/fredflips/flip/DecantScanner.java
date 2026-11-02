package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Value;

/**
 * Potion decanting: buy the dose size that's cheapest per dose, decant it for free at the GE (Bob Barter),
 * and sell the dose size that's worth most per dose.
 * <p>
 * Prices are cautious, like the flip list: buying uses the higher of the latest and hourly instant-sell
 * price plus 1gp, selling uses the lower of the latest and hourly instant-buy price minus 1gp, after tax.
 */
public final class DecantScanner
{
	private static final Pattern DOSES = Pattern.compile("^(.*)\\((\\d)\\)$");
	// Potions have at most 4 doses. Charged jewellery like Amulet of glory(6) uses the same naming but can't be decanted.
	private static final int MAX_POTION_DOSES = 4;
	private static final long SECONDS_PER_MINUTE = 60;

	@Value
	public static class Option
	{
		String potion;
		int buyItemId;
		int buyDoses;
		long buyPrice;
		int sellItemId;
		int sellDoses;
		long sellPrice;
		/**
		 * Profit for each potion you sell, after tax, counting the doses it took to make.
		 */
		long profitPerPotion;
		double roiPercent;
		long volumePerHour;
	}

	private DecantScanner()
	{
	}

	public static List<Option> scan(MarketSnapshot snapshot, FredFlipsConfig config)
	{
		Set<Integer> taxExempt = GeTax.exemptIds(snapshot.getItems());
		Map<String, List<ItemMapping>> byPotion = new HashMap<>();
		Map<Integer, Integer> doses = new HashMap<>();
		for (ItemMapping item : snapshot.getItems())
		{
			Matcher matcher = DOSES.matcher(item.getName());
			if (matcher.matches() && (config.membersItems() || !item.isMembers()))
			{
				byPotion.computeIfAbsent(matcher.group(1).trim(), name -> new ArrayList<>()).add(item);
				doses.put(item.getId(), Integer.parseInt(matcher.group(2)));
			}
		}

		List<Option> options = new ArrayList<>();
		for (Map.Entry<String, List<ItemMapping>> potion : byPotion.entrySet())
		{
			Option best = bestOption(potion.getKey(), potion.getValue(), doses, snapshot, taxExempt, config);
			if (best != null && best.getProfitPerPotion() >= config.minMargin())
			{
				options.add(best);
			}
		}
		options.sort(Comparator.comparingDouble(Option::getRoiPercent).reversed());
		return options.size() > config.maxResults() ? new ArrayList<>(options.subList(0, config.maxResults())) : options;
	}

	private static Option bestOption(String potion, List<ItemMapping> sizes, Map<Integer, Integer> doses,
		MarketSnapshot snapshot, Set<Integer> taxExempt, FredFlipsConfig config)
	{
		if (sizes.size() < 2)
		{
			return null;
		}
		for (ItemMapping size : sizes)
		{
			if (doses.get(size.getId()) > MAX_POTION_DOSES)
			{
				return null;
			}
		}
		ItemMapping cheapest = null;
		double cheapestPerDose = Double.MAX_VALUE;
		long cheapestPrice = 0;
		ItemMapping dearest = null;
		double dearestPerDose = 0;
		long dearestPrice = 0;
		long volume = 0;

		for (ItemMapping size : sizes)
		{
			LatestPrice latest = snapshot.getLatest().get(size.getId());
			AveragePrice hour = snapshot.getOneHour().get(size.getId());
			if (latest == null || hour == null || latest.getHigh() == null || latest.getLow() == null
				|| latest.getHighTime() == null || latest.getLowTime() == null
				|| (snapshot.getFetchedAt() - Math.min(latest.getHighTime(), latest.getLowTime())) / SECONDS_PER_MINUTE > config.maxPriceAgeMinutes())
			{
				continue;
			}
			int dose = doses.get(size.getId());
			long buy = (hour.getAvgLowPrice() == null ? latest.getLow() : Math.max(latest.getLow(), hour.getAvgLowPrice())) + 1;
			long sell = (hour.getAvgHighPrice() == null ? latest.getHigh() : Math.min(latest.getHigh(), hour.getAvgHighPrice())) - 1;
			long sellAfterTax = sell - GeTax.perItem(sell, taxExempt.contains(size.getId()));

			if (hour.getLowPriceVolume() >= config.minTradesPerHour() && (double) buy / dose < cheapestPerDose)
			{
				cheapest = size;
				cheapestPerDose = (double) buy / dose;
				cheapestPrice = buy;
			}
			if (hour.getHighPriceVolume() >= config.minTradesPerHour() && (double) sellAfterTax / dose > dearestPerDose)
			{
				dearest = size;
				dearestPerDose = (double) sellAfterTax / dose;
				dearestPrice = sell;
			}
			volume += hour.getLowPriceVolume() + hour.getHighPriceVolume();
		}

		if (cheapest == null || dearest == null || cheapest.getId() == dearest.getId())
		{
			return null;
		}
		int sellDoses = doses.get(dearest.getId());
		long costPerPotion = Math.round(cheapestPerDose * sellDoses);
		long profit = Math.round(dearestPerDose * sellDoses) - costPerPotion;
		double roi = costPerPotion == 0 ? 0 : profit * 100.0 / costPerPotion;
		return new Option(potion, cheapest.getId(), doses.get(cheapest.getId()), cheapestPrice,
			dearest.getId(), sellDoses, dearestPrice, profit, roi, volume);
	}
}
