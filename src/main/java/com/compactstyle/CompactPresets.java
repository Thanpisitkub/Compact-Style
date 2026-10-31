package com.compactstyle;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import com.blackbone.skill.BlackBoneSkills;
import reascer.wom.gameasset.WOMSkills;
import reascer.wom.gameasset.animations.weapons.AnimsAgony;
import reascer.wom.gameasset.animations.weapons.AnimsBlackstar;
import reascer.wom.gameasset.animations.weapons.AnimsEnderblaster;
import reascer.wom.gameasset.animations.weapons.AnimsHerrscher;
import reascer.wom.gameasset.animations.weapons.AnimsMoonless;
import reascer.wom.gameasset.animations.weapons.AnimsNapoleon;
import reascer.wom.gameasset.animations.weapons.AnimsNova;
import reascer.wom.gameasset.animations.weapons.AnimsOrbit;
import reascer.wom.gameasset.animations.weapons.AnimsRuine;
import reascer.wom.gameasset.animations.weapons.AnimsSatsujin;
import reascer.wom.gameasset.animations.weapons.AnimsSolar;
import reascer.wom.gameasset.colliders.WOMWeaponColliders;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.forgeevent.WeaponCapabilityPresetRegistryEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.EpicFightSounds;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
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
        types.put(id("capoeira"), CompactPresets::capoeira);
        types.put(id("judo"), CompactPresets::judo);
        types.put(id("bjj"), CompactPresets::bjj);
        types.put(id("kickboxing"), CompactPresets::kickboxing);
        types.put(id("jkd"), CompactPresets::jkd);
        types.put(id("sanda"), CompactPresets::sanda);
        types.put(id("kyokushin"), CompactPresets::kyokushin);
        types.put(id("basic"), CompactPresets::basic);
        types.put(id("breakdance"), CompactPresets::breakdance);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CompactStyle.MODID, path);
    }

    /** Classic: Ironclad gauntlet punches (EF Champion Zone) mixed with Agony. Hits a downed enemy with punch 1. */
    private static CapabilityItem.Builder classic(Item item) {
        return withDownedPunch(base(() -> WOMSkills.AGONY_PLUNGE)).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                cz("SFA_Animations", "IRONCLAD_AUTO1", AnimsAgony.AGONY_AUTO_1),
                cz("SFA_Animations", "IRONCLAD_AUTO2", AnimsAgony.AGONY_AUTO_2),
                AnimsAgony.AGONY_AUTO_3,
                AnimsAgony.AGONY_AUTO_4,
                cz("SFA_Animations", "IRONCLAD_DASH", AnimsAgony.AGONY_CLAWSTRIKE),
                cz("SFA_Animations", "IRONCLAD_AIRSLASH", AnimsAgony.AGONY_RIPPING_FANGS));
    }

    /** Muay Thai: the Ender Blaster one-hand set (punches, knee, kick, dash and jump kick). */
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

    /** Chinese: the whole Nightfall nf_meen set (stance, four strikes, dash, air strike). */
    private static CapabilityItem.Builder chinese(Item item) {
        WeaponCapability.Builder builder = base(() -> WOMSkills.AGONY_PLUNGE);
        withStance(builder, NightfallAnimations.MEEN_IDLE, NightfallAnimations.MEEN_WALK, NightfallAnimations.MEEN_RUN);
        return builder.newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                sanji(NightfallAnimations.MEEN_AUTO_1, AnimsSatsujin.SATSUJIN_AUTO_1),
                sanji(NightfallAnimations.MEEN_AUTO_2, AnimsSatsujin.SATSUJIN_AUTO_2),
                sanji(NightfallAnimations.MEEN_AUTO_3, AnimsSatsujin.SATSUJIN_AUTO_3),
                sanji(NightfallAnimations.MEEN_AUTO_4, AnimsSatsujin.SATSUJIN_AUTO_3),
                sanji(NightfallAnimations.MEEN_DASH, AnimsSatsujin.SATSUJIN_HARUSAKI),
                sanji(NightfallAnimations.MEEN_AIRSLASH, AnimsNova.NOVA_ATTACK_AIRSLASH));
    }

    /** Taekwondo: the Ender Blaster two-hand set with its jump kick and the scythe hold-attack skill. */
    private static CapabilityItem.Builder taekwondo(Item item) {
        applySpeedTweaks();
        return withDownedPunch(base(CompactPresets::scytheSkill)).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_1,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_2,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_3,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_AUTO_4,
                AnimsEnderblaster.ENDERBLASTER_ONEHAND_DASH,
                AnimsEnderblaster.ENDERBLASTER_TWOHAND_TISHNAW);
    }


    /** Capoeira: the Moonless set (verso strikes) with Agony's run-then-punch dash. */
    private static CapabilityItem.Builder capoeira(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsMoonless.MOONLESS_AUTO_1_VERSO,
                AnimsMoonless.MOONLESS_AUTO_2_VERSO,
                AnimsMoonless.MOONLESS_AUTO_3_VERSO,
                AnimsMoonless.MOONLESS_AUTO_1,
                AnimsAgony.AGONY_CLAWSTRIKE,
                AnimsMoonless.MOONLESS_AUTO_3);
    }

    /**
     * Basic: Solar's Obscuridad form (stance and combo), with Ruine's sword jump attack (Comet).
     */
    private static CapabilityItem.Builder basic(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.IDLE, AnimsSolar.SOLAR_OBSCURIDAD_IDLE)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.WALK, AnimsSolar.SOLAR_OBSCURIDAD_WALK)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.CHASE, AnimsSolar.SOLAR_OBSCURIDAD_RUN)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.RUN, AnimsSolar.SOLAR_OBSCURIDAD_RUN)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.BLOCK, AnimsSolar.SOLAR_GUARD)
                .newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsSolar.SOLAR_OBSCURIDAD_AUTO_1,
                AnimsSolar.SOLAR_OBSCURIDAD_AUTO_2,
                AnimsSolar.SOLAR_OBSCURIDAD_AUTO_3,
                AnimsSolar.SOLAR_OBSCURIDAD_AUTO_4,
                AnimsSolar.SOLAR_OBSCURIDAD_IMPACTO,
                AnimsRuine.RUINE_COMET);
    }

    /** Breakdance: the whole Epic Sanji set (stance and four kicks; its autos are reused for dash and air). */
    private static CapabilityItem.Builder breakdance(Item item) {
        WeaponCapability.Builder builder = base(() -> WOMSkills.AGONY_PLUNGE);
        withStance(builder, SanjiAnimations.IDLE, SanjiAnimations.WALK, SanjiAnimations.RUN);
        return builder.newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                sanji(SanjiAnimations.AUTO_1, AnimsMoonless.MOONLESS_AUTO_1_VERSO),
                sanji(SanjiAnimations.AUTO_2, AnimsMoonless.MOONLESS_AUTO_2_VERSO),
                sanji(SanjiAnimations.AUTO_3, AnimsMoonless.MOONLESS_AUTO_3_VERSO),
                sanji(SanjiAnimations.AUTO_4, AnimsMoonless.MOONLESS_AUTO_3_VERSO),
                sanji(SanjiAnimations.AUTO_4, AnimsAgony.AGONY_CLAWSTRIKE),
                sanji(SanjiAnimations.AUTO_2, AnimsMoonless.MOONLESS_AUTO_3));
    }

    /** Judo: the Herrscher / Gesetz throwing set. */
    private static CapabilityItem.Builder judo(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsHerrscher.HERRSCHER_AUTO_1,
                AnimsHerrscher.HERRSCHER_AUTO_2,
                AnimsHerrscher.HERRSCHER_AUTO_3,
                AnimsHerrscher.GESETZ_AUTO_1,
                AnimsHerrscher.GESETZ_KRUMMEN,
                AnimsAgony.AGONY_AIR_ATTACK_3);
    }

    /** Brazilian Jiu-Jitsu: the Ruine set (four strikes, dash and air strike). */
    private static CapabilityItem.Builder bjj(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsRuine.RUINE_AUTO_1,
                AnimsRuine.RUINE_AUTO_2,
                AnimsRuine.RUINE_AUTO_3,
                AnimsRuine.RUINE_AUTO_4,
                AnimsRuine.RUINE_PUNITION,
                AnimsRuine.RUINE_COMET);
    }

    /** Kickboxing: the Blackstar set. */
    private static CapabilityItem.Builder kickboxing(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsBlackstar.BLACKSTAR_ATTACK_1,
                AnimsBlackstar.BLACKSTAR_ATTACK_2,
                AnimsBlackstar.BLACKSTAR_ATTACK_3,
                AnimsBlackstar.BLACKSTAR_ATTACK_4,
                AnimsOrbit.ORBIT_ATTACK_4,
                AnimsAgony.AGONY_AIR_ATTACK_4);
    }

    /** Jeet Kune Do: the whole Nova set (four strikes, dash, air slash). */
    private static CapabilityItem.Builder jkd(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsNova.NOVA_ATTACK_1,
                AnimsNova.NOVA_ATTACK_2,
                AnimsNova.NOVA_ATTACK_3,
                AnimsNova.NOVA_ATTACK_4,
                AnimsNova.NOVA_ATTACK_DASH,
                AnimsNova.NOVA_ATTACK_AIRSLASH);
    }

    /** Sanda: the Solar set (four strikes and Horno dash). */
    private static CapabilityItem.Builder sanda(Item item) {
        return base(() -> WOMSkills.AGONY_PLUNGE).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                AnimsSolar.SOLAR_AUTO_1,
                AnimsSolar.SOLAR_AUTO_2,
                AnimsSolar.SOLAR_AUTO_3,
                AnimsSolar.SOLAR_AUTO_4,
                AnimsSolar.SOLAR_HORNO,
                AnimsAgony.AGONY_AIR_ATTACK_1);
    }

    /** Kyokushin: Napoleon strikes with the Ironclad charge. Hits a downed enemy. */
    private static CapabilityItem.Builder kyokushin(Item item) {
        return withDownedPunch(base(() -> WOMSkills.AGONY_PLUNGE)).newStyleCombo(CapabilityItem.Styles.TWO_HAND,
                cz("SFA_Animations", "IRONCLAD_AUTO2", AnimsNapoleon.NAPOLEON_AUTO_1),
                AnimsNapoleon.NAPOLEON_AUTO_2,
                AnimsNapoleon.NAPOLEON_AUTO_3,
                AnimsNapoleon.NAPOLEON_AUTO_4,
                cz("SFA_Animations", "IRONCLAD_FIST_CHARGE", AnimsNapoleon.NAPOLEON_AUTO_4),
                AnimsAgony.AGONY_AIR_ATTACK_3);
    }

    /** Adds the weapon passive that makes a downed enemy get punched with the style's first punch. */
    private static WeaponCapability.Builder withDownedPunch(WeaponCapability.Builder builder) {
        return BlackBoneSkills.DOWNED_PUNCH == null ? builder : builder.passiveSkill(BlackBoneSkills.DOWNED_PUNCH);
    }

    /**
     * An attack animation from Epic Fight Champion Zone (ef_cz), looked up by name so this mod still
     * loads without it. Falls back to a Weapons of Miracles animation when ef_cz is missing or the
     * field is not an attack animation.
     */
    @SuppressWarnings("unchecked")
    private static AnimationManager.AnimationAccessor<? extends AttackAnimation> cz(String className, String field,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> fallback) {
        try {
            Object value = Class.forName("com.pr1me_m4sk.ef_cz.gameasset.Animations.biped.player." + className).getField(field).get(null);

            if (value instanceof AnimationManager.AnimationAccessor<?> accessor && accessor.get() instanceof AttackAnimation) {
                return (AnimationManager.AnimationAccessor<? extends AttackAnimation>) accessor;
            }
        } catch (Throwable ignored) {
            // ef_cz not installed or renamed: use the fallback
        }

        return fallback;
    }

    /** Replaces the stance (idle / walk / run) when its animations are available; otherwise keeps Agony's. */
    private static void withStance(WeaponCapability.Builder builder, AnimationManager.AnimationAccessor<? extends StaticAnimation> idle,
            AnimationManager.AnimationAccessor<? extends StaticAnimation> walk, AnimationManager.AnimationAccessor<? extends StaticAnimation> run) {
        if (idle == null || walk == null || run == null) {
            return;
        }

        builder.livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.IDLE, idle)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.WALK, walk)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.CHASE, run)
                .livingMotionModifier(CapabilityItem.Styles.TWO_HAND, LivingMotions.RUN, run);
    }

    /** An Epic Sanji move re-created in SanjiAnimations, or the given Weapons of Miracles move when it is not available. */
    private static AnimationManager.AnimationAccessor<? extends AttackAnimation> sanji(
            AnimationManager.AnimationAccessor<? extends AttackAnimation> move,
            AnimationManager.AnimationAccessor<? extends AttackAnimation> fallback) {
        return move != null ? move : fallback;
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
    private static final float TAEKWONDO_JUMPKICK_TARGET_ATTACK_SPEED = 3.75F;

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
        matchAttackSpeed(AnimsEnderblaster.ENDERBLASTER_TWOHAND_TISHNAW, CompactStyle.TAEKWONDO_STYLE::get,
                TAEKWONDO_JUMPKICK_TARGET_ATTACK_SPEED);
    }

    /**
     * Makes the animation play as if the player had {@code targetAttackSpeed}. Epic Fight plays
     * an attack at 1 + (x - 1) * ATTACK_SPEED_FACTOR where x grows with the attack speed, so the
     * change is applied to x and not to the final speed.
     */
    private static void matchAttackSpeed(AnimationManager.AnimationAccessor<? extends StaticAnimation> accessor,
                                         Supplier<Item> heldItem, float targetAttackSpeed) {
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
                    if (base > 0.5F && entity instanceof LivingEntity living
                            && living.getMainHandItem().is(heldItem.get())
                            && entitypatch instanceof PlayerPatch<?> player) {
                        float current = player.getAttackSpeed(InteractionHand.MAIN_HAND);
                        float factor = self instanceof StaticAnimation sa
                                ? sa.getProperty(AnimationProperty.AttackAnimationProperty.ATTACK_SPEED_FACTOR).orElse(1.0F)
                                : 1.0F;
                        if (current > 0.0F && factor > 0.0F) {
                            float x = (base - 1.0F) / factor + 1.0F;
                            return 1.0F + (x * targetAttackSpeed / current - 1.0F) * factor;
                        }
                    }
                    return base;
                });
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
