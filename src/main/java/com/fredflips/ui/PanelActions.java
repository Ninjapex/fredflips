package com.fredflips.ui;

import com.fredflips.flip.MoverType;
import com.fredflips.flip.SortOrder;
import com.fredflips.insights.ChartPeriod;
import com.fredflips.money.MoneySort;
import com.fredflips.wiki.ItemMapping;
import java.util.List;

/**
 * Things the sidebar asks the plugin to do. Called on the Swing thread.
 */
public interface PanelActions
{
	void refreshPrices();

	void changeSort(SortOrder order);

	/**
	 * Save one of FredFlips' settings, e.g. from the Flips filter panel.
	 */
	void changeSetting(String key, Object value);

	void toggleFavourite(int itemId);

	boolean isFavourite(int itemId);

	void changeMoverType(MoverType type);

	/**
	 * Load the ATL (false) or time-of-day arbitrage (true) list, which needs price history.
	 */
	void loadHistory(boolean arbitrage);

	void changeMoneySort(MoneySort sort);

	void openItem(int itemId);

	/**
	 * Fill the next GE item search with this item.
	 */
	void findOnGe(int itemId);

	void changeChartPeriod(ChartPeriod period);

	List<ItemMapping> searchItems(String query);

	void buildPlan();

	/**
	 * Replace one plan item with the next-best flip.
	 */
	void swapPlanEntry(int itemId);

	/**
	 * Save the cash stack typed in the Plan tab.
	 */
	void setCashStack(long gp);

	void clearTradeLog();
}
