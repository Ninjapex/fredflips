package com.fredflips.trades;

import java.util.Arrays;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BuyLimitsTest
{
	private static final long HOUR = 60 * 60 * 1000L;

	@Test
	public void countsBuysInTheLastFourHours()
	{
		long now = 10 * HOUR;
		BuyLimits.Usage usage = BuyLimits.usage(Arrays.asList(
			new Trade(now - 5 * HOUR, 4151, "Whip", true, 40, 1),   // too old
			new Trade(now - 3 * HOUR, 4151, "Whip", true, 20, 1),
			new Trade(now - HOUR, 4151, "Whip", true, 10, 1),
			new Trade(now - HOUR, 4151, "Whip", false, 99, 1),      // a sell, doesn't count
			new Trade(now - HOUR, 11802, "AGS", true, 5, 1)),       // another item
			4151, now);

		assertEquals(30, usage.getBought());
		// The 3-hour-old buy drops out in 1 hour
		assertEquals(now + HOUR, usage.getResetsAt());
		assertEquals(40, usage.remaining(70));
		assertEquals(0, usage.remaining(20));
	}
}
