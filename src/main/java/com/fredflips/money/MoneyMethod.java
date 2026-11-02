package com.fredflips.money;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A processing money maker from the OSRS Wiki's money making guides, e.g. "Enchanting sapphire rings".
 * Loaded from money_makers.json, which tools/build_money_makers.py builds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoneyMethod
{
	private String name;
	/**
	 * Wiki page title, e.g. "Money making guide/Enchanting sapphire rings".
	 */
	private String page;
	private String category;
	private boolean members;
	private List<Requirement> requirements;
	/**
	 * Actions per hour according to the wiki, or null when the guide only gives hourly amounts.
	 */
	private Double actionsPerHour;
	private List<Ingredient> inputsPerHour;
	private List<Ingredient> outputsPerHour;
	/**
	 * True when the wiki's guide says it can be done standing at a bank.
	 */
	private boolean bankStanding;
	/**
	 * Extra requirements or assumptions, e.g. "Needs Dream Mentor", or null.
	 */
	private String notes;

	public MoneyMethod(String name, String page, String category, boolean members, List<Requirement> requirements,
		Double actionsPerHour, List<Ingredient> inputsPerHour, List<Ingredient> outputsPerHour)
	{
		this(name, page, category, members, requirements, actionsPerHour, inputsPerHour, outputsPerHour, true, null);
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Requirement
	{
		private String skill;
		private int level;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Ingredient
	{
		private int id;
		private String name;
		/**
		 * How many per hour.
		 */
		private double quantity;
	}
}
