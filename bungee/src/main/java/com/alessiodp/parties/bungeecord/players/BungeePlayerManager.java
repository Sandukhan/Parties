package com.alessiodp.parties.bungeecord.players;

import com.alessiodp.parties.bungeecord.players.objects.BungeePartyPlayerImpl;
import com.alessiodp.parties.common.PartiesPlugin;
import com.alessiodp.parties.common.players.PlayerManager;
import com.alessiodp.parties.common.players.objects.PartyPlayerImpl;

import java.util.UUID;

public class BungeePlayerManager extends PlayerManager {
	public BungeePlayerManager(PartiesPlugin instance) {
		super(instance);
	}
	
	@Override
	public void reload() {
		super.reload();
	}
	
	@Override
	public PartyPlayerImpl initializePlayer(UUID playerUUID) {
		return new BungeePartyPlayerImpl(plugin, playerUUID);
	}
}
