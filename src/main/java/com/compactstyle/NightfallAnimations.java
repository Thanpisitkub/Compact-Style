package com.compactstyle;

import reascer.wom.animation.attacks.BasicMultipleAttackAnimation;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.model.armature.HumanoidArmature;

/**
 * Hand-to-hand moves from the Epic Fight Nightfall animation files (nf_meen and nf_claw sets). Only the hit
 * timing is defined here (measured from the keyframes: the window around the fastest hand or foot movement).
 * The keyframe files (assets/compact_style/animmodels/animations/biped/nightfall/*.json) are NOT part of this
 * project's source: they are added to the jar by hand for personal use. When they are missing nothing is
 * registered and the styles fall back to Weapons of Miracles moves.
 */
public final class NightfallAnimations {
	private static final String BASE = "/assets/" + CompactStyle.MODID + "/animmodels/animations/biped/nightfall/";
	private static final String[] FILES = {
		"nf_meen_auto1",
		"nf_meen_auto2",
		"nf_meen_auto3",
		"nf_meen_auto4",
		"nf_meen_airslash",
		"nf_meen_dash",
		"nf_claw_auto1",
		"nf_claw_auto2",
		"nf_claw_auto3",
		"nf_claw_airslash",
		"nf_claw_dash"
	};

	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_AUTO_1;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_AUTO_2;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_AUTO_3;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_AUTO_4;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_AIRSLASH;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> MEEN_DASH;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CLAW_AUTO_1;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CLAW_AUTO_2;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CLAW_AUTO_3;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CLAW_AIRSLASH;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CLAW_DASH;

	private NightfallAnimations() {
	}

	public static boolean available() {
		for (String file : FILES) {
			if (NightfallAnimations.class.getResource(BASE + file + ".json") == null) {
				return false;
			}
		}

		return true;
	}

	private static HumanoidArmature biped() {
		return (HumanoidArmature) Armatures.BIPED.get();
	}

	static void build(AnimationManager.AnimationBuilder builder) {
		MEEN_AUTO_1 = builder.nextAccessor("biped/nightfall/nf_meen_auto1", (acc) -> attack(acc, 0.45F, 0.64F, biped().toolR, WOMWeaponColliders.PUNCH));
		MEEN_AUTO_2 = builder.nextAccessor("biped/nightfall/nf_meen_auto2", (acc) -> attack(acc, 0.60F, 0.72F, biped().toolR, WOMWeaponColliders.PUNCH));
		MEEN_AUTO_3 = builder.nextAccessor("biped/nightfall/nf_meen_auto3", (acc) -> attack(acc, 0.99F, 1.13F, biped().toolR, WOMWeaponColliders.PUNCH));
		MEEN_AUTO_4 = builder.nextAccessor("biped/nightfall/nf_meen_auto4", (acc) -> attack(acc, 1.01F, 1.16F, biped().toolL, WOMWeaponColliders.PUNCH));
		MEEN_AIRSLASH = builder.nextAccessor("biped/nightfall/nf_meen_airslash", (acc) -> attack(acc, 0.45F, 0.59F, biped().legL, WOMWeaponColliders.KICK_HUGE));
		MEEN_DASH = builder.nextAccessor("biped/nightfall/nf_meen_dash", (acc) -> attack(acc, 0.82F, 1.08F, biped().legL, WOMWeaponColliders.KICK_HUGE));
		CLAW_AUTO_1 = builder.nextAccessor("biped/nightfall/nf_claw_auto1", (acc) -> attack(acc, 0.34F, 0.54F, biped().toolR, WOMWeaponColliders.PUNCH));
		CLAW_AUTO_2 = builder.nextAccessor("biped/nightfall/nf_claw_auto2", (acc) -> attack(acc, 0.29F, 0.42F, biped().toolL, WOMWeaponColliders.PUNCH));
		CLAW_AUTO_3 = builder.nextAccessor("biped/nightfall/nf_claw_auto3", (acc) -> attack(acc, 0.59F, 0.76F, biped().toolR, WOMWeaponColliders.PUNCH));
		CLAW_AIRSLASH = builder.nextAccessor("biped/nightfall/nf_claw_airslash", (acc) -> attack(acc, 0.64F, 0.79F, biped().toolR, WOMWeaponColliders.PUNCH));
		CLAW_DASH = builder.nextAccessor("biped/nightfall/nf_claw_dash", (acc) -> attack(acc, 0.37F, 0.49F, biped().toolR, WOMWeaponColliders.PUNCH));
	}

	private static BasicMultipleAttackAnimation attack(AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> acc,
			float hitStart, float hitEnd, Joint joint, Collider collider) {
		BasicMultipleAttackAnimation animation = new BasicMultipleAttackAnimation(0.1F, acc, Armatures.BIPED,
			new AttackAnimation.Phase(0.0F, hitStart, hitEnd, hitEnd + 0.05F, Float.MAX_VALUE, joint, collider));
		animation.addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.0F));
		animation.addProperty(AnimationProperty.AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F);
		return animation;
	}
}
