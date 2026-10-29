package com.blackbone.skill;

import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

/**
 * Two extra Identity-category slots so Flow and Zone can both be equipped at the same time
 * (Epic Fight itself only ships one Identity slot). They are generic: any Identity skill can go into any
 * Identity slot, and Epic Fight puts a newly learned skill into the first empty one.
 * Registered with {@code SkillSlot.ENUM_MANAGER.registerEnumCls} in the mod constructor, as described
 * in the Epic Fight developer docs ("Registering custom skill slots").
 *
 * The enum names must be globally unique; the lang keys are
 * {@code epicfight.skill_slot.blackbone_identity_2} and {@code epicfight.skill_slot.blackbone_identity_3}.
 */
public enum BlackBoneSlots implements SkillSlot {
	BLACKBONE_IDENTITY_2(SkillCategories.IDENTITY),
	BLACKBONE_IDENTITY_3(SkillCategories.IDENTITY);

	private final SkillCategory category;
	private final int id;

	BlackBoneSlots(SkillCategory category) {
		this.category = category;
		this.id = SkillSlot.ENUM_MANAGER.assign(this);
	}

	@Override
	public SkillCategory category() {
		return this.category;
	}

	@Override
	public int universalOrdinal() {
		return this.id;
	}
}
