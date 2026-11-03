package com.blackbone.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Computes where a player's eyes are in world space.
 *
 * Preferred source: the Epic Fight head joint, evaluated with the exact same pose the model is rendered with, so the
 * eyes follow every animation (dodges, spins, lying down, kicks ...). If Epic Fight cannot give a plausible answer the
 * vanilla head rotation is used instead.
 */
final class EyeGeometry {
	/** Eye offsets in Head-joint space (blocks): +X is the player's right, +Y up, -Z forward. */
	private static final float EYE_SIDE = 0.125F;
	private static final float EYE_UP = 0.01F;
	private static final float EYE_FORWARD = 0.262F;

	/** Direction the face looks at. */
	final Vec3 forward;
	/** Up direction of the head. */
	final Vec3 up;
	/** The player's left. */
	final Vec3 left;
	final Vec3 leftEye;
	final Vec3 rightEye;

	private static boolean epicFightBroken;

	EyeGeometry(Player player, float partialTick) {
		float yawDeg = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
		float pitchDeg = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		double yaw = Math.toRadians(yawDeg);
		double pitch = Math.toRadians(pitchDeg);

		Vec3 vForward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
		Vec3 vUp = new Vec3(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch));
		Vec3 vLeft = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));

		double x = Mth.lerp(partialTick, player.xo, player.getX());
		double y = Mth.lerp(partialTick, player.yo, player.getY());
		double z = Mth.lerp(partialTick, player.zo, player.getZ());

		// Head centre, then the eye row of the default skin layout (eyes sit slightly below the head centre
		// and 2 px in from each side of the face; the face plane is 4 px = 0.25 blocks in front of the centre).
		Vec3 head = new Vec3(x, y + player.getEyeHeight(player.getPose()) - 0.07D, z);
		Vec3 eyeCenter = head.add(vUp.scale(-0.03D)).add(vForward.scale(0.262D));

		Vec3 vLeftEye = eyeCenter.add(vLeft.scale(0.125D));
		Vec3 vRightEye = eyeCenter.add(vLeft.scale(-0.125D));

		Vec3[] ef = epicFightEyes(player, partialTick, x, y, z, eyeCenter, vForward);

		if (ef != null) {
			this.leftEye = ef[0];
			this.rightEye = ef[1];
			this.forward = ef[2];
			this.up = ef[3];
			this.left = ef[4];
		} else {
			this.leftEye = vLeftEye;
			this.rightEye = vRightEye;
			this.forward = vForward;
			this.up = vUp;
			this.left = vLeft;
		}
	}

	/** Returns {leftEye, rightEye, forward, up, left} or null when Epic Fight data is unavailable or implausible. */
	private static Vec3[] epicFightEyes(Player player, float partialTick, double x, double y, double z, Vec3 vanillaEyeCenter, Vec3 vanillaForward) {
		if (epicFightBroken) {
			return null;
		}

		try {
			LivingEntityPatch<?> patch = EpicFightCapabilities.getEntityPatch(player, LivingEntityPatch.class);

			if (patch == null) {
				return null;
			}

			Armature armature = patch.getArmature();
			Joint head = armature.searchJointByName("Head");

			if (head == null) {
				return null;
			}

			Pose pose = patch.getAnimator().getPose(partialTick);
			OpenMatrix4f headMatrix = armature.getBoundTransformFor(pose, head);

			OpenMatrix4f toWorld = OpenMatrix4f.createTranslation((float)x, (float)y, (float)z)
				.mulBack(OpenMatrix4f.createRotatorDeg(180.0F, Vec3f.Y_AXIS).mulBack(patch.getModelMatrix(partialTick)));
			OpenMatrix4f full = headMatrix.mulFront(toWorld);

			Vec3 origin = point(full, 0.0F, 0.0F, 0.0F);
			Vec3 right = point(full, 1.0F, 0.0F, 0.0F).subtract(origin);
			Vec3 up = point(full, 0.0F, 1.0F, 0.0F).subtract(origin);
			Vec3 back = point(full, 0.0F, 0.0F, 1.0F).subtract(origin);

			if (right.lengthSqr() < 1.0E-6D || up.lengthSqr() < 1.0E-6D || back.lengthSqr() < 1.0E-6D) {
				return null;
			}

			Vec3 forward = back.normalize().scale(-1.0D);
			Vec3 upN = up.normalize();
			Vec3 leftN = right.normalize().scale(-1.0D);

			Vec3 leftEye = point(full, -EYE_SIDE, EYE_UP, -EYE_FORWARD);
			Vec3 rightEye = point(full, EYE_SIDE, EYE_UP, -EYE_FORWARD);
			Vec3 center = leftEye.add(rightEye).scale(0.5D);

			// Plausibility: the head must be near where the vanilla head is and must face roughly the same way
			// (an animation can turn the body a lot, so this is deliberately loose).
			if (center.distanceTo(vanillaEyeCenter) > 1.6D || Double.isNaN(center.x) || Double.isNaN(forward.x)) {
				return null;
			}

			return new Vec3[] {leftEye, rightEye, forward, upN, leftN};
		} catch (Throwable t) {
			epicFightBroken = true;
			return null;
		}
	}

	private static Vec3 point(OpenMatrix4f m, float lx, float ly, float lz) {
		Vec3f v = OpenMatrix4f.transform3v(m, new Vec3f(lx, ly, lz), null);
		return new Vec3(v.x, v.y, v.z);
	}
}
