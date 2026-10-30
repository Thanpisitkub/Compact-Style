package com.blackbone.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Computes where a player's eyes are in world space from the interpolated head rotation. */
final class EyeGeometry {
	/** Direction the face looks at. */
	final Vec3 forward;
	/** Up direction of the head. */
	final Vec3 up;
	/** The player's left. */
	final Vec3 left;
	final Vec3 leftEye;
	final Vec3 rightEye;

	EyeGeometry(Player player, float partialTick) {
		float yawDeg = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
		float pitchDeg = Mth.lerp(partialTick, player.xRotO, player.getXRot());
		double yaw = Math.toRadians(yawDeg);
		double pitch = Math.toRadians(pitchDeg);

		this.forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
		this.up = new Vec3(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch));
		this.left = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));

		double x = Mth.lerp(partialTick, player.xo, player.getX());
		double y = Mth.lerp(partialTick, player.yo, player.getY());
		double z = Mth.lerp(partialTick, player.zo, player.getZ());

		// Head centre, then the eye row of the default skin layout (eyes sit slightly below the head centre
		// and 2 px in from each side of the face; the face plane is 4 px = 0.25 blocks in front of the centre).
		Vec3 head = new Vec3(x, y + player.getEyeHeight(player.getPose()) - 0.07D, z);
		Vec3 eyeCenter = head.add(this.up.scale(-0.03D)).add(this.forward.scale(0.262D));

		this.leftEye = eyeCenter.add(this.left.scale(0.125D));
		this.rightEye = eyeCenter.add(this.left.scale(-0.125D));
	}
}
