package com.blackbone.client;

import java.util.Random;

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
 * Body aura for Flow (red) and Zone (blue), anime "power-up" style.
 *
 * Drawn in world space (not as a player RenderLayer) because Epic Fight replaces the vanilla player
 * renderer in battle mode; this way it works with every skin and animation and is seen by other players.
 *
 * Parts:
 *  - three nested translucent shells around the whole body with a rim glow (brighter at the silhouette,
 *    so it reads as a real 3D volume) and bands of light scrolling upwards
 *  - flame-like wisps rising around the body (Zone: jagged lightning arcs on top of that)
 *  - a glowing ring on the ground
 * Dust and spark particles are added on the client tick. Not drawn for the local player in first person.
 */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AuraEffectRenderer {
	private static final int LAT = 12;
	private static final int LON = 20;
	private static final int WISPS = 14;

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

			float pulse = 0.88F + 0.12F * Mth.sin(time * 0.35F + player.getId());
			float[] inner = core(state.mode);
			float[] glow = outer(state.mode);

			// Nested shells, outermost faintest.
			shell(consumer, m, feet, height, 0.50F, 0.00F, camPos, inner, glow, 0.70F * pulse, time);
			shell(consumer, m, feet, height, 0.62F, 0.10F, camPos, glow, glow, 0.50F * pulse, time + 7.0F);
			shell(consumer, m, feet, height, 0.78F, 0.22F, camPos, glow, glow, 0.32F * pulse, time + 13.0F);

			wisps(consumer, m, feet, height, camPos, state.mode, glow, inner, time, player.getId());

			if (state.mode == BlackBoneMode.ZONE) {
				arcs(consumer, m, feet, height, camPos, inner, time, player.getId());
			}

			groundRing(consumer, m, feet, glow, inner, pulse, time);

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

	/**
	 * Ellipsoid shell around the body. Alpha follows a rim-light term (1 - |normal . view|) so the edge
	 * of the volume glows, plus a band that scrolls upwards.
	 */
	private static void shell(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, float radius, float grow, Vec3 camPos,
			float[] colorA, float[] colorB, float strength, float time) {
		double cx = feet.x;
		double cy = feet.y + height * 0.5D;
		double cz = feet.z;
		double rx = radius + grow * 0.3F + 0.03F * Mth.sin(time * 0.25F);
		double ry = height * 0.5D + 0.12D + grow;

		for (int i = 0; i < LAT; i++) {
			double phi0 = Math.PI * (-0.5D + (double)i / LAT);
			double phi1 = Math.PI * (-0.5D + (double)(i + 1) / LAT);

			for (int j = 0; j < LON; j++) {
				double th0 = Mth.TWO_PI * j / LON;
				double th1 = Mth.TWO_PI * (j + 1) / LON;

				vertexOnShell(vc, m, cx, cy, cz, rx, ry, phi0, th0, camPos, colorA, colorB, strength, time);
				vertexOnShell(vc, m, cx, cy, cz, rx, ry, phi0, th1, camPos, colorA, colorB, strength, time);
				vertexOnShell(vc, m, cx, cy, cz, rx, ry, phi1, th1, camPos, colorA, colorB, strength, time);
				vertexOnShell(vc, m, cx, cy, cz, rx, ry, phi1, th0, camPos, colorA, colorB, strength, time);
			}
		}
	}

	private static void vertexOnShell(VertexConsumer vc, Matrix4f m, double cx, double cy, double cz, double rx, double ry,
			double phi, double theta, Vec3 camPos, float[] colorA, float[] colorB, float strength, float time) {
		double cp = Math.cos(phi);
		double nx = cp * Math.cos(theta);
		double ny = Math.sin(phi);
		double nz = cp * Math.sin(theta);
		double x = cx + nx * rx;
		double y = cy + ny * ry;
		double z = cz + nz * rx;

		// Ellipsoid normal and rim term.
		double ex = nx / rx;
		double ey = ny / ry;
		double ez = nz / rx;
		double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
		double vx = camPos.x - x;
		double vy = camPos.y - y;
		double vz = camPos.z - z;
		double vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
		double facing = vl < 1.0E-6D ? 0.0D : Math.abs((ex * vx + ey * vy + ez * vz) / (el * vl));
		float rim = (float)Math.pow(1.0D - facing, 1.6D);

		// Bands of light travelling up the body.
		float band = 0.55F + 0.45F * Mth.sin((float)(ny * 5.0D * Math.PI) - time * 0.25F);
		// Fade out at the very top and bottom so the shell has no hard caps.
		float cap = (float)Math.min(1.0D, 2.4D * (1.0D - Math.abs(ny) * 0.92D));

		float alpha = (0.07F + 0.65F * rim) * band * cap * strength;
		float t = (float)(ny * 0.5D + 0.5D);
		float[] c = {Mth.lerp(t, colorB[0], colorA[0]), Mth.lerp(t, colorB[1], colorA[1]), Mth.lerp(t, colorB[2], colorA[2])};
		v(vc, m, x, y, z, c, alpha);
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

	/** Flame-like wisps that rise from the body and sway. */
	private static void wisps(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, BlackBoneMode mode,
			float[] glow, float[] inner, float time, int seedId) {
		boolean zone = mode == BlackBoneMode.ZONE;

		for (int i = 0; i < WISPS; i++) {
			float seed = i * 2.399F + seedId;
			float phase = (time * (zone ? 0.045F : 0.035F) + i * 0.317F) % 1.0F;
			float angle = seed + Mth.sin(time * 0.04F + seed) * 0.4F;
			float radius = 0.38F + 0.12F * Mth.sin(seed * 1.7F);
			double sway = Mth.sin(time * 0.15F + seed) * 0.08D;

			double bx = feet.x + Mth.cos(angle) * radius;
			double bz = feet.z + Mth.sin(angle) * radius;
			double by = feet.y + height * (0.05D + 0.65D * ((i * 0.37D + 0.1D) % 1.0D)) + phase * 0.35D;

			Vec3 start = new Vec3(bx, by, bz);
			double len = (zone ? 0.7D : 0.9D) + 0.5D * Mth.sin(seed * 3.1F) * Mth.sin(seed * 3.1F);
			Vec3 end = start.add(sway, len * (0.6D + 0.8D * phase), sway * 0.7D);
			float alpha = (1.0F - phase) * 0.85F;
			float width = (zone ? 0.045F : 0.065F) * (1.0F - 0.4F * phase);
			ribbon(vc, m, start, end, camPos, width, glow, inner, alpha);
		}
	}

	/** Zone only: short jagged lightning arcs hugging the body. Pattern changes every few ticks. */
	private static void arcs(VertexConsumer vc, Matrix4f m, Vec3 feet, float height, Vec3 camPos, float[] inner, float time, int seedId) {
		Random rnd = new Random(seedId * 7919L + (long)(time / 3.0F));
		float[] white = {0.85F, 0.95F, 1.0F};

		for (int a = 0; a < 5; a++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0D;
			double y = feet.y + rnd.nextDouble() * height * 0.8D;
			Vec3 prev = new Vec3(feet.x + Math.cos(angle) * 0.5D, y, feet.z + Math.sin(angle) * 0.5D);

			for (int s = 0; s < 4; s++) {
				angle += (rnd.nextDouble() - 0.4D) * 0.7D;
				y += (rnd.nextDouble() - 0.35D) * 0.45D;
				double r = 0.5D + rnd.nextDouble() * 0.22D;
				Vec3 next = new Vec3(feet.x + Math.cos(angle) * r, y, feet.z + Math.sin(angle) * r);
				ribbon(vc, m, prev, next, camPos, 0.022F, inner, white, 0.95F - s * 0.15F);
				prev = next;
			}
		}
	}

	/** Pulsing ring on the ground. */
	private static void groundRing(VertexConsumer vc, Matrix4f m, Vec3 feet, float[] glow, float[] inner, float pulse, float time) {
		double y = feet.y + 0.03D;
		double r0 = 0.55D + 0.08D * Mth.sin(time * 0.2F);
		double r1 = r0 + 0.45D;
		int seg = 28;

		for (int i = 0; i < seg; i++) {
			double a0 = Mth.TWO_PI * i / seg;
			double a1 = Mth.TWO_PI * (i + 1) / seg;
			v(vc, m, feet.x + Math.cos(a0) * r0, y, feet.z + Math.sin(a0) * r0, inner, 0.55F * pulse);
			v(vc, m, feet.x + Math.cos(a1) * r0, y, feet.z + Math.sin(a1) * r0, inner, 0.55F * pulse);
			v(vc, m, feet.x + Math.cos(a1) * r1, y, feet.z + Math.sin(a1) * r1, glow, 0.0F);
			v(vc, m, feet.x + Math.cos(a0) * r1, y, feet.z + Math.sin(a0) * r1, glow, 0.0F);
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

			boolean flow = state.mode == BlackBoneMode.FLOW;
			DustParticleOptions dust = new DustParticleOptions(flow ? new Vector3f(1.0F, 0.10F, 0.06F) : new Vector3f(0.20F, 0.65F, 1.0F), 0.8F);
			float height = player.getBbHeight();

			for (int i = 0; i < 3; i++) {
				double angle = level.random.nextDouble() * Math.PI * 2.0D;
				double r = 0.45D + level.random.nextDouble() * 0.2D;
				level.addParticle(dust, player.getX() + Math.cos(angle) * r, player.getY() + level.random.nextDouble() * height, player.getZ() + Math.sin(angle) * r,
						0.0D, 0.06D + level.random.nextDouble() * 0.05D, 0.0D);
			}

			if (!flow && level.random.nextInt(3) == 0) {
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
