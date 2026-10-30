package com.blackbone.client;

import java.util.Random;

import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
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
 * Body aura for Flow (red) and Zone (blue), anime "power-up" style.
 *
 * Drawn in world space (not as a player RenderLayer) because Epic Fight replaces the vanilla player
 * renderer in battle mode; this way it works with every skin and animation and is seen by other players.
 *
 * Parts:
 *  - jagged lightning arcs hugging the body (red for Flow, blue for Zone), re-rolled every few ticks
 * Electric spark particles are added on the client tick. Not drawn for the local player in first person.
 */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AuraEffectRenderer {
	private AuraEffectRenderer() {
	}

	private static float[] core(BlackBoneMode mode) {
		return mode == BlackBoneMode.FLOW ? new float[] {1.0F, 0.18F, 0.10F} : new float[] {0.30F, 0.80F, 1.0F};
	}

	private static float[] outer(BlackBoneMode mode) {
		return mode == BlackBoneMode.FLOW ? new float[] {0.95F, 0.04F, 0.04F} : new float[] {0.10F, 0.42F, 1.0F};
	}

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

			// The camera is inside the shells in first person.
			if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
				continue;
			}

			if (consumer == null) {
				consumer = buffers.getBuffer(BlackBoneRenderTypes.GLOW);
			}

			double px = Mth.lerp(partialTick, player.xo, player.getX());
			double py = Mth.lerp(partialTick, player.yo, player.getY());
			double pz = Mth.lerp(partialTick, player.zo, player.getZ());
			float height = Math.max(player.getBbHeight(), 0.6F);
			Vec3 feet = new Vec3(px, py, pz);

			poseStack.pushPose();
			poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
			Matrix4f m = poseStack.last().pose();

						float[] inner = core(state.mode);
			float[] glow = outer(state.mode);

			// Lightning only: no body shells, wisps or ground ring.
			arcs(consumer, m, feet, height, camPos, inner, glow, time, player.getId());

			poseStack.popPose();
		}

		if (consumer != null) {
			buffers.endBatch(BlackBoneRenderTypes.GLOW);
		}
	}

	// ---------------------------------------------------------------- primitives

	private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, float[] c, float a) {
		vc.vertex(m, (float)x, (float)y, (float)z).color(c[0], c[1], c[2], Mth.clamp(a, 0.0F, 1.0F)).endVertex();
	}

	private static void v(VertexConsumer vc, Matrix4f m, Vec3 p, float[] c, float a) {
		v(vc, m, p.x, p.y, p.z, c, a);
	}

	/** Camera-facing tapered ribbon, bright at the start and transparent at the tip. */
	private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 start, Vec3 end, Vec3 camPos, float width, float[] tip, float[] base, float alpha) {
		Vec3 dir = end.subtract(start);
		Vec3 side = dir.cross(camPos.subtract(start));

		if (side.lengthSqr() < 1.0E-8D) {
			return;
		}

		side = side.normalize().scale(width);
		v(vc, m, start.add(side), base, alpha);
		v(vc, m, start.subtract(side), base, alpha);
		v(vc, m, end.subtract(side.scale(0.15D)), tip, 0.0F);
		v(vc, m, end.add(side.scale(0.15D)), tip, 0.0F);
	}

	/** Short jagged lightning arcs hugging the body. Pattern changes every few ticks. */
	private static void arcs(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, float[] inner, float[] glow, float time, int seedId) {
		Random rnd = new Random(seedId * 7919L + (long)(time / 3.0F));
		float[] white = {Mth.lerp(0.7F, inner[0], 1.0F), Mth.lerp(0.7F, inner[1], 1.0F), Mth.lerp(0.7F, inner[2], 1.0F)};

		for (int a = 0; a < 8; a++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double y = feet.y + rnd.nextDouble() * height * 0.8D;
			Vec3 prev = new Vec3(feet.x + Math.cos(angle) * 0.5D, y, feet.z + Math.sin(angle) * 0.5D);

			for (int s = 0; s < 4; s++) {
				angle += (rnd.nextDouble() - 0.4D) * 0.7D;
				y += (rnd.nextDouble() - 0.35D) * 0.45D;
				double r = 0.5D + rnd.nextDouble() * 0.22D;
				Vec3 next = new Vec3(feet.x + Math.cos(angle) * r, y, feet.z + Math.sin(angle) * r);
				ribbon(vc, m, prev, next, camPos, 0.026F, glow, white, 0.95F - s * 0.15F);
				prev = next;
			}
		}
	}

	// ----------------------------------------------------------------- particles

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;

		if (level == null || mc.isPaused() || BlackBoneStates.CLIENT.isEmpty()) {
			return;
		}

		for (Player player : level.players()) {
			BlackBoneState state = BlackBoneStates.CLIENT.get(player.getId());

			if (state == null || state.mode == BlackBoneMode.NONE || player.isInvisible() || player.isSpectator()) {
				continue;
			}

			float height = player.getBbHeight();

			if (level.random.nextInt(2) == 0) {
				level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX() + (level.random.nextDouble() - 0.5D) * 0.9D, player.getY() + level.random.nextDouble() * height,
						player.getZ() + (level.random.nextDouble() - 0.5D) * 0.9D, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	/** Forget everything when leaving a world so stale entity ids cannot leak into the next one. */
	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		BlackBoneStates.CLIENT.clear();
	}
}
