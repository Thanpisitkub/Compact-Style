package com.compactstyle;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Spawns a zombie that holds one Compact Style item (so it fights with that style's animations and damage). */
public class StyleZombieEggItem extends Item {
	private final Supplier<Item> style;

	public StyleZombieEggItem(Properties properties, Supplier<Item> style) {
		super(properties);
		this.style = style;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();

		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}

		BlockPos clicked = context.getClickedPos();
		Direction face = context.getClickedFace();
		BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(face);
		Zombie zombie = EntityType.ZOMBIE.create(serverLevel);

		if (zombie == null) {
			return InteractionResult.FAIL;
		}

		zombie.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
		zombie.finalizeSpawn(serverLevel, level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null, null);
		// After finalizeSpawn, which may have given the zombie its own random gear.
		zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(style.get()));
		zombie.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		zombie.setCanPickUpLoot(false);
		zombie.setPersistenceRequired();
		serverLevel.addFreshEntityWithPassengers(zombie);

		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}

		return InteractionResult.CONSUME;
	}
}
