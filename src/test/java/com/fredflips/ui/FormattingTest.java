package com.fredflips.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class FormattingTest
{
	@Test
	public void shortGpMatchesTheFlipListStyle()
	{
		assertEquals("950", Ui.shortGp(950));
		assertEquals("106.1K", Ui.shortGp(106_100));
		assertEquals("1K", Ui.shortGp(1_000));
		assertEquals("4.7M", Ui.shortGp(4_700_000));
		assertEquals("95.07M", Ui.shortGp(95_070_000));
		assertEquals("2.77B", Ui.shortGp(2_770_000_000L));
		assertEquals("-1.38M", Ui.shortGp(-1_380_000));
		assertEquals("+16.57M", Ui.signedShortGp(16_570_000));
	}

	@Test
	public void cashStackAcceptsShorthand()
	{
		assertEquals(Long.valueOf(50_000_000), PlanTab.parseGp("50m"));
		assertEquals(Long.valueOf(1_500_000_000), PlanTab.parseGp("1.5B"));
		assertEquals(Long.valueOf(200_000), PlanTab.parseGp(" 200k "));
		assertEquals(Long.valueOf(12_500_000), PlanTab.parseGp("12,500,000"));
		assertEquals(Long.valueOf(3_000_000), PlanTab.parseGp("3m gp"));
		assertNull(PlanTab.parseGp("lots"));
		assertNull(PlanTab.parseGp(""));
	}

	@Test
	public void ages()
	{
		assertEquals("now", Ui.age(0));
		assertEquals("3m", Ui.age(3));
		assertEquals("2h", Ui.age(130));
		assertEquals("1d", Ui.age(60 * 24 + 5));
	}
}
