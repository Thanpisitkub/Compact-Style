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
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), 7.0, 1.90));

    public static final RegistryObject<Item> MUAYTHAI_GLOVES = ITEMS.register("muaythai_gloves",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6.5, 1.90));

    public static final RegistryObject<Item> CHINESE_STYLE = ITEMS.register("chinese_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 1.80));

    public static final RegistryObject<Item> TAEKWONDO_STYLE = ITEMS.register("taekwondo_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 6.5, 1.90));

    public static final RegistryObject<Item> CAPOEIRA_STYLE = ITEMS.register("capoeira_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2.5, 2.00));

    public static final RegistryObject<Item> JUDO_STYLE = ITEMS.register("judo_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 4.5, 1.60));

    public static final RegistryObject<Item> BJJ_STYLE = ITEMS.register("bjj_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 1.70));

    public static final RegistryObject<Item> KICKBOXING_STYLE = ITEMS.register("kickboxing_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 2.00));

    public static final RegistryObject<Item> JKD_STYLE = ITEMS.register("jkd_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 2.5, 2.10));

    public static final RegistryObject<Item> SANDA_STYLE = ITEMS.register("sanda_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.5, 1.90));

    public static final RegistryObject<Item> KYOKUSHIN_STYLE = ITEMS.register("kyokushin_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 5.5, 1.50));

    public static final RegistryObject<Item> BASIC_STYLE = ITEMS.register("basic_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 7.0, 1.90));

    public static final RegistryObject<Item> BREAKDANCE_STYLE = ITEMS.register("breakdance_style",
            () -> new StyleItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), 3.0, 2.00));

    // One spawn egg per style: spawns a zombie holding that style.
    public static final RegistryObject<Item> ZOMBIE_EGG_CLASSIC = ITEMS.register("zombie_egg_classic",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), CLASSIC::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_MUAYTHAI = ITEMS.register("zombie_egg_muaythai",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), MUAYTHAI_GLOVES::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_CHINESE = ITEMS.register("zombie_egg_chinese",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), CHINESE_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_TAEKWONDO = ITEMS.register("zombie_egg_taekwondo",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), TAEKWONDO_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_CAPOEIRA = ITEMS.register("zombie_egg_capoeira",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), CAPOEIRA_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_JUDO = ITEMS.register("zombie_egg_judo",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), JUDO_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_BJJ = ITEMS.register("zombie_egg_bjj",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), BJJ_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_KICKBOXING = ITEMS.register("zombie_egg_kickboxing",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), KICKBOXING_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_JKD = ITEMS.register("zombie_egg_jkd",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), JKD_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_SANDA = ITEMS.register("zombie_egg_sanda",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), SANDA_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_KYOKUSHIN = ITEMS.register("zombie_egg_kyokushin",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), KYOKUSHIN_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_BASIC = ITEMS.register("zombie_egg_basic",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), BASIC_STYLE::get));

    public static final RegistryObject<Item> ZOMBIE_EGG_BREAKDANCE = ITEMS.register("zombie_egg_breakdance",
            () -> new StyleZombieEggItem(new Item.Properties().stacksTo(16), BREAKDANCE_STYLE::get));

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
                        output.accept(BASIC_STYLE.get());
                        output.accept(BREAKDANCE_STYLE.get());
                        output.accept(ZOMBIE_EGG_CLASSIC.get());
                        output.accept(ZOMBIE_EGG_MUAYTHAI.get());
                        output.accept(ZOMBIE_EGG_CHINESE.get());
                        output.accept(ZOMBIE_EGG_TAEKWONDO.get());
                        output.accept(ZOMBIE_EGG_CAPOEIRA.get());
                        output.accept(ZOMBIE_EGG_JUDO.get());
                        output.accept(ZOMBIE_EGG_BJJ.get());
                        output.accept(ZOMBIE_EGG_KICKBOXING.get());
                        output.accept(ZOMBIE_EGG_JKD.get());
                        output.accept(ZOMBIE_EGG_SANDA.get());
                        output.accept(ZOMBIE_EGG_KYOKUSHIN.get());
                        output.accept(ZOMBIE_EGG_BASIC.get());
                        output.accept(ZOMBIE_EGG_BREAKDANCE.get());
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
        bus.addListener(SanjiAnimations::register);
        // Black Bone skills (Flow, Zone, Black Bone) are part of this jar
        com.blackbone.BlackBoneMod.init(bus);
    }
}
