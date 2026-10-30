package com.blackbone.skill;

import com.blackbone.effect.EffectTracker;
import com.blackbone.network.BlackBoneNetwork;
import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;
import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Server-side authority for Flow / Zone: stacks, the 60 second timer, effects, feedback and sync.
 * Everything here runs on the server thread only.
 */
public final class BlackBoneManager {
	public static final int MAX_STACK = 100;
	public static final int STACK_PER_HIT = 10;
	public static final int DURATION_TICKS = 60 * 20;

	private static final String NBT_FLOW = "blackbone_flow";
	private static final String NBT_ZONE = "blackbone_zone";

	public enum Result {
		OK, ALREADY_ACTIVE, OTHER_ACTIVE, NOT_ENOUGH
	}

	public enum EndReason {
		EXPIRED, INTERRUPTED, DEATH, LOGOUT
	}

	private BlackBoneManager() {
	}

	public static BlackBoneState state(ServerPlayer player) {
		return BlackBoneStates.SERVER.computeIfAbsent(player.getUUID(), id -> new BlackBoneState());
	}

	private static SkillContainer containerOf(ServerPlayer player, BlackBoneMode mode) {
		ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
		Skill skill = BlackBoneSkills.of(mode);

		if (patch == null || skill == null) {
			return null;
		}

		return patch.getSkill(skill);
	}

	public static boolean hasSkill(ServerPlayer player, BlackBoneMode mode) {
		return containerOf(player, mode) != null;
	}

	// ------------------------------------------------------------------ stacks

	/** Called for every damage event that already passed the valid-enemy and source filters. */
	public static void onValidHit(ServerPlayer player, LivingEntity target) {
		BlackBoneState state = state(player);

		// No stacks are generated while Flow or Zone is running.
		if (state.mode != BlackBoneMode.NONE) {
			return;
		}

		long now = player.level().getGameTime();
		Long last = state.lastHitTick.get(target.getId());

		// One stack gain per target per hit: repeated damage events of the same hit are ignored.
		if (last != null && now - last < 2L) {
			return;
		}

		state.lastHitTick.put(target.getId(), now);

		if (state.lastHitTick.size() > 64) {
			state.lastHitTick.values().removeIf(tick -> now - tick > 40L);
		}

		boolean changed = false;

		for (BlackBoneMode mode : new BlackBoneMode[] {BlackBoneMode.FLOW, BlackBoneMode.ZONE}) {
			if (hasSkill(player, mode) && state.stacksOf(mode) < MAX_STACK) {
				int before = state.stacksOf(mode);
				int after = Math.min(MAX_STACK, before + STACK_PER_HIT);
				state.setStacks(mode, after);
				changed = true;

				if (after >= MAX_STACK) {
					player.displayClientMessage(Component.translatable("message.blackbone.ready", Component.translatable(modeKey(mode))), true);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.6F);
				} else {
					player.displayClientMessage(Component.translatable("message.blackbone.stacks", Component.translatable(modeKey(mode)), after), true);
				}
			}
		}

