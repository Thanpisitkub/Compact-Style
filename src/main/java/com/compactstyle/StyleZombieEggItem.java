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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

/**
 * Spawn egg for a zombie that holds one Compact Style item (so it fights with that style's animations and
 * damage), has 300 health, cannot burn and runs at twice the normal speed.
 */
public class StyleZombieEggItem extends ForgeSpawnEggItem {
	private final Supplier<Item> style;

	/** Uses the normal zombie spawn egg look (zombie colours); useOn is replaced so the zombie holds the style. */
	public StyleZombieEggItem(Properties properties, Supplier<Item> style) {
		super(() -> EntityType.ZOMBIE, 0x00AFAF, 0x799C65, properties);
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
		// 300 health, immune to fire (no sunlight burning) and far faster than a normal zombie (0.23).
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300.0D);
		zombie.setHealth(300.0F);
		zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.46D);
		zombie.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		serverLevel.addFreshEntityWithPassengers(zombie);

		if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
			context.getItemInHand().shrink(1);
		}

		return InteractionResult.CONSUME;
	}
}
