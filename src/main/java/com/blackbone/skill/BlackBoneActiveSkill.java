package com.blackbone.skill;

import com.blackbone.state.BlackBoneMode;
import com.blackbone.state.BlackBoneState;
import com.blackbone.state.BlackBoneStates;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillBuilder;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Flow and Zone. Both are Identity-category skills that are cast through Epic Fight's normal
 * skill request pipeline (SkillContainer#sendCastRequest -> CPSkillRequest -> executeOnServer).
 * The 100% stack requirement, the exclusivity of Flow/Zone and the 60 second timer are enforced
 * on the server by {@link BlackBoneManager}.
 */
public class BlackBoneActiveSkill extends Skill {
	private final BlackBoneMode mode;

	public static SkillBuilder<Skill> createBuilder() {
		SkillBuilder<Skill> builder = new SkillBuilder<>();
		builder.setCategory(SkillCategories.IDENTITY);
		builder.setActivateType(ActivateType.DURATION);
		builder.setResource(Resource.NONE);

		return builder;
	}

	public BlackBoneActiveSkill(SkillBuilder<? extends Skill> builder, BlackBoneMode mode) {
		super(builder);

		this.mode = mode;
		this.maxDuration = BlackBoneManager.DURATION_TICKS;
	}

	public BlackBoneMode getMode() {
		return this.mode;
	}

	@Override
	public void setParams(CompoundTag parameters) {
		super.setParams(parameters);

		// Duration is fixed at exactly 60 seconds regardless of datapack values.
		this.maxDuration = BlackBoneManager.DURATION_TICKS;
		this.consumption = 0.0F;
	}

	@Override
	public void onInitiate(SkillContainer container) {
		super.onInitiate(container);
		container.setMaxDuration(BlackBoneManager.DURATION_TICKS);
	}

	/** Allow casting in the air; only Epic Fight's own "can use skill" state check applies. */
	@Override
	public boolean isExecutableState(PlayerPatch<?> executor) {
		return !executor.getOriginal().isSpectator() && executor.getEntityState().canUseSkill();
	}

	/** Used by the client (greys the icon) and the server; the server re-checks in executeOnServer. */
	@Override
	public boolean checkExecuteCondition(SkillContainer container) {
		BlackBoneState state = BlackBoneStates.view(container.getExecutor().getOriginal());

		return state != null && state.mode == BlackBoneMode.NONE && state.stacksOf(this.mode) >= BlackBoneManager.MAX_STACK;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void validationFeedback(SkillContainer container) {
		Player player = container.getExecutor().getOriginal();
		BlackBoneState state = BlackBoneStates.view(player);

		if (state == null) {
			return;
		}

		BlackBoneManager.Result result;

		if (state.mode == this.mode) {
			result = BlackBoneManager.Result.ALREADY_ACTIVE;
		} else if (state.mode != BlackBoneMode.NONE) {
			result = BlackBoneManager.Result.OTHER_ACTIVE;
		} else {
			result = BlackBoneManager.Result.NOT_ENOUGH;
		}

		Component message = BlackBoneManager.messageFor(result, this.mode, state.stacksOf(this.mode));
		player.displayClientMessage(message, true);
	}

	@Override
	public void executeOnServer(SkillContainer container, FriendlyByteBuf args) {
		ServerPlayerPatch executor = container.getServerExecutor();
		ServerPlayer player = executor.getOriginal();
		BlackBoneManager.Result result = BlackBoneManager.tryActivate(player, this.mode);

		if (result != BlackBoneManager.Result.OK) {
			// Rejected: nothing is consumed and the container is not activated.
			player.displayClientMessage(BlackBoneManager.messageFor(result, this.mode, BlackBoneManager.state(player).stacksOf(this.mode)), true);
			return;
		}

		// Activates the container (duration display) and sends the standard "executed" feedback to the client.
		super.executeOnServer(container, args);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public boolean shouldDraw(SkillContainer container) {
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void drawOnGui(BattleModeGui gui, SkillContainer container, GuiGraphics guiGraphics, float x, float y, float partialTick) {
		PoseStack poseStack = guiGraphics.pose();
		poseStack.pushPose();
		poseStack.translate(0, (float)gui.getSlidingProgression(), 0);
		guiGraphics.blit(this.getSkillTexture(), (int)x, (int)y, 24, 24, 0, 0, 1, 1, 1, 1);

		BlackBoneState state = BlackBoneStates.view(container.getExecutor().getOriginal());
		String text = "0%";

		if (state != null) {
			text = state.mode == this.mode ? ((state.remainingTicks + 19) / 20) + "s" : state.stacksOf(this.mode) + "%";
		}

		guiGraphics.drawString(gui.getFont(), text, x + 12 - 3 * text.length(), y + 6, 16777215, true);
		poseStack.popPose();
	}
}
