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
	static final double DIP_PERCENT = 2;
	static final double VOLATILE_SPREAD_PERCENT = 5;
	// Cheap items with a handful of trades swing wildly and aren't worth the effort, so they're left out.
	static final long MIN_PRICE = 100;
	// Moves bigger than these are almost always thin-market noise rather than something to trade on.
	static final double MAX_DROP_PERCENT = 50;
	static final double MAX_SPIKE_PERCENT = 100;
	static final double MAX_SPREAD_PERCENT = 40;
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
				case DIPS:
					mover = dip(item, latest, hour, day, snapshot.getFetchedAt(), config);
					break;
				case VOLATILE:
					mover = volatile_(item, latest, hour, snapshot.getFetchedAt(), config);
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

		// High alch by profit each; dips and volatile by gp at stake (size of move x price x volume),
		// so busy, valuable items beat cheap ones with big percentages; dumps and spikes by size of move.
		Comparator<Mover> order = type == MoverType.HIGH_ALCH
			? Comparator.comparingLong(Mover::getProfit).reversed()
			: type == MoverType.DIPS || type == MoverType.VOLATILE
			? Comparator.comparingDouble((Mover m) -> Math.abs(m.getChangePercent()) * m.getPrice() * m.getVolumePerHour()).reversed()
			: Comparator.comparingDouble((Mover m) -> Math.abs(m.getChangePercent())).reversed();
		movers.sort(order);
		return movers.size() > config.maxResults() ? new ArrayList<>(movers.subList(0, config.maxResults())) : movers;
	}

	/**
	 * The last hour's average instant-sell price against today's, so one odd trade isn't enough.
	 */
	static Mover dip(ItemMapping item, LatestPrice latest, AveragePrice hour, AveragePrice day, long now, FredFlipsConfig config)
	{
		if (day == null || day.getAvgLowPrice() == null || hour.getAvgLowPrice() == null || latest.getLowTime() == null
			|| isStale(latest.getLowTime(), now, config)
			|| hour.getLowPriceVolume() < config.minTradesPerHour())
		{
			return null;
		}
		double change = percentChange(hour.getAvgLowPrice(), day.getAvgLowPrice());
		if (change > -DIP_PERCENT || change < -MAX_DROP_PERCENT || day.getAvgLowPrice() < MIN_PRICE)
		{
			return null;
		}
		return new Mover(item.getId(), item.getName(), hour.getAvgLowPrice(), day.getAvgLowPrice(), change, 0,
			hour.getLowPriceVolume() + hour.getHighPriceVolume(), limitOf(item));
	}

	/**
	 * Gap between the latest instant-buy and instant-sell prices, for items trading on both sides.
	 */
	static Mover volatile_(ItemMapping item, LatestPrice latest, AveragePrice hour, long now, FredFlipsConfig config)
	{
		if (latest.getHigh() == null || latest.getLow() == null || latest.getLow() <= 0
			|| latest.getHighTime() == null || latest.getLowTime() == null
			|| isStale(latest.getHighTime(), now, config) || isStale(latest.getLowTime(), now, config)
			|| hour.getLowPriceVolume() < config.minTradesPerHour() || hour.getHighPriceVolume() < config.minTradesPerHour())
		{
			return null;
		}
		double spread = percentChange(latest.getHigh(), latest.getLow());
		if (spread < VOLATILE_SPREAD_PERCENT || spread > MAX_SPREAD_PERCENT || latest.getLow() < MIN_PRICE)
		{
			return null;
		}
		return new Mover(item.getId(), item.getName(), latest.getLow(), latest.getHigh(), spread, 0,
			hour.getLowPriceVolume() + hour.getHighPriceVolume(), limitOf(item));
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
		if (change > -config.moverThresholdPercent() || change < -MAX_DROP_PERCENT || day.getAvgLowPrice() < MIN_PRICE)
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
		if (change < config.moverThresholdPercent() || change > MAX_SPIKE_PERCENT || day.getAvgHighPrice() < MIN_PRICE)
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
