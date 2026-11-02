package com.fredflips.money;

import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.MarketSnapshot;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * The bundled list of processing money makers, and ranking them by live GP/hr.
 */
@Slf4j
@Singleton
public class MoneyMakers
{
	private static final String RESOURCE = "/com/fredflips/money_makers.json";
	// A method counts as processing when bought materials are at least this share of what you sell.
	static final double PROCESSING_MIN_COST_SHARE = 0.2;

	private final List<MoneyMethod> methods;

	@Inject
	MoneyMakers(Gson gson)
	{
		this.methods = load(gson);
	}

	public List<MoneyMethod> getMethods()
	{
		return methods;
	}

	/**
	 * @param levels      your real skill levels by lower-case skill name, or null if unknown (not logged in)
	 * @param onlyMyLevels hide methods you don't have the levels for (only when levels are known)
	 */
	public static List<MoneyResult> rank(List<MoneyMethod> methods, MarketSnapshot snapshot, Map<Integer, ItemMapping> items,
		Set<Integer> taxExempt, boolean membersItems, Map<String, Integer> levels, boolean onlyMyLevels,
		boolean processingOnly, boolean bankStandingOnly, int maxPriceAgeMinutes, MoneySort sort, int maxResults)
	{
		List<MoneyResult> results = new ArrayList<>();
		for (MoneyMethod method : methods)
		{
			if ((method.isMembers() && !membersItems) || (bankStandingOnly && !method.isBankStanding())
				|| (onlyMyLevels && levels != null && !meetsLevels(method, levels)))
			{
				continue;
			}
			MoneyResult result = MoneyMakerCalculator.calculate(method, snapshot, items, taxExempt, maxPriceAgeMinutes);
			if (result != null && sort.getValue().applyAsLong(result) > 0 && (!processingOnly || isProcessing(result)))
			{
				results.add(result);
			}
		}
		results.sort(Comparator.comparingLong(sort.getValue()).reversed());
		return results.size() > maxResults ? new ArrayList<>(results.subList(0, maxResults)) : results;
	}

	/**
	 * Buy materials, make something, sell it - as opposed to gathering or combat with a few cheap supplies.
	 */
	static boolean isProcessing(MoneyResult result)
	{
		long revenue = result.getInstantGpPerHour() + result.getCostPerHour();
		return revenue > 0 && result.getCostPerHour() >= revenue * PROCESSING_MIN_COST_SHARE;
	}

	static boolean meetsLevels(MoneyMethod method, Map<String, Integer> levels)
	{
		if (method.getRequirements() == null)
		{
			return true;
		}
		for (MoneyMethod.Requirement requirement : method.getRequirements())
		{
			Integer level = levels.get(requirement.getSkill().toLowerCase(Locale.ROOT));
			if (level != null && level < requirement.getLevel())
			{
				return false;
			}
		}
		return true;
	}

	private static List<MoneyMethod> load(Gson gson)
	{
		try (InputStream in = MoneyMakers.class.getResourceAsStream(RESOURCE))
		{
			if (in == null)
			{
				log.warn("FredFlips money maker list is missing");
				return Collections.emptyList();
			}
			try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
			{
				Data data = gson.fromJson(reader, Data.class);
				return data == null || data.methods == null ? Collections.emptyList() : data.methods;
			}
		}
		catch (Exception e)
		{
			log.warn("Couldn't load FredFlips money maker list", e);
			return Collections.emptyList();
		}
	}

	private static class Data
	{
		List<MoneyMethod> methods;
	}
}
