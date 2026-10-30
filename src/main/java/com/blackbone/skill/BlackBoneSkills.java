package com.blackbone.skill;

import com.blackbone.BlackBoneMod;
import com.blackbone.state.BlackBoneMode;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.api.forgeevent.SkillBuildEvent;
import yesman.epicfight.api.forgeevent.SkillBuildEvent.ModRegistryWorker;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.passive.PassiveSkill;

/**
 * Skill registration through Epic Fight's SkillBuildEvent (mod event bus), the same mechanism
 * Epic Fight uses for its own skills in {@code EpicFightSkills}.
 */
@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BlackBoneSkills {
	public static Skill BLACK_BONE;
	public static Skill FLOW;
	public static Skill ZONE;
	public static Skill DOWNED_PUNCH;

	private BlackBoneSkills() {
	}

	public static Skill of(BlackBoneMode mode) {
		return switch (mode) {
			case FLOW -> FLOW;
			case ZONE -> ZONE;
			default -> null;
		};
	}

	@SubscribeEvent
	public static void buildSkillEvent(SkillBuildEvent build) {
		ModRegistryWorker registry = build.createRegistryWorker(BlackBoneMod.MODID);

		BLACK_BONE = registry.build("black_bone", BlackBoneSkill::new, PassiveSkill.createPassiveBuilder());
		FLOW = registry.build("flow", builder -> new BlackBoneActiveSkill(builder, BlackBoneMode.FLOW), BlackBoneActiveSkill.createBuilder());
		ZONE = registry.build("zone", builder -> new BlackBoneActiveSkill(builder, BlackBoneMode.ZONE), BlackBoneActiveSkill.createBuilder());
		DOWNED_PUNCH = registry.build("downed_punch", DownedPunchSkill::new, DownedPunchSkill.createBuilder());
	}
}
