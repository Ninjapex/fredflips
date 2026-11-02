package com.fredflips.wiki;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One entry from the wiki's /mapping endpoint: static facts about an item.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemMapping
{
	private int id;
	private String name;
	private boolean members;
	/**
	 * GE buy limit per 4 hours, or null when the wiki doesn't know it.
	 */
	private Integer limit;
	/**
	 * High alchemy value, or null if the item can't be alched.
	 */
	private Integer highalch;

	public ItemMapping(int id, String name, boolean members, Integer limit)
	{
		this(id, name, members, limit, null);
	}
}
