package com.fredflips.ui;

import com.fredflips.FredFlipsConfig;
import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.FlipPlan;
import com.fredflips.flip.Mover;
import com.fredflips.flip.MoverType;
import com.fredflips.flip.SortOrder;
import com.fredflips.insights.ChartPeriod;
import com.fredflips.insights.ItemInsights;
import com.fredflips.money.MoneyResult;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.Trade;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.TimeseriesPoint;
import java.awt.BorderLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar: a status line, the tab bar and the selected tab.
 * All methods must be called on the Swing thread.
 */
public class FredFlipsPanel extends PluginPanel
{
	static final String FLIPS = "Flips";
	static final String PLAN = "Plan";
	static final String ACTIVE = "Active";
	static final String DONE = "Done";
	static final String MOVERS = "Movers";
	static final String ITEM = "Item";
	static final String MONEY = "Money";

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	private final JLabel status = Ui.muted("Loading prices...");
	private final TabBar tabBar;
	private final JPanel content = new JPanel(new BorderLayout());
	private final Map<String, JComponent> tabs = new LinkedHashMap<>();

	private final FlipsTab flipsTab;
	private final PlanTab planTab;
	private final ActiveTab activeTab;
	private final DoneTab doneTab;
	private final MoversTab moversTab;
	private final ItemTab itemTab;
	private final MoneyTab moneyTab;

	public FredFlipsPanel(ItemManager itemManager, PanelActions actions, FredFlipsConfig config)
	{
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		flipsTab = new FlipsTab(itemManager, actions, config.sortOrder());
		planTab = new PlanTab(actions);
		activeTab = new ActiveTab();
		doneTab = new DoneTab(actions);
		moversTab = new MoversTab(itemManager, actions);
		itemTab = new ItemTab(itemManager, actions, config);
		moneyTab = new MoneyTab(actions, config.moneySort());

		tabs.put(FLIPS, flipsTab);
		tabs.put(PLAN, planTab);
		tabs.put(ACTIVE, activeTab);
		tabs.put(DONE, doneTab);
		tabs.put(MOVERS, moversTab);
		tabs.put(ITEM, itemTab);
		tabs.put(MONEY, moneyTab);

		tabBar = new TabBar(this::showTab);
		for (String name : tabs.keySet())
		{
			tabBar.addTab(name);
		}

		JPanel header = Ui.stack();
		Ui.addSpaced(header, status);
		Ui.addSpaced(header, tabBar);
		add(header, BorderLayout.NORTH);

		content.setOpaque(false);
		add(content, BorderLayout.CENTER);

		applyTabVisibility(config);
		tabBar.select(FLIPS);
	}

	// Only the selected tab is added, so the sidebar scrolls to that tab's height.
	private void showTab(String name)
	{
		content.removeAll();
		content.add(tabs.get(name), BorderLayout.CENTER);
		content.revalidate();
		content.repaint();
	}

	public void applyTabVisibility(FredFlipsConfig config)
	{
		tabBar.setTabVisible(PLAN, config.showPlanTab());
		tabBar.setTabVisible(ACTIVE, config.showActiveTab());
		tabBar.setTabVisible(DONE, config.showDoneTab());
		tabBar.setTabVisible(MOVERS, config.showMoversTab());
		tabBar.setTabVisible(ITEM, config.showItemTab());
		tabBar.setTabVisible(MONEY, config.showMoneyTab());
	}

	public void showStatus(String text)
	{
		status.setText(text);
	}

	public void showPricesTime(long fetchedAtEpochSeconds)
	{
		status.setText("Prices from " + TIME_FORMAT.format(Instant.ofEpochSecond(fetchedAtEpochSeconds)) + " (OSRS Wiki)");
	}

	public void setSortOrder(SortOrder order)
	{
		flipsTab.setSortOrder(order);
	}

	public void showFlips(List<FlipCandidate> flips)
	{
		flipsTab.showFlips(flips);
	}

	public void showMoney(List<MoneyResult> results, boolean levelsKnown)
	{
		moneyTab.showResults(results, levelsKnown);
	}

	public MoverType getMoverType()
	{
		return moversTab.getType();
	}

	public void showMovers(List<Mover> movers, MoverType type)
	{
		moversTab.showMovers(movers, type);
	}

	public void showPlan(FlipPlan plan, String message, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings)
	{
		planTab.show(plan, message, slots, holdings);
	}

	public void showActive(Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		Map<Integer, LatestPrice> market, Set<Integer> taxExempt, int staleMinutes, boolean tracking, long now)
	{
		activeTab.show(slots, holdings, market, taxExempt, staleMinutes, tracking, now);
	}

	public void showDone(FlipLedger ledger, List<Trade> trades, long now)
	{
		doneTab.show(ledger, trades, now);
	}

	public int getOpenItemId()
	{
		return itemTab.getItemId();
	}

	public ChartPeriod getChartPeriod()
	{
		return itemTab.getPeriod();
	}

	public void openItem(ItemMapping item, FlipCandidate numbers)
	{
		itemTab.showItem(item, numbers);
		tabBar.select(ITEM);
	}

	public void showItemChart(int itemId, ChartPeriod period, List<TimeseriesPoint> points)
	{
		itemTab.showChart(itemId, period, points);
	}

	public void showItemInsights(int itemId, ItemInsights insights, FlipCandidate numbers)
	{
		itemTab.showInsights(itemId, insights, numbers);
	}

	public void showItemInsightsError(int itemId)
	{
		itemTab.showInsightsError(itemId);
	}
}
