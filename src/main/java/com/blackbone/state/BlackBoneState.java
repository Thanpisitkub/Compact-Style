package com.blackbone.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackbone.effect.GrantedEffect;

/**
 * Per-player Flow / Zone state. On the server this object is authoritative;
 * on the client it is only a mirror filled by {@code SyncStatePacket}.
 */
public final class BlackBoneState {
	public int flowStacks;
	public int zoneStacks;
	public BlackBoneMode mode = BlackBoneMode.NONE;
	public int remainingTicks;

	// Server-side only bookkeeping (unused on the client mirror).
	public final Map<Integer, Long> lastHitTick = new HashMap<>();
	public final List<GrantedEffect> grantedEffects = new ArrayList<>();

	public int stacksOf(BlackBoneMode m) {
		return switch (m) {
			case FLOW -> this.flowStacks;
			case ZONE -> this.zoneStacks;
			default -> 0;
		};
	}

	public void setStacks(BlackBoneMode m, int value) {
		switch (m) {
			case FLOW -> this.flowStacks = value;
			case ZONE -> this.zoneStacks = value;
			default -> { }
		}
	}
}
