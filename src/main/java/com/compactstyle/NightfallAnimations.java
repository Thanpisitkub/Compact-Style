package com.compactstyle;

import java.util.HashMap;
import java.util.Map;

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
import yesman.epicfight.world.damagesource.StunType;

/**
 * Moves from Epic Fight Nightfall animation sets: nf_dual (two blades), nf_sword (sword) and ng_great (greatsword),
 * played with bare hands. Only the hit timing is defined here (measured from the keyframes: the window around the
 * fastest hand movement). The keyframe files (assets/compact_style/animmodels/animations/biped/nightfall/*.json) are
 * NOT part of this project's source: they are added to the jar by hand for personal use. A set is registered only when
 * all its files are present; otherwise the styles fall back to Weapons of Miracles moves.
 */
public final class NightfallAnimations {
	private static final String BASE = "/assets/" + CompactStyle.MODID + "/animmodels/animations/biped/nightfall/";

	/** A move: the file name, whether it is an attack, whether it leaps, and its hits as (hand 0=right 1=left, start, end). */
	private static final class Move {
		final String file;
		final boolean attack;
		final boolean jump;
		final float[] hits;

		Move(String file, boolean attack, boolean jump, float... hits) {
			this.file = file;
			this.attack = attack;
			this.jump = jump;
			this.hits = hits;
		}

		String set() {
			return this.file.substring(0, this.file.lastIndexOf('_'));
		}
	}

	private static final Move[] MOVES = {
		new Move("nf_dual_auto1", true, false, 1, 0.19F, 0.35F, 0, 0.37F, 0.53F),
		new Move("nf_dual_auto2", true, false, 0, 0.19F, 0.35F),
		new Move("nf_dual_auto3", true, false, 1, 0.25F, 0.41F),
		new Move("nf_dual_auto4", true, false, 0, 0.35F, 0.51F),
		new Move("nf_dual_airslash", true, true, 0, 0.2F, 0.36F),
		new Move("nf_dual_dash", true, false, 1, 0.17F, 0.33F),
		new Move("nf_dual_idle", false, false),
		new Move("nf_dual_walk", false, false),
		new Move("nf_dual_run", false, false),
		new Move("nf_sword_auto1", true, false, 0, 0.19F, 0.35F),
		new Move("nf_sword_auto2", true, false, 0, 0.27F, 0.43F),
		new Move("nf_sword_auto3", true, false, 0, 0.3F, 0.46F),
		new Move("nf_sword_auto4", true, false, 0, 0.25F, 0.41F),
		new Move("nf_sword_airslash", true, true, 0, 0.27F, 0.43F),
		new Move("nf_sword_dash", true, false, 0, 0.34F, 0.5F),
		new Move("nf_sword_idle", false, false),
		new Move("nf_sword_walk", false, false),
		new Move("nf_sword_run", false, false),
		new Move("ng_great_auto1", true, false, 0, 0.59F, 0.75F),
		new Move("ng_great_auto2", true, false, 0, 0.47F, 0.63F),
		new Move("ng_great_auto3", true, false, 0, 0.4F, 0.53F),
		new Move("ng_great_dash", true, false, 0, 0.45F, 0.61F),
		new Move("ng_great_airslash", true, true, 0, 0.72F, 0.88F),
		new Move("ng_great_idle", false, false),
		new Move("ng_great_walk", false, false),
		new Move("ng_great_run", false, false)
	};

	private static final Map<String, AnimationManager.AnimationAccessor<? extends StaticAnimation>> STATICS = new HashMap<>();
	private static final Map<String, AnimationManager.AnimationAccessor<? extends AttackAnimation>> ATTACKS = new HashMap<>();

	private NightfallAnimations() {
	}

	private static boolean present(String file) {
		return NightfallAnimations.class.getResource(BASE + file + ".json") != null;
	}

	/** True when a set is complete (every file of the set was added to the jar). */
	private static boolean setAvailable(String set) {
		for (Move move : MOVES) {
			if (move.set().equals(set) && !present(move.file)) {
				return false;
			}
		}

		return true;
	}

	/** True when at least one set is complete. */
	public static boolean available() {
		for (Move move : MOVES) {
			if (setAvailable(move.set())) {
				return true;
			}
		}

		return false;
	}

	/** An attack of a registered set, or null when its set is not available. */
	public static AnimationManager.AnimationAccessor<? extends AttackAnimation> attack(String file) {
		return ATTACKS.get(file);
	}

	/** A stance animation (idle, walk or run) of a registered set, or null. */
	public static AnimationManager.AnimationAccessor<? extends StaticAnimation> stance(String file) {
		return STATICS.get(file);
	}

	private static HumanoidArmature biped() {
		return (HumanoidArmature) Armatures.BIPED.get();
	}

	static void build(AnimationManager.AnimationBuilder builder) {
		for (Move move : MOVES) {
			if (!setAvailable(move.set())) {
				continue;
			}

			String path = "biped/nightfall/" + move.file;

			if (move.attack) {
				AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> accessor =
					builder.nextAccessor(path, (acc) -> attack(acc, move));
				ATTACKS.put(move.file, accessor);
			} else if (move.file.endsWith("_idle")) {
				AnimationManager.AnimationAccessor<StaticAnimation> accessor =
					builder.nextAccessor(path, (acc) -> new StaticAnimation(0.1F, true, acc, Armatures.BIPED));
				STATICS.put(move.file, accessor);
			} else {
				AnimationManager.AnimationAccessor<MovementAnimation> accessor =
					builder.nextAccessor(path, (acc) -> new MovementAnimation(0.1F, true, acc, Armatures.BIPED));
				STATICS.put(move.file, accessor);
			}
		}
	}

	private static BasicMultipleAttackAnimation attack(AnimationManager.AnimationAccessor<BasicMultipleAttackAnimation> acc, Move move) {
		int count = move.hits.length / 3;
		AttackAnimation.Phase[] phases = new AttackAnimation.Phase[count];
		float previousEnd = 0.0F;

		for (int i = 0; i < count; i++) {
			Joint joint = move.hits[i * 3] == 0.0F ? biped().toolR : biped().toolL;
			float contact = move.hits[i * 3 + 2] + 0.02F;
			float antic = Math.min(Math.max(move.hits[i * 3 + 1] - 0.02F, previousEnd), contact - 0.05F);
			phases[i] = new AttackAnimation.Phase(previousEnd, antic, contact, contact, i == count - 1 ? Float.MAX_VALUE : contact,
				joint, WOMWeaponColliders.KICK_HUGE);
			previousEnd = contact;
		}

		BasicMultipleAttackAnimation animation = new BasicMultipleAttackAnimation(0.1F, acc, Armatures.BIPED, phases);
		// Several hits in one move share one move's worth of damage (plus a little).
		animation.addProperty(AnimationProperty.AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(count == 1 ? 1.0F : 1.3F / count));
		animation.addProperty(AnimationProperty.AttackPhaseProperty.STUN_TYPE, StunType.HOLD);
		animation.addProperty(AnimationProperty.AttackAnimationProperty.BASIS_ATTACK_SPEED, 1.0F);

		if (move.jump) {
			// Lets a leaping move actually rise (Epic Fight ignores the vertical part of a move's motion otherwise).
			animation.addProperty(AnimationProperty.ActionAnimationProperty.MOVE_VERTICAL, true);
		}

		return animation;
	}
}
