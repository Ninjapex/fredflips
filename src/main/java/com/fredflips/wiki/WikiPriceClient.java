package com.fredflips.wiki;

import com.google.gson.Gson;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Talks to the OSRS Wiki real-time prices API.
 * See https://oldschool.runescape.wiki/w/RuneScape:Real-time_Prices
 */
@Slf4j
@Singleton
public class WikiPriceClient
{
	private static final String BASE_URL = "https://prices.runescape.wiki/api/v1/osrs";
	// The wiki asks every tool to identify itself so they can contact the author if it misbehaves.
	private static final String USER_AGENT = "FredFlips RuneLite plugin - github.com/Ninjapex/fredflips";

	private final OkHttpClient httpClient;
	private final Gson gson;

	// Item names and buy limits rarely change, so this is fetched once per session.
	private volatile List<ItemMapping> mapping;

	@Inject
	WikiPriceClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public CompletableFuture<MarketSnapshot> fetchSnapshot()
	{
		CompletableFuture<List<ItemMapping>> mappingFuture = mapping != null
			? CompletableFuture.completedFuture(mapping)
			: get("/mapping", ItemMapping[].class).thenApply(items ->
			{
				mapping = Arrays.asList(items);
				return mapping;
			});
		CompletableFuture<LatestResponse> latestFuture = get("/latest", LatestResponse.class);
		CompletableFuture<AverageResponse> fiveMinuteFuture = get("/5m", AverageResponse.class);
		CompletableFuture<AverageResponse> oneHourFuture = get("/1h", AverageResponse.class);
		CompletableFuture<AverageResponse> oneDayFuture = get("/24h", AverageResponse.class);

		return CompletableFuture.allOf(mappingFuture, latestFuture, fiveMinuteFuture, oneHourFuture, oneDayFuture)
			.thenApply(ignored -> new MarketSnapshot(
				mappingFuture.join(),
				latestFuture.join().data,
				fiveMinuteFuture.join().data,
				oneHourFuture.join().data,
				oneDayFuture.join().data,
				Instant.now().getEpochSecond()));
	}

	/**
	 * Price history for one item, oldest first.
	 *
	 * @param timestep "5m", "1h", "6h" or "24h". The wiki returns up to 365 buckets.
	 */
	public CompletableFuture<List<TimeseriesPoint>> fetchTimeseries(int itemId, String timestep)
	{
		return get("/timeseries?id=" + itemId + "&timestep=" + timestep, TimeseriesResponse.class)
			.thenApply(response -> response.data == null
				? Collections.<TimeseriesPoint>emptyList()
				: Arrays.asList(response.data));
	}

	private <T> CompletableFuture<T> get(String path, Class<T> type)
	{
		Request request = new Request.Builder()
			.url(BASE_URL + path)
			.header("User-Agent", USER_AGENT)
			.build();

		CompletableFuture<T> future = new CompletableFuture<>();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				future.completeExceptionally(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						throw new IOException("Wiki returned HTTP " + response.code() + " for " + path);
					}
					T parsed = gson.fromJson(body.charStream(), type);
					if (parsed == null)
					{
						throw new IOException("Wiki returned an empty body for " + path);
					}
					future.complete(parsed);
				}
				catch (Exception e)
				{
					log.debug("Failed to load {}", path, e);
					future.completeExceptionally(e);
				}
			}
		});
		return future;
	}

	private static class LatestResponse
	{
		Map<Integer, LatestPrice> data;
	}

	private static class AverageResponse
	{
		Map<Integer, AveragePrice> data;
	}

	private static class TimeseriesResponse
	{
		TimeseriesPoint[] data;
	}
}
