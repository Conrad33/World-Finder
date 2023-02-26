package com.worldfinder.WorldFinder;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class WorldFinderPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(WorldFinderPlugin.class);
		RuneLite.main(args);
	}
}