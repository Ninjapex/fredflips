package com.fredflips;

import com.fredflips.flip.SortOrder;
import com.fredflips.insights.ChartPeriod;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(FredFlipsConfig.GROUP)
public interface FredFlipsConfig extends Config
{
	String GROUP = "fredflips";

	@ConfigSection(name = "General", description = "Your cash stack and how often prices refresh", position = 0)
	String generalSection = "general";

	@ConfigSection(name = "Tabs", description = "Show or hide sidebar tabs", position = 10)
	String tabsSection = "tabs";

	@ConfigSection(name = "Safety filters", description = "Hide flips that are more likely to lose money", position = 20)
	String safetySection = "safety";

	@ConfigSection(name = "Plan mode", description = "How Plan mode spreads your cash across GE slots", position = 30)
	String planSection = "plan";

	@ConfigSection(name = "Movers", description = "Dumps, spikes and high alch lists", position = 40)
	String moversSection = "movers";

	@ConfigSection(name = "Item tab", description = "Show or hide sections of the Item tab", position = 50)
	String itemSection = "item";

	@ConfigSection(name = "Trade tracker", description = "Your GE slots, trade log and profit", position = 60)
	String tradesSection = "trades";

	@ConfigSection(name = "GE helpers", description = "Click-to-fill helpers on the Grand Exchange screen", position = 70)
	String geSection = "ge";

	@ConfigSection(name = "Money makers", description = "Processing money makers ranked by live GP/hr", position = 80)
	String moneySection = "money";

	// Money makers

	@ConfigItem(keyName = "showMoneyTab", name = "Show Money tab", description = "Show the Money tab", position = 81, section = moneySection)
	default boolean showMoneyTab()
	{
		return true;
	}

	@ConfigItem(keyName = "moneySort", name = "Sort by", description = "How to rank money makers", position = 82, section = moneySection)
	default com.fredflips.money.MoneySort moneySort()
	{
		return com.fredflips.money.MoneySort.REALISTIC;
	}

