package com.blackbone;

import com.blackbone.network.BlackBoneNetwork;
import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * Black Bone skills, built into Compact Style. Not a separate mod: Compact Style's
 * constructor calls {@link #init(IEventBus)}. MODID is only the resource namespace
 * (assets/blackbone, data/blackbone), the network channel and the skill id prefix.
 */
public class BlackBoneMod {
	public static final String MODID = "blackbone";
	public static final Logger LOGGER = LogUtils.getLogger();

	private BlackBoneMod() {}

	public static void init(IEventBus modBus) {
		// Flow and Zone share Epic Fight's single Identity slot, so only one can be equipped at a time.
		modBus.addListener(BlackBoneMod::commonSetup);
	}

	private static void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(BlackBoneNetwork::register);
	}
}
