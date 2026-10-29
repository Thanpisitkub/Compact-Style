package com.blackbone.network;

import java.util.function.Supplier;

import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: stack counters and active mode of one player (identified by entity id). */
public final class SyncStatePacket {
	private final int entityId;
	private final int flowStacks;
	private final int zoneStacks;
	private final int modeId;
	private final int remainingTicks;

	public SyncStatePacket(int entityId, int flowStacks, int zoneStacks, BlackBoneMode mode, int remainingTicks) {
		this.entityId = entityId;
		this.flowStacks = flowStacks;
		this.zoneStacks = zoneStacks;
		this.modeId = mode.id;
		this.remainingTicks = remainingTicks;
	}

	public static void encode(SyncStatePacket msg, FriendlyByteBuf buf) {
		buf.writeVarInt(msg.entityId);
		buf.writeVarInt(msg.flowStacks);
		buf.writeVarInt(msg.zoneStacks);
		buf.writeVarInt(msg.modeId);
		buf.writeVarInt(msg.remainingTicks);
	}

	public static SyncStatePacket decode(FriendlyByteBuf buf) {
		int entityId = buf.readVarInt();
		int flow = buf.readVarInt();
		int zone = buf.readVarInt();
		BlackBoneMode mode = BlackBoneMode.byId(buf.readVarInt());
		int remaining = buf.readVarInt();

		return new SyncStatePacket(entityId, flow, zone, mode, remaining);
	}

	/** Runs on the client main thread; only touches common classes, so it is dedicated-server safe. */
	public static void handle(SyncStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
		BlackBoneState state = BlackBoneStates.CLIENT.computeIfAbsent(msg.entityId, id -> new BlackBoneState());
		state.flowStacks = msg.flowStacks;
		state.zoneStacks = msg.zoneStacks;
		state.mode = BlackBoneMode.byId(msg.modeId);
		state.remainingTicks = msg.remainingTicks;

		ctx.get().setPacketHandled(true);
	}
}
