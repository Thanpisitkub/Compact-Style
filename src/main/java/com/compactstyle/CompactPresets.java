package com.compactstyle;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import reascer.wom.gameasset.WOMSkills;
import reascer.wom.gameasset.animations.weapons.AnimsAgony;
import reascer.wom.gameasset.animations.weapons.AnimsEnderblaster;
import reascer.wom.gameasset.animations.weapons.AnimsMoonless;
import reascer.wom.gameasset.animations.weapons.AnimsNova;
import reascer.wom.gameasset.animations.weapons.AnimsOrbit;
import reascer.wom.gameasset.animations.weapons.AnimsSatsujin;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/**
 * Weapon types ("presets") for Compact Style.
 *
 * Every style shares Agony's stance: idle, walk, run (and guard).
 * Only the attack combo differs per style. Each combo is 6 animations:
 *   [auto 1, auto 2, auto 3, auto 4, dash attack, air attack]
 * (the last two slots are the dash and air-attack, same layout WoM uses).
 *
 * To change a style's moves, edit the animation names in the matching
 * method below. Names come from the WoM AnimsXxx classes.
 *
 * The preset ids are compact_style:classic, compact_style:muaythai,
 * compact_style:chinese and compact_style:taekwondo. The datapack files in
 * data/compact_style/capabilities/weapons/ refer to these ids.
 */
public final class CompactPresets {
    private CompactPresets() {}

