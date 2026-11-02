package com.fredflips.ge;

import lombok.Value;

/**
 * Price and quantity FredFlips suggests for the item open in the GE offer screen.
 * Either value can be null when there's nothing sensible to suggest.
 */
@Value
public class GeSuggestion
{
	/**
	 * "Plan" when the item is in your plan, otherwise "FredFlips".
	 */
	String source;
	Long price;
	Long quantity;
}
