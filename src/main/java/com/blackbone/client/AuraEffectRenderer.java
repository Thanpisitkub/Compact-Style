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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Flow (red) and Zone (blue) effects, drawn in world space so they are real 3D around the player.
 * (Epic Fight replaces the vanilla player renderer in battle mode, so a player RenderLayer is not
 * reliable; world-space geometry works with every skin and animation and is seen by other players.)
 *
 * Five layers, matching the reference sheet:
 *  1. body aura   - flame blades rising from a ring at the feet and curving in over the body, with white lightning veins
 *  2. power rings - four glowing rings that rise around the body and fade out
 *  3. lightning   - bold tapered bolts striking up from the ground, plus thin forked ones
 *  4. shards      - faceted crystal shards drifting upwards, spinning
 *  5. sparkles    - four-point light flares that twinkle
 *
 * Everything is additive. Not drawn for the local player in first person (the camera is inside it).
 */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AuraEffectRenderer {
	private static final int BLADES = 30;
	private static final int RINGS = 4;
	private static final int SHARDS = 16;
	private static final int SPARKLES = 14;

	private AuraEffectRenderer() {
	}

	/** hot (white-ish core), main, deep (dark edge) */
	private record Palette(float[] hot, float[] main, float[] deep) {
		static Palette of(BlackBoneMode mode) {
			return mode == BlackBoneMode.FLOW
				? new Palette(new float[] {1.0F, 0.88F, 0.82F}, new float[] {1.0F, 0.22F, 0.20F}, new float[] {0.75F, 0.03F, 0.08F})
				: new Palette(new float[] {0.88F, 0.96F, 1.0F}, new float[] {0.32F, 0.70F, 1.0F}, new float[] {0.06F, 0.30F, 0.95F});
		}
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
		Vec3 camLeft = new Vec3(camera.getLeftVector());
		Vec3 camUp = new Vec3(camera.getUpVector());
		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = null;
		float time = level.getGameTime() + partialTick;

		for (Player player : level.players()) {
			BlackBoneState state = BlackBoneStates.CLIENT.get(player.getId());

			if (state == null || state.mode == BlackBoneMode.NONE || player.isInvisible() || player.isSpectator()) {
				continue;
			}

			if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
				continue;
			}

			if (vc == null) {
				vc = buffers.getBuffer(BlackBoneRenderTypes.GLOW);
			}

			Vec3 feet = new Vec3(Mth.lerp(partialTick, player.xo, player.getX()), Mth.lerp(partialTick, player.yo, player.getY()), Mth.lerp(partialTick, player.zo, player.getZ()));
			float height = Math.max(player.getBbHeight(), 0.6F);
			Palette pal = Palette.of(state.mode);
			int id = player.getId();

			poseStack.pushPose();
			poseStack.translate(-camPos.x, -camPos.y, -camPos.z);
			Matrix4f m = poseStack.last().pose();

			bodyAura(vc, m, feet, height, camPos, pal, time, id);
			rings(vc, m, feet, height, pal, time, id);
			shards(vc, m, feet, height, pal, time, id);
			lightning(vc, m, feet, height, camPos, pal, time, id);
			sparkles(vc, m, feet, height, camLeft, camUp, pal, time, id);

			poseStack.popPose();
		}

		if (vc != null) {
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

	private static float frac(float x) {
		return x - (float)Math.floor(x);
	}

	/** Camera-facing quad from a to b with its width tapering from w0 to w1. */
	private static void strip(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 camPos, float w0, float w1, float[] c0, float[] c1, float a0, float a1) {
		Vec3 mid = a.add(b).scale(0.5D);
		Vec3 side = b.subtract(a).cross(camPos.subtract(mid));

		if (side.lengthSqr() < 1.0E-10D) {
			return;
		}

		side = side.normalize();
		v(vc, m, a.add(side.scale(w0)), c0, a0);
		v(vc, m, a.subtract(side.scale(w0)), c0, a0);
		v(vc, m, b.subtract(side.scale(w1)), c1, a1);
		v(vc, m, b.add(side.scale(w1)), c1, a1);
	}

	private static void triangle(VertexConsumer vc, Matrix4f m, Vec3 p0, float[] c0, float a0, Vec3 p1, float[] c1, float a1, Vec3 p2, float[] c2, float a2) {
		v(vc, m, p0, c0, a0);
		v(vc, m, p1, c1, a1);
		v(vc, m, p2, c2, a2);
		v(vc, m, p2, c2, a2);
	}

	/** Soft radial glow facing the camera. */
	private static void glowDisc(VertexConsumer vc, Matrix4f m, Vec3 center, Vec3 left, Vec3 up, float radius, float[] color, float alpha) {
		int seg = 12;

		for (int i = 0; i < seg; i++) {
			float a0 = Mth.TWO_PI * i / seg;
			float a1 = Mth.TWO_PI * (i + 1) / seg;
			Vec3 p0 = center.add(left.scale(Mth.cos(a0) * radius)).add(up.scale(Mth.sin(a0) * radius));
			Vec3 p1 = center.add(left.scale(Mth.cos(a1) * radius)).add(up.scale(Mth.sin(a1) * radius));
			triangle(vc, m, center, color, alpha, p0, color, 0.0F, p1, color, 0.0F);
		}
	}

	// -------------------------------------------------------------- 1. body aura

	/**
	 * Flame blades: each starts on a ring around the feet, rises and curls in towards the body's axis,
	 * leaving the oval hollow like the reference. Bright and wide at the base, thin and faint at the tip.
	 */
	private static void bodyAura(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, Palette pal, float time, int id) {
		for (int i = 0; i < BLADES; i++) {
			float cycle = time * (0.040F + 0.012F * ((i * 7) % 5) / 4.0F) + i * 0.6180339F;
			float phase = frac(cycle);
			Random r = new Random(id * 1000003L + i * 7919L + (long)Math.floor(cycle) * 104729L);

			float baseAngle = r.nextFloat() * Mth.TWO_PI;
			float baseRadius = 0.45F + 0.55F * r.nextFloat();
			float blade = height * (0.75F + 0.85F * r.nextFloat());
			float curl = 0.6F + 0.35F * r.nextFloat();
			float lean = (r.nextFloat() - 0.5F) * 0.9F;
			float maxWidth = 0.05F + 0.07F * r.nextFloat();
			float grow = Mth.clamp(phase * 2.6F, 0.0F, 1.0F);
			float fade = Mth.clamp((1.0F - phase) * 2.2F, 0.0F, 1.0F);
			boolean vein = i % 3 == 0;

			int steps = 9;
			Vec3 prev = null;
			Vec3 prevVein = null;

			for (int s = 0; s <= steps; s++) {
				float u = (float)s / steps * grow;
				float radius = baseRadius * (1.0F - curl * (float)Math.pow(u, 1.4F));
				float angle = baseAngle + lean * u + Mth.sin(time * 0.22F + i + u * 5.0F) * 0.10F * u;
				double wob = Mth.sin(time * 0.3F + i * 1.3F + u * 7.0F) * 0.04D * u;
				Vec3 p = new Vec3(feet.x + Mth.cos(angle) * radius + wob, feet.y + u * blade, feet.z + Mth.sin(angle) * radius + wob);

				if (prev != null) {
					float w = maxWidth * (1.0F - 0.92F * u);
					float a = fade * (0.95F - 0.75F * u);
					float uPrev = (float)(s - 1) / steps * grow;
					float wPrev = maxWidth * (1.0F - 0.92F * uPrev);
					float aPrev = fade * (0.95F - 0.75F * uPrev);
					strip(vc, m, prev, p, camPos, wPrev * 2.6F, w * 2.6F, pal.deep(), pal.deep(), aPrev * 0.30F, a * 0.30F);
					strip(vc, m, prev, p, camPos, wPrev, w, pal.main(), pal.main(), aPrev * 0.8F, a * 0.8F);
				}

				prev = p;

				if (vein) {
					Vec3 jag = p.add((r.nextDouble() - 0.5D) * 0.07D, 0.0D, (r.nextDouble() - 0.5D) * 0.07D);

					if (prevVein != null) {
						strip(vc, m, prevVein, jag, camPos, 0.010F, 0.007F, pal.hot(), pal.hot(), fade * (0.9F - 0.6F * u), fade * (0.9F - 0.6F * u));
					}

					prevVein = jag;
				}
			}
		}
	}

	// ------------------------------------------------------------ 2. power rings

	private static void rings(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Palette pal, float time, int id) {
		int seg = 44;

		for (int k = 0; k < RINGS; k++) {
			float phase = frac(time * 0.018F + (float)k / RINGS);
			float env = Mth.sin(phase * Mth.PI);
			double y = feet.y + 0.05D + phase * (height + 0.5D);
			float radius = 0.80F + 0.14F * env + 0.05F * Mth.sin(time * 0.3F + k);
			float spin = time * (k % 2 == 0 ? 0.08F : -0.08F);

			for (int i = 0; i < seg; i++) {
				float a0 = Mth.TWO_PI * i / seg;
				float a1 = Mth.TWO_PI * (i + 1) / seg;
				// Brighter and dimmer stretches rotate around the ring.
				float b0 = 0.55F + 0.45F * Mth.sin(a0 * 3.0F + spin * 4.0F);
				float b1 = 0.55F + 0.45F * Mth.sin(a1 * 3.0F + spin * 4.0F);
				ringQuad(vc, m, feet, y, a0, a1, radius - 0.045F, radius + 0.045F, pal.main(), 0.0F, 0.85F * env * b0, 0.85F * env * b1);
				ringQuad(vc, m, feet, y, a0, a1, radius - 0.012F, radius + 0.012F, pal.hot(), 0.0F, 0.95F * env * b0, 0.95F * env * b1);
				ringQuad(vc, m, feet, y, a0, a1, radius + 0.07F, radius + 0.095F, pal.deep(), 0.0F, 0.45F * env, 0.45F * env);
			}

			// Debris orbiting the ring.
			for (int d = 0; d < 6; d++) {
				float ang = spin * 2.0F + d * Mth.TWO_PI / 6.0F + k;
				float rr = radius + 0.12F * Mth.sin(d * 2.3F + time * 0.1F);
				Vec3 c = new Vec3(feet.x + Mth.cos(ang) * rr, y + 0.06D * Mth.sin(d + time * 0.2F), feet.z + Mth.sin(ang) * rr);
				shard(vc, m, c, 0.03F, 0.05F, ang * 3.0F, 0.4F, pal, 0.9F * env);
			}
		}
	}

	private static void ringQuad(VertexConsumer vc, Matrix4f m, Vec3 feet, double y, float a0, float a1, float r0, float r1, float[] color, float fadeUnused, float alpha0, float alpha1) {
		v(vc, m, feet.x + Mth.cos(a0) * r0, y, feet.z + Mth.sin(a0) * r0, color, alpha0);
		v(vc, m, feet.x + Mth.cos(a0) * r1, y, feet.z + Mth.sin(a0) * r1, color, alpha0);
		v(vc, m, feet.x + Mth.cos(a1) * r1, y, feet.z + Mth.sin(a1) * r1, color, alpha1);
		v(vc, m, feet.x + Mth.cos(a1) * r0, y, feet.z + Mth.sin(a1) * r0, color, alpha1);
	}

	// ----------------------------------------------------------- 4. energy shards

	private static void shards(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Palette pal, float time, int id) {
		for (int i = 0; i < SHARDS; i++) {
			float cycle = time * (0.012F + 0.006F * ((i * 5) % 4) / 3.0F) + i * 0.381966F;
			float phase = frac(cycle);
			Random r = new Random(id * 1000003L + 5000L + i * 7919L + (long)Math.floor(cycle) * 104729L);
			float angle = r.nextFloat() * Mth.TWO_PI;
			float radius = 0.55F + 0.9F * r.nextFloat();
			float size = 0.05F + 0.11F * r.nextFloat();
			float env = Mth.sin(phase * Mth.PI);
			Vec3 c = new Vec3(
				feet.x + Mth.cos(angle + phase * 1.2F) * radius,
				feet.y + 0.1D + r.nextFloat() * height * 0.6D + phase * 1.6D,
				feet.z + Mth.sin(angle + phase * 1.2F) * radius);
			shard(vc, m, c, size, size * 2.3F, time * 0.12F + i, 0.5F * Mth.sin(time * 0.05F + i), pal, Math.min(1.0F, env * 1.6F));
		}
	}

	/** Elongated octahedron (a crystal). Faces are shaded by a fixed light so it reads as faceted. */
	private static void shard(VertexConsumer vc, Matrix4f m, Vec3 c, float width, float length, float spin, float tilt, Palette pal, float alpha) {
		if (alpha <= 0.02F) {
			return;
		}

		Vec3 s1 = new Vec3(Mth.cos(spin), 0.0D, Mth.sin(spin)).scale(width);
		Vec3 s2 = new Vec3(-Mth.sin(spin), 0.0D, Mth.cos(spin)).scale(width);
		Vec3 up = new Vec3(Mth.sin(tilt) * 0.5D, Mth.cos(tilt), Mth.sin(tilt) * 0.3D).normalize().scale(length);
		Vec3 top = c.add(up);
		Vec3 bottom = c.subtract(up.scale(0.8D));
		Vec3[] ring = {c.add(s1), c.add(s2), c.subtract(s1), c.subtract(s2)};
		Vec3 light = new Vec3(0.35D, 0.8D, 0.5D).normalize();

		for (int i = 0; i < 4; i++) {
			Vec3 a = ring[i];
			Vec3 b = ring[(i + 1) % 4];

			for (int half = 0; half < 2; half++) {
				Vec3 apex = half == 0 ? top : bottom;
				Vec3 n = b.subtract(a).cross(apex.subtract(a));
				double len = n.length();
				float shade = len < 1.0E-9D ? 0.5F : 0.30F + 0.70F * (float)Math.abs(n.scale(1.0D / len).dot(light));
				triangle(vc, m, apex, pal.hot(), alpha * shade, a, pal.main(), alpha * shade * 0.9F, b, pal.deep(), alpha * shade * 0.8F);
			}
		}
	}

	// ----------------------------------------------------------------- 3. lightning

	private static void lightning(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, Palette pal, float time, int id) {
		// Bold strikes from the ground.
		for (int k = 0; k < 3; k++) {
			float cycle = time * 0.07F + k / 3.0F;
			float phase = frac(cycle);
			Random r = new Random(id * 31L + k * 977L + (long)Math.floor(cycle) * 7919L);
			float env = Mth.sin(phase * Mth.PI);
			env = Mth.clamp(env * 1.6F, 0.0F, 1.0F);
			double angle = r.nextDouble() * Math.PI * 2.0D;
			double rad = 0.9D + r.nextDouble() * 0.9D;
			Vec3 from = new Vec3(feet.x + Math.cos(angle) * rad, feet.y + 0.02D, feet.z + Math.sin(angle) * rad);
			Vec3 to = from.add((r.nextDouble() - 0.5D) * 1.2D, height + 0.7D + r.nextDouble() * 1.2D, (r.nextDouble() - 0.5D) * 1.2D);
			bolt(vc, m, from, to, camPos, 7, 0.30D, 0.085F, pal, env, new Random(r.nextLong() + (long)(time / 2.0F)), 0.45D, 1);
		}

		// Thin forked lightning crawling over the body.
		Random rnd = new Random(id * 7919L + (long)(time / 2.0F));

		for (int a = 0; a < 7; a++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double y = feet.y + rnd.nextDouble() * height;
			Vec3 from = new Vec3(feet.x + Math.cos(angle) * 0.55D, y, feet.z + Math.sin(angle) * 0.55D);
			double angle2 = angle + (rnd.nextDouble() - 0.5D) * 1.8D;
			Vec3 to = new Vec3(feet.x + Math.cos(angle2) * 0.7D, Mth.clamp(y + (rnd.nextDouble() - 0.5D) * 1.0D, feet.y, feet.y + height + 0.3D), feet.z + Math.sin(angle2) * 0.7D);
			bolt(vc, m, from, to, camPos, 6, 0.10D, 0.016F, pal, 0.55F + 0.45F * rnd.nextFloat(), rnd, 0.45D, 2);
		}
	}

	/** Jagged bolt that tapers to a point, with a soft glow under a thin white core. Can fork. */
	private static void bolt(VertexConsumer vc, Matrix4f m, Vec3 start, Vec3 end, Vec3 camPos, int segments, double jitter, float width, Palette pal, float alpha, Random rnd, double forkChance, int depth) {
		if (alpha <= 0.02F) {
			return;
		}

		Vec3 prev = start;

		for (int s = 1; s <= segments; s++) {
			float t0 = (float)(s - 1) / segments;
			float t1 = (float)s / segments;
			Vec3 point = start.lerp(end, t1);

			if (s < segments) {
				point = point.add((rnd.nextDouble() - 0.5D) * 2.0D * jitter, (rnd.nextDouble() - 0.5D) * jitter, (rnd.nextDouble() - 0.5D) * 2.0D * jitter);
			}

			float w0 = width * (1.0F - 0.9F * t0);
			float w1 = width * (1.0F - 0.9F * t1);
			float a0 = alpha * (1.0F - 0.3F * t0);
			float a1 = alpha * (1.0F - 0.3F * t1);
			strip(vc, m, prev, point, camPos, w0 * 3.4F, w1 * 3.4F, pal.main(), pal.main(), a0 * 0.35F, a1 * 0.35F);
			strip(vc, m, prev, point, camPos, w0 * 1.5F, w1 * 1.5F, pal.main(), pal.main(), a0 * 0.8F, a1 * 0.8F);
			strip(vc, m, prev, point, camPos, w0 * 0.55F, w1 * 0.55F, pal.hot(), pal.hot(), a0, a1);

			if (depth > 0 && s < segments && rnd.nextDouble() < forkChance) {
				Vec3 dir = end.subtract(start);
				Vec3 branchEnd = point.add(dir.scale(0.3D)).add((rnd.nextDouble() - 0.5D) * 0.9D, (rnd.nextDouble() - 0.5D) * 0.5D, (rnd.nextDouble() - 0.5D) * 0.9D);
				bolt(vc, m, point, branchEnd, camPos, Math.max(2, segments / 2), jitter * 0.8D, width * 0.5F, pal, alpha * 0.8F, rnd, 0.0D, depth - 1);
			}

			prev = point;
		}
	}

	// ----------------------------------------------------------------- 5. sparkles

	private static void sparkles(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 left, Vec3 up, Palette pal, float time, int id) {
		for (int i = 0; i < SPARKLES; i++) {
			float cycle = time * (0.025F + 0.015F * ((i * 3) % 5) / 4.0F) + i * 0.7548777F;
			float phase = frac(cycle);
			Random r = new Random(id * 1000003L + 9000L + i * 7919L + (long)Math.floor(cycle) * 104729L);
			float angle = r.nextFloat() * Mth.TWO_PI;
			float radius = 0.35F + 1.1F * r.nextFloat();
			float size = 0.14F + 0.22F * r.nextFloat();
			float twinkle = Mth.sin(phase * Mth.PI);
			twinkle = twinkle * twinkle * (0.75F + 0.25F * Mth.sin(time * 0.9F + i));
			Vec3 c = new Vec3(feet.x + Mth.cos(angle) * radius, feet.y - 0.1D + r.nextFloat() * (height + 0.7D) + phase * 0.3D, feet.z + Mth.sin(angle) * radius);

			float s = size * (0.5F + 0.5F * twinkle);
			glowDisc(vc, m, c, left, up, s * 0.9F, pal.main(), 0.55F * twinkle);
			spike(vc, m, c, up, left, s * 1.9F, 0.016F * (1.0F + s), pal, twinkle);
			spike(vc, m, c, left, up, s * 1.5F, 0.016F * (1.0F + s), pal, twinkle);
		}
	}

	/** One long thin diamond spike through c in both directions of dir. */
	private static void spike(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 dir, Vec3 perp, float length, float halfWidth, Palette pal, float alpha) {
		Vec3 tipA = c.add(dir.scale(length));
		Vec3 tipB = c.subtract(dir.scale(length));
		Vec3 sideA = c.add(perp.scale(halfWidth));
		Vec3 sideB = c.subtract(perp.scale(halfWidth));
		triangle(vc, m, sideA, pal.hot(), alpha, sideB, pal.hot(), alpha, tipA, pal.main(), 0.0F);
		triangle(vc, m, sideA, pal.hot(), alpha, sideB, pal.hot(), alpha, tipB, pal.main(), 0.0F);
	}

	/** Forget everything when leaving a world so stale entity ids cannot leak into the next one. */
	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		BlackBoneStates.CLIENT.clear();
	}
}
