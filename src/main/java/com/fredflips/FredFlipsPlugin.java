package com.fredflips;

import com.fredflips.flip.FlipCandidate;
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
import com.fredflips.trades.FlipLedger;
import com.fredflips.trades.OfferSnapshot;
import com.fredflips.trades.OfferStatus;
import com.fredflips.trades.Trade;
import com.fredflips.trades.TradeStore;
import com.fredflips.trades.TradeTracker;
import com.fredflips.ui.FredFlipsPanel;
import com.fredflips.ui.PanelActions;
import com.fredflips.wiki.ItemMapping;
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
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "FredFlips",
	description = "Finds Grand Exchange flips using OSRS Wiki real-time prices, and tracks your own trades",
	tags = {"flipping", "flip", "merching", "grand exchange", "ge", "prices", "margin", "profit", "tracker"}
)
public class FredFlipsPlugin extends Plugin implements PanelActions
{
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
		if (!FredFlipsConfig.GROUP.equals(event.getGroup()) || TradeStore.isStorageKey(event.getKey()))
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
			case "showActiveTab":
			case "showDoneTab":
			case "showMoversTab":
			case "showItemTab":
				onSwing(p -> p.applyTabVisibility(config));
				return;
			case "showMoneyTab":
				onSwing(p -> p.applyTabVisibility(config));
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
				trades.addAll(newTrades);
			}

			Map<Integer, OfferSnapshot> slotsCopy = new HashMap<>(tracker.getSlots());
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
				onSwing(p -> p.showStatus("Couldn't reach the OSRS Wiki - showing older prices"));
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
			return;
		}
		trades.addAll(tradeStore.loadTrades());
		tracker.restore(tradeStore.loadSlots());
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
			p.showPlan(currentPlan, message, slots, ledger.getOpen());
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
					return new GeSuggestion("Plan", sell ? flip.getSellPrice() : flip.getBuyPrice(), sell ? null : entry.getQuantity());
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
		Long quantity = sell || numbers.getQuantity() <= 0 ? null : numbers.getQuantity();
		return new GeSuggestion("FredFlips", sell ? numbers.getSellPrice() : numbers.getBuyPrice(), quantity);
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
