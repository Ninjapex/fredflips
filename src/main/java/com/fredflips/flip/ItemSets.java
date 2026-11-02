package com.fredflips.flip;

import com.fredflips.FredFlipsConfig;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.MarketSnapshot;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Grand Exchange item sets (e.g. Dharok's armour set) and whether it pays to combine the pieces into the set,
 * or split the set into pieces, at a GE clerk. The clerk does this for free.
 */
@Slf4j
@Singleton
public class ItemSets
{
	private static final String RESOURCE = "/com/fredflips/item_sets.json";
	private static final long SECONDS_PER_MINUTE = 60;

	@Data
	public static class ItemSet
	{
		private int id;
		private String name;
		private List<Piece> pieces;
	}

	@Data
	public static class Piece
	{
		private int id;
		private String name;
		private int quantity;
	}

	@Value
	public static class Option
	{
		int setId;
		String setName;
		/**
		 * True: buy the pieces and sell the set. False: buy the set and sell the pieces.
		 */
		boolean combine;
		long cost;
		/**
		 * What you get after GE tax.
		 */
		long revenue;
		int pieceCount;

		public long getProfit()
		{
			return revenue - cost;
		}

		public double getRoiPercent()
		{
			return cost == 0 ? 0 : getProfit() * 100.0 / cost;
		}
	}

	private final List<ItemSet> sets;

	@Inject
	ItemSets(Gson gson)
	{
		this.sets = load(gson);
	}

	public List<ItemSet> getSets()
	{
		return sets;
	}

	public static List<Option> scan(List<ItemSet> sets, MarketSnapshot snapshot, Set<Integer> taxExempt, FredFlipsConfig config)
	{
		List<Option> options = new ArrayList<>();
		for (ItemSet set : sets)
		{
			Prices setPrices = Prices.of(set.getId(), snapshot, taxExempt, config);
			if (setPrices == null)
			{
				continue;
			}
			long piecesBuy = 0;
			long piecesSell = 0;
			boolean priced = true;
			for (Piece piece : set.getPieces())
			{
				Prices piecePrices = Prices.of(piece.getId(), snapshot, taxExempt, config);
				if (piecePrices == null)
				{
					priced = false;
					break;
				}
				piecesBuy += piecePrices.buy * piece.getQuantity();
				piecesSell += piecePrices.sellAfterTax * piece.getQuantity();
			}
			if (!priced)
			{
				continue;
			}

			int count = set.getPieces().size();
			Option combine = new Option(set.getId(), set.getName(), true, piecesBuy, setPrices.sellAfterTax, count);
			Option split = new Option(set.getId(), set.getName(), false, setPrices.buy, piecesSell, count);
			Option best = combine.getProfit() >= split.getProfit() ? combine : split;
			if (best.getProfit() >= config.minMargin())
			{
				options.add(best);
			}
		}
		options.sort(Comparator.comparingLong(Option::getProfit).reversed());
		return options;
	}

	/**
	 * Cautious prices for one item, the same way as the flip list. Null if it hasn't traded recently on both sides.
	 */
	private static final class Prices
	{
		final long buy;
		final long sellAfterTax;

		private Prices(long buy, long sellAfterTax)
		{
			this.buy = buy;
			this.sellAfterTax = sellAfterTax;
		}

		static Prices of(int itemId, MarketSnapshot snapshot, Set<Integer> taxExempt, FredFlipsConfig config)
		{
			LatestPrice latest = snapshot.getLatest().get(itemId);
			AveragePrice hour = snapshot.getOneHour().get(itemId);
			if (latest == null || hour == null || latest.getHigh() == null || latest.getLow() == null
				|| latest.getHighTime() == null || latest.getLowTime() == null
				|| (snapshot.getFetchedAt() - Math.min(latest.getHighTime(), latest.getLowTime())) / SECONDS_PER_MINUTE > config.maxPriceAgeMinutes()
				|| hour.getHighPriceVolume() + hour.getLowPriceVolume() == 0)
			{
				return null;
			}
			long buy = (hour.getAvgLowPrice() == null ? latest.getLow() : Math.max(latest.getLow(), hour.getAvgLowPrice())) + 1;
			long sell = (hour.getAvgHighPrice() == null ? latest.getHigh() : Math.min(latest.getHigh(), hour.getAvgHighPrice())) - 1;
			return new Prices(buy, sell - GeTax.perItem(sell, taxExempt.contains(itemId)));
		}
	}

	private static List<ItemSet> load(Gson gson)
	{
		try (InputStream in = ItemSets.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("FredFlips item set list is missing");
				return Collections.emptyList();
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				SetFile file = gson.fromJson(reader, SetFile.class);
				return file == null || file.sets == null ? Collections.emptyList() : file.sets;
			}
		}
		catch (Exception e)
		{
			log.warn("Couldn't load FredFlips item set list", e);
			return Collections.emptyList();
		}
	}

	private static class SetFile
	{
		List<ItemSet> sets;
	}
}
