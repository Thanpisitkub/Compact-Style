package com.blackbone.skill;

import java.util.UUID;

import com.blackbone.network.BlackBoneNetwork;
import com.blackbone.network.DownedStrikePacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * Weapon passive used by Classic, Kyokushin and Taekwondo: pressing the weapon-skill button (right mouse
 * button by default) while the enemy in front of you is lying knocked down cancels the normal weapon skill
 * and plays the first hit of the scythe animation (Epic Fight Extra) on the downed enemy instead. The left
 * button is unchanged. The client intercepts SKILL_CAST_EVENT and asks the server through a packet.
 */
public class DownedPunchSkill extends Skill {
	private static final UUID EVENT_UUID = UUID.fromString("5d3c3a8e-7a53-4a0f-9f5e-b1ac4b0e0010");
	private static final double REACH = 3.5D;

	public static SkillBuilder<Skill> createBuilder() {
		SkillBuilder<Skill> builder = new SkillBuilder<>();
		builder.setCategory(SkillCategories.WEAPON_PASSIVE);
		return builder;
	}

	public DownedPunchSkill(SkillBuilder<? extends Skill> builder) {
		super(builder);
	}

	@Override
	public void onInitiate(SkillContainer container) {
		super.onInitiate(container);

		PlayerEventListener listener = container.getExecutor().getEventListener();

		// Epic Fight fires SKILL_CAST_EVENT on the client before it sends the cast request, so this is where
		// the weapon-skill button can be intercepted. The server is then asked to play the move.
		listener.addEventListener(EventType.SKILL_CAST_EVENT, EVENT_UUID, (event) -> {
			if (event.getSkillContainer().getSlot() != SkillSlots.WEAPON_INNATE || event.getPlayerPatch() instanceof ServerPlayerPatch) {
				return;
			}

			Player player = event.getPlayerPatch().getOriginal();

			if (!player.level().isClientSide || !targetIsDowned(player)) {
				return;
			}

			event.setCanceled(true);

			if (player.tickCount - lastSentTick >= 8 || player.tickCount < lastSentTick) {
				lastSentTick = player.tickCount;
				BlackBoneNetwork.CHANNEL.sendToServer(new DownedStrikePacket());
			}
		});
	}

	private static int lastSentTick = -100;

	/** Server side: plays the scythe's first attack (or the style's first punch) on the downed enemy. */
	public static void performOnServer(ServerPlayer player) {
		ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);

		if (patch == null || !targetIsDowned(player)) {
			return;
		}

		CapabilityItem capability = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND);

		if (!(capability instanceof WeaponCapability weapon) || !patch.getEntityState().canBasicAttack()) {
			return;
		}

		var combo = weapon.getAutoAttackMotion(patch);
		AnimationManager.AnimationAccessor<?> move = scytheFirstAttack();

		if (move == null && !combo.isEmpty()) {
			move = combo.get(0);
		}

		if (move == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		AnimationManager.AnimationAccessor<? extends StaticAnimation> toPlay = (AnimationManager.AnimationAccessor<? extends StaticAnimation>) move;
		patch.playAnimationSynchronized(toPlay, 0.0F);

		SkillContainer basicAttack = patch.getSkill(SkillSlots.BASIC_ATTACK);

		if (basicAttack != null) {
			// The next left-click continues with punch 2.
			basicAttack.getDataManager().setData(SkillDataKeys.COMBO_COUNTER.get(), 1);
		}
	}

	/** First hit of the scythe combo from Epic Fight Extra (looked up by name; null when it is not installed). */
	private static AnimationManager.AnimationAccessor<?> scytheFirstAttack() {
		try {
			Object value = Class.forName("com.asanginxst.epicfightx.gameassets.animations.ExtraAnimations")
				.getField("SCYTHE_TWOHAND_AUTO1").get(null);

			if (value instanceof AnimationManager.AnimationAccessor<?> accessor) {
				return accessor;
			}
		} catch (Throwable ignored) {
			// Epic Fight Extra not installed: fall back to the style's own first punch
		}

		return null;
	}

	@Override
	public void onRemoved(SkillContainer container) {
		super.onRemoved(container);
		container.getExecutor().getEventListener().removeListener(EventType.SKILL_CAST_EVENT, EVENT_UUID);
	}

	/** The living entity under the crosshair is knocked down (Epic Fight) or sleeping. */
	public static boolean targetIsDowned(Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(REACH));
		AABB box = player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
			(e) -> e instanceof LivingEntity living && living.isAlive() && !e.isSpectator(), REACH * REACH);

		if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
			return false;
		}

		LivingEntityPatch<?> targetPatch = EpicFightCapabilities.getEntityPatch(target, LivingEntityPatch.class);

		if (targetPatch != null && targetPatch.getEntityState().knockDown()) {
			return true;
		}

		return target.getPose() == Pose.SLEEPING;
	}
}
