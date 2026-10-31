package com.compactstyle;

import reascer.wom.animation.attacks.BasicMultipleAttackAnimation;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

/**
 * Epic Sanji's stance and kick combo, re-created for Epic Fight 20.14 (Epic Sanji itself only exists for older
 * Epic Fight versions and cannot be loaded). Only the attack timings and hit boxes are written here; the
 * keyframe files (assets/compact_style/animmodels/animations/biped/.../sanji_*.json) are NOT part of this
 * project's source: they are added to the jar by hand for personal use. When they are missing nothing is
 * registered and the styles fall back to Weapons of Miracles moves.
 */
public final class SanjiAnimations {
	private static final String BASE = "/assets/" + CompactStyle.MODID + "/animmodels/animations/";

	public static AnimationManager.AnimationAccessor<StaticAnimation> IDLE;
	public static AnimationManager.AnimationAccessor<MovementAnimation> WALK;
	public static AnimationManager.AnimationAccessor<MovementAnimation> RUN;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> AUTO_1;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> AUTO_2;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> AUTO_3;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> AUTO_4;

	private SanjiAnimations() {
	}

	private static boolean present(String path) {
		return SanjiAnimations.class.getResource(BASE + path + ".json") != null;
	}

	/** True when all keyframe files were added to the jar. */
	public static boolean available() {
		return present("biped/living/sanji_idle") && present("biped/living/sanji_walk") && present("biped/living/sanji_run")
			&& present("biped/combat/sanji_auto_1") && present("biped/combat/sanji_auto_2")
			&& present("biped/combat/sanji_auto_3") && present("biped/combat/sanji_auto_4");
	}

	public static void register(AnimationManager.AnimationRegistryEvent event) {
		if (available()) {
			event.newBuilder(CompactStyle.MODID, SanjiAnimations::build);
		}
	}

	private static HumanoidArmature biped() {
		return (HumanoidArmature) Armatures.BIPED.get();
	}

	private static void build(AnimationManager.AnimationBuilder builder) {
		IDLE = builder.nextAccessor("biped/living/sanji_idle", (acc) -> new StaticAnimation(0.1F, true, acc, Armatures.BIPED));
		WALK = builder.nextAccessor("biped/living/sanji_walk", (acc) -> new MovementAnimation(0.1F, true, acc, Armatures.BIPED));
		RUN = builder.nextAccessor("biped/living/sanji_run", (acc) -> new MovementAnimation(0.1F, true, acc, Armatures.BIPED));

		AUTO_1 = builder.nextAccessor("biped/combat/sanji_auto_1", (acc) -> attack(acc, 0.1F, 0.6F, 1.05F,
			phase(0.0F, 0.2F, 0.38F, 0.4F, Float.MAX_VALUE, biped().legL)));

		AUTO_2 = builder.nextAccessor("biped/combat/sanji_auto_2", (acc) -> attack(acc, 0.05F, 0.65F, 1.05F,
			phase(0.0F, 0.3F, 0.5F, 0.55F, 0.55F, biped().legR),
			phase(0.55F, 0.6F, 0.7F, 1.1F, Float.MAX_VALUE, biped().toolL)));

		AUTO_3 = builder.nextAccessor("biped/combat/sanji_auto_3", (acc) -> {
			// A kick followed by a spinning flurry: nine quick hits around the body.
			AttackAnimation.Phase[] phases = new AttackAnimation.Phase[10];
			phases[0] = phase(0.0F, 0.25F, 0.35F, 0.4F, 0.4F, biped().toolL);

			for (int i = 1; i < 10; i++) {
				float t = 0.4F + (i - 1) * 0.25F;
				phases[i] = phase(t, t + 0.15F, t + 0.2F, t + 0.25F, i == 9 ? Float.MAX_VALUE : t + 0.25F, biped().rootJoint);
			}

			return attack(acc, 0.1F, 0.3F, 1.05F, phases);
		});

		AUTO_4 = builder.nextAccessor("biped/combat/sanji_auto_4", (acc) -> attack(acc, 0.05F, 1.4F, 1.35F,
			phase(0.0F, 0.4F, 0.6F, 0.7F, Float.MAX_VALUE, biped().legL)));
	}

	private static AttackAnimation.Phase phase(float start, float antic, float contact, float recovery, float end, Joint joint) {
		// KICK_HUGE is the large hit box the original Sanji moves used; AGONY's box is shaped for a held weapon
		// and missed (no damage) when attached to a hand or foot.
		return new AttackAnimation.Phase(start, antic, contact, recovery, end, joint, WOMWeaponColliders.KICK_HUGE);
	}

	private static BasicMultipleAttackAnimation attack(AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> acc, float convert,
			float damage, float speed, AttackAnimation.Phase... phases) {
		BasicMultipleAttackAnimation animation = new BasicMultipleAttackAnimation(convert, acc, Armatures.BIPED, phases);
		animation.addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(damage));
		animation.addProperty(AnimationProperty.AttackAnimationProperty.BASIS_ATTACK_SPEED, speed);
		return animation;
	}
}
