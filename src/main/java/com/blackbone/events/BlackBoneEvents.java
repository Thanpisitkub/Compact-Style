package com.blackbone.events;

import com.blackbone.BlackBoneMod;
import com.blackbone.combat.BlackBoneReflection;
import com.blackbone.network.BlackBoneNetwork;
import com.blackbone.skill.BlackBoneManager;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Common (server-authoritative) Forge event handlers. Contains no client-only references. */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID)
public final class BlackBoneEvents {
	private BlackBoneEvents() {
	}

	/**
	 * Flow / Zone stack gain. Runs once per applied damage (LivingDamageEvent is only fired when the hit
	 * actually deals damage, so blocked / invulnerable hits never count).
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent event) {
		DamageSource source = event.getSource();

		if (!(source.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		// Only direct melee hits by the player: no environmental damage, projectiles, thorns or the reflection itself.
		if (source.getDirectEntity() != player
			|| source.is(BlackBoneReflection.REFLECT_TYPE)
			|| source.is(DamageTypes.THORNS)
			|| source.is(DamageTypeTags.IS_PROJECTILE)
			|| source.is(DamageTypeTags.IS_EXPLOSION)) {
			return;
		}

		if (event.getAmount() <= 0.0F || !isValidEnemy(player, event.getEntity())) {
			return;
		}

		BlackBoneManager.onValidHit(player, event.getEntity());
	}

	private static boolean isValidEnemy(ServerPlayer player, LivingEntity target) {
		if (target == player || !target.isAlive() || target.isInvulnerable() || target instanceof ArmorStand) {
			return false;
		}

		if (target.isAlliedTo(player) || (target instanceof TamableAnimal pet && pet.isOwnedBy(player))) {
			return false;
		}

		if (target instanceof Enemy) {
			return true;
		}

		if (target instanceof Player other) {
			return !other.isCreative() && !other.isSpectator();
		}

		// Neutral mobs only count while they are actually fighting this player.
		return target instanceof Mob mob && mob.getTarget() == player;
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
			BlackBoneManager.tick(player);
		}
	}

	@SubscribeEvent
	public static void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BlackBoneManager.onDeath(player);
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BlackBoneManager.onLogin(player);
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BlackBoneManager.onLogout(player);
		}
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			BlackBoneManager.resync(player);
		}
	}

	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		// Effects and the timer survive a dimension change; just re-assert and re-sync.
		if (event.getEntity() instanceof ServerPlayer player) {
			BlackBoneManager.resync(player);
		}
	}

	/** A player that starts tracking another one needs its Flow / Zone mode to render the eyes. */
	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
			BlackBoneState state = BlackBoneStates.SERVER.get(subject.getUUID());

			if (state != null) {
				BlackBoneNetwork.syncToViewer(viewer, subject, state);
			}
		}
	}
}
