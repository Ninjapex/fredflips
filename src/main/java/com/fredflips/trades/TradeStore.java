package com.fredflips.trades;

import com.fredflips.FredFlipsConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Saves the trade log and GE slot memory in RuneLite's per-account settings.
 * Nothing leaves your computer except through RuneLite's own settings sync, if you use it.
 */
@Slf4j
@Singleton
public class TradeStore
{
	static final String TRADES_KEY = "tradeLog";
	static final String SLOTS_KEY = "geSlots";
	// Keeps the saved setting a sensible size. Oldest trades are dropped first.
	static final int MAX_TRADES = 5000;

	private static final Type TRADES_TYPE = new TypeToken<List<Trade>>()
	{
	}.getType();
	private static final Type SLOTS_TYPE = new TypeToken<Map<Integer, OfferSnapshot>>()
	{
	}.getType();

	private final ConfigManager configManager;
	private final Gson gson;

	@Inject
	TradeStore(ConfigManager configManager, Gson gson)
	{
		this.configManager = configManager;
		this.gson = gson;
	}

	public static boolean isStorageKey(String key)
	{
		return TRADES_KEY.equals(key) || SLOTS_KEY.equals(key);
	}

	public List<Trade> loadTrades()
	{
		List<Trade> trades = read(TRADES_KEY, TRADES_TYPE);
		return trades == null ? new ArrayList<>() : new ArrayList<>(trades);
	}

	public void saveTrades(List<Trade> trades)
	{
		List<Trade> kept = trades.size() > MAX_TRADES
			? trades.subList(trades.size() - MAX_TRADES, trades.size())
			: trades;
		configManager.setRSProfileConfiguration(FredFlipsConfig.GROUP, TRADES_KEY, gson.toJson(kept, TRADES_TYPE));
	}

	public Map<Integer, OfferSnapshot> loadSlots()
	{
		Map<Integer, OfferSnapshot> slots = read(SLOTS_KEY, SLOTS_TYPE);
		return slots == null ? new HashMap<>() : slots;
	}

	public void saveSlots(Map<Integer, OfferSnapshot> slots)
	{
		configManager.setRSProfileConfiguration(FredFlipsConfig.GROUP, SLOTS_KEY, gson.toJson(slots, SLOTS_TYPE));
	}

	public void clearTrades()
	{
		configManager.unsetRSProfileConfiguration(FredFlipsConfig.GROUP, TRADES_KEY);
	}

	private <T> T read(String key, Type type)
	{
		String json = configManager.getRSProfileConfiguration(FredFlipsConfig.GROUP, key);
		if (json == null || json.isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, type);
		}
		catch (RuntimeException e)
		{
			log.warn("Couldn't read saved FredFlips {} - starting fresh", key, e);
			return null;
		}
	}
}
