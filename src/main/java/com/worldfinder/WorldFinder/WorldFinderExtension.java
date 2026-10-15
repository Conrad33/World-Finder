package com.worldfinder.WorldFinder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.http.api.worlds.World;

/**
 * Everything World Finder adds on top of the upstream RuneLite World Hopper plugin.
 * The upstream files only call into this class from hooks marked with "// World Finder".
 */
@Singleton
class WorldFinderExtension
{
	private static final int HOP_START_TIMEOUT_TICKS = 5;
	private static final DateTimeFormatter TIME_24H = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("hh:mm");

	@Inject
	private Client client;

	@Inject
	private WorldHopperConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private EventBus eventBus;

	@Inject
	private PartyService partyService;

	@Inject
	private WSClient wsClient;

	private final Map<Integer, Long> lastVisited = new ConcurrentHashMap<>();
	private Runnable onTimersChanged;
	// client thread only
	private int hopSentTick = -1;
	private boolean hopStarted;

	void startUp(Runnable onTimersChanged)
	{
		this.onTimersChanged = onTimersChanged;
		migrateConfig();
		wsClient.registerMessage(WorldTimerUpdate.class);
		eventBus.register(this);
	}

	void shutDown()
	{
		eventBus.unregister(this);
		wsClient.unregisterMessage(WorldTimerUpdate.class);
		clearHop();
	}

	private void migrateConfig()
	{
		// World Finder's old TARGET world type filter is upstream's BOUNTY_HUNTER. Unknown enum names
		// deserialize to null, which would break the panel, so rename it in place.
		String worldTypes = configManager.getConfiguration(WorldHopperConfig.GROUP, "worldTypeFilter");
		if (worldTypes != null && worldTypes.contains("\"TARGET\""))
		{
			configManager.setConfiguration(WorldHopperConfig.GROUP, "worldTypeFilter",
				worldTypes.replace("\"TARGET\"", "\"BOUNTY_HUNTER\""));
		}
	}

	/**
	 * Stamps both the world hopped off of and the world hopped onto, and shares them with the party.
	 */
	void onWorldChanged(int fromWorld, int toWorld)
	{
		stamp(fromWorld);
		stamp(toWorld);
		onTimersChanged.run();
	}

	private void stamp(int worldId)
	{
		if (worldId <= 0)
		{
			return;
		}

		long now = System.currentTimeMillis();
		lastVisited.put(worldId, now);
		if (partyService.isInParty())
		{
			partyService.send(new WorldTimerUpdate(worldId, now));
		}
	}

	@Subscribe
	public void onWorldTimerUpdate(WorldTimerUpdate update)
	{
		lastVisited.put(update.getWorldId(), update.getTimeStamp());
		onTimersChanged.run();
	}

	@Nullable
	Long getLastVisited(int worldId)
	{
		return lastVisited.get(worldId);
	}

	String formatLastVisited(@Nullable Long timestamp)
	{
		if (timestamp == null)
		{
			return "-";
		}

		DateTimeFormatter format = config.twelveHourTime() ? TIME_12H : TIME_24H;
		return format.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
	}

	/**
	 * @return true if a world should be hidden from the panel and skipped by quick-hop
	 */
	boolean isFiltered(World world)
	{
		Set<SkillTotalFilter> skillTotals = config.skillTotalFilter();
		String activity = world.getActivity();
		if (!skillTotals.isEmpty() && activity != null && activity.contains("skill total"))
		{
			return skillTotals.stream().noneMatch(f -> activity.equals(f.getSkillTotal()));
		}

		return false;
	}

	/**
	 * True from the moment a hop is sent to the server until it has completed (or clearly failed to start).
	 * Requests made in that window are dropped, otherwise they are queued and fire as a second hop.
	 */
	boolean isHopInProgress()
	{
		return hopSentTick >= 0;
	}

	void onHopSent()
	{
		hopSentTick = client.getTickCount();
		hopStarted = false;
	}

	private void clearHop()
	{
		hopSentTick = -1;
		hopStarted = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (!isHopInProgress())
		{
			return;
		}

		switch (event.getGameState())
		{
			case HOPPING:
				hopStarted = true;
				break;
			case LOGGED_IN:
				if (hopStarted)
				{
					clearHop();
				}
				break;
			case LOGIN_SCREEN:
				clearHop();
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// The server refused the hop (eg. in combat), so stop blocking new ones
		if (isHopInProgress() && !hopStarted && client.getTickCount() - hopSentTick > HOP_START_TIMEOUT_TICKS)
		{
			clearHop();
		}
	}
}
