package com.worldfinder.WorldFinder;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Set;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("worldfinder")
public interface WorldFinderConfig extends Config
{
	String GROUP = "worldfinder";

	@ConfigItem(
		keyName = "previousKey",
		name = "Quick-hop previous",
		description = "When you press this key you'll hop to the previous world",
		position = 0
	)
	default Keybind previousKey()
	{
		return new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "nextKey",
		name = "Quick-hop next",
		description = "When you press this key you'll hop to the next world",
		position = 1
	)
	default Keybind nextKey()
	{
		return new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "quickhopOutOfDanger",
		name = "Quick-hop avoid dangerous",
		description = "Don't hop to a PVP/high risk world when quick-hopping",
		position = 2
	)
	default boolean quickhopOutOfDanger()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSidebar",
		name = "Show world switcher sidebar",
		description = "Show sidebar containing all worlds that mimics in-game interface",
		position = 4
	)
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ping",
		name = "Show world ping",
		description = "Shows ping to each game world",
		position = 5
	)
	default boolean ping()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMessage",
		name = "Show world hop message in chat",
		description = "Shows what world is being hopped to in the chat",
		position = 6
	)
	default boolean showWorldHopMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menuOption",
		name = "Show Hop-to menu option",
		description = "Adds Hop-to menu option to the friends list and friends chat members list",
		position = 7
	)
	default boolean menuOption()
	{
		return true;
	}

	@ConfigItem(
		keyName = "subscriptionFilter",
		name = "Subscription filter",
		description = "Only show free worlds, member worlds, or both types of worlds in sidebar",
		position = 8
	)
	default SubscriptionFilterMode subscriptionFilter()
	{
		return SubscriptionFilterMode.BOTH;
	}

	@ConfigItem(
		keyName = "regionFilter",
		name = "Region filter",
		description = "Only show worlds in specific regions (ctrl+click to choose multiple)",
		position = 9
	)
	default Set<RegionFilterMode> regionFilter()
	{
		return Collections.emptySet();
	}

	@ConfigItem(
		keyName = "worldTypeFilter",
		name = "World type filter",
		description = "Only show worlds of specific types (ctrl+click to choose multiple)",
		position = 10
	)
	default Set<WorldTypeFilter> worldTypeFilter()
	{
		return Collections.emptySet();
	}

	@ConfigItem(
		keyName = "skillTotalFilter",
		name = "Skill Total filter",
		description = "Only show worlds of skill total (ctrl+click to choose multiple)",
		position = 11
	)
	default Set<SkillTotalFilter> skillTotalFilter()
	{
		return Collections.emptySet();
	}

	@ConfigItem(
		keyName = "displayPing",
		name = "Display current ping",
		description = "Displays ping to current game world",
		position = 12
	)
	default boolean displayPing()
	{
		return false;
	}

	@ConfigItem(
		keyName = "twelveHourTime",
		name = "12 Hour Time",
		description = "Show time in 12 hour format.",
		position = 13
	)
	default boolean twelveHourTime()
	{
		return false;
	}

	@ConfigItem(
		keyName = "pingFilter",
		name = "Max Ping Filter",
		description = "Filter worlds with ping greater than... (0 is no filter)",
		position = 14
	)
	default int pingFilter()
	{
		return 0;
	}
}
