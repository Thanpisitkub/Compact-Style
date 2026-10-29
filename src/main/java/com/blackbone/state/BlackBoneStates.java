package com.blackbone.state;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * Holds the state maps. This class contains no client-only references,
 * so it is safe to load on a dedicated server.
 */
public final class BlackBoneStates {
	/** Authoritative, keyed by player UUID (server thread only). */
	public static final Map<UUID, BlackBoneState> SERVER = new ConcurrentHashMap<>();

	/** Mirror of what the server told this client, keyed by entity id. */
	public static final Map<Integer, BlackBoneState> CLIENT = new ConcurrentHashMap<>();

	private BlackBoneStates() {
	}

	/** Returns the state visible from the logical side of the given player (may be null). */
	public static BlackBoneState view(Player player) {
		if (player.level().isClientSide) {
			return CLIENT.get(player.getId());
		}

		return SERVER.get(player.getUUID());
	}
}
