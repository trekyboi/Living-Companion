package com.livingcompanion.ai;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.EnumSet;

/** Wanders around the position where the owner issued PATROL. */
public final class CompanionPatrolGoal extends Goal {
    private final CompanionEntity companion;
    private int nextPatrolTick;

    public CompanionPatrolGoal(CompanionEntity companion) {
        this.companion = companion;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return companion.getCommand() == CompanionEntity.Command.PATROL
                && companion.getTarget() == null && companion.getOwnerPlayer() != null;
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void start() { nextPatrolTick = 0; }

    @Override
    public void tick() {
        if (companion.getTarget() != null) return;
        if (--nextPatrolTick > 0 && !companion.getNavigation().isDone()) return;
        BlockPos anchor = companion.getPatrolAnchor();
        int radius = (int) Math.ceil(CompanionConfig.PATROL_RADIUS.get());
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = anchor.getX() + companion.getRandom().nextInt(radius * 2 + 1) - radius;
            int z = anchor.getZ() + companion.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = companion.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos destination = new BlockPos(x, y, z);
            if (!companion.level().isLoaded(destination)
                    || !companion.level().getBlockState(destination).getFluidState().isEmpty()
                    || destination.distSqr(anchor) > radius * radius) continue;
            if (companion.getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 0.9D)) {
                nextPatrolTick = 80 + companion.getRandom().nextInt(81);
                return;
            }
        }
        nextPatrolTick = 40;
    }
}
