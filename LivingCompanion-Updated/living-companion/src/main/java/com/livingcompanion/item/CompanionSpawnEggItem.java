package com.livingcompanion.item;

import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.resurrection.CompanionResurrectionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeSpawnEggItem;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Spawn egg that defers entity type resolution until Forge has completed
 * registration, then binds the using player as the companion's owner.
 */
public class CompanionSpawnEggItem extends ForgeSpawnEggItem {

    private final Supplier<? extends EntityType<? extends Mob>> entityTypeSupplier;

    public CompanionSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type,
                                 int backgroundColor, int highlightColor,
                                 Properties properties) {
        super(type, backgroundColor, highlightColor, properties);
        this.entityTypeSupplier = type;
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
            ServerLevel serverLevel = (ServerLevel) level;

            // One owned companion per player. Check every dimension so a second
            // spawn egg cannot create a duplicate while the first is elsewhere.
            if (player != null && (hasOwnedCompanion(serverLevel, player)
                    || CompanionResurrectionData.get(serverLevel.getServer()).hasPendingResurrection(player.getUUID()))) {
                player.sendSystemMessage(Component.literal("You already have a companion. He is still out there."));
                return InteractionResult.FAIL;
            }

            EntityType<?> type = entityTypeSupplier.get();
            Entity entity = type.spawn(serverLevel, stack, player, spawnPos,
                    MobSpawnType.SPAWN_EGG, true,
                    !Objects.equals(clicked, spawnPos) && face == Direction.UP);

            if (entity instanceof CompanionEntity companion) {
                if (player != null) {
                    companion.bindOwner(player);
                }
                if (player == null || !player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean hasOwnedCompanion(ServerLevel level, Player owner) {
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive() && companion.isOwnedBy(owner)) {
                    return true;
                }
            }
        }
        return false;
    }
}
