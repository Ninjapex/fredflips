package com.fredflips.trades;

import java.util.List;
import lombok.Value;

/**
 * The GE lets you buy a limited amount of each item every 4 hours. This works out how much of that you've used,
 * from your recorded buys.
 */
public final class BuyLimits
{
	public static final long WINDOW_MILLIS = 4 * 60 * 60 * 1000L;

	@Value
	public static class Usage
	{
		long bought;
		/**
		 * Epoch millis when the oldest buy in the window drops out, or 0 if nothing was bought.
		 */
		long resetsAt;

		public long remaining(int limit)
		{
			return Math.max(0, limit - bought);
		}
	}

	private BuyLimits()
	{
	}

	public static Usage usage(List<Trade> trades, int itemId, long now)
	{
		long bought = 0;
		long oldest = Long.MAX_VALUE;
		for (Trade trade : trades)
		{
			if (trade.isBuy() && trade.getItemId() == itemId && trade.getTime() > now - WINDOW_MILLIS)
			{
				bought += trade.getQuantity();
				oldest = Math.min(oldest, trade.getTime());
			}
		}
		return new Usage(bought, bought == 0 ? 0 : oldest + WINDOW_MILLIS);
	}
}
