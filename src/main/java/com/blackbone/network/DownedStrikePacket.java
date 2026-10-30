package com.blackbone.network;

import java.util.function.Supplier;

import com.blackbone.skill.DownedPunchSkill;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client -> server: "the weapon-skill button was pressed while looking at a downed enemy". */
public final class DownedStrikePacket {
	public static void encode(DownedStrikePacket msg, FriendlyByteBuf buf) {
	}

	public static DownedStrikePacket decode(FriendlyByteBuf buf) {
		return new DownedStrikePacket();
	}

	public static void handle(DownedStrikePacket msg, Supplier<NetworkEvent.Context> ctx) {
		ServerPlayer sender = ctx.get().getSender();

		if (sender != null) {
			DownedPunchSkill.performOnServer(sender);
		}

		ctx.get().setPacketHandled(true);
	}
}
