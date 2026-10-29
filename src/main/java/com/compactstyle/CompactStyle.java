package com.compactstyle;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(CompactStyle.MODID)
public class CompactStyle {
    public static final String MODID = "compact_style";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Plain Items (not BlockItems), so none of them can place a block.
    public static final RegistryObject<Item> CLASSIC = ITEMS.register("classic",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 7.0, 2.30));

    public static final RegistryObject<Item> MUAYTHAI_GLOVES = ITEMS.register("muaythai_gloves",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 2.15));

    public static final RegistryObject<Item> CHINESE_STYLE = ITEMS.register("chinese_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 2.15));

    public static final RegistryObject<Item> TAEKWONDO_STYLE = ITEMS.register("taekwondo_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4.0, 2.15));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("compact_style",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.compact_style"))
                    .icon(() -> new ItemStack(CLASSIC.get()))
                    .displayItems((params, output) -> {
                        output.accept(CLASSIC.get());
                        output.accept(MUAYTHAI_GLOVES.get());
                        output.accept(CHINESE_STYLE.get());
                        output.accept(TAEKWONDO_STYLE.get());
                    })
                    .build());

    public CompactStyle() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        // Registers the compact_style:* weapon types (needs Epic Fight and WoM)
        bus.addListener(CompactPresets::register);
    }
}
