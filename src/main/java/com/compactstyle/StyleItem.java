package com.compactstyle;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

/**
 * A weapon-like item that shows "Attack Damage" and "Attack Speed" in its
 * tooltip the same way a vanilla sword does.
 *
 * @param damage      the number added to the player's base damage (a diamond sword is 6.0)
 * @param attackSpeed the attack speed shown in the tooltip (a diamond sword is 1.6)
 */
public class StyleItem extends Item {
    private final Multimap<Attribute, AttributeModifier> modifiers;

    public StyleItem(Properties properties, double damage, double attackSpeed) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed - 4.0, AttributeModifier.Operation.ADDITION));
        this.modifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.modifiers : super.getDefaultAttributeModifiers(slot);
    }
}
