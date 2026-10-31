package com.compactstyle;

import reascer.wom.animation.attacks.BasicMultipleAttackAnimation;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.MovementAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.collider.MultiOBBCollider;
import yesman.epicfight.api.collider.OBBCollider;
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
	/** Sanji's jump attack (Concasse): leaps and lands with an area hit. */
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> CONCASSER;
	/** The Diable Jambe (leg-fire) set: stance, four kicks and the kick rush. */
	public static AnimationManager.AnimationAccessor<StaticAnimation> DIABLE_IDLE;
	public static AnimationManager.AnimationAccessor<MovementAnimation> DIABLE_WALK;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> DIABLE_AUTO_1;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> DIABLE_AUTO_2;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> DIABLE_AUTO_3;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> DIABLE_AUTO_4;
	public static AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> DIABLE_ANTIMANER;

	/** The Concasse landing box (taken from Epic Sanji): a wide, low box around the body. */
	private static final Collider LANDING_BOX = new OBBCollider(2.0D, 1.5D, 2.0D, 0.0D, 0.75D, 0.0D);
	/** The Anti-Manner kick box (taken from Epic Sanji): long and narrow. */
	private static final Collider LONG_KICK_BOX = new MultiOBBCollider(4, 0.8D, 2.0D, 0.8D, 0.0D, 0.0D, 0.0D);

	private SanjiAnimations() {
	}

	private static boolean present(String path) {
		return SanjiAnimations.class.getResource(BASE + path + ".json") != null;
	}

	/** True when all keyframe files were added to the jar. */
	public static boolean available() {
		return present("biped/living/sanji_idle") && present("biped/living/sanji_walk") && present("biped/living/sanji_run")
			&& present("biped/combat/sanji_auto_1") && present("biped/combat/sanji_auto_2")
			&& present("biped/combat/sanji_auto_3") && present("biped/combat/sanji_auto_4")
			&& present("biped/combat/sanji_concasser") && present("biped/living/sanji_diable_idle")
			&& present("biped/living/sanji_diable_walk") && present("biped/skill/sanji_diable_auto_1")
			&& present("biped/skill/sanji_diable_auto_2") && present("biped/skill/sanji_diable_auto_3")
			&& present("biped/skill/sanji_diable_auto_4") && present("biped/skill/sanji_diable_antimaner");
	}

	/**
	 * Epic Fight allows one animation builder per namespace, so the Epic Sanji moves and the Nightfall moves are
	 * registered together here.
	 */
	public static void register(AnimationManager.AnimationRegistryEvent event) {
		boolean sanji = available();
		boolean nightfall = NightfallAnimations.available();

		if (sanji || nightfall) {
			event.newBuilder(CompactStyle.MODID, (builder) -> {
				if (sanji) {
					build(builder);
				}

				if (nightfall) {
					NightfallAnimations.build(builder);
				}
			});
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

		// Concasse: the leap lasts until about 1.9s, the area hit is active the whole time (as in Epic Sanji).
		CONCASSER = builder.nextAccessor("biped/combat/sanji_concasser", (acc) -> {
			BasicMultipleAttackAnimation animation = attack(acc, 0.1F, 1.6F, 1.05F,
				new AttackAnimation.Phase(0.0F, 1.15F, 1.9F, 2.35F, Float.MAX_VALUE, biped().rootJoint, LANDING_BOX));
			animation.addProperty(AnimationProperty.AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(10.0F));
			animation.addProperty(AnimationProperty.ActionAnimationProperty.MOVE_VERTICAL, true);
			return animation;
		});

		// Diable Jambe: timings and joints as in Epic Sanji (right leg), damage scaled to the style's own damage.
		DIABLE_IDLE = builder.nextAccessor("biped/living/sanji_diable_idle", (acc) -> new StaticAnimation(0.1F, true, acc, Armatures.BIPED));
		DIABLE_WALK = builder.nextAccessor("biped/living/sanji_diable_walk", (acc) -> new MovementAnimation(0.1F, true, acc, Armatures.BIPED));

		DIABLE_AUTO_1 = builder.nextAccessor("biped/skill/sanji_diable_auto_1", (acc) -> attack(acc, 0.1F, 1.0F, 1.25F,
			phase(0.0F, 0.42F, 0.6F, 0.7F, Float.MAX_VALUE, biped().legR)));

		DIABLE_AUTO_2 = builder.nextAccessor("biped/skill/sanji_diable_auto_2", (acc) -> {
			// A flurry of six quick kicks.
			float[][] t = {{0.0F, 0.25F, 0.3F, 0.35F}, {0.35F, 0.4F, 0.45F, 0.5F}, {0.5F, 0.55F, 0.6F, 0.65F},
				{0.65F, 0.7F, 0.75F, 0.8F}, {0.8F, 0.85F, 0.9F, 0.95F}, {0.95F, 1.1F, 1.2F, 1.25F}};
			AttackAnimation.Phase[] phases = new AttackAnimation.Phase[t.length];

			for (int i = 0; i < t.length; i++) {
				phases[i] = phase(t[i][0], t[i][1], t[i][2], t[i][3], i == t.length - 1 ? Float.MAX_VALUE : t[i][3], biped().legR);
			}

			BasicMultipleAttackAnimation animation = attack(acc, 0.05F, 0.2F, 1.25F, phases);
			animation.addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.5F), 5);
			return animation;
		});

		DIABLE_AUTO_3 = builder.nextAccessor("biped/skill/sanji_diable_auto_3", (acc) -> {
			BasicMultipleAttackAnimation animation = attack(acc, 0.05F, 0.5F, 1.25F,
				new AttackAnimation.Phase(0.0F, 0.45F, 0.65F, 0.7F, 0.7F, biped().legR, LONG_KICK_BOX),
				phase(0.7F, 1.2F, 1.4F, 1.6F, Float.MAX_VALUE, biped().legR));
			animation.addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.8F), 1);
			return animation;
		});

		DIABLE_AUTO_4 = builder.nextAccessor("biped/skill/sanji_diable_auto_4", (acc) -> attack(acc, 0.1F, 1.5F, 1.25F,
			phase(0.0F, 0.45F, 0.65F, 0.85F, Float.MAX_VALUE, biped().legR)));

		DIABLE_ANTIMANER = builder.nextAccessor("biped/skill/sanji_diable_antimaner", (acc) -> attack(acc, 0.1F, 1.0F, 1.45F,
			phase(0.0F, 0.4F, 0.55F, 0.65F, Float.MAX_VALUE, biped().legR)));
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
