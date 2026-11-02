package com.fredflips;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class GeSlotOverlayTest
{
	@Test
	public void timerFormat()
	{
		assertEquals("45s", GeSlotOverlay.shortDuration(45_000));
		assertEquals("12m", GeSlotOverlay.shortDuration(12 * 60_000));
		assertEquals("3h05", GeSlotOverlay.shortDuration((3 * 60 + 5) * 60_000L));
		assertEquals("0s", GeSlotOverlay.shortDuration(-5));
	}

	@Test
	public void clock()
	{
		assertEquals("00:25:20", GeSlotOverlay.clock((25 * 60 + 20) * 1000L, false));
		assertEquals("02:05", GeSlotOverlay.clock((2 * 3600 + 5 * 60 + 9) * 1000L, true));
	}
}
