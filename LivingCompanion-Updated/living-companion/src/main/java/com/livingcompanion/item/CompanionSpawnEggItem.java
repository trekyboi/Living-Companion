package com.livingcompanion.item;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Spawn egg that explicitly binds the using player as the companion's owner.
 *
 * <p>Vanilla {@link SpawnEggItem#useOn} cannot hand us the spawned entity, so we
 * replicate its spawn call (including the vanilla Y-offset rule for upward
 * faces) and bind ownership directly. Using the egg on a mob spawner
 * intentionally does NOT convert the spawner — a companion spawner would be
 * exploitable; eggs always spawn one companion.</p>
 *
 * <p>Tint colors are handled automatically: {@code SpawnEggItem}'s constructor
 * registers into {@code BY_ID}, which vanilla's {@code ItemColors} iterates.</p>
 */
public class CompanionSpawnEggItem extends SpawnEggItem {

    public CompanionSpawnEggItem(EntityType<? extends Mob> type, int backgroundColor,
                                 int highlightColor, Properties properties) {
        super(type, backgroundColor, highlightColor, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            ItemStack stack = context.getItemInHand();
            BlockPos clicked = context.getClickedPos();
            Direction face = context.getClickedFace();
            BlockPos spawnPos = clicked.relative(face);
            Player player = context.getPlayer();

            EntityType<?> type = com.livingcompanion.registry.ModEntities.COMPANION.get();
            Entity entity = type.spawn((ServerLevel) level, stack, player, spawnPos,
                    MobSpawnType.SPAWN_EGG, true,
                    !Objects.equals(clicked, spawnPos) && face == Direction.UP);

            if (entity instanceof CompanionEntity companion && player != null) {
                companion.bindOwner(player);
            }
            stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
