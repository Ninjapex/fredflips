package com.fredflips.money;

import com.fredflips.flip.GeTax;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.gameval.ItemID;

/**
 * Works out live GP/hr for money makers from current GE prices.
 * <p>
 * The headline number is the worst case: you pay the instant-buy price for inputs and get the
 * instant-sell price for outputs, then pay GE tax on what you sell. Placing offers usually does
 * better, so that's shown as the best case.
 */
public final class MoneyMakerCalculator
{
	static final int COINS = ItemID.COINS;
	// GE buy limits reset every 4 hours.
	static final double LIMIT_WINDOW_HOURS = 4;
	private static final long SECONDS_PER_MINUTE = 60;

	private MoneyMakerCalculator()
	{
	}

	/**
	 * @return the result, or null if any item has no price
	 */
	public static MoneyResult calculate(MoneyMethod method, MarketSnapshot snapshot, Map<Integer, ItemMapping> items,
		Set<Integer> taxExempt, int maxPriceAgeMinutes)
	{
		double instantCost = 0;
		double offersCost = 0;
		double buyLimitShare = 1;
		String limitingInput = null;
		boolean stale = false;

		for (MoneyMethod.Ingredient input : method.getInputsPerHour())
		{
			if (input.getId() == COINS)
			{
				instantCost += input.getQuantity();
				offersCost += input.getQuantity();
				continue;
			}
			Prices prices = Prices.of(input.getId(), snapshot);
			if (prices == null)
			{
				return null;
			}
			instantCost += prices.instantBuy * input.getQuantity();
			offersCost += prices.instantSell * input.getQuantity();
			stale |= prices.ageMinutes(snapshot.getFetchedAt()) > maxPriceAgeMinutes;

			ItemMapping item = items.get(input.getId());
			if (item != null && item.getLimit() != null && item.getLimit() > 0)
			{
				double share = item.getLimit() / LIMIT_WINDOW_HOURS / input.getQuantity();
				if (share < buyLimitShare)
				{
					buyLimitShare = share;
					limitingInput = input.getName();
				}
			}
		}

		double instantRevenue = 0;
		double offersRevenue = 0;
		double tax = 0;
		List<String> thinOutputs = new ArrayList<>();
		double marketShare = 1;
		for (MoneyMethod.Ingredient output : method.getOutputsPerHour())
		{
			if (output.getId() == COINS)
			{
				instantRevenue += output.getQuantity();
				offersRevenue += output.getQuantity();
				continue;
			}
			Prices prices = Prices.of(output.getId(), snapshot);
			if (prices == null)
			{
				return null;
			}
			boolean exempt = taxExempt.contains(output.getId());
			long instantTaxEach = GeTax.perItem(prices.instantSell, exempt);
			long offersTaxEach = GeTax.perItem(prices.instantBuy, exempt);
			instantRevenue += (prices.instantSell - instantTaxEach) * output.getQuantity();
			offersRevenue += (prices.instantBuy - offersTaxEach) * output.getQuantity();
			tax += instantTaxEach * output.getQuantity();
			stale |= prices.ageMinutes(snapshot.getFetchedAt()) > maxPriceAgeMinutes;
			if (prices.volumePerHour < output.getQuantity())
			{
				thinOutputs.add(output.getName());
				marketShare = Math.min(marketShare, (double) prices.volumePerHour / output.getQuantity());
			}
		}

		return new MoneyResult(method,
			Math.round(instantRevenue - instantCost),
			Math.round(offersRevenue - offersCost),
			Math.round(instantCost),
			Math.round(tax),
			buyLimitShare,
			buyLimitShare < 1 ? limitingInput : null,
			thinOutputs,
			marketShare,
			stale);
	}

	/**
	 * Current prices for one item. Each side falls back to its hourly average when the latest trade is missing.
	 */
	private static final class Prices
	{
		final long instantBuy;
		final long instantSell;
		final long volumePerHour;
		final long oldestTradeTime;

		private Prices(long instantBuy, long instantSell, long volumePerHour, long oldestTradeTime)
		{
			this.instantBuy = instantBuy;
			this.instantSell = instantSell;
			this.volumePerHour = volumePerHour;
			this.oldestTradeTime = oldestTradeTime;
		}

		static Prices of(int itemId, MarketSnapshot snapshot)
		{
			LatestPrice latest = snapshot.getLatest().get(itemId);
			AveragePrice hour = snapshot.getOneHour().get(itemId);
			Long high = latest != null && latest.getHigh() != null ? latest.getHigh() : hour == null ? null : hour.getAvgHighPrice();
			Long low = latest != null && latest.getLow() != null ? latest.getLow() : hour == null ? null : hour.getAvgLowPrice();
			if (high == null || low == null)
			{
				return null;
			}
			long volume = hour == null ? 0 : hour.getHighPriceVolume() + hour.getLowPriceVolume();
			long oldest = latest == null || latest.getHighTime() == null || latest.getLowTime() == null
				? 0
				: Math.min(latest.getHighTime(), latest.getLowTime());
			return new Prices(high, low, volume, oldest);
		}

		long ageMinutes(long now)
		{
			return oldestTradeTime == 0 ? Long.MAX_VALUE : (now - oldestTradeTime) / SECONDS_PER_MINUTE;
		}
	}
}
