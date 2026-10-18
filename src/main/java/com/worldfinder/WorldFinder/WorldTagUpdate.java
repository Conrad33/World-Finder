package com.worldfinder.WorldFinder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import net.runelite.client.party.messages.PartyMemberMessage;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorldTagUpdate extends PartyMemberMessage
{
	int worldId;
	boolean tagged;
}
