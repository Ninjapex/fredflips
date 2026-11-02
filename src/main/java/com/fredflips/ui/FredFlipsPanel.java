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
	static final String PLAN = "Plan";
	static final String ITEM = "Item";
	static final String FAVS = "Favs";
	static final String FLIPS = "Flips";
	static final String TRADES = "Trades";
	static final String OTHER = "Other";

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
		.withZone(ZoneId.systemDefault());
	private static final String GITHUB_URL = "https://github.com/Ninjapex/fredflips";

	private final JLabel live = Ui.label("\u25CF Loading", Ui.WARN);
	private final JLabel status = Ui.muted("");
	private final SearchField search = new SearchField("Search all items...");
	private long fetchedAt;
	private final TabBar tabBar;
	private final JPanel content = new JPanel(new BorderLayout());
	private final Map<String, JComponent> tabs = new LinkedHashMap<>();

	private final FlipsTab flipsTab;
	private final PlanTab planTab;
	private final TradesTab tradesTab;
	private final MoversTab moversTab;
	private final ItemTab itemTab;
	private final MoneyTab moneyTab;
	private final FavsTab favsTab;
	private final DecantView decantView;
	private final HistoryView historyView;
	private final ItemSetView itemSetView;

	public FredFlipsPanel(ItemManager itemManager, PanelActions actions, FredFlipsConfig config)
	{
		setLayout(new BorderLayout(0, 6));
		setBorder(new EmptyBorder(8, 8, 8, 8));

		flipsTab = new FlipsTab(itemManager, actions, config);
		planTab = new PlanTab(itemManager, actions, config.cashStack());
		tradesTab = new TradesTab(new ActiveTab(itemManager), new DoneTab(actions));
		moversTab = new MoversTab(itemManager, actions);
		itemTab = new ItemTab(itemManager, actions, config);
		moneyTab = new MoneyTab(actions, config.moneySort());
		favsTab = new FavsTab(itemManager, actions);
		decantView = new DecantView(itemManager, actions);
		historyView = new HistoryView(itemManager, actions);
		itemSetView = new ItemSetView(itemManager, actions);

		// Same order as the tab bar: Plan, Item, Favs on the first row; Flips, Trades, Other on the second.
		tabs.put(PLAN, planTab);
		tabs.put(ITEM, itemTab);
		tabs.put(FAVS, favsTab);
		tabs.put(FLIPS, flipsTab);
		tabs.put(TRADES, tradesTab);
		tabs.put(OTHER, new OtherTab(moversTab, moneyTab, decantView, historyView, itemSetView));

		tabBar = new TabBar(this::showTab);
		for (String name : tabs.keySet())
		{
			tabBar.addTab(name);
		}

		JLabel title = new JLabel("FredFlips");
		title.setFont(Ui.TITLE);
		title.setForeground(Ui.ORANGE);
		JPanel titleRow = Ui.split(title, live);

		// Typing in the header search jumps to the Item tab with the results.
		search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
		{
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e)
			{
				searchChanged();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e)
			{
				searchChanged();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e)
			{
				searchChanged();
			}
		});

		JPanel header = Ui.stack();
		Ui.addSpaced(header, titleRow);
		Ui.addSpaced(header, search);
		status.setVisible(false);
		Ui.addSpaced(header, status);
		Ui.addSpaced(header, tabBar);
		add(header, BorderLayout.NORTH);

		content.setOpaque(false);
		add(content, BorderLayout.CENTER);

		javax.swing.JButton github = new javax.swing.JButton("FredFlips on GitHub");
		github.setFont(Ui.SMALL_BOLD);
		github.setForeground(java.awt.Color.BLACK);
		github.setBackground(Ui.ORANGE);
		github.setFocusPainted(false);
		github.addActionListener(e -> net.runelite.client.util.LinkBrowser.browse(GITHUB_URL));
		JPanel footer = new JPanel(new BorderLayout());
		footer.setOpaque(false);
		footer.setBorder(new EmptyBorder(8, 0, 0, 0));
		footer.add(github, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

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
		tabBar.setTabVisible(ITEM, config.showItemTab());
		tabBar.setTabVisible(FAVS, config.showFavsTab());
		tabBar.setTabVisible(TRADES, config.showTradesTab());
		tabBar.setTabVisible(OTHER, config.showOtherTab());
	}

	private void searchChanged()
	{
		String query = search.getText().trim();
		if (query.length() >= 2)
		{
			itemTab.searchFor(query);
			tabBar.select(ITEM);
		}
	}

	/**
	 * A short message under the search box, e.g. what to do after Find on GE.
	 */
	public void showStatus(String text)
	{
		status.setText(Ui.wrap(text));
		status.setVisible(true);
		revalidate();
	}

	public void showOffline(String text)
	{
		live.setText("\u25CF Offline");
		live.setForeground(Ui.BAD);
		showStatus(text);
	}

	public void showPricesTime(long fetchedAtEpochSeconds)
	{
		fetchedAt = fetchedAtEpochSeconds;
		live.setText("\u25CF Live");
		live.setForeground(Ui.GOOD);
		live.setToolTipText("OSRS Wiki prices from " + TIME_FORMAT.format(Instant.ofEpochSecond(fetchedAtEpochSeconds)));
	}

	public void setSortOrder(SortOrder order)
	{
		flipsTab.setSortOrder(order);
	}

	public void showFlips(List<FlipCandidate> flips)
	{
		flipsTab.showFlips(flips, fetchedAt);
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

	public void showPlan(FlipPlan plan, String message, Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		List<Trade> trades)
	{
		planTab.show(plan, message, slots, holdings, trades);
	}

	public void showActive(Map<Integer, OfferSnapshot> slots, List<FlipLedger.OpenPosition> holdings,
		Map<Integer, LatestPrice> market, Set<Integer> taxExempt, int staleMinutes, boolean tracking, long now)
	{
		tradesTab.showActive(slots, holdings, market, taxExempt, staleMinutes, tracking, now);
	}

	public void showDone(FlipLedger ledger, List<Trade> trades, long now)
	{
		tradesTab.showDone(ledger, trades, now);
	}

	public void showHistory(List<com.fredflips.flip.HistoryScanner.Result> results, boolean arbitrage)
	{
		historyView.show(results, arbitrage);
	}

	public void showItemSets(List<com.fredflips.flip.ItemSets.Option> options)
	{
		itemSetView.show(options);
	}

	public void showDecants(List<com.fredflips.flip.DecantScanner.Option> options)
	{
		decantView.show(options);
	}

	public void showFavourites(List<FlipCandidate> favourites)
	{
		favsTab.show(favourites);
	}

	public void refreshFavouriteStar()
	{
		itemTab.refreshStar();
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

	public void showLimitUsage(int itemId, long bought, int limit, long resetsInMillis)
	{
		itemTab.showLimitUsage(itemId, bought, limit, resetsInMillis);
	}

	public void showItemChart(int itemId, ChartPeriod period, List<TimeseriesPoint> points)
	{
		itemTab.showChart(itemId, period, points);
	}

	public void showItemInsights(int itemId, ItemInsights insights, FlipCandidate numbers)
	{
		itemTab.showInsights(itemId, insights, numbers);
	}

	public void showItemLongTerm(int itemId, com.fredflips.insights.LongTermInsights insights, int buyLimit)
	{
		itemTab.showLongTerm(itemId, insights, buyLimit);
	}

	public void showItemInsightsError(int itemId)
	{
		itemTab.showInsightsError(itemId);
	}
}
