package com.blackbone.client;

import com.blackbone.BlackBoneMod;
import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Glowing eyes for Flow (red) and Zone (blue).
 *
 * Why a world-space renderer instead of a player RenderLayer: in battle mode Epic Fight replaces the
 * vanilla player rendering with its own patched renderer, so a vanilla layer would not be reliable.
 * Drawing additive geometry in the level render stage works with every skin, every Epic Fight animation
 * and with other players' clients, and never touches the skin texture.
 *
 * Parts: an emissive eye box (real 3D depth), a soft radial glow around each eye, and animated energy
 * streaks that radiate away from the eyes (jagged for Zone). Dust / spark particles are added on the client tick.
 */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class EyeEffectRenderer {
	private static final int STREAKS = 7;
	private static final int GLOW_SEGMENTS = 16;

	private EyeEffectRenderer() {
	}

	// ------------------------------------------------------------------ colours

	private static float[] coreColor(BlackBoneMode mode) {
		return mode == BlackBoneMode.FLOW ? new float[] {1.0F, 0.10F, 0.07F} : new float[] {0.25F, 0.78F, 1.0F};
	}

	private static float[] glowColor(BlackBoneMode mode) {
		return mode == BlackBoneMode.FLOW ? new float[] {1.0F, 0.04F, 0.04F} : new float[] {0.08F, 0.40F, 1.0F};
	}

	// ------------------------------------------------------------------- render

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;

		if (level == null || BlackBoneStates.CLIENT.isEmpty()) {
			return;
		}

		float partialTick = event.getPartialTick();
		Camera camera = event.getCamera();
		Vec3 camPos = camera.getPosition();
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer consumer = null;
		float time = level.getGameTime() + partialTick;

		for (Player player : level.players()) {
			BlackBoneState state = BlackBoneStates.CLIENT.get(player.getId());

			if (state == null || state.mode == BlackBoneMode.NONE || player.isInvisible() || player.isSpectator()) {
				continue;
			}

			// The camera sits inside the head in first person, so skip the local player there.
			if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
				continue;
			}

			if (consumer == null) {
				consumer = buffers.getBuffer(BlackBoneRenderTypes.GLOW);
			}

			poseStack.pushPose();
			poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
			Matrix4f matrix = poseStack.last().pose();

			EyeGeometry geometry = new EyeGeometry(player, partialTick);
			renderEye(consumer, matrix, geometry, geometry.leftEye, state.mode, camera, camPos, time, 0.0F);
			renderEye(consumer, matrix, geometry, geometry.rightEye, state.mode, camera, camPos, time, 3.7F);

			poseStack.popPose();
		}

		if (consumer != null) {
			buffers.endBatch(BlackBoneRenderTypes.GLOW);
		}
	}

	private static void renderEye(VertexConsumer vc, Matrix4f m, EyeGeometry g, Vec3 eye, BlackBoneMode mode, Camera camera, Vec3 camPos, float time, float seedOffset) {
		float[] core = coreColor(mode);
		float[] glow = glowColor(mode);
		float pulse = 0.85F + 0.15F * Mth.sin(time * 0.6F + seedOffset);

		Vec3 camLeft = new Vec3(camera.getLeftVector());
		Vec3 camUp = new Vec3(camera.getUpVector());

		// Soft radial glow (camera-facing discs, centre bright -> edge transparent).
		Vec3 glowCenter = eye.add(g.forward.scale(0.02D));
		disc(vc, m, glowCenter, camLeft, camUp, 0.42F * pulse, glow, 0.38F * pulse);
		disc(vc, m, glowCenter, camLeft, camUp, 0.16F, glow, 0.75F * pulse);

		// Emissive eye: a small box with real depth. Front is white-hot, sides take the colour.
		float[] hot = {Mth.lerp(0.55F, core[0], 1.0F), Mth.lerp(0.55F, core[1], 1.0F), Mth.lerp(0.55F, core[2], 1.0F)};
		box(vc, m, eye, g, 0.058D, 0.036D, 0.02D, core, hot, pulse);

		// Energy streaks radiating from the eye.
		boolean zone = mode == BlackBoneMode.ZONE;
		int count = zone ? STREAKS + 2 : STREAKS;

		for (int i = 0; i < count; i++) {
			float seed = i * 1.618F + seedOffset;
			float phase = (time * (zone ? 0.11F : 0.07F) + i * 0.371F) % 1.0F;
			float angle = i * (Mth.TWO_PI / count) + Mth.sin(time * 0.05F + seed) * 0.45F;

			Vec3 dir = g.left.scale(Mth.cos(angle) * 0.9D).add(g.up.scale(Mth.sin(angle) * 0.9D)).add(g.forward.scale(0.55D)).normalize();
			float length = (zone ? 0.35F : 0.25F) + (zone ? 0.75F : 0.55F) * phase;
			float width = (zone ? 0.016F : 0.024F) * (1.0F - 0.5F * phase);
			float alpha = (1.0F - phase) * 0.9F * pulse;

			Vec3 start = eye.add(dir.scale(0.06D));
			Vec3 end = eye.add(dir.scale(length));

			if (zone) {
				// Jagged, lightning-like streak: bend the middle point sideways.
				Vec3 side = dir.cross(g.forward).normalize();
				Vec3 mid = start.add(end).scale(0.5D).add(side.scale(Mth.sin(time * 2.3F + seed) * 0.05D));
				streak(vc, m, start, mid, camPos, width, glow, core, alpha);
				streak(vc, m, mid, end, camPos, width * 0.7F, glow, core, alpha * 0.8F);
			} else {
				streak(vc, m, start, end, camPos, width, glow, core, alpha);
			}
		}
	}

	// ---------------------------------------------------------------- primitives

	private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, float[] c, float a) {
		vc.vertex(m, (float)p.x, (float)p.y, (float)p.z).color(c[0], c[1], c[2], Mth.clamp(a, 0.0F, 1.0F)).endVertex();
	}

	/** Radial gradient disc made of quads that degenerate into triangles. */
	private static void disc(VertexConsumer vc, Matrix4f m, Vec3 center, Vec3 right, Vec3 up, float radius, float[] color, float centerAlpha) {
		for (int i = 0; i < GLOW_SEGMENTS; i++) {
			float a0 = i * Mth.TWO_PI / GLOW_SEGMENTS;
			float a1 = (i + 1) * Mth.TWO_PI / GLOW_SEGMENTS;
			Vec3 p0 = center.add(right.scale(Mth.cos(a0) * radius)).add(up.scale(Mth.sin(a0) * radius));
			Vec3 p1 = center.add(right.scale(Mth.cos(a1) * radius)).add(up.scale(Mth.sin(a1) * radius));

			vertex(vc, m, center, color, centerAlpha);
			vertex(vc, m, p0, color, 0.0F);
			vertex(vc, m, p1, color, 0.0F);
			vertex(vc, m, p1, color, 0.0F);
		}
	}

	/** Camera-facing tapered ribbon: bright at the start, transparent at the tip. */
	private static void streak(VertexConsumer vc, Matrix4f m, Vec3 start, Vec3 end, Vec3 camPos, float width, float[] outer, float[] inner, float alpha) {
		Vec3 dir = end.subtract(start);
		Vec3 toCamera = camPos.subtract(start);
		Vec3 side = dir.cross(toCamera);

		if (side.lengthSqr() < 1.0E-8D) {
			return;
		}

		side = side.normalize().scale(width);

		vertex(vc, m, start.add(side), inner, alpha);
		vertex(vc, m, start.subtract(side), inner, alpha);
		vertex(vc, m, end, outer, 0.0F);
		vertex(vc, m, end, outer, 0.0F);
	}

	/** Five faces of a box (the back face is inside the head). Axes follow the head orientation. */
	private static void box(VertexConsumer vc, Matrix4f m, Vec3 center, EyeGeometry g, double hw, double hh, double hd, float[] side, float[] front, float pulse) {
		Vec3 w = g.left.scale(hw);
		Vec3 h = g.up.scale(hh);
		Vec3 d = g.forward.scale(hd);

		// Front (towards where the player looks).
		Vec3 fc = center.add(d);
		vertex(vc, m, fc.add(w).add(h), front, pulse);
		vertex(vc, m, fc.subtract(w).add(h), front, pulse);
		vertex(vc, m, fc.subtract(w).subtract(h), front, pulse);
		vertex(vc, m, fc.add(w).subtract(h), front, pulse);

		float a = 0.8F * pulse;

		// Left / right / top / bottom faces.
		quadFace(vc, m, center.add(w), h, d, side, a);
		quadFace(vc, m, center.subtract(w), h, d, side, a);
		quadFace(vc, m, center.add(h), w, d, side, a);
		quadFace(vc, m, center.subtract(h), w, d, side, a);
	}

	private static void quadFace(VertexConsumer vc, Matrix4f m, Vec3 faceCenter, Vec3 axisA, Vec3 axisB, float[] color, float alpha) {
		vertex(vc, m, faceCenter.add(axisA).add(axisB), color, alpha);
		vertex(vc, m, faceCenter.subtract(axisA).add(axisB), color, alpha);
		vertex(vc, m, faceCenter.subtract(axisA).subtract(axisB), color, alpha * 0.6F);
		vertex(vc, m, faceCenter.add(axisA).subtract(axisB), color, alpha * 0.6F);
	}

	// ----------------------------------------------------------------- particles

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;

		if (level == null || mc.isPaused() || BlackBoneStates.CLIENT.isEmpty() || level.getGameTime() % 2L != 0L) {
			return;
		}

		for (Player player : level.players()) {
			BlackBoneState state = BlackBoneStates.CLIENT.get(player.getId());

			if (state == null || state.mode == BlackBoneMode.NONE || player.isInvisible() || player.isSpectator()) {
				continue;
			}

			if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
				continue;
			}

			EyeGeometry g = new EyeGeometry(player, 1.0F);
			boolean flow = state.mode == BlackBoneMode.FLOW;
			DustParticleOptions dust = new DustParticleOptions(flow ? new Vector3f(1.0F, 0.08F, 0.06F) : new Vector3f(0.2F, 0.65F, 1.0F), 0.6F);

			for (Vec3 eye : new Vec3[] {g.leftEye, g.rightEye}) {
				Vec3 v = g.forward.scale(0.04D).add(g.left.scale((level.random.nextDouble() - 0.5D) * 0.06D)).add(g.up.scale((level.random.nextDouble() - 0.3D) * 0.06D));
				level.addParticle(dust, eye.x, eye.y, eye.z, v.x, v.y, v.z);

				if (!flow && level.random.nextInt(3) == 0) {
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, eye.x, eye.y, eye.z, v.x * 2.0D, v.y * 2.0D, v.z * 2.0D);
				}
			}
		}
	}

	/** Forget everything when leaving a world so stale entity ids cannot leak into the next one. */
	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		BlackBoneStates.CLIENT.clear();
	}
}
