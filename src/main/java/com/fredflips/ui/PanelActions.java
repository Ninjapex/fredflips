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

	void changeMoverType(MoverType type);

	void changeMoneySort(MoneySort sort);

	void openItem(int itemId);

	/**
	 * Fill the next GE item search with this item.
	 */
	void findOnGe(int itemId);

	void changeChartPeriod(ChartPeriod period);

	List<ItemMapping> searchItems(String query);

	void buildPlan();

	void clearTradeLog();
}
