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

@Singleton
class WorldFinderExtension
{
	private static final int HOP_START_TIMEOUT_TICKS = 5;
	private static final DateTimeFormatter TIME_24H = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter TIME_12H = DateTimeFormatter.ofPattern("hh:mm");

	@Inject
	private Client client;

	@Inject
	private WorldFinderConfig config;

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
		// ConfigManager deserializes unknown enum names to null, which breaks WorldTypeFilter matching
		String worldTypes = configManager.getConfiguration(WorldFinderConfig.GROUP, "worldTypeFilter");
		if (worldTypes != null && worldTypes.contains("\"TARGET\""))
		{
			configManager.setConfiguration(WorldFinderConfig.GROUP, "worldTypeFilter",
				worldTypes.replace("\"TARGET\"", "\"BOUNTY_HUNTER\""));
		}
	}

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

	// A hop requested before the current one completes would be queued and fire after landing
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
		// The hop never started, e.g. the server refused it during combat
		if (isHopInProgress() && !hopStarted && client.getTickCount() - hopSentTick > HOP_START_TIMEOUT_TICKS)
		{
			clearHop();
		}
	}
}
