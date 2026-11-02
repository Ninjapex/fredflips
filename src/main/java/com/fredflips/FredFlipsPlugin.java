package com.fredflips;

import com.fredflips.flip.DecantScanner;
import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.HistoryScanner;
import com.fredflips.flip.ItemSets;
import com.fredflips.flip.FlipFinder;
import com.fredflips.flip.FlipPlan;
import com.fredflips.flip.GeTax;
import com.fredflips.flip.MarketScanner;
import com.fredflips.flip.Mover;
import com.fredflips.flip.MoverType;
import com.fredflips.flip.SortOrder;
import com.fredflips.ge.GeHelper;
import com.fredflips.ge.GeSuggestion;
import com.fredflips.insights.ChartPeriod;
import com.fredflips.insights.ItemInsights;
import com.fredflips.money.MoneyMakers;
import com.fredflips.money.MoneyResult;
import com.fredflips.money.MoneySort;
import com.fredflips.trades.BuyLimits;
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.OfferStatus;
import com.fredflips.trades.Trade;
import com.fredflips.trades.TradeStore;
import com.fredflips.trades.TradeTracker;
import com.fredflips.ui.FredFlipsPanel;
import com.fredflips.ui.PanelActions;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.AveragePrice;
import com.fredflips.wiki.LatestPrice;
import com.fredflips.wiki.TimeseriesPoint;
import com.fredflips.wiki.MarketSnapshot;
import com.fredflips.wiki.WikiPriceClient;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.input.KeyManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "FredFlips",
	description = "Finds Grand Exchange flips using OSRS Wiki real-time prices, and tracks your own trades",
	tags = {"flipping", "flip", "merching", "grand exchange", "ge", "prices", "margin", "profit", "tracker"}
)
public class FredFlipsPlugin extends Plugin implements PanelActions
{
	private static final String FAVOURITES_KEY = "favourites";
	// ATL and Arbitrage look at this many of the most-traded items.
	private static final int HISTORY_ITEMS = 50;
	private static final long HISTORY_CACHE_MILLIS = 30 * 60_000L;

	private final Map<String, CachedSeries> historyCache = new ConcurrentHashMap<>();

	// Prices on these worlds don't match the main game, so trades there aren't tracked.
	private static final Set<WorldType> UNTRACKED_WORLDS = EnumSet.of(
		WorldType.SEASONAL, WorldType.DEADMAN, WorldType.BETA_WORLD, WorldType.FRESH_START_WORLD,
		WorldType.TOURNAMENT_WORLD, WorldType.NOSAVE_MODE, WorldType.QUEST_SPEEDRUNNING);

	@Inject
	private Client client;

	@Inject
	private FredFlipsConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private WikiPriceClient priceClient;

	@Inject
	private TradeStore tradeStore;

	@Inject
	private GeHelper geHelper;

	@Inject
	private MoneyMakers moneyMakers;

	@Inject
	private ItemSets itemSets;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GeSlotOverlay geSlotOverlay;

	@Inject
	private KeyManager keyManager;

	@Inject
	private GpDropOverlay gpDropOverlay;

	@Inject
	private InventoryCostOverlay inventoryCostOverlay;

	// Read-only copy of what you're holding, by item, for the inventory tooltip.
	private volatile Map<Integer, FlipLedger.OpenPosition> holdingsView = Collections.emptyMap();

	// Price hotkeys only act while the GE price box is open, so they don't get in the way anywhere else.
	private final HotkeyListener quickKey = priceHotkey(() -> config.geQuickKey(), 0);
	private final HotkeyListener fredFlipsKey = priceHotkey(() -> config.geFredFlipsKey(), 1);
	private final HotkeyListener patientKey = priceHotkey(() -> config.gePatientKey(), 2);

	private HotkeyListener priceHotkey(java.util.function.Supplier<net.runelite.client.config.Keybind> key, int which)
	{
		return new HotkeyListener(key)
		{
			@Override
			public void hotkeyPressed()
			{
				if (config.gePriceHotkeys())
				{
					geHelper.pressHotkey(which);
				}
			}
		};
	}

	private void updateHotkeyLabels()
	{
		geHelper.setQuickKeyLabel(config.geQuickKey().toString());
		geHelper.setFredFlipsKeyLabel(config.geFredFlipsKey().toString());
		geHelper.setPatientKeyLabel(config.gePatientKey().toString());
	}