	@ConfigItem(
		keyName = "moneyOnlyMyLevels",
		name = "Only methods I can do",
		description = "When logged in, hide methods you don't have the skill levels for",
		position = 83,
		section = moneySection
	)
	default boolean moneyOnlyMyLevels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moneyProcessingOnly",
		name = "Processing only",
		description = "Only show buy-make-sell methods (enchanting, fletching, herblore...). Turn off to also see"
			+ " gathering and combat methods",
		position = 84,
		section = moneySection
	)
	default boolean moneyProcessingOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moneyBankStandingOnly",
		name = "Bank-standing only",
		description = "Only show methods the wiki says you can do standing at a bank or the Grand Exchange",
		position = 85,
		section = moneySection
	)
	default boolean moneyBankStandingOnly()
	{
		return true;
	}

	// GE helpers

	@ConfigItem(
		keyName = "geSearchHelper",
		name = "Find on GE",
		description = "After you click Find on GE in the sidebar, the next GE item search you open is filled in for you",
		position = 71,
		section = geSection
	)
	default boolean geSearchHelper()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gePriceHelper",
		name = "Price and quantity clicks",
		description = "Add clickable FredFlips (or Plan) prices and quantities to the GE's price and quantity boxes."
			+ " Nothing is entered until you click, and you still press Enter and Confirm yourself",
		position = 72,
		section = geSection
	)
	default boolean gePriceHelper()
	{
		return true;
	}

	// General

	@ConfigItem(
		keyName = "cashStack",
		name = "Cash stack",
		description = "GP you have available for flipping. Limits suggested quantities and is what Plan mode splits up."
			+ " 0 means no limit (Plan mode needs this set)",
		position = 1,
		section = generalSection
	)
	default int cashStack()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "sortOrder",
		name = "Sort flips by",
		description = "How to order the flip list",
		position = 2,
		section = generalSection
	)
	default SortOrder sortOrder()
	{
		return SortOrder.POTENTIAL_PROFIT;
	}

	@Range(min = 60, max = 600)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "refreshSeconds",
		name = "Refresh every",
		description = "How often to download new prices from the OSRS Wiki. The wiki updates about once a minute",
		position = 3,
		section = generalSection
	)
	default int refreshSeconds()
	{
		return 60;
	}

	@Range(min = 10, max = 200)
	@ConfigItem(
		keyName = "maxResults",
		name = "Items to show",
		description = "How many items to list in the Flips and Movers tabs",
		position = 4,
		section = generalSection
	)
	default int maxResults()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "membersItems",
		name = "Members items",
		description = "Include members-only items",
		position = 5,
		section = generalSection
	)
	default boolean membersItems()
	{
		return true;
	}

	// Tabs

	@ConfigItem(keyName = "showPlanTab", name = "Plan", description = "Show the Plan tab", position = 11, section = tabsSection)
	default boolean showPlanTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showActiveTab", name = "Active", description = "Show the Active tab (GE slots and items you hold)", position = 12, section = tabsSection)
	default boolean showActiveTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showDoneTab", name = "Done", description = "Show the Done tab (completed flips and trade log)", position = 13, section = tabsSection)
	default boolean showDoneTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showMoversTab", name = "Movers", description = "Show the Movers tab (dumps, spikes, high alch)", position = 14, section = tabsSection)
	default boolean showMoversTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showItemTab", name = "Item", description = "Show the Item tab (chart, indicators, volume)", position = 15, section = tabsSection)
	default boolean showItemTab()
	{
		return true;
	}

	// Safety filters

	@Range(min = 1)
	@ConfigItem(
		keyName = "minMargin",
		name = "Minimum margin",
		description = "Smallest profit per item in gp, after tax, worth listing",
		position = 21,
		section = safetySection
	)
	default int minMargin()
	{
		return 1;
	}

	@Range(max = 1000)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "minRoiPercent",
		name = "Minimum ROI",
		description = "Smallest return on investment, after tax, worth listing",
		position = 22,
		section = safetySection
	)
	default int minRoiPercent()
	{
		return 0;
	}

	@Range(max = 100)
	@ConfigItem(
		keyName = "minConfidence",
		name = "Minimum confidence",
		description = "Hide flips scoring below this (0-100). Tiers: S 85+, A 70+, B 55+, C below",
		position = 23,
		section = safetySection
	)
	default int minConfidence()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "hideRiskySignals",
		name = "Hide risky signals",
		description = "Leave Spiking, Dumping and Falling items out of the flip list. Their price is moving fast and"
			+ " could turn against you before you sell. They still show in the Movers tab",
		position = 24,
		section = safetySection
	)
	default boolean hideRiskySignals()
	{
		return true;
	}

	@Range(min = 1)
	@ConfigItem(
		keyName = "minTradesPerHour",
		name = "Min trades per hour",
		description = "Both buyers and sellers must have traded at least this many items in the last hour."
			+ " Thin markets are easy to manipulate and slow to fill",
		position = 25,
		section = safetySection
	)
	default int minTradesPerHour()
	{
		return 10;
	}

	@Range(min = 1, max = 240)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "maxPriceAgeMinutes",
		name = "Max price age",
		description = "Hide items whose last buy or sell trade is older than this. Old prices are often wrong",
		position = 26,
		section = safetySection
	)
	default int maxPriceAgeMinutes()
	{
		return 15;
	}

	// Plan mode

	@Range(min = 1, max = 8)
	@ConfigItem(
		keyName = "planSlots",
		name = "Slots to use",
		description = "How many GE slots Plan mode may fill. Slots you're already using are taken off this",
		position = 31,
		section = planSection
	)
	default int planSlots()
	{
		return 8;
	}

	@Range(max = 100)
	@ConfigItem(
		keyName = "planMinConfidence",
		name = "Minimum confidence",
		description = "Plan mode only picks flips at or above this confidence. 55 = tier B or better",
		position = 32,
		section = planSection
	)
	default int planMinConfidence()
	{
		return 55;
	}

	// Movers

	@Range(min = 1, max = 90)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "moverThresholdPercent",
		name = "Dump / spike size",
		description = "How far the latest price must be from the daily average to count as a dump or spike",
		position = 41,
		section = moversSection
	)
	default int moverThresholdPercent()
	{
		return 5;
	}

	// Item tab

	@ConfigItem(keyName = "itemLivePrices", name = "Live prices", description = "Show buy, sell, margin, tax, ROI, confidence and signal", position = 51, section = itemSection)
	default boolean itemLivePrices()
	{
		return true;
	}

	@ConfigItem(keyName = "itemChart", name = "Price chart", description = "Show the buy/sell price chart", position = 52, section = itemSection)
	default boolean itemChart()
	{
		return true;
	}

	@ConfigItem(keyName = "itemIndicators", name = "Technical indicators", description = "Show RSI, MACD, moving average, % change and price ranges", position = 53, section = itemSection)
	default boolean itemIndicators()
	{
		return true;
	}

	@ConfigItem(keyName = "itemVolume", name = "Volume", description = "Show hourly and daily volume and the buyer/seller split", position = 54, section = itemSection)
	default boolean itemVolume()
	{
		return true;
	}

	@ConfigItem(keyName = "defaultChartPeriod", name = "Default chart range", description = "Which chart range the Item tab opens on", position = 55, section = itemSection)
	default ChartPeriod defaultChartPeriod()
	{
		return ChartPeriod.DAY;
	}

	// Trade tracker

	@ConfigItem(
		keyName = "trackTrades",
		name = "Track my trades",
		description = "Record your GE buys and sells to work out real profit. Saved in your RuneLite settings only",
		position = 61,
		section = tradesSection
	)
	default boolean trackTrades()
	{
		return true;
	}

	@Range(min = 1, max = 1440)
	@Units(Units.MINUTES)
	@ConfigItem(
		keyName = "staleOfferMinutes",
		name = "Offer waiting warning",
		description = "Highlight an active offer in orange when nothing has filled for this long",
		position = 62,
		section = tradesSection
	)
	default int staleOfferMinutes()
	{
		return 30;
	}
}
