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

	@ConfigSection(name = "Flip filters", description = "The Filter panel on the Flips tab", position = 15, closedByDefault = true)
	String filterSection = "filters";

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

	@ConfigSection(name = "GP drop and inventory", description = "The +gp drop when a flip completes, and the inventory tooltip", position = 75)
	String dropSection = "drop";

	@ConfigItem(keyName = "inventoryCostTooltip", name = "Inventory cost tooltip",
		description = "Hover an item you bought to see what you paid and its profit if sold now", position = 1, section = dropSection)
	default boolean inventoryCostTooltip()
	{
		return true;
	}

	@ConfigItem(keyName = "gpDrop", name = "Show GP drop", description = "Float +gp / -gp above the GE when a sale completes a flip",
		position = 2, section = dropSection)
	default boolean gpDrop()
	{
		return true;
	}

	@Range(min = 300, max = 10_000)
	@Units(Units.MILLISECONDS)
	@ConfigItem(keyName = "gpDropMillis", name = "Animation time", description = "How long each drop lasts", position = 3, section = dropSection)
	default int gpDropMillis()
	{
		return 1_500;
	}

	@Range(min = -500, max = 500)
	@ConfigItem(keyName = "gpDropOffsetX", name = "Offset X", description = "Move the drop left (negative) or right", position = 4, section = dropSection)
	default int gpDropOffsetX()
	{
		return 0;
	}

	@Range(min = -500, max = 500)
	@ConfigItem(keyName = "gpDropOffsetY", name = "Offset Y", description = "Move the drop up (negative) or down", position = 5, section = dropSection)
	default int gpDropOffsetY()
	{
		return 0;
	}

	@Range(min = 8, max = 40)
	@ConfigItem(keyName = "gpDropFontSize", name = "Font size", description = "Size of the drop text", position = 6, section = dropSection)
	default int gpDropFontSize()
	{
		return 14;
	}

	@ConfigItem(keyName = "gpDropProfitColour", name = "Profit colour", description = "Colour for a profitable flip", position = 7, section = dropSection)
	default java.awt.Color gpDropProfitColour()
	{
		return new java.awt.Color(0x00C27A);
	}

	@ConfigItem(keyName = "gpDropLossColour", name = "Loss colour", description = "Colour for a losing flip", position = 8, section = dropSection)
	default java.awt.Color gpDropLossColour()
	{
		return new java.awt.Color(0xE85050);
	}

	@ConfigSection(name = "Money makers", description = "Processing money makers ranked by live GP/hr", position = 80)
	String moneySection = "money";

	// Money makers

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

	@ConfigItem(
		keyName = "geSlotHighlights",
		name = "Highlight GE slots",
		description = "Colour each active GE offer: green if your price is competitive, grey if slightly off or"
			+ " waiting a while, red if well off the market. Shows how long each offer has been listed",
		position = 73,
		section = geSection
	)
	default boolean geSlotHighlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "geSlotHoverPanel",
		name = "GE slot hover panel",
		description = "Hover over a GE offer to see its progress, your price, the market and FredFlips' price",
		position = 74,
		section = geSection
	)
	default boolean geSlotHoverPanel()
	{
		return true;
	}

	@ConfigItem(keyName = "gePriceHotkeys", name = "Price hotkeys",
		description = "While the GE price box is open, press a key to fill in the Quick, FredFlips or Patient price."
			+ " You still press Enter and Confirm yourself", position = 72, section = geSection)
	default boolean gePriceHotkeys()
	{
		return true;
	}

	@ConfigItem(keyName = "geQuickKey", name = "Quick price key", description = "Fills the price most likely to fill straight away",
		position = 72, section = geSection)
	default net.runelite.client.config.Keybind geQuickKey()
	{
		return new net.runelite.client.config.Keybind(java.awt.event.KeyEvent.VK_I, 0);
	}

	@ConfigItem(keyName = "geFredFlipsKey", name = "FredFlips price key", description = "Fills FredFlips' (or your Plan's) price",
		position = 72, section = geSection)
	default net.runelite.client.config.Keybind geFredFlipsKey()
	{
		return new net.runelite.client.config.Keybind(java.awt.event.KeyEvent.VK_O, 0);
	}

	@ConfigItem(keyName = "gePatientKey", name = "Patient price key", description = "Fills a better price that may take longer to fill",
		position = 72, section = geSection)
	default net.runelite.client.config.Keybind gePatientKey()
	{
		return new net.runelite.client.config.Keybind(java.awt.event.KeyEvent.VK_P, 0);
	}

	@ConfigItem(keyName = "geOpenItemOnOffer", name = "Open Item tab on GE offers",
		description = "When you pick an item to buy or sell on the GE, open it in FredFlips' Item tab",
		position = 72, section = geSection)
	default boolean geOpenItemOnOffer()
	{
		return true;
	}

	@ConfigItem(keyName = "geFillCounter", name = "Fill counter on offers",
		description = "Show how much of each offer has filled (e.g. 37 / 100) and how long since the last fill",
		position = 75, section = geSection)
	default boolean geFillCounter()
	{
		return true;
	}

	@ConfigItem(keyName = "geTimerCompact", name = "Compact timer (HH:MM)",
		description = "Show the time-on-sale timer without seconds", position = 76, section = geSection)
	default boolean geTimerCompact()
	{
		return false;
	}

	@Range(min = 1, max = 1440)
	@Units(Units.MINUTES)
	@ConfigItem(keyName = "geTimerBorderlineMinutes", name = "Timer borderline after",
		description = "The timer switches to the borderline colour once an offer has been listed this long",
		position = 77, section = geSection)
	default int geTimerBorderlineMinutes()
	{
		return 30;
	}

	@Range(min = 1, max = 2880)
	@Units(Units.MINUTES)
	@ConfigItem(keyName = "geTimerOffMarketMinutes", name = "Timer off-market after",
		description = "The timer switches to the off-market colour once an offer has been listed this long",
		position = 78, section = geSection)
	default int geTimerOffMarketMinutes()
	{
		return 120;
	}

	@ConfigItem(keyName = "geColourCompetitive", name = "Competitive colour",
		description = "Border and timer colour when your price is competitive", position = 79, section = geSection)
	default java.awt.Color geColourCompetitive()
	{
		return new java.awt.Color(0x00C27A);
	}

	@ConfigItem(keyName = "geColourBorderline", name = "Borderline colour",
		description = "Border and timer colour when your price is slightly off, or the offer has been waiting a while",
		position = 80, section = geSection)
	default java.awt.Color geColourBorderline()
	{
		return new java.awt.Color(0xA0A0A0);
	}

	@ConfigItem(keyName = "geColourOffMarket", name = "Off-market colour",
		description = "Border and timer colour when your price is well off the market, or the offer is very old",
		position = 81, section = geSection)
	default java.awt.Color geColourOffMarket()
	{
		return new java.awt.Color(0xE85050);
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

	@Range(min = 10, max = 500)
	@ConfigItem(
		keyName = "maxResults",
		name = "Items to show",
		description = "How many items to list in the Flips and Movers tabs",
		position = 4,
		section = generalSection
	)
	default int maxResults()
	{
		return 200;
	}

	@ConfigItem(keyName = "flipPreset", name = "Preset", description = "Narrow the flip list to one kind of flip",
		position = 16, section = filterSection)
	default com.fredflips.flip.FlipPreset flipPreset()
	{
		return com.fredflips.flip.FlipPreset.ALL;
	}

	@ConfigItem(keyName = "flipMinBuyPrice", name = "Min buy price", description = "Hide items cheaper than this",
		position = 17, section = filterSection)
	default com.fredflips.flip.PriceFloor flipMinBuyPrice()
	{
		return com.fredflips.flip.PriceFloor.ANY;
	}

	@ConfigItem(keyName = "flipMinProfit", name = "Min profit", description = "Hide flips with less potential profit than this",
		position = 18, section = filterSection)
	default com.fredflips.flip.ProfitFloor flipMinProfit()
	{
		return com.fredflips.flip.ProfitFloor.ANY;
	}

	@ConfigItem(keyName = "flipF2pOnly", name = "F2P only", description = "Only free-to-play items",
		position = 19, section = filterSection)
	default boolean flipF2pOnly()
	{
		return false;
	}

	@ConfigItem(keyName = "flipTaxFreeOnly", name = "Tax-free only",
		description = "Only flips that pay no GE tax (sell price under 50gp, or a tax-exempt item)",
		position = 20, section = filterSection)
	default boolean flipTaxFreeOnly()
	{
		return false;
	}

	@ConfigItem(keyName = "flipCapitalFilter", name = "Only what I can afford",
		description = "Hide items whose buy price is more than your Cash stack", position = 21, section = filterSection)
	default boolean flipCapitalFilter()
	{
		return false;
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

	@ConfigItem(keyName = "showFavsTab", name = "Favs", description = "Show the Favs tab (your watchlist)", position = 12, section = tabsSection)
	default boolean showFavsTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showTradesTab", name = "Trades", description = "Show the Trades tab (GE slots, completed flips, profit)", position = 13, section = tabsSection)
	default boolean showTradesTab()
	{
		return true;
	}

	@ConfigItem(keyName = "showOtherTab", name = "Other", description = "Show the Other tab (dumps, spikes, high alch, money makers)", position = 14, section = tabsSection)
	default boolean showOtherTab()
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

	@ConfigItem(keyName = "itemPriceRange", name = "Price range (90d)",
		description = "Show the 90-day range, where today sits in it, and the drawdown from the high", position = 54, section = itemSection)
	default boolean itemPriceRange()
	{
		return true;
	}

	@ConfigItem(keyName = "itemTrend", name = "Trend", description = "Show 7d and 30d moving averages, their crossover, 30d change and Bollinger position",
		position = 54, section = itemSection)
	default boolean itemTrend()
	{
		return true;
	}

	@ConfigItem(keyName = "itemLiquidity", name = "Liquidity",
		description = "Show loss-making hours in the last day and how long buying and selling should take", position = 54, section = itemSection)
	default boolean itemLiquidity()
	{
		return true;
	}

	@ConfigItem(keyName = "itemProjection", name = "Projection",
		description = "Show a likely price range in 30 days and 3 months, from how much the price usually moves", position = 54, section = itemSection)
	default boolean itemProjection()
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
