package com.blackbone.network;

import com.blackbone.BlackBoneMod;
import com.blackbone.state.BlackBoneState;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class BlackBoneNetwork {
	private static final String PROTOCOL = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(BlackBoneMod.MODID, "main"),
		() -> PROTOCOL,
		PROTOCOL::equals,
		PROTOCOL::equals
	);

	private BlackBoneNetwork() {
	}

	public static void register() {
		CHANNEL.messageBuilder(SyncStatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
			.encoder(SyncStatePacket::encode)
			.decoder(SyncStatePacket::decode)
			.consumerMainThread(SyncStatePacket::handle)
			.add();

		CHANNEL.messageBuilder(DownedStrikePacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
			.encoder(DownedStrikePacket::encode)
			.decoder(DownedStrikePacket::decode)
			.consumerMainThread(DownedStrikePacket::handle)
			.add();
	}

	private static SyncStatePacket packetFor(ServerPlayer player, BlackBoneState state) {
		return new SyncStatePacket(player.getId(), state.flowStacks, state.zoneStacks, state.mode, state.remainingTicks);
	}

	/** Sends to the player itself and everyone tracking it. */
	public static void syncToSelfAndTrackers(ServerPlayer player, BlackBoneState state) {
		CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packetFor(player, state));
	}

	/** Sends the state of {@code subject} to a single viewer (used when the viewer starts tracking it). */
	public static void syncToViewer(ServerPlayer viewer, ServerPlayer subject, BlackBoneState state) {
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packetFor(subject, state));
	}
}
