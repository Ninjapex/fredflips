package com.fredflips.flip;

import java.awt.Color;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A letter grade for a flip, from its confidence score.
 */
@Getter
@RequiredArgsConstructor
public enum Tier
{
	S(85, new Color(0xFFB000)),
	A(70, new Color(0x00C27A)),
	B(55, new Color(0x4AA3FF)),
	C(0, new Color(0xA0A0A0));

	private final int minConfidence;
	private final Color color;

	public static Tier fromConfidence(int confidence)
	{
		for (Tier tier : values())
		{
			if (confidence >= tier.minConfidence)
			{
				return tier;
			}
		}
		return C;
	}
}
