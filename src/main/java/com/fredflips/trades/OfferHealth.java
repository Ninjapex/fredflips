package com.fredflips.trades;

import com.fredflips.wiki.LatestPrice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Is an active offer's price still in line with the market?
 * <p>
 * A buy offer fills when people instant-sell at or below your price, so it's compared with the latest
 * instant-sell price (the wiki's "low"). A sell offer fills when people instant-buy at or above your price,
 * so it's compared with the latest instant-buy price (the wiki's "high").
 */
@Getter
@RequiredArgsConstructor
public enum OfferHealth
{
	COMPETITIVE("Competitive", Level.GOOD),
	BUY_SLIGHTLY_LOW("Just under market - may fill slowly", Level.WARN),
	BUY_TOO_LOW("Below market - unlikely to fill", Level.BAD),
	BUY_OVERPAYING("Above instant-buy price - overpaying", Level.WARN),
	SELL_SLIGHTLY_HIGH("Just over market - may sell slowly", Level.WARN),
	SELL_TOO_HIGH("Above market - unlikely to sell", Level.BAD),
	SELL_UNDERPRICED("Below instant-sell price - underpriced", Level.WARN),
	UNKNOWN("No market data", Level.WARN);

	// How far past the market price still counts as "slightly" off.
	static final double SLIGHT_PERCENT = 1.0;

	private final String description;
	private final Level level;

	public enum Level
	{
		GOOD, WARN, BAD
	}

	public static OfferHealth check(OfferSnapshot offer, LatestPrice market)
	{
		if (market == null || market.getHigh() == null || market.getLow() == null)
		{
			return UNKNOWN;
		}
		long price = offer.getPrice();
		long low = market.getLow();
		long high = market.getHigh();

		if (offer.isBuy())
		{
			if (price > high)
			{
				return BUY_OVERPAYING;
			}
			if (price >= low)
			{
				return COMPETITIVE;
			}
			return price >= low * (1 - SLIGHT_PERCENT / 100) ? BUY_SLIGHTLY_LOW : BUY_TOO_LOW;
		}

		if (price < low)
		{
			return SELL_UNDERPRICED;
		}
		if (price <= high)
		{
			return COMPETITIVE;
		}
		return price <= high * (1 + SLIGHT_PERCENT / 100) ? SELL_SLIGHTLY_HIGH : SELL_TOO_HIGH;
	}
}
