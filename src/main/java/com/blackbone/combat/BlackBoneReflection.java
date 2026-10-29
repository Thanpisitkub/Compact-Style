package com.blackbone.combat;

import com.blackbone.BlackBoneMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Black Bone damage reflection (10% of qualifying unarmed melee hits). */
public final class BlackBoneReflection {
	public static final float REFLECT_RATIO = 0.10F;
	public static final float REDUCTION_MULTIPLIER = 0.80F;

	/** Custom damage type, defined in data/blackbone/damage_type/black_bone_reflect.json. */
	public static final ResourceKey<DamageType> REFLECT_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(BlackBoneMod.MODID, "black_bone_reflect"));

	/** Re-entrancy guard: reflected damage can never trigger another reflection. */
	private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

	private BlackBoneReflection() {
	}

	/**
	 * @param finalDamage damage that was actually applied to the victim (after Black Bone's 20% reduction and armor)
	 */
	public static void tryReflect(ServerPlayer victim, DamageSource source, float finalDamage) {
		if (REFLECTING.get() || finalDamage <= 0.0F || source.is(REFLECT_TYPE)) {
			return;
		}

		Entity causing = source.getEntity();

		if (!(causing instanceof LivingEntity attacker) || attacker == victim || !attacker.isAlive()) {
			return;
		}

		// "Punch": direct, unarmed melee hit by an enemy. Projectiles, magic, fire, explosions and thorns never qualify.
		if (source.getDirectEntity() != attacker
			|| source.is(DamageTypeTags.IS_PROJECTILE)
			|| source.is(DamageTypeTags.IS_EXPLOSION)
			|| source.is(DamageTypeTags.IS_FIRE)
			|| source.is(DamageTypeTags.IS_FALL)
			|| source.is(DamageTypes.THORNS)
			|| source.is(DamageTypes.MAGIC)
			|| source.is(DamageTypes.INDIRECT_MAGIC)) {
			return;
		}

		if (!attacker.getMainHandItem().isEmpty() || attacker.isAlliedTo(victim)) {
			return;
		}

		float reflected = finalDamage * REFLECT_RATIO;

		if (reflected < 0.05F) {
			return;
		}

		DamageSource reflectSource = new DamageSource(victim.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(REFLECT_TYPE), victim);

		REFLECTING.set(Boolean.TRUE);

		try {
			attacker.hurt(reflectSource, reflected);
		} finally {
			REFLECTING.set(Boolean.FALSE);
		}
	}
}
