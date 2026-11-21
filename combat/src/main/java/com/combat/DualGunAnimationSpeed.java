package com.combat;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * Makes the WOM dual-gun (Enderblaster two-hand) combo animations play 10% faster, directly on the
 * animation (Epic Fight PLAY_SPEED_MODIFIER), and only while the player holds Combat style 2.
 * Everything else (other animations, other weapons, the item's own speed value) is untouched.
 */
public final class DualGunAnimationSpeed {
    private static final float SPEED_MULTIPLIER = 1.10F;

    private static final String[] ANIMATIONS = {
            "wom:biped/combat/enderblaster_twohand_auto_1",
            "wom:biped/combat/enderblaster_twohand_auto_2",
            "wom:biped/combat/enderblaster_twohand_auto_3"
    };

    private static final AnimationProperty.PlaybackSpeedModifier MODIFIER = DualGunAnimationSpeed::modify;

    private DualGunAnimationSpeed() {
    }

    private static float modify(DynamicAnimation self, LivingEntityPatch<?> patch, float speed,
                                float prevElapsedTime, float elapsedTime) {
        if (patch != null && patch.getOriginal() != null) {
            ItemStack held = patch.getOriginal().getMainHandItem();
            if (CombatMod.FIST_2.equals(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
                return speed * SPEED_MULTIPLIER;
            }
        }
        return speed;
    }

    /** Safe to call repeatedly; (re)attaches the modifier if an animation does not have it yet. */
    public static void apply() {
        for (String name : ANIMATIONS) {
            try {
                AnimationManager.AnimationAccessor<StaticAnimation> accessor = AnimationManager.byKey(name);
                if (accessor == null || AnimationManager.checkNull(accessor)) {
                    continue;
                }
                StaticAnimation animation = accessor.get();
                if (animation == null) {
                    continue;
                }
                if (animation.getProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null) != MODIFIER) {
                    animation.addProperty(AnimationProperty.StaticAnimationProperty.PLAY_SPEED_MODIFIER, MODIFIER);
                }
            } catch (Throwable ignored) {
                // animation not available (WOM missing / not loaded yet): try again later
            }
        }
    }
}