	private FredFlipsPanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> refreshTask;
	private final AtomicBoolean fetching = new AtomicBoolean();

	private volatile MarketSnapshot lastSnapshot;
	private volatile Map<Integer, ItemMapping> itemsById = Collections.emptyMap();
	private volatile Set<Integer> taxExempt = Collections.emptySet();

	// Trade state is changed on the client thread and read elsewhere, so it's guarded by tradeLock.
	private final Object tradeLock = new Object();
	private final TradeTracker tracker = new TradeTracker();
	private final List<Trade> trades = new ArrayList<>();
	private String loadedProfile;
	// Read-only copy of the GE slots for the overlay, which draws every frame.
	private volatile Map<Integer, OfferSnapshot> slotView = Collections.emptyMap();

	// Your real skill levels by lower-case name, filled in from StatChanged while logged in.
	private final Map<String, Integer> skillLevels = new ConcurrentHashMap<>();
	private final AtomicBoolean moneyRefreshQueued = new AtomicBoolean();

	private volatile FlipPlan plan;
	private volatile String planMessage = "Set your Cash stack in the settings, then press Build plan.";

	@Override
	protected void startUp()
	{
		panel = new FredFlipsPanel(itemManager, this, config);

		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		navButton = NavigationButton.builder()
			.tooltip("FredFlips")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		geHelper.setSearchEnabled(config.geSearchHelper());
		geHelper.setSuggestionsEnabled(config.gePriceHelper());
		geHelper.setSuggestions(this::geSuggestion);
		overlayManager.add(geSlotOverlay);
		overlayManager.add(gpDropOverlay);
		overlayManager.add(inventoryCostOverlay);
		keyManager.registerKeyListener(quickKey);
		keyManager.registerKeyListener(fredFlipsKey);
		keyManager.registerKeyListener(patientKey);
		updateHotkeyLabels();

		executor.execute(() ->
		{
			synchronized (tradeLock)
			{
				loadTradeDataIfProfileChanged();
			}
			updateTradeViews();
		});
		scheduleRefresh();
	}

	@Override
	protected void shutDown()
	{
		cancelRefresh();
		geHelper.setSuggestions(null);
		overlayManager.remove(geSlotOverlay);
		overlayManager.remove(gpDropOverlay);
		overlayManager.remove(inventoryCostOverlay);
		keyManager.unregisterKeyListener(quickKey);
		keyManager.unregisterKeyListener(fredFlipsKey);
		keyManager.unregisterKeyListener(patientKey);
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
		lastSnapshot = null;
		plan = null;
		synchronized (tradeLock)
		{
			trades.clear();
			tracker.restore(null);
			loadedProfile = null;
		}
	}

