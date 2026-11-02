package com.fredflips.trades;

import com.fredflips.wiki.LatestPrice;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OfferHealthTest
{
	// Market: people instant-sell at 1,000 and instant-buy at 1,100.
	private static final LatestPrice MARKET = new LatestPrice(1_100L, 0L, 1_000L, 0L);

	private static OfferHealth check(boolean buy, long price)
	{
		return OfferHealth.check(new OfferSnapshot(1, "x", buy, price, 10, 0, 0, OfferStatus.BUYING, 0, 0), MARKET);
	}

	@Test
	public void buyOffers()
	{
		assertEquals(OfferHealth.COMPETITIVE, check(true, 1_000));
		assertEquals(OfferHealth.COMPETITIVE, check(true, 1_050));
		assertEquals(OfferHealth.BUY_SLIGHTLY_LOW, check(true, 995));
		assertEquals(OfferHealth.BUY_TOO_LOW, check(true, 950));
		assertEquals(OfferHealth.BUY_OVERPAYING, check(true, 1_200));
	}

	@Test
	public void sellOffers()
	{
		assertEquals(OfferHealth.COMPETITIVE, check(false, 1_100));
		assertEquals(OfferHealth.COMPETITIVE, check(false, 1_050));
		assertEquals(OfferHealth.SELL_SLIGHTLY_HIGH, check(false, 1_105));
		assertEquals(OfferHealth.SELL_TOO_HIGH, check(false, 1_200));
		assertEquals(OfferHealth.SELL_UNDERPRICED, check(false, 900));
	}

	@Test
	public void noMarketData()
	{
		assertEquals(OfferHealth.UNKNOWN, OfferHealth.check(
			new OfferSnapshot(1, "x", true, 1_000, 10, 0, 0, OfferStatus.BUYING, 0, 0), null));
	}
}