    public static void register(WeaponCapabilityPresetRegistryEvent event) {
        Map<ResourceLocation, Function<Item, CapabilityItem.Builder>> types = event.getTypeEntry();
        types.put(id("classic"), CompactPresets::classic);
        types.put(id("muaythai"), CompactPresets::muaythai);
        types.put(id("chinese"), CompactPresets::chinese);
        types.put(id("taekwondo"), CompactPresets::taekwondo);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CompactStyle.MODID, path);
    }

    /** Classic: Agony's own full combo. */
    private static CapabilityItem.Builder classic(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsAgony.AGONY_AUTO_1,
                AnimsAgony.AGONY_AUTO_2,
                AnimsAgony.AGONY_AUTO_3,
                AnimsAgony.AGONY_AUTO_4,
                AnimsAgony.AGONY_CLAWSTRIKE,
                AnimsAgony.AGONY_RIPPING_FANGS);
    }

    /** Muay Thai: Ender Blaster one-hand set (punches, knee, kick) with its jump kick. */
    private static CapabilityItem.Builder muaythai(Item item) {
        applySpeedTweaks();
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_AUTO_1,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_AUTO_2,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_AUTO_3,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_AUTO_4,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_DASH,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_JUMPKICK);
    }

    /** Chinese: flowing mix of Satsujin, Orbit and Moonless strikes. */
    private static CapabilityItem.Builder chinese(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsSatsujin.SATSUJIN_AUTO_1,
                AnimsOrbit.ORBIT_ATTACK_2,
                AnimsSatsujin.SATSUJIN_AUTO_3,
                AnimsMoonless.MOONLESS_AUTO_2,
                AnimsSatsujin.SATSUJIN_HARUSAKI,
                AnimsNova.NOVA_ATTACK_AIRSLASH);
    }

    /** Taekwondo: Ender Blaster two-hand set with Nova's dash attack. */
    private static CapabilityItem.Builder taekwondo(Item item) {
        applySpeedTweaks();
        return base(CompactPresets::scytheSkill).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_1,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_2,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_3,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_4,
                AnimsNova.NOVA_ATTACK_DASH,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_TISHNAW);
    }

    /** Shared part: fist category, Agony's stance animations, Agony collider. */
    private static WeaponCapability.Builder base(Supplier<Skill> innateSkill) {
        return WeaponCapability.builder()
                .category(CapabilityItem.WeaponCategories.FIST)
                .styleProvider(entitypatch -> CapabilityItem.Styles.TWO_HAND)
                .collider(WOMWeaponColliders.AGONY)
                .hitSound(EpicFightSounds.BLUNT_HIT.get())
                .canBePlacedOffhand(false)
                .innateSkill(CapabilityItem.Styles.TWO_HAND, itemstack -> innateSkill.get())
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.IDLE, AnimsAgony.AGONY_IDLE)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.WALK, AnimsAgony.AGONY_WALK)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.CHASE, AnimsAgony.AGONY_RUN)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.RUN, AnimsAgony.AGONY_RUN)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.SWIM, Animations.BIPED_HOLD_SPEAR)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.BLOCK, AnimsAgony.AGONY_GUARD);
    }

    // ---------------------------------------------------------------------
    // Tweaks
    // ---------------------------------------------------------------------

    /** How much faster the tweaked animations play (1.15 = 15% faster). */
    private static final float MUAYTHAI_JUMPKICK_SPEED = 1.15F;
    private static final float MUAYTHAI_JUMPSPIN_SPEED = 1.15F;

    /**
     * Taekwondo's jump kick is the same animation the Clawed Gauntle (Jabberwocky) uses.
     * Jabberwocky has attack speed 3.75 and the Taekwondo item has 2.15, so with the same
     * animation it plays slower. This multiplier (3.75 / 2.15) gives it the same speed.
     */
    private static final float TAEKWONDO_JUMPKICK_SPEED = 3.75F / 2.15F;

    private static boolean speedTweaksApplied = false;

    /**
     * Speeds up some WoM animations, but only while one of our items is in the
     * main hand. The animations are shared with Weapons of Miracles (for example the
     * Ender Blaster and Jabberwocky), and those weapons are left unchanged.
     * Runs once, after the animations have been registered.
     */
    private static synchronized void applySpeedTweaks() {
        if (speedTweaksApplied) {
            return;
        }
        speedTweaksApplied = true;
        scaleSpeed(AnimsEnderblaster.ENDERBLASTER_ONEHAND_JUMPKICK, CompactStyle.MUAYTHAI_GLOVES::get, MUAYTHAI_JUMPKICK_SPEED);
        scaleSpeed(AnimsEnderblaster.ENDERBLASTER_ONEHAND_DASH, CompactStyle.MUAYTHAI_GLOVES::get, MUAYTHAI_JUMPSPIN_SPEED);
        scaleSpeed(AnimsEnderblaster.ENDERBLASTER_TWOHAND_TISHNAW, CompactStyle.TAEKWONDO_STYLE::get, TAEKWONDO_JUMPKICK_SPEED);
    }

    private static void scaleSpeed(AnimationManager.AnimationAccessor<? extends StaticAnimation> accessor,
                                   Supplier<Item> heldItem, float factor) {
        StaticAnimation animation = accessor.get();
        AnimationProperty.PlaybackSpeedModifier previous = animation
                .getProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER)
                .orElse(null);
        animation.addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER,
                (self, entitypatch, speed, prevElapsedTime, elapsedTime) -> {
                    float base = previous == null
                            ? speed
                            : previous.modify(self, entitypatch, speed, prevElapsedTime, elapsedTime);
                    Entity entity = entitypatch.getOriginal();
                    if (entity instanceof LivingEntity living && living.getMainHandItem().is(heldItem.get())) {
                        return base * factor;
                    }
                    return base;
                });
    }

    /**
     * The scythe's hold-attack skill from Epic Fight Extra. Looked up by name
     * so this mod still loads without Epic Fight Extra (falls back to Agony's skill).
     */
    private static Skill scytheSkill() {
        try {
            Object skill = Class.forName("com.asanginxst.epicfightx.gameassets.ExtraSkills")
                    .getField("SCYTHE_SKILL").get(null);
            if (skill instanceof Skill) {
                return (Skill) skill;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // Epic Fight Extra is not installed
        }
        return WOMSkills.AGONY_PLUNGE;
    }
}
