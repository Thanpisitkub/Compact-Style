package com.blackbone.effect;

import javax.annotation.Nullable;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Remembers one effect that a skill tried to grant, so it can be removed (and the
 * previous potion / beacon effect restored) without touching anything it does not own.
 */
public final class GrantedEffect {
	public final MobEffect effect;
	public final int amplifier;
	/** True if the skill's instance is the one currently controlling the effect. */
	public final boolean owned;
	/** Copy of the instance that existed before activation (potion, beacon, other mod), or null. */
	@Nullable
	public final MobEffectInstance prior;

	public GrantedEffect(MobEffect effect, int amplifier, boolean owned, @Nullable MobEffectInstance prior) {
		this.effect = effect;
		this.amplifier = amplifier;
		this.owned = owned;
		this.prior = prior;
	}
}
