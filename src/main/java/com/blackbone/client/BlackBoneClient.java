package com.blackbone.client;

import com.blackbone.BlackBoneMod;
import com.blackbone.skill.BlackBoneManager;
import com.blackbone.skill.BlackBoneSkills;
import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;

/** Client-only: key bindings that trigger the skills through Epic Fight's cast pipeline, and the stack HUD. */
public final class BlackBoneClient {
	public static final KeyMapping FLOW_KEY = new KeyMapping("key.blackbone.flow", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.blackbone");
	public static final KeyMapping ZONE_KEY = new KeyMapping("key.blackbone.zone", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.blackbone");

	private BlackBoneClient() {
	}

	@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModBus {
		private ModBus() {
		}

		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event) {
			event.register(FLOW_KEY);
			event.register(ZONE_KEY);
		}

		@SubscribeEvent
		public static void registerOverlays(RegisterGuiOverlaysEvent event) {
			event.registerAboveAll("stacks", (gui, graphics, partialTick, width, height) -> {
				Minecraft mc = Minecraft.getInstance();
				Player player = mc.player;

				if (player == null || mc.options.hideGui) {
					return;
				}

				BlackBoneState state = BlackBoneStates.CLIENT.get(player.getId());
				LocalPlayerPatch patch = ClientEngine.getInstance().getPlayerPatch();

				if (state == null || patch == null) {
					return;
				}

				int y = height - 64;

				if (state.mode != BlackBoneMode.NONE) {
					int color = state.mode == BlackBoneMode.FLOW ? 0xFF4040 : 0x40B8FF;
					Component text = Component.translatable("hud.blackbone.active", Component.translatable(BlackBoneManager.modeKey(state.mode)), (state.remainingTicks + 19) / 20);
					graphics.drawCenteredString(mc.font, text, width / 2, y, color);
					return;
				}

				if (patch.getSkill(BlackBoneSkills.FLOW) != null) {
					boolean ready = state.flowStacks >= BlackBoneManager.MAX_STACK;
					graphics.drawCenteredString(mc.font, Component.translatable("hud.blackbone.stacks", Component.translatable(BlackBoneManager.modeKey(BlackBoneMode.FLOW)), state.flowStacks), width / 2, y, ready ? 0xFF4040 : 0xBBBBBB);
					y -= 10;
				}

				if (patch.getSkill(BlackBoneSkills.ZONE) != null) {
					boolean ready = state.zoneStacks >= BlackBoneManager.MAX_STACK;
					graphics.drawCenteredString(mc.font, Component.translatable("hud.blackbone.stacks", Component.translatable(BlackBoneManager.modeKey(BlackBoneMode.ZONE)), state.zoneStacks), width / 2, y, ready ? 0x40B8FF : 0xBBBBBB);
				}
			});
		}
	}

	@Mod.EventBusSubscriber(modid = com.compactstyle.CompactStyle.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static final class ForgeBus {
		private ForgeBus() {
		}

		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) {
				return;
			}

			while (FLOW_KEY.consumeClick()) {
				cast(BlackBoneMode.FLOW);
			}

			while (ZONE_KEY.consumeClick()) {
				cast(BlackBoneMode.ZONE);
			}
		}

		/** Goes through Epic Fight: SkillContainer#sendCastRequest checks canUse() and queues the CPSkillRequest packet. */
		private static void cast(BlackBoneMode mode) {
			Minecraft mc = Minecraft.getInstance();
			LocalPlayerPatch patch = ClientEngine.getInstance().getPlayerPatch();

			if (mc.player == null || patch == null || mc.screen != null) {
				return;
			}

			Skill skill = BlackBoneSkills.of(mode);
			SkillContainer container = skill == null ? null : patch.getSkill(skill);

			if (container == null) {
				mc.player.displayClientMessage(Component.translatable("message.blackbone.not_equipped", Component.translatable(BlackBoneManager.modeKey(mode))), true);
				return;
			}

			container.sendCastRequest(patch, ClientEngine.getInstance().controlEngine);
		}
	}
}
