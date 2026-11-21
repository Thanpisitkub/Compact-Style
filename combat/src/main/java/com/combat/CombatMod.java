package com.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.UUID;

/**
 * Combat - adds the weapon items used by the Epic Fight datapack to the creative inventory.
 *
 * The item IDs are intentionally the same as the keys used by the datapack files
 * (data/efn/..., data/fiststyle/..., data/epicfight/...), so those files work unchanged.
 */
@Mod(CombatMod.MODID)
public class CombatMod {
    public static final String MODID = "combat";

    // Fist 1 -> data/efn/capabilities/weapons/test.json
    public static final ResourceLocation FIST_1 = new ResourceLocation("efn", "test");
    // Fist 2 -> data/fiststyle/capabilities/weapons/style_1.json
    public static final ResourceLocation FIST_2 = new ResourceLocation("fiststyle", "style_1");
    // Knife -> existing Epic Fight item, configured by data/epicfight/capabilities/weapons/netherite_dagger.json
    public static final ResourceLocation KNIFE = new ResourceLocation("epicfight", "netherite_dagger");

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> COMBAT_TAB = TABS.register("combat", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.combat"))
                    .icon(() -> new ItemStack(ForgeRegistries.ITEMS.getValue(FIST_1)))
                    .displayItems((parameters, output) -> {
                        for (ResourceLocation id : new ResourceLocation[]{FIST_1, FIST_2, KNIFE}) {
                            if (ForgeRegistries.ITEMS.containsKey(id)) {
                                output.accept(ForgeRegistries.ITEMS.getValue(id));
                            }
                        }
                    })
                    .build());

    // Style 2 animations play 10% faster than style 1 (Epic Fight scales attack animation
    // playback with the attack speed attribute). The datapack speed value stays 1.90 for both.
    private static final UUID STYLE_2_SPEED_ID = UUID.fromString("5f1c7b0e-3c2a-4d56-9a41-2b7a8f0c1d11");
    private static final double STYLE_2_ANIMATION_SPEED_BONUS = 0.10;

    public CombatMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CombatMod::onRegister);
        TABS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(CombatMod::onItemAttributes);
    }

    private static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() == EquipmentSlot.MAINHAND
                && ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()).equals(FIST_2)) {
            event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(
                    STYLE_2_SPEED_ID, "combat_style_2_animation_speed",
                    STYLE_2_ANIMATION_SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            helper.register(FIST_1, new Item(new Item.Properties().stacksTo(1)));
            helper.register(FIST_2, new Item(new Item.Properties().stacksTo(1)));
        });
    }
}
