package com.blackbone.effect;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Applies and removes skill effects while leaving effects from potions, beacons
 * or other mods alone.
 *
 * Rules:
 *  - If the player already has a stronger (or equal and longer / infinite) instance, the skill does not
 *    take ownership and never removes it.
 *  - If the player has a weaker / shorter instance, a copy is kept and restored (with its remaining time)
 *    when the skill ends.
 *  - On release, the effect is only removed if the live instance still looks like the skill's own
 *    (same amplifier and not extended by someone else).
 */
public final class EffectTracker {
	/** The effect lasts a few ticks longer than the skill so it never lapses early; the skill removes it itself. */
	public static final int PAD_TICKS = 5;

	private EffectTracker() {
	}

	public static void grant(ServerPlayer player, MobEffect effect, int amplifier, int skillTicks, List<GrantedEffect> out) {
		MobEffectInstance existing = player.getEffect(effect);
		int duration = skillTicks + PAD_TICKS;

		if (existing != null) {
			boolean strongerLevel = existing.getAmplifier() > amplifier;
			boolean sameLevelAndLonger = existing.getAmplifier() == amplifier && (existing.isInfiniteDuration() || existing.getDuration() >= duration);

			if (strongerLevel || sameLevelAndLonger) {
				out.add(new GrantedEffect(effect, amplifier, false, null));
				return;
			}
		}

		MobEffectInstance prior = existing == null ? null : new MobEffectInstance(existing);
		player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true, true));
		out.add(new GrantedEffect(effect, amplifier, true, prior));
	}

	/** Re-adds an owned effect that was cleared by something else (e.g. milk) while the skill is still running. */
	public static void reassert(ServerPlayer player, List<GrantedEffect> granted, int remainingTicks) {
		for (GrantedEffect g : granted) {
			if (g.owned && player.getEffect(g.effect) == null) {
				player.addEffect(new MobEffectInstance(g.effect, remainingTicks + PAD_TICKS, g.amplifier, false, true, true));
			}
		}
	}

	public static void release(ServerPlayer player, List<GrantedEffect> granted, int elapsedTicks, int remainingTicks) {
		for (GrantedEffect g : granted) {
			if (!g.owned) {
				continue;
			}

			MobEffectInstance current = player.getEffect(g.effect);

			if (current != null && current.getAmplifier() == g.amplifier && !current.isInfiniteDuration() && current.getDuration() <= remainingTicks + PAD_TICKS + 2) {
				player.removeEffect(g.effect);
			}

			if (g.prior != null && player.getEffect(g.effect) == null) {
				int left = g.prior.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION : g.prior.getDuration() - elapsedTicks;

				if (left == MobEffectInstance.INFINITE_DURATION || left > 0) {
					player.addEffect(new MobEffectInstance(g.effect, left, g.prior.getAmplifier(), g.prior.isAmbient(), g.prior.isVisible(), g.prior.showIcon()));
				}
			}
		}

		granted.clear();
	}
}
