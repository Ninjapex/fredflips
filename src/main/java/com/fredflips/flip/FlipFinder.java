package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Turns raw wiki prices into a ranked list of flips.
 * <p>
 * Every estimate here leans pessimistic on purpose:
 * <ul>
 *     <li>The buy price is the HIGHEST of the latest instant-sell price, the 5 minute average and
 *     the 1 hour average. The sell price is the LOWEST of the latest instant-buy price and the same
 *     two averages. A short spike or dump (a typo, a panic sale, a manipulation attempt) can't create
 *     a fake margin, and items whose price is moving fast in either direction drop out.</li>
 *     <li>You bid 1gp above the buy price and list 1gp under the sell price, like a real flipper
 *     competing for the fill.</li>
 *     <li>Tax is always subtracted.</li>
 *     <li>Items with old prices or few trades on either side are hidden.</li>
 * </ul>
 */
public final class FlipFinder
{
	private static final long SECONDS_PER_MINUTE = 60;
	// Price moving fast: it could turn against you between buying and selling.
	static final Set<Signal> RISKY_SIGNALS = EnumSet.of(Signal.SPIKING, Signal.DUMPING, Signal.FALLING);

	private FlipFinder()
	{
	}

	/**
	 * The flip list: every item that passes the filters, best first, cut to the configured length.
	 */
	public static List<FlipCandidate> find(MarketSnapshot snapshot, FredFlipsConfig config)
	{
		List<FlipCandidate> flips = findAll(snapshot, config);
		flips.sort(config.sortOrder().getComparator().reversed());
		return flips.size() > config.maxResults() ? new ArrayList<>(flips.subList(0, config.maxResults())) : flips;
	}

	/**
	 * Every item that passes the filters, unsorted.
	 */
	public static List<FlipCandidate> findAll(MarketSnapshot snapshot, FredFlipsConfig config)
	{
		Set<Integer> taxExempt = GeTax.exemptIds(snapshot.getItems());
		List<FlipCandidate> flips = new ArrayList<>();

		for (ItemMapping item : snapshot.getItems())
		{
			FlipCandidate flip = evaluate(item, snapshot, taxExempt.contains(item.getId()), config);
			if (flip != null)
			{
				flips.add(flip);
			}
		}
		return flips;
	}

	/**
	 * Works out one item's flip, or returns null if it fails any filter.
	 */
	static FlipCandidate evaluate(ItemMapping item, MarketSnapshot snapshot, boolean taxExempt, FredFlipsConfig config)
	{
		if (item.isMembers() && !config.membersItems())
		{
			return null;
		}

		FlipCandidate flip = calculate(item, snapshot, taxExempt, config.cashStack());
		if (flip == null
			|| flip.getBuyPriceAgeMinutes() > config.maxPriceAgeMinutes()
			|| flip.getSellPriceAgeMinutes() > config.maxPriceAgeMinutes()
			|| flip.getSellersPerHour() < config.minTradesPerHour()
			|| flip.getBuyersPerHour() < config.minTradesPerHour()
			|| flip.getMargin() < config.minMargin()
			|| flip.getRoiPercent() < config.minRoiPercent()
			|| flip.getConfidence() < config.minConfidence()
			|| (config.hideRiskySignals() && RISKY_SIGNALS.contains(flip.getSignal()))
			|| flip.getQuantity() < 1)
		{
			return null;
		}
		return flip;
	}

	/**
	 * Works out one item's numbers without applying any filters, for the Item tab.
	 * Returns null only when the wiki has no prices for it.
	 */
	public static FlipCandidate calculate(ItemMapping item, MarketSnapshot snapshot, boolean taxExempt, long cashStack)
	{
		LatestPrice latest = snapshot.getLatest().get(item.getId());
		AveragePrice hour = snapshot.getOneHour().get(item.getId());
		if (latest == null || hour == null
			|| latest.getHigh() == null || latest.getLow() == null
			|| latest.getHighTime() == null || latest.getLowTime() == null)
		{
			return null;
		}
		AveragePrice fiveMinute = snapshot.getFiveMinute().get(item.getId());
		AveragePrice day = snapshot.getOneDay() == null ? null : snapshot.getOneDay().get(item.getId());

		long buyPriceAgeMinutes = (snapshot.getFetchedAt() - latest.getLowTime()) / SECONDS_PER_MINUTE;
		long sellPriceAgeMinutes = (snapshot.getFetchedAt() - latest.getHighTime()) / SECONDS_PER_MINUTE;
		long sellersPerHour = hour.getLowPriceVolume();
		long buyersPerHour = hour.getHighPriceVolume();
		long dailyVolume = day == null ? 0 : day.getHighPriceVolume() + day.getLowPriceVolume();

		long buyPrice = latest.getLow();
		long sellPrice = latest.getHigh();
		if (fiveMinute != null)
		{
			buyPrice = higher(buyPrice, fiveMinute.getAvgLowPrice());
			sellPrice = lower(sellPrice, fiveMinute.getAvgHighPrice());
		}
		buyPrice = higher(buyPrice, hour.getAvgLowPrice()) + 1;
		sellPrice = lower(sellPrice, hour.getAvgHighPrice()) - 1;
		if (buyPrice <= 0 || sellPrice <= 0)
		{
			return null;
		}

		long tax = GeTax.perItem(sellPrice, taxExempt);
		long margin = sellPrice - tax - buyPrice;
		double roiPercent = margin * 100.0 / buyPrice;

		int buyLimit = item.getLimit() == null ? 0 : item.getLimit();
		long quantity = Math.max(0, suggestedQuantity(buyLimit, cashStack, buyPrice, Math.min(sellersPerHour, buyersPerHour)));

		int confidence = FlipScore.confidence(margin, taxExempt, sellersPerHour, buyersPerHour,
			Math.max(buyPriceAgeMinutes, sellPriceAgeMinutes), hour, day);

		return new FlipCandidate(
			item.getId(), item.getName(), item.isMembers(), buyLimit,
			buyPrice, sellPrice, tax, margin, roiPercent,
			quantity, Math.max(0, margin) * quantity,
			sellersPerHour, buyersPerHour, dailyVolume,
			buyPriceAgeMinutes, sellPriceAgeMinutes,
			confidence, Tier.fromConfidence(confidence), FlipScore.signal(latest, hour, day));
	}

	/**
	 * How many to buy. Never more than the buy limit or what you can afford, and never more than
	 * one hour of the slower side's trading - you share the market with other flippers, so
	 * expecting to fill more than that is optimistic.
	 */
	static long suggestedQuantity(int buyLimit, long cashStack, long buyPrice, long slowerSidePerHour)
	{
		long quantity = slowerSidePerHour;
		if (buyLimit > 0)
		{
			quantity = Math.min(quantity, buyLimit);
		}
		if (cashStack > 0)
		{
			quantity = Math.min(quantity, cashStack / buyPrice);
		}
		return quantity;
	}

	/**
	 * The higher of the two prices, ignoring an average that is null because nothing traded.
	 */
	private static long higher(long price, Long average)
	{
		return average == null ? price : Math.max(price, average);
	}

	/**
	 * The lower of the two prices, ignoring an average that is null because nothing traded.
	 */
	private static long lower(long price, Long average)
	{
		return average == null ? price : Math.min(price, average);
	}
}
