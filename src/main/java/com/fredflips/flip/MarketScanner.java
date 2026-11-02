package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.runelite.api.gameval.ItemID;

/**
 * Finds items for the Movers tab: dumps, spikes and high alch profits.
 */
public final class MarketScanner
{
	static final int NATURE_RUNE_ID = ItemID.NATURERUNE;
	private static final long SECONDS_PER_MINUTE = 60;

	private MarketScanner()
	{
	}

	public static List<Mover> scan(MoverType type, MarketSnapshot snapshot, FredFlipsConfig config)
	{
		Long natureRuneCost = type == MoverType.HIGH_ALCH ? natureRuneCost(snapshot) : null;
		List<Mover> movers = new ArrayList<>();

		for (ItemMapping item : snapshot.getItems())
		{
			if (item.isMembers() && !config.membersItems())
			{
				continue;
			}
			LatestPrice latest = snapshot.getLatest().get(item.getId());
			AveragePrice hour = snapshot.getOneHour().get(item.getId());
			AveragePrice day = snapshot.getOneDay() == null ? null : snapshot.getOneDay().get(item.getId());
			if (latest == null || hour == null)
			{
				continue;
			}

			Mover mover;
			switch (type)
			{
				case DUMPS:
					mover = dump(item, latest, hour, day, snapshot.getFetchedAt(), config);
					break;
				case SPIKES:
					mover = spike(item, latest, hour, day, snapshot.getFetchedAt(), config);
					break;
				default:
					mover = natureRuneCost == null ? null
						: highAlch(item, latest, hour, snapshot.getFetchedAt(), natureRuneCost, config);
					break;
			}
			if (mover != null)
			{
				movers.add(mover);
			}
		}

		Comparator<Mover> order = type == MoverType.HIGH_ALCH
			? Comparator.comparingLong(Mover::getProfit).reversed()
			: Comparator.comparingDouble((Mover m) -> Math.abs(m.getChangePercent())).reversed();
		movers.sort(order);
		return movers.size() > config.maxResults() ? new ArrayList<>(movers.subList(0, config.maxResults())) : movers;
	}

	static Mover dump(ItemMapping item, LatestPrice latest, AveragePrice hour, AveragePrice day, long now, FredFlipsConfig config)
	{
		if (day == null || day.getAvgLowPrice() == null || latest.getLow() == null || latest.getLowTime() == null
			|| isStale(latest.getLowTime(), now, config)
			|| hour.getLowPriceVolume() < config.minTradesPerHour())
		{
			return null;
		}
		double change = percentChange(latest.getLow(), day.getAvgLowPrice());
		if (change > -config.moverThresholdPercent())
		{
			return null;
		}
		return new Mover(item.getId(), item.getName(), latest.getLow(), day.getAvgLowPrice(), change, 0,
			hour.getLowPriceVolume() + hour.getHighPriceVolume(), limitOf(item));
	}

	static Mover spike(ItemMapping item, LatestPrice latest, AveragePrice hour, AveragePrice day, long now, FredFlipsConfig config)
	{
		if (day == null || day.getAvgHighPrice() == null || latest.getHigh() == null || latest.getHighTime() == null
			|| isStale(latest.getHighTime(), now, config)
			|| hour.getHighPriceVolume() < config.minTradesPerHour())
		{
			return null;
		}
		double change = percentChange(latest.getHigh(), day.getAvgHighPrice());
		if (change < config.moverThresholdPercent())
		{
			return null;
		}
		return new Mover(item.getId(), item.getName(), latest.getHigh(), day.getAvgHighPrice(), change, 0,
			hour.getLowPriceVolume() + hour.getHighPriceVolume(), limitOf(item));
	}

	/**
	 * Buy price is pessimistic (the higher of the latest instant-sell and the hourly average, plus 1gp),
	 * and the nature rune is priced at its instant-buy price.
	 */
	static Mover highAlch(ItemMapping item, LatestPrice latest, AveragePrice hour, long now, long natureRuneCost, FredFlipsConfig config)
	{
		if (item.getHighalch() == null || item.getHighalch() <= 0 || latest.getLow() == null || latest.getLowTime() == null
			|| isStale(latest.getLowTime(), now, config)
			|| hour.getLowPriceVolume() < config.minTradesPerHour())
		{
			return null;
		}
		long buyPrice = (hour.getAvgLowPrice() == null ? latest.getLow() : Math.max(latest.getLow(), hour.getAvgLowPrice())) + 1;
		long profit = item.getHighalch() - buyPrice - natureRuneCost;
		if (profit < config.minMargin())
		{
			return null;
		}
		return new Mover(item.getId(), item.getName(), buyPrice, item.getHighalch(),
			percentChange(item.getHighalch(), buyPrice), profit,
			hour.getLowPriceVolume() + hour.getHighPriceVolume(), limitOf(item));
	}

	private static Long natureRuneCost(MarketSnapshot snapshot)
	{
		LatestPrice nature = snapshot.getLatest().get(NATURE_RUNE_ID);
		return nature == null ? null : nature.getHigh();
	}

	private static boolean isStale(long tradeTime, long now, FredFlipsConfig config)
	{
		return (now - tradeTime) / SECONDS_PER_MINUTE > config.maxPriceAgeMinutes();
	}

	private static double percentChange(long price, long reference)
	{
		return reference == 0 ? 0 : (price - reference) * 100.0 / reference;
	}

	private static int limitOf(ItemMapping item)
	{
		return item.getLimit() == null ? 0 : item.getLimit();
	}
}