	@Provides
	FredFlipsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FredFlipsConfig.class);
	}

	// ---------------------------------------------------------------- events

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FredFlipsConfig.GROUP.equals(event.getGroup()) || TradeStore.isStorageKey(event.getKey())
			|| FAVOURITES_KEY.equals(event.getKey()))
		{
			return;
		}

		switch (event.getKey())
		{
			case "refreshSeconds":
				scheduleRefresh();
				return;
			case "sortOrder":
				SortOrder order = config.sortOrder();
				onSwing(p -> p.setSortOrder(order));
				break;
			case "showPlanTab":
			case "showItemTab":
			case "showFavsTab":
			case "showTradesTab":
			case "showOtherTab":
				onSwing(p -> p.applyTabVisibility(config));
				return;
			case "geQuickKey":
			case "geFredFlipsKey":
			case "gePatientKey":
				updateHotkeyLabels();
				return;
			case "geSearchHelper":
				geHelper.setSearchEnabled(config.geSearchHelper());
				return;
			case "gePriceHelper":
				geHelper.setSuggestionsEnabled(config.gePriceHelper());
				return;
			default:
				break;
		}

		// Filters changed: re-rank the prices we already have instead of downloading again.
		executor.execute(() ->
		{
			rebuildMarketViews();
			updateTradeViews();
		});
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		executor.execute(() ->
		{
			synchronized (tradeLock)
			{
				loadTradeDataIfProfileChanged();
			}
			updateTradeViews();
		});
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Integer previous = skillLevels.put(event.getSkill().getName().toLowerCase(Locale.ROOT), client.getRealSkillLevel(event.getSkill()));
		if (previous == null || previous != client.getRealSkillLevel(event.getSkill()))
		{
			queueMoneyRefresh();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN && !skillLevels.isEmpty())
		{
			skillLevels.clear();
			queueMoneyRefresh();
		}
	}

	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event)
	{
		geHelper.onVarClientIntChanged(event.getIndex());
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (!config.trackTrades() || client.getEnvironment() != 0
			|| !Collections.disjoint(client.getWorldType(), UNTRACKED_WORLDS))
		{
			return;
		}

		GrandExchangeOffer offer = event.getOffer();
		int slot = event.getSlot();
		long now = System.currentTimeMillis();
		List<Trade> newTrades;

		synchronized (tradeLock)
		{
			// Make sure we're comparing against this account's saved slots, not the last account's.
			loadTradeDataIfProfileChanged();
			if (loadedProfile == null)
			{
				return;
			}

			if (offer.getState() == GrandExchangeOfferState.EMPTY)
			{
				// The game blanks every slot while logging in or hopping; only trust EMPTY once logged in.
				if (client.getGameState() != GameState.LOGGED_IN)
				{
					return;
				}
				tracker.onSlotEmptied(slot);
				newTrades = Collections.emptyList();
			}
			else
			{
				OfferSnapshot snapshot = new OfferSnapshot(offer.getItemId(), itemName(offer.getItemId()),
					isBuy(offer.getState()), offer.getPrice(), offer.getTotalQuantity(), offer.getQuantitySold(),
					offer.getSpent(), toStatus(offer.getState()), 0, 0);
				newTrades = tracker.onSlotChanged(slot, snapshot, now);
				if (newTrades.stream().anyMatch(trade -> !trade.isBuy()))
				{
					long before = FlipLedger.build(trades, taxExempt, 0).getTotalProfit();
					trades.addAll(newTrades);
					gpDropOverlay.addDrop(FlipLedger.build(trades, taxExempt, 0).getTotalProfit() - before);
				}
				else
				{
					trades.addAll(newTrades);
				}
			}

			Map<Integer, OfferSnapshot> slotsCopy = new HashMap<>(tracker.getSlots());
			slotView = slotsCopy;
			List<Trade> tradesCopy = newTrades.isEmpty() ? null : new ArrayList<>(trades);
			executor.execute(() ->
			{
				tradeStore.saveSlots(slotsCopy);
				if (tradesCopy != null)
				{
					tradeStore.saveTrades(tradesCopy);
				}
			});
		}

		for (Trade trade : newTrades)
		{
			log.debug("Recorded trade: {}", trade);
		}
		executor.execute(this::updateTradeViews);
	}

	// ---------------------------------------------------------------- panel actions (Swing thread)

	@Override
	public void refreshPrices()
	{
		executor.execute(this::refreshNow);
	}

	@Override
	public void changeSort(SortOrder order)
	{
		configManager.setConfiguration(FredFlipsConfig.GROUP, "sortOrder", order);
	}

	@Override
	public void changeSetting(String key, Object value)
	{
		configManager.setConfiguration(FredFlipsConfig.GROUP, key, value);
	}

	@Override
	public void toggleFavourite(int itemId)
	{
		List<Integer> favourites = favouriteIds();
		if (!favourites.remove((Integer) itemId))
		{
			favourites.add(itemId);
		}
		StringBuilder csv = new StringBuilder();
		for (Integer id : favourites)
		{
			csv.append(csv.length() == 0 ? "" : ",").append(id);
		}
		configManager.setConfiguration(FredFlipsConfig.GROUP, FAVOURITES_KEY, csv.toString());
		onSwing(FredFlipsPanel::refreshFavouriteStar);
		executor.execute(this::showFavourites);
	}

	@Override
	public boolean isFavourite(int itemId)
	{
		return favouriteIds().contains(itemId);
	}

	/**
	 * Starred item IDs in the order they were starred. Stored as a hidden setting, not shown in the config panel.
	 */
	private List<Integer> favouriteIds()
	{
		List<Integer> ids = new ArrayList<>();
		String csv = configManager.getConfiguration(FredFlipsConfig.GROUP, FAVOURITES_KEY);
		if (csv != null)
		{
			for (String part : csv.split(","))
			{
				try
				{
					ids.add(Integer.parseInt(part.trim()));
				}
				catch (NumberFormatException ignored)
				{
					// Skip anything that isn't an item ID.
				}
			}
		}
		return ids;
	}

	private void showFavourites()
	{
		MarketSnapshot snapshot = lastSnapshot;
		if (snapshot == null)
		{
			return;
		}
		List<FlipCandidate> favourites = new ArrayList<>();
		for (int id : favouriteIds())
		{
			ItemMapping item = itemsById.get(id);
			FlipCandidate numbers = item == null ? null : FlipFinder.calculate(item, snapshot, taxExempt.contains(id), config.cashStack());
			if (numbers != null)
			{
				favourites.add(numbers);
			}
		}
		onSwing(p -> p.showFavourites(favourites));
	}

	@Override
	public void loadHistory(boolean arbitrage)
	{
		MarketSnapshot snapshot = lastSnapshot;
		if (snapshot == null)
		{
			return;
		}
		// Most-traded items by gp value, so the history requests go where they matter.
		List<ItemMapping> candidates = new ArrayList<>();
		for (ItemMapping item : snapshot.getItems())
		{
			if ((config.membersItems() || !item.isMembers()) && snapshot.getOneDay() != null && snapshot.getOneDay().get(item.getId()) != null
				&& snapshot.getLatest().get(item.getId()) != null && snapshot.getLatest().get(item.getId()).getLow() != null)
			{
				candidates.add(item);
			}
		}
		// Half by gp traded (the big items), half by number of trades (everyday items like runes and potions).
		java.util.LinkedHashSet<ItemMapping> chosenSet = new java.util.LinkedHashSet<>();
		candidates.sort(Comparator.comparingDouble((ItemMapping item) -> gpTradedPerDay(snapshot, item.getId())).reversed());
		chosenSet.addAll(candidates.subList(0, Math.min(HISTORY_ITEMS / 2, candidates.size())));
		candidates.sort(Comparator.comparingLong((ItemMapping item) -> tradesPerDay(snapshot, item.getId())).reversed());
		for (ItemMapping item : candidates)
		{
			if (chosenSet.size() >= HISTORY_ITEMS)
			{
				break;
			}
			chosenSet.add(item);
		}
		List<ItemMapping> chosen = new ArrayList<>(chosenSet);

		String timestep = arbitrage ? "1h" : "24h";
		List<CompletableFuture<HistoryScanner.Result>> futures = new ArrayList<>();
		for (ItemMapping item : chosen)
		{
			LatestPrice latest = snapshot.getLatest().get(item.getId());
			futures.add(cachedTimeseries(item.getId(), timestep).thenApply(points -> arbitrage
				? HistoryScanner.timeOfDay(item.getId(), item.getName(), points, taxExempt.contains(item.getId()), ZoneId.systemDefault())
				: HistoryScanner.allTimeLow(item.getId(), item.getName(), points, latest.getLow(),
					latest.getHigh() == null ? latest.getLow() : latest.getHigh()))
				.exceptionally(error -> null));
		}
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((ignored, error) ->
		{
			List<HistoryScanner.Result> results = new ArrayList<>();
			for (CompletableFuture<HistoryScanner.Result> future : futures)
			{
				HistoryScanner.Result result = future.getNow(null);
				if (result != null)
				{
					results.add(result);
				}
			}
			results.sort(arbitrage
				? Comparator.comparingDouble((HistoryScanner.Result r) -> r.getProfitEach() * r.getPercent()).reversed()
				: Comparator.comparingDouble(HistoryScanner.Result::getPercent));
			onSwing(p -> p.showHistory(results, arbitrage));
		});
	}

	private static long tradesPerDay(MarketSnapshot snapshot, int itemId)
	{
		AveragePrice day = snapshot.getOneDay().get(itemId);
		return day.getHighPriceVolume() + day.getLowPriceVolume();
	}

	private static double gpTradedPerDay(MarketSnapshot snapshot, int itemId)
	{
		AveragePrice day = snapshot.getOneDay().get(itemId);
		Long price = day.getAvgLowPrice() != null ? day.getAvgLowPrice() : day.getAvgHighPrice();
		return price == null ? 0 : (double) price * (day.getHighPriceVolume() + day.getLowPriceVolume());
	}

	/**
	 * Price history from the wiki, kept for {@link #HISTORY_CACHE_MILLIS} so reopening a list doesn't refetch it.
	 */
	private CompletableFuture<List<TimeseriesPoint>> cachedTimeseries(int itemId, String timestep)
	{
		String key = itemId + "/" + timestep;
		CachedSeries cached = historyCache.get(key);
		if (cached != null && System.currentTimeMillis() - cached.fetchedAt < HISTORY_CACHE_MILLIS)
		{
			return CompletableFuture.completedFuture(cached.points);
		}
		return priceClient.fetchTimeseries(itemId, timestep).thenApply(points ->
		{
			historyCache.put(key, new CachedSeries(points, System.currentTimeMillis()));
			return points;
		});
	}

	@lombok.Value
	private static class CachedSeries
	{
		List<TimeseriesPoint> points;
		long fetchedAt;
	}

	@Override
	public void changeMoneySort(MoneySort sort)
	{
		configManager.setConfiguration(FredFlipsConfig.GROUP, "moneySort", sort);
	}

	@Override
	public void changeMoverType(MoverType type)
	{
		executor.execute(() -> showMovers(type));
	}

	@Override
	public void openItem(int itemId)
	{
		ItemMapping item = itemsById.get(itemId);
		MarketSnapshot snapshot = lastSnapshot;
		if (item == null || snapshot == null || panel == null)
		{
			return;
		}

		FlipCandidate numbers = FlipFinder.calculate(item, snapshot, taxExempt.contains(itemId), config.cashStack());
		panel.openItem(item, numbers);
		if (item.getLimit() != null && item.getLimit() > 0)
		{
			BuyLimits.Usage usage = limitUsage(itemId);
			long resetIn = usage.getResetsAt() - System.currentTimeMillis();
			panel.showLimitUsage(itemId, usage.getBought(), item.getLimit(), usage.getBought() == 0 ? 0 : resetIn);
		}
		ChartPeriod period = panel.getChartPeriod();

		priceClient.fetchTimeseries(itemId, "1h").whenComplete((hourly, error) ->
		{
			if (error != null)
			{
				log.debug("Couldn't load history for {}", itemId, error);
				onSwing(p -> p.showItemInsightsError(itemId));
				return;
			}
			ItemInsights insights = ItemInsights.fromHourly(hourly);
			long price = numbers != null ? numbers.getBuyPrice()
				: snapshot.getLatest().get(itemId) != null && snapshot.getLatest().get(itemId).getLow() != null
				? snapshot.getLatest().get(itemId).getLow() : 0;
			int buyLimit = item.getLimit() == null ? 0 : item.getLimit();
			cachedTimeseries(itemId, "24h").whenComplete((daily, dailyError) ->
			{
				if (dailyError == null && price > 0)
				{
					com.fredflips.insights.LongTermInsights longTerm =
						com.fredflips.insights.LongTermInsights.of(daily, hourly, price, taxExempt.contains(itemId));
					onSwing(p -> p.showItemLongTerm(itemId, longTerm, buyLimit));
				}
			});
			onSwing(p -> p.showItemInsights(itemId, insights, numbers));
			if (period == ChartPeriod.WEEK)
			{
				onSwing(p -> p.showItemChart(itemId, period, hourly));
			}
		});
		if (period != ChartPeriod.WEEK)
		{
			loadChart(itemId, period);
		}
	}

	@Override
	public void findOnGe(int itemId)
	{
		ItemMapping item = itemsById.get(itemId);
		if (item == null)
		{
			return;
		}
		geHelper.requestSearch(item.getName());
		if (panel != null)
		{
			panel.showStatus("Now click Buy on an empty GE slot - " + item.getName() + " will be searched for you");
		}
	}

	@Override
	public void changeChartPeriod(ChartPeriod period)
	{
		if (panel != null && panel.getOpenItemId() >= 0)
		{
			loadChart(panel.getOpenItemId(), period);
		}
	}

	@Override
	public List<ItemMapping> searchItems(String query)
	{
		String needle = query.toLowerCase(Locale.ROOT);
		List<ItemMapping> matches = new ArrayList<>();
		for (ItemMapping item : itemsById.values())
		{
			if (item.getName().toLowerCase(Locale.ROOT).contains(needle))
			{
				matches.add(item);
			}
		}
		// Names that start with the search come first, then shorter names.
		matches.sort(Comparator
			.comparing((ItemMapping item) -> !item.getName().toLowerCase(Locale.ROOT).startsWith(needle))
			.thenComparingInt(item -> item.getName().length()));
		return matches;
	}

	@Override
	public void buildPlan()
	{
		executor.execute(() ->
		{
			MarketSnapshot snapshot = lastSnapshot;
			int occupied;
			synchronized (tradeLock)
			{
				occupied = tracker.getSlots().size();
			}
			int freeSlots = Math.max(0, config.planSlots() - occupied);

			if (config.cashStack() <= 0)
			{
				plan = null;
				planMessage = "Set your Cash stack in the settings first. Plan mode splits it across your free slots.";
			}
			else if (snapshot == null)
			{
				plan = null;
				planMessage = "Prices are still loading - try again in a moment.";
			}
			else if (freeSlots == 0)
			{
				plan = null;
				planMessage = "No free GE slots. Collect or cancel an offer, or raise \"Slots to use\" in the settings.";
			}
			else
			{
				FlipPlan built = FlipPlan.build(FlipFinder.findAll(snapshot, config), config.cashStack(), freeSlots,
					config.planMinConfidence());
				plan = built;
				planMessage = built.getEntries().isEmpty()
					? "No flips meet the plan rules right now. Try again later or lower the plan's minimum confidence."
					: null;
			}
			updateTradeViews();
		});
	}

	@Override
	public void swapPlanEntry(int itemId)
	{
		FlipPlan current = plan;
		if (current != null)
		{
			plan = current.swap(itemId);
			executor.execute(this::updateTradeViews);
		}
	}

	@Override
	public void setCashStack(long gp)
	{
		// The setting is an int, so very large stacks are capped at about 2.1b.
		configManager.setConfiguration(FredFlipsConfig.GROUP, "cashStack", (int) Math.min(gp, Integer.MAX_VALUE));
	}

	@Override
	public void clearTradeLog()
	{
		executor.execute(() ->
		{
			synchronized (tradeLock)
			{
				trades.clear();
				tradeStore.clearTrades();
			}
			updateTradeViews();
		});
	}

	// ---------------------------------------------------------------- prices

	private void scheduleRefresh()
	{
		cancelRefresh();
		refreshTask = executor.scheduleWithFixedDelay(this::refreshNow, 0, config.refreshSeconds(), TimeUnit.SECONDS);
	}

	private void cancelRefresh()
	{
		if (refreshTask != null)
		{
			refreshTask.cancel(false);
			refreshTask = null;
		}
	}

	private void refreshNow()
	{
		// Skip if the previous download hasn't finished, so slow responses can't pile up.
		if (!fetching.compareAndSet(false, true))
		{
			return;
		}

		priceClient.fetchSnapshot().whenComplete((snapshot, error) ->
		{
			fetching.set(false);
			if (error != null)
			{
				log.warn("Couldn't download prices from the OSRS Wiki", error);
				onSwing(p -> p.showOffline("Couldn't reach the OSRS Wiki - showing older prices"));
				return;
			}

			Map<Integer, ItemMapping> byId = new HashMap<>();
			for (ItemMapping item : snapshot.getItems())
			{
				byId.put(item.getId(), item);
			}
			itemsById = byId;
			taxExempt = new HashSet<>(GeTax.exemptIds(snapshot.getItems()));
			lastSnapshot = snapshot;

			rebuildMarketViews();
			updateTradeViews();
		});
	}

	private void rebuildMarketViews()
	{
		MarketSnapshot snapshot = lastSnapshot;
		if (snapshot == null || panel == null)
		{
			return;
		}
		List<FlipCandidate> flips = FlipFinder.find(snapshot, config);
		onSwing(p ->
		{
			p.showPricesTime(snapshot.getFetchedAt());
			p.showFlips(flips);
			executor.execute(() -> showMovers(p.getMoverType()));
		});
		showMoneyMakers();
		showFavourites();
		List<DecantScanner.Option> decants = DecantScanner.scan(snapshot, config);
		onSwing(p -> p.showDecants(decants));
		List<ItemSets.Option> sets = ItemSets.scan(itemSets.getSets(), snapshot, taxExempt, config);
		onSwing(p -> p.showItemSets(sets));
	}

	/**
	 * Skill levels arrive one StatChanged at a time on login, so refreshes are bunched into one.
	 */
	private void queueMoneyRefresh()
	{
		if (moneyRefreshQueued.compareAndSet(false, true))
		{
			executor.schedule(() ->
			{
				moneyRefreshQueued.set(false);
				showMoneyMakers();
			}, 1, TimeUnit.SECONDS);
		}
	}

	private void showMoneyMakers()
	{
		MarketSnapshot snapshot = lastSnapshot;
		if (snapshot == null)
		{
			return;
		}
		Map<String, Integer> levels = skillLevels.size() >= Skill.values().length - 1 ? new HashMap<>(skillLevels) : null;
		List<MoneyResult> results = MoneyMakers.rank(moneyMakers.getMethods(), snapshot, itemsById, taxExempt,
			config.membersItems(), levels, config.moneyOnlyMyLevels(), config.moneyProcessingOnly(),
			config.moneyBankStandingOnly(),
			config.maxPriceAgeMinutes(), config.moneySort(),
			config.maxResults());
		onSwing(p -> p.showMoney(results, levels != null));
	}

	private void showMovers(MoverType type)
	{
		MarketSnapshot snapshot = lastSnapshot;
		if (snapshot == null)
		{
			return;
		}
		List<Mover> movers = MarketScanner.scan(type, snapshot, config);
		onSwing(p -> p.showMovers(movers, type));
	}

	private void loadChart(int itemId, ChartPeriod period)
	{
		priceClient.fetchTimeseries(itemId, period.getTimestep()).whenComplete((points, error) ->
		{
			if (error == null)
			{
				onSwing(p -> p.showItemChart(itemId, period, points));
			}
		});
	}

	// ---------------------------------------------------------------- trades

	/**
	 * Loads saved trades and slots when the logged-in account changes. Caller must hold tradeLock.
	 */
	private void loadTradeDataIfProfileChanged()
	{
		String profile = configManager.getRSProfileKey();
		if (Objects.equals(profile, loadedProfile))
		{
			return;
		}
		loadedProfile = profile;
		trades.clear();
		if (profile == null)
		{
			tracker.restore(null);
			slotView = Collections.emptyMap();
			return;
		}
		trades.addAll(tradeStore.loadTrades());
		tracker.restore(tradeStore.loadSlots());
		slotView = new HashMap<>(tracker.getSlots());
		log.debug("Loaded {} trades for profile {}", trades.size(), profile);
	}

	private void updateTradeViews()
	{
		if (panel == null)
		{
			return;
		}
		Map<Integer, OfferSnapshot> slots;
		List<Trade> tradesCopy;
		synchronized (tradeLock)
		{
			slots = new HashMap<>(tracker.getSlots());
			tradesCopy = new ArrayList<>(trades);
		}
		long now = System.currentTimeMillis();
		long startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
		FlipLedger ledger = FlipLedger.build(tradesCopy, taxExempt, startOfToday);
		Map<Integer, FlipLedger.OpenPosition> holdings = new HashMap<>();
		for (FlipLedger.OpenPosition position : ledger.getOpen())
		{
			holdings.put(position.getItemId(), position);
		}
		holdingsView = holdings;
		MarketSnapshot snapshot = lastSnapshot;
		Set<Integer> exempt = taxExempt;
		FlipPlan currentPlan = plan;
		String message = planMessage;
		boolean tracking = config.trackTrades();
		int staleMinutes = config.staleOfferMinutes();

		onSwing(p ->
		{
			p.showActive(slots, ledger.getOpen(), snapshot == null ? null : snapshot.getLatest(), exempt, staleMinutes, tracking, now);
			p.showDone(ledger, tradesCopy, now);
			p.showPlan(currentPlan, message, slots, ledger.getOpen(), tradesCopy);
		});
	}

	/**
	 * What to offer in the GE price/quantity boxes: the plan's numbers if the item is in your plan,
	 * otherwise FredFlips' usual (cautious) prices. Called on the client thread.
	 */
	private GeSuggestion geSuggestion(int itemId, boolean sell)
	{
		FlipPlan currentPlan = plan;
		if (currentPlan != null)
		{
			for (FlipPlan.Entry entry : currentPlan.getEntries())
			{
				FlipCandidate flip = entry.getFlip();
				if (flip.getItemId() == itemId)
				{
					LatestPrice market = marketPrice(itemId);
					return new GeSuggestion("Plan", sell ? flip.getSellPrice() : flip.getBuyPrice(), sell ? null : capToLimitLeft(itemId, entry.getQuantity()),
						quickPrice(market, sell), patientPrice(market, sell));
				}
			}
		}

		MarketSnapshot snapshot = lastSnapshot;
		ItemMapping item = itemsById.get(itemId);
		if (snapshot == null || item == null)
		{
			return null;
		}
		FlipCandidate numbers = FlipFinder.calculate(item, snapshot, taxExempt.contains(itemId), config.cashStack());
		if (numbers == null)
		{
			return null;
		}
		Long quantity = sell || numbers.getQuantity() <= 0 ? null : capToLimitLeft(itemId, numbers.getQuantity());
		LatestPrice market = snapshot.getLatest().get(itemId);
		return new GeSuggestion("FredFlips", sell ? numbers.getSellPrice() : numbers.getBuyPrice(), quantity,
			quickPrice(market, sell), patientPrice(market, sell));
	}

	/**
	 * Never suggest buying more than what's left of the item's 4-hour buy limit.
	 */
	private Long capToLimitLeft(int itemId, long quantity)
	{
		ItemMapping item = itemsById.get(itemId);
		if (item == null || item.getLimit() == null || item.getLimit() <= 0)
		{
			return quantity;
		}
		long left = limitUsage(itemId).remaining(item.getLimit());
		return left <= 0 ? null : Math.min(quantity, left);
	}

	private BuyLimits.Usage limitUsage(int itemId)
	{
		synchronized (tradeLock)
		{
			return BuyLimits.usage(trades, itemId, System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onVarbitChanged(net.runelite.api.events.VarbitChanged event)
	{
		// Picking an item in the GE search sets this; open it in the Item tab.
		if (event.getVarpId() != net.runelite.api.gameval.VarPlayerID.TRADINGPOST_SEARCH || !config.geOpenItemOnOffer())
		{
			return;
		}
		int itemId = event.getValue();
		if (itemId > 0 && itemsById.containsKey(itemId))
		{
			javax.swing.SwingUtilities.invokeLater(() -> openItem(itemId));
		}
	}

	/**
	 * Most likely to fill now: pay the latest instant-buy price, or sell at the latest instant-sell price.
	 */
	private static Long quickPrice(LatestPrice market, boolean sell)
	{
		return market == null ? null : sell ? market.getLow() : market.getHigh();
	}

	/**
	 * A better price that may take longer: bid the latest instant-sell price, or ask the latest instant-buy price.
	 */
	private static Long patientPrice(LatestPrice market, boolean sell)
	{
		return market == null ? null : sell ? market.getHigh() : market.getLow();
	}

	Map<Integer, FlipLedger.OpenPosition> getHoldingsView()
	{
		return holdingsView;
	}

	boolean isTaxExempt(int itemId)
	{
		return taxExempt.contains(itemId);
	}

	Map<Integer, OfferSnapshot> getSlotView()
	{
		return slotView;
	}

	LatestPrice marketPrice(int itemId)
	{
		MarketSnapshot snapshot = lastSnapshot;
		return snapshot == null ? null : snapshot.getLatest().get(itemId);
	}

	GeSuggestion suggestionFor(int itemId, boolean sell)
	{
		return geSuggestion(itemId, sell);
	}

	private String itemName(int itemId)
	{
		ItemMapping item = itemsById.get(itemId);
		return item != null ? item.getName() : itemManager.getItemComposition(itemId).getName();
	}

	private static boolean isBuy(GrandExchangeOfferState state)
	{
		return state == GrandExchangeOfferState.BUYING
			|| state == GrandExchangeOfferState.BOUGHT
			|| state == GrandExchangeOfferState.CANCELLED_BUY;
	}

	private static OfferStatus toStatus(GrandExchangeOfferState state)
	{
		switch (state)
		{
			case BUYING:
				return OfferStatus.BUYING;
			case BOUGHT:
				return OfferStatus.BOUGHT;
			case SELLING:
				return OfferStatus.SELLING;
			case SOLD:
				return OfferStatus.SOLD;
			case CANCELLED_BUY:
				return OfferStatus.CANCELLED_BUY;
			default:
				return OfferStatus.CANCELLED_SELL;
		}
	}

	private void onSwing(java.util.function.Consumer<FredFlipsPanel> update)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				update.accept(panel);
			}
		});
	}
}
