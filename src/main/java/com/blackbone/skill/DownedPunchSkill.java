package com.blackbone.skill;

import java.util.UUID;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
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
 * and plays the style's first punch (auto 1) on the downed enemy instead. The left button is unchanged.
 * Runs on SKILL_CAST_EVENT, which Epic Fight fires on the server before it executes a skill.
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

		listener.addEventListener(EventType.SKILL_CAST_EVENT, EVENT_UUID, (event) -> {
			// Only the weapon skill, and only on the server (the client just sends the request).
			if (event.getSkillContainer().getSlot() != SkillSlots.WEAPON_INNATE || !(event.getPlayerPatch() instanceof ServerPlayerPatch patch)) {
				return;
			}

			if (!targetIsDowned(patch.getOriginal())) {
				return;
			}

			CapabilityItem capability = patch.getHoldingItemCapability(InteractionHand.MAIN_HAND);

			if (!(capability instanceof WeaponCapability weapon)) {
				return;
			}

			var combo = weapon.getAutoAttackMotion(patch);

			if (combo.isEmpty()) {
				return;
			}

			event.setCanceled(true);
			patch.playAnimationSynchronized(combo.get(0), 0.0F);

			SkillContainer basicAttack = patch.getSkill(SkillSlots.BASIC_ATTACK);

			if (basicAttack != null) {
				// The next left-click continues with punch 2.
				basicAttack.getDataManager().setData(SkillDataKeys.COMBO_COUNTER.get(), 1);
			}
		});
	}

	@Override
	public void onRemoved(SkillContainer container) {
		super.onRemoved(container);
		container.getExecutor().getEventListener().removeListener(EventType.SKILL_CAST_EVENT, EVENT_UUID);
	}

	/** The living entity under the crosshair is knocked down (Epic Fight) or sleeping. */
	private static boolean targetIsDowned(Player player) {
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
