package com.blackbone.skill;

import java.util.UUID;

import com.blackbone.combat.BlackBoneReflection;

import net.minecraft.tags.DamageTypeTags;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.passive.PassiveSkill;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener.EventType;

/**
 * Black Bone (passive, always active while equipped):
 *  - takes 20% less damage (TAKE_DAMAGE_EVENT_HURT value modifier)
 *  - reflects 10% of the damage of unarmed enemy melee hits (TAKE_DAMAGE_EVENT_DAMAGE)
 */
public class BlackBoneSkill extends PassiveSkill {
	private static final UUID EVENT_UUID = UUID.fromString("5d3c3a8e-7a53-4a0f-9f5e-b1ac4b0e0001");

	public BlackBoneSkill(SkillBuilder<? extends PassiveSkill> builder) {
		super(builder);
	}

	@Override
	public void onInitiate(SkillContainer container) {
		super.onInitiate(container);

		PlayerEventListener listener = container.getExecutor().getEventListener();

		// Both event types are server-side only; addEventListener is a no-op on the client.
		listener.addEventListener(EventType.TAKE_DAMAGE_EVENT_HURT, EVENT_UUID, (event) -> {
			// Do not reduce damage that is meant to bypass everything (void, /kill).
			if (event.getDamageSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
				return;
			}

			event.attachValueModifier(ValueModifier.multiplier(BlackBoneReflection.REDUCTION_MULTIPLIER));
		});

		listener.addEventListener(EventType.TAKE_DAMAGE_EVENT_DAMAGE, EVENT_UUID, (event) -> {
			BlackBoneReflection.tryReflect(event.getPlayerPatch().getOriginal(), event.getDamageSource(), event.getDamage());
		});
	}

	@Override
	public void onRemoved(SkillContainer container) {
		super.onRemoved(container);

		PlayerEventListener listener = container.getExecutor().getEventListener();
		listener.removeListener(EventType.TAKE_DAMAGE_EVENT_HURT, EVENT_UUID);
		listener.removeListener(EventType.TAKE_DAMAGE_EVENT_DAMAGE, EVENT_UUID);
	}
}
