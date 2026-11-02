package com.fredflips.ge;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Prices and quantity FredFlips suggests for the item open in the GE offer screen.
 * Any value can be null when there's nothing sensible to suggest.
 */
@Value
@AllArgsConstructor
public class GeSuggestion
{
	/**
	 * "Plan" when the item is in your plan, otherwise "FredFlips".
	 */
	String source;
	/**
	 * The plan's or FredFlips' cautious price.
	 */
	Long price;
	Long quantity;
	/**
	 * Most likely to fill straight away: the instant-buy price when buying, the instant-sell price when selling.
	 */
	Long quickPrice;
	/**
	 * A better price that may take longer: the instant-sell price when buying, the instant-buy price when selling.
	 */
	Long patientPrice;

	public GeSuggestion(String source, Long price, Long quantity)
	{
		this(source, price, quantity, null, null);
	}
}
