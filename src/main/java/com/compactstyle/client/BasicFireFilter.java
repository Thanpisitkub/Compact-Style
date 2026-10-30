package com.compactstyle.client;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.compactstyle.CompactStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.particle.LavaParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Basic style borrows Solar's Obscuridad animations, which spawn fire (flame, lava, smoke and Weapons of
 * Miracles' Solar hit particles). While a player holds the Basic item, any such particle that appears near
 * them is removed right away, so the style looks plain.
 */
@Mod.EventBusSubscriber(modid = CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BasicFireFilter {
	private static final double RADIUS_SQ = 12.0D * 12.0D;
	private static final Set<Particle> SEEN = Collections.newSetFromMap(new IdentityHashMap<>());
	private static Field particlesField;
	private static boolean failed;

	private BasicFireFilter() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || failed) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();

		if (mc.level == null) {
			SEEN.clear();
			return;
		}

		java.util.List<Player> holders = null;

		for (Player player : mc.level.players()) {
			if (player.getMainHandItem().is(CompactStyle.BASIC_STYLE.get())) {
				if (holders == null) {
					holders = new java.util.ArrayList<>();
				}

				holders.add(player);
			}
		}

		try {
			Map<?, ?> map = particleMap(mc.particleEngine);
			SEEN.removeIf((p) -> !p.isAlive());

			for (Object queue : map.values()) {
				if (!(queue instanceof Queue<?> particles)) {
					continue;
				}

				for (Object o : particles) {
					if (!(o instanceof Particle particle) || !SEEN.add(particle) || holders == null || !isFire(particle)) {
						continue;
					}

					for (Player holder : holders) {
						if (holder.distanceToSqr(particle.getBoundingBox().getCenter()) <= RADIUS_SQ) {
							particle.remove();
							break;
						}
					}
				}
			}
		} catch (Throwable t) {
			failed = true; // never break the game over a cosmetic filter
		}
	}

	private static boolean isFire(Particle particle) {
		return particle instanceof FlameParticle || particle instanceof LavaParticle || particle instanceof SmokeParticle
			|| particle.getClass().getName().startsWith("reascer.wom.particle");
	}

	/** ParticleEngine's private Map<ParticleRenderType, Queue<Particle>>, found by type so it works with any mappings. */
	private static Map<?, ?> particleMap(ParticleEngine engine) throws ReflectiveOperationException {
		if (particlesField == null) {
			for (Field field : ParticleEngine.class.getDeclaredFields()) {
				if (Map.class.isAssignableFrom(field.getType())) {
					field.setAccessible(true);
					Object value = field.get(engine);

					if (value instanceof Map<?, ?> m && !m.isEmpty() && m.values().iterator().next() instanceof Queue) {
						particlesField = field;
						break;
					}
				}
			}

			if (particlesField == null) {
				return Map.of(); // no particles yet: look again next tick
			}
		}

		return (Map<?, ?>) particlesField.get(engine);
	}
}
