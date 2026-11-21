package com.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

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

    public CombatMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CombatMod::onRegister);
        TABS.register(modBus);
    }

    private static void onRegister(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> {
            helper.register(FIST_1, new Item(new Item.Properties().stacksTo(1)));
            helper.register(FIST_2, new Item(new Item.Properties().stacksTo(1)));
        });
    }
}