		if (changed) {
			sync(player);
		}
	}

	// -------------------------------------------------------------- activation

	public static Result tryActivate(ServerPlayer player, BlackBoneMode mode) {
		BlackBoneState state = state(player);

		if (state.mode == mode) {
			return Result.ALREADY_ACTIVE;
		}

		if (state.mode != BlackBoneMode.NONE) {
			return Result.OTHER_ACTIVE;
		}

		if (state.stacksOf(mode) < MAX_STACK) {
			return Result.NOT_ENOUGH;
		}

		state.setStacks(mode, 0);
		state.mode = mode;
		state.remainingTicks = DURATION_TICKS;
		state.grantedEffects.clear();

		if (mode == BlackBoneMode.FLOW) {
			EffectTracker.grant(player, MobEffects.DIG_SPEED, 0, DURATION_TICKS, state.grantedEffects);
			EffectTracker.grant(player, MobEffects.DAMAGE_BOOST, 0, DURATION_TICKS, state.grantedEffects);
		} else {
			EffectTracker.grant(player, MobEffects.DAMAGE_BOOST, 0, DURATION_TICKS, state.grantedEffects);
			EffectTracker.grant(player, MobEffects.DAMAGE_RESISTANCE, 0, DURATION_TICKS, state.grantedEffects);
		}

		activationFeedback(player, mode);
		sync(player);

		return Result.OK;
	}

	public static Component messageFor(Result result, BlackBoneMode mode, int stacks) {
		Component name = Component.translatable(modeKey(mode));

		return switch (result) {
			case ALREADY_ACTIVE -> Component.translatable("message.blackbone.already_active", name);
			case OTHER_ACTIVE -> Component.translatable("message.blackbone.other_active");
			case NOT_ENOUGH -> Component.translatable("message.blackbone.not_enough", name, stacks);
			default -> Component.empty();
		};
	}

	public static String modeKey(BlackBoneMode mode) {
		return mode == BlackBoneMode.FLOW ? "skill.blackbone.flow" : "skill.blackbone.zone";
	}

	private static void activationFeedback(ServerPlayer player, BlackBoneMode mode) {
		ServerLevel level = (ServerLevel)player.level();
		boolean flow = mode == BlackBoneMode.FLOW;

		player.displayClientMessage(Component.translatable("message.blackbone.activated", Component.translatable(modeKey(mode))), true);

		if (flow) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.2F, 0.8F);
		} else {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.8F, 1.3F);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.8F);
		}

		DustParticleOptions dust = new DustParticleOptions(flow ? new Vector3f(1.0F, 0.08F, 0.06F) : new Vector3f(0.15F, 0.6F, 1.0F), 1.4F);

		for (int i = 0; i < 40; i++) {
			double angle = i * (Math.PI * 2.0D / 40.0D);
			double dx = Math.cos(angle);
			double dz = Math.sin(angle);
			level.sendParticles(dust, player.getX() + dx * 0.6D, player.getY() + 0.1D, player.getZ() + dz * 0.6D, 1, dx * 0.05D, 0.25D, dz * 0.05D, 0.2D);
		}

		level.sendParticles(flow ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.5D, 0.8D, 0.5D, 0.15D);
	}

	// ------------------------------------------------------------------ ending

	public static void end(ServerPlayer player, EndReason reason) {
		BlackBoneState state = BlackBoneStates.SERVER.get(player.getUUID());

		if (state == null || state.mode == BlackBoneMode.NONE) {
			return;
		}

		BlackBoneMode mode = state.mode;
		int remaining = Math.max(0, state.remainingTicks);
		int elapsed = DURATION_TICKS - remaining;

		state.mode = BlackBoneMode.NONE;
		state.remainingTicks = 0;
		state.setStacks(mode, 0);

		EffectTracker.release(player, state.grantedEffects, elapsed, remaining);

		SkillContainer container = containerOf(player, mode);

		if (container != null && container.isActivated()) {
			if (reason == EndReason.LOGOUT) {
				container.deactivate();
			} else {
				container.getSkill().cancelOnServer(container, null);
				container.deactivate();
			}
		}

		if (reason != EndReason.LOGOUT) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.displayClientMessage(Component.translatable("message.blackbone.ended", Component.translatable(modeKey(mode))), true);
			sync(player);
		}
	}

	public static void tick(ServerPlayer player) {
		BlackBoneState state = BlackBoneStates.SERVER.get(player.getUUID());

		if (state == null || state.mode == BlackBoneMode.NONE) {
			return;
		}

		if (!player.isAlive()) {
			end(player, EndReason.DEATH);
			return;
		}

		// The skill was unequipped (or the container replaced) while running.
		if (!hasSkill(player, state.mode)) {
			end(player, EndReason.INTERRUPTED);
			return;
		}

		state.remainingTicks--;

		if (state.remainingTicks <= 0) {
			end(player, EndReason.EXPIRED);
			return;
		}

		if (state.remainingTicks % 20 == 0) {
			EffectTracker.reassert(player, state.grantedEffects, state.remainingTicks);
			sync(player);
		}
	}

	// ------------------------------------------------------- lifecycle / persistence

	public static void onLogin(ServerPlayer player) {
		BlackBoneState state = new BlackBoneState();
		CompoundTag data = player.getPersistentData();
		state.flowStacks = Mth.clamp(data.getInt(NBT_FLOW), 0, MAX_STACK);
		state.zoneStacks = Mth.clamp(data.getInt(NBT_ZONE), 0, MAX_STACK);
		BlackBoneStates.SERVER.put(player.getUUID(), state);
		sync(player);
	}

	public static void onLogout(ServerPlayer player) {
		end(player, EndReason.LOGOUT);

		BlackBoneState state = BlackBoneStates.SERVER.remove(player.getUUID());

		if (state != null) {
			CompoundTag data = player.getPersistentData();
			data.putInt(NBT_FLOW, state.flowStacks);
			data.putInt(NBT_ZONE, state.zoneStacks);
		}
	}

	public static void onDeath(ServerPlayer player) {
		end(player, EndReason.DEATH);

		BlackBoneState state = state(player);
		state.flowStacks = 0;
		state.zoneStacks = 0;
		state.lastHitTick.clear();
		sync(player);
	}

	/** Respawn / dimension change / tracking changes: push the current state again. */
	public static void resync(ServerPlayer player) {
		BlackBoneState state = BlackBoneStates.SERVER.get(player.getUUID());

		if (state == null) {
			return;
		}

		if (state.mode != BlackBoneMode.NONE) {
			EffectTracker.reassert(player, state.grantedEffects, state.remainingTicks);
		}

		sync(player);
	}

	public static void sync(ServerPlayer player) {
		BlackBoneState state = BlackBoneStates.SERVER.get(player.getUUID());

		if (state != null) {
			BlackBoneNetwork.syncToSelfAndTrackers(player, state);
		}
	}
}
