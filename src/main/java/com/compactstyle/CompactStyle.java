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
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6.5, 2.15));

    public static final RegistryObject<Item> CHINESE_STYLE = ITEMS.register("chinese_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 2.15));

    public static final RegistryObject<Item> TAEKWONDO_STYLE = ITEMS.register("taekwondo_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6.5, 2.15));

    public static final RegistryObject<Item> CAPOEIRA_STYLE = ITEMS.register("capoeira_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2.5, 2.35));

    public static final RegistryObject<Item> JUDO_STYLE = ITEMS.register("judo_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4.5, 1.9));

    public static final RegistryObject<Item> BJJ_STYLE = ITEMS.register("bjj_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 2.0));

    public static final RegistryObject<Item> KICKBOXING_STYLE = ITEMS.register("kickboxing_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 2.25));

    public static final RegistryObject<Item> JKD_STYLE = ITEMS.register("jkd_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2.5, 2.5));

    public static final RegistryObject<Item> SANDA_STYLE = ITEMS.register("sanda_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 2.2));

    public static final RegistryObject<Item> KYOKUSHIN_STYLE = ITEMS.register("kyokushin_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5.5, 1.85));

    public static final RegistryObject<Item> BREAKDANCE_STYLE = ITEMS.register("breakdance_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 2.25));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("compact_style",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.compact_style"))
                    .icon(() -> new ItemStack(CLASSIC.get()))
                    .displayItems((params, output) -> {
                        output.accept(CLASSIC.get());
                        output.accept(MUAYTHAI_GLOVES.get());
                        output.accept(CHINESE_STYLE.get());
                        output.accept(TAEKWONDO_STYLE.get());
                        output.accept(CAPOEIRA_STYLE.get());
                        output.accept(JUDO_STYLE.get());
                        output.accept(BJJ_STYLE.get());
                        output.accept(KICKBOXING_STYLE.get());
                        output.accept(JKD_STYLE.get());
                        output.accept(SANDA_STYLE.get());
                        output.accept(KYOKUSHIN_STYLE.get());
                        output.accept(BREAKDANCE_STYLE.get());
                        addSkillBooks(output);

                    })
                    .build());

    /** Epic Fight skill books for the Black Bone skills, so they can be taken from this tab. */
    private static void addSkillBooks(CreativeModeTab.Output output) {
        for (yesman.epicfight.skill.Skill skill : new yesman.epicfight.skill.Skill[] {
                com.blackbone.skill.BlackBoneSkills.BLACK_BONE,
                com.blackbone.skill.BlackBoneSkills.FLOW,
                com.blackbone.skill.BlackBoneSkills.ZONE}) {
            if (skill != null) {
                ItemStack book = new ItemStack(yesman.epicfight.world.item.EpicFightItems.SKILLBOOK.get());
                yesman.epicfight.world.item.SkillBookItem.setContainingSkill(skill, book);
                output.accept(book);
            }
        }
    }

    public CompactStyle() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        // Registers the compact_style:* weapon types (needs Epic Fight and WoM)
        bus.addListener(CompactPresets::register);
        // Black Bone skills (Flow, Zone, Black Bone) are part of this jar
        com.blackbone.BlackBoneMod.init(bus);
    }
}
