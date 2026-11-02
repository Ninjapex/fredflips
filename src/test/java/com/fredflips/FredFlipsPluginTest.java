package com.fredflips;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches RuneLite with FredFlips loaded. Run with: gradlew run
 */
public class FredFlipsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FredFlipsPlugin.class);
		RuneLite.main(args);
	}
}
