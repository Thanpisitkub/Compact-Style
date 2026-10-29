package com.blackbone.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderType;

/**
 * Extends RenderType only to reach the protected RenderStateShard constants.
 * Additive (SRC_ALPHA, ONE) blending, no culling and no depth writes, so overlapping glow layers never
 * hide each other while the player's head can still occlude the effect.
 */
final class BlackBoneRenderTypes extends RenderType {
	static final RenderType GLOW = RenderType.create(
		"blackbone_glow",
		DefaultVertexFormat.POSITION_COLOR,
		VertexFormat.Mode.QUADS,
		1536,
		false,
		false,
		RenderType.CompositeState.builder()
			.setShaderState(POSITION_COLOR_SHADER)
			.setTransparencyState(LIGHTNING_TRANSPARENCY)
			.setCullState(NO_CULL)
			.setWriteMaskState(COLOR_WRITE)
			.createCompositeState(false)
	);

	private BlackBoneRenderTypes() {
		super("blackbone_unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> { }, () -> { });
	}
}
