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
		ribbon(vc, m, start, end, camPos, width, tip, base, alpha, 0.0F);
	}

	private static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 start, Vec3 end, Vec3 camPos, float width, float[] tip, float[] base, float alpha, float endAlpha) {
		Vec3 dir = end.subtract(start);
		Vec3 side = dir.cross(camPos.subtract(start));

		if (side.lengthSqr() < 1.0E-8D) {
			return;
		}

		side = side.normalize().scale(width);
		v(vc, m, start.add(side), base, alpha);
		v(vc, m, start.subtract(side), base, alpha);
		v(vc, m, end.subtract(side.scale(0.85D)), tip, endAlpha);
		v(vc, m, end.add(side.scale(0.85D)), tip, endAlpha);
	}

	/**
	 * The lightning show, built to look composed rather than random:
	 *  - three electric strands spiralling up around the body and slowly rotating
	 *  - a ring of crackling arcs jumping around the feet
	 *  - tall bolts that strike down at random spots around the player, fading in and out smoothly,
	 *    each with a soft glow flare on the ground
	 *  - a few forked arcs skittering over the body
	 * Every bolt is a wide soft glow under a thin white-hot core and tapers towards its tip.
	 * The jitter re-rolls every 2 ticks so it crackles.
	 */
	private static void arcs(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, float[] inner, float[] glow, float time, int seedId) {
		Random rnd = new Random(seedId * 7919L + (long)(time / 2.0F));
		float[] hot = {Mth.lerp(0.80F, inner[0], 1.0F), Mth.lerp(0.80F, inner[1], 1.0F), Mth.lerp(0.80F, inner[2], 1.0F)};
		float flicker = 0.8F + 0.2F * rnd.nextFloat();
		float top = height + 0.3F;

		// Spiral strands around the body.
		for (int k = 0; k < 3; k++) {
			float base = time * 0.09F + k * Mth.TWO_PI / 3.0F;
			int points = 26;
			Vec3 prev = null;

			for (int i = 0; i <= points; i++) {
				float t = (float)i / points;
				float angle = base + t * 7.0F;
				float radius = 0.50F + 0.07F * Mth.sin(t * 9.0F + time * 0.2F + k);
				double jitter = 0.035D;
				Vec3 point = new Vec3(
						feet.x + Mth.cos(angle) * radius + (rnd.nextDouble() - 0.5D) * jitter,
						feet.y + t * top + (rnd.nextDouble() - 0.5D) * jitter,
						feet.z + Mth.sin(angle) * radius + (rnd.nextDouble() - 0.5D) * jitter);

				if (prev != null) {
					float env = Mth.sin(t * Mth.PI);
					float a = Mth.clamp(env * 1.4F, 0.0F, 1.0F) * flicker;
					float w = 0.026F * (0.35F + 0.65F * env);
					ribbon(vc, m, prev, point, camPos, w * 3.4F, glow, glow, 0.30F * a, 0.30F * a);
					ribbon(vc, m, prev, point, camPos, w, hot, hot, 0.95F * a, 0.95F * a);
				}

				prev = point;
			}
		}

		// Crackling ring of arcs around the feet.
		int ring = 10;

		for (int i = 0; i < ring; i++) {
			float a0 = time * -0.05F + i * Mth.TWO_PI / ring;
			float a1 = a0 + Mth.TWO_PI / ring * (0.55F + 0.35F * rnd.nextFloat());
			Vec3 from = new Vec3(feet.x + Mth.cos(a0) * 0.95D, feet.y + 0.05D, feet.z + Mth.sin(a0) * 0.95D);
			Vec3 to = new Vec3(feet.x + Mth.cos(a1) * 0.95D, feet.y + 0.05D + rnd.nextDouble() * 0.5D, feet.z + Mth.sin(a1) * 0.95D);
			bolt(vc, m, from, to, camPos, 5, 0.06D, 0.022F, glow, hot, flicker * 0.9F, rnd, 0.15D, 1);
		}

		// Tall strikes that fade in and out; each one picks a new spot when it starts.
		for (int k = 0; k < 3; k++) {
			float cycle = time * 0.055F + k / 3.0F;
			float phase = cycle - (float)Math.floor(cycle);
			Random strike = new Random(seedId * 31L + k * 977L + (long)Math.floor(cycle) * 7919L);
			double angle = strike.nextDouble() * Math.PI * 2.0D;
			double r = 1.0D + strike.nextDouble() * 1.1D;
			float env = Mth.sin(phase * Mth.PI);
			env = env * env;
			Vec3 from = new Vec3(feet.x + Math.cos(angle) * r, feet.y + 0.02D, feet.z + Math.sin(angle) * r);
			Vec3 to = from.add((strike.nextDouble() - 0.5D) * 0.5D, height + 1.0D + strike.nextDouble() * 1.2D, (strike.nextDouble() - 0.5D) * 0.5D);
			bolt(vc, m, from, to, camPos, 12, 0.16D, 0.045F, glow, hot, flicker * env, rnd, 0.28D, 2);
			flare(vc, m, from, 0.55F + 0.35F * env, glow, hot, 0.8F * env);
		}

		// A few forked arcs over the body.
		for (int a = 0; a < 6; a++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double y = feet.y + rnd.nextDouble() * height;
			Vec3 from = new Vec3(feet.x + Math.cos(angle) * 0.5D, y, feet.z + Math.sin(angle) * 0.5D);
			double angle2 = angle + (rnd.nextDouble() - 0.5D) * 1.6D;
			Vec3 to = new Vec3(feet.x + Math.cos(angle2) * 0.55D, Mth.clamp(y + (rnd.nextDouble() - 0.5D) * 0.9D, feet.y, feet.y + top), feet.z + Math.sin(angle2) * 0.55D);
			bolt(vc, m, from, to, camPos, 5, 0.1D, 0.03F, glow, hot, flicker, rnd, 0.4D, 1);
		}
	}

	/** Flat soft glow on the ground where a bolt lands (radial gradient, additive). */
	private static void flare(VertexConsumer vc, Matrix4f m, Vec3 center, float radius, float[] glow, float[] hot, float alpha) {
		if (alpha <= 0.01F) {
			return;
		}

		int seg = 16;
		double y = center.y + 0.03D;

		for (int i = 0; i < seg; i++) {
			double a0 = Mth.TWO_PI * i / seg;
			double a1 = Mth.TWO_PI * (i + 1) / seg;
			v(vc, m, center.x, y, center.z, hot, alpha);
			v(vc, m, center.x + Math.cos(a0) * radius, y, center.z + Math.sin(a0) * radius, glow, 0.0F);
			v(vc, m, center.x + Math.cos(a1) * radius, y, center.z + Math.sin(a1) * radius, glow, 0.0F);
			v(vc, m, center.x + Math.cos(a1) * radius, y, center.z + Math.sin(a1) * radius, glow, 0.0F);
		}
	}

	/** A jagged bolt from start to end, with optional forks (depth levels). */
	private static void bolt(VertexConsumer vc, Matrix4f m, Vec3 start, Vec3 end, Vec3 camPos, int segments, double jitter, float width,
			float[] glow, float[] hot, float alpha, Random rnd, double forkChance, int depth) {
		Vec3 prev = start;

		for (int s = 1; s <= segments; s++) {
			double t = (double)s / segments;
			Vec3 point = start.lerp(end, t);

			if (s < segments) {
				point = point.add((rnd.nextDouble() - 0.5D) * 2.0D * jitter, (rnd.nextDouble() - 0.5D) * 2.0D * jitter, (rnd.nextDouble() - 0.5D) * 2.0D * jitter);
			}

			float fade = 1.0F - 0.35F * (float)t;
			// Wide glow under a thin hot core.
			ribbon(vc, m, prev, point, camPos, width * 3.2F, glow, glow, 0.38F * alpha * fade, 0.38F * alpha * fade * 0.9F);
			ribbon(vc, m, prev, point, camPos, width, hot, hot, alpha * fade, alpha * fade * 0.9F);

			if (depth > 0 && s < segments && rnd.nextDouble() < forkChance) {
				Vec3 dir = end.subtract(start);
				Vec3 branchEnd = point.add(dir.scale(0.35D)).add((rnd.nextDouble() - 0.5D) * 1.0D, (rnd.nextDouble() - 0.5D) * 0.7D, (rnd.nextDouble() - 0.5D) * 1.0D);
				bolt(vc, m, point, branchEnd, camPos, Math.max(2, segments / 2), jitter * 0.8D, width * 0.6F, glow, hot, alpha * 0.8F, rnd, 0.0D, depth - 1);
			}

			prev = point;
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

			for (int i = 0; i < 3; i++) {
				level.addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX() + (level.random.nextDouble() - 0.5D) * 1.6D, player.getY() + level.random.nextDouble() * (height + 0.5D),
						player.getZ() + (level.random.nextDouble() - 0.5D) * 1.6D, (level.random.nextDouble() - 0.5D) * 0.2D, 0.05D, (level.random.nextDouble() - 0.5D) * 0.2D);
			}
		}
	}

	/** Forget everything when leaving a world so stale entity ids cannot leak into the next one. */
	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		BlackBoneStates.CLIENT.clear();
	}
}
