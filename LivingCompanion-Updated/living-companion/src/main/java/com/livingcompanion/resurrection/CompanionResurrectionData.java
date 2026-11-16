package com.livingcompanion.resurrection;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** Persistent queue for companions waiting to resurrect. Stored in the overworld data folder. */
public final class CompanionResurrectionData extends SavedData {
    private static final String DATA_NAME = "livingcompanion_resurrections";
    private final List<PendingResurrection> pending = new ArrayList<>();

    public static CompanionResurrectionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                CompanionResurrectionData::load,
                CompanionResurrectionData::new,
                DATA_NAME);
    }

    public static CompanionResurrectionData load(CompoundTag tag) {
        CompanionResurrectionData data = new CompanionResurrectionData();
        ListTag list = tag.getList("Pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("Owner") && entry.contains("Entity", Tag.TAG_COMPOUND)) {
                data.pending.add(PendingResurrection.load(entry));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PendingResurrection entry : pending) list.add(entry.save());
        tag.put("Pending", list);
        return tag;
    }

    /** Captures the dead companion's equipment, owner, command, and memory journal. */
    public void enqueue(CompanionEntity companion, MinecraftServer server) {
        UUID ownerId = companion.getOwnerUUID();
        if (ownerId == null || contains(ownerId)) return;

        CompoundTag entityTag = companion.saveWithoutId(new CompoundTag());
        // A new entity UUID is assigned during resurrection; the old entity has died.
        entityTag.remove("UUID");
        entityTag.putFloat("Health", companion.getMaxHealth());
        entityTag.putShort("DeathTime", (short) 0);
        entityTag.putShort("HurtTime", (short) 0);

        ResourceKey<Level> dimension = Level.OVERWORLD;
        BlockPos respawnPos = server.overworld().getSharedSpawnPos();
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            ResourceKey<Level> ownerDimension = owner.getRespawnDimension();
            BlockPos ownerBed = owner.getRespawnPosition();
            if (ownerBed != null) {
                dimension = ownerDimension;
                respawnPos = ownerBed;
            }
        }

        // Creative owners get an immediate return. Survival owners receive a short countdown.
        boolean creativeOwner = owner != null && owner.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
        long delay = creativeOwner ? 0L : CompanionConfig.RESURRECTION_DELAY_SECONDS.get() * 20L;
        pending.add(new PendingResurrection(ownerId, dimension, respawnPos,
                server.overworld().getGameTime() + delay, entityTag));
        setDirty();
    }

    /** Called once per server tick. Removes completed entries only after a spawn succeeds. */
    public void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        Iterator<PendingResurrection> iterator = pending.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            PendingResurrection entry = iterator.next();
            if (now < entry.resurrectAt) {
                ServerPlayer owner = server.getPlayerList().getPlayer(entry.owner);
                if (owner != null) {
                    long remainingTicks = entry.resurrectAt - now;
                    int remainingSeconds = (int) ((remainingTicks + 19L) / 20L);
                    if (remainingSeconds > 0 && remainingSeconds != entry.lastAnnouncedSecond) {
                        owner.sendSystemMessage(Component.literal("Knight respawns in "
                                + remainingSeconds + "s"), true);
                        entry.lastAnnouncedSecond = remainingSeconds;
                    }
                }
                continue;
            }
            if (hasLivingCompanion(server, entry.owner)) {
                iterator.remove();
                changed = true;
                continue;
            }

            ServerLevel destination = server.getLevel(entry.dimension);
            if (destination == null) destination = server.overworld();
            BlockPos spawnPos = resolveSpawnPosition(destination, entry);
            CompanionEntity companion = ModEntities.COMPANION.get().create(destination);
            if (companion == null) continue;

            CompoundTag entityTag = entry.entityData.copy();
            entityTag.remove("UUID");
            entityTag.putFloat("Health", companion.getMaxHealth());
            entityTag.putShort("DeathTime", (short) 0);
            entityTag.putShort("HurtTime", (short) 0);
            companion.load(entityTag);
            companion.setOwnerUUID(entry.owner);
            companion.markForFullHealthRestore();
            companion.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(),
                    spawnPos.getZ() + 0.5D, destination.random.nextFloat() * 360.0F, 0.0F);
            companion.setHealth(companion.getMaxHealth());

            if (destination.addFreshEntity(companion)) {
                ServerPlayer owner = server.getPlayerList().getPlayer(entry.owner);
                if (owner != null) owner.sendSystemMessage(Component.literal(
                        "Knight: I'm back. That bed was a welcome sight."));
                iterator.remove();
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    public boolean hasPendingResurrection(UUID owner) {
        return owner != null && contains(owner);
    }

    private boolean contains(UUID owner) {
        return pending.stream().anyMatch(entry -> entry.owner.equals(owner));
    }

    private static boolean hasLivingCompanion(MinecraftServer server, UUID owner) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive() && owner.equals(companion.getOwnerUUID())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockPos resolveSpawnPosition(ServerLevel level, PendingResurrection entry) {
        BlockPos requested = entry.position;

        // Force-load the saved respawn chunk before checking it. The old
        // hasChunkAt() guard sent companions to world spawn whenever the
        // owner's bed chunk happened to be unloaded.
        if (level.dimension().equals(entry.dimension)) {
            level.getChunkAt(requested);
            if (level.getBlockState(requested).getBlock() instanceof BedBlock) {
                // Try the bed first, then the four adjacent blocks. This also
                // handles the head half of a double bed and obstructed bed tops.
                BlockPos[] candidates = new BlockPos[] {
                        requested.above(),
                        requested.north(), requested.south(),
                        requested.east(), requested.west(),
                        requested.north().above(), requested.south().above(),
                        requested.east().above(), requested.west().above()
                };
                for (BlockPos candidate : candidates) {
                    if (isSafeSpawnPosition(level, candidate)) return candidate;
                }
            }
        }

        // A missing, broken, or obstructed bed uses the dimension's safe
        // shared spawn area. Search nearby positions before using the exact
        // spawn point as a final fallback.
        BlockPos worldSpawn = level.getSharedSpawnPos();
        level.getChunkAt(worldSpawn);
        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos candidate = worldSpawn.offset(dx, 0, dz).above();
                    if (isSafeSpawnPosition(level, candidate)) return candidate;
                }
            }
        }
        return worldSpawn.above();
    }

    private static boolean isSafeSpawnPosition(ServerLevel level, BlockPos position) {
        BlockPos below = position.below();
        return level.getBlockState(position).isAir()
                && level.getBlockState(position.above()).isAir()
                && level.getBlockState(below).isSolidRender(level, below);
    }

    private static final class PendingResurrection {
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final BlockPos position;
        private final long resurrectAt;
        private final CompoundTag entityData;
        private int lastAnnouncedSecond = -1;

        private PendingResurrection(UUID owner, ResourceKey<Level> dimension, BlockPos position,
                                    long resurrectAt, CompoundTag entityData) {
            this.owner = owner;
            this.dimension = dimension;
            this.position = position;
            this.resurrectAt = resurrectAt;
            this.entityData = entityData;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Owner", owner);
            tag.putString("Dimension", dimension.location().toString());
            tag.putLong("Position", position.asLong());
            tag.putLong("ResurrectAt", resurrectAt);
            tag.put("Entity", entityData.copy());
            return tag;
        }

        private static PendingResurrection load(CompoundTag tag) {
            ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(tag.getString("Dimension")));
            return new PendingResurrection(tag.getUUID("Owner"), dimension,
                    BlockPos.of(tag.getLong("Position")), tag.getLong("ResurrectAt"),
                    tag.getCompound("Entity"));
        }
    }
}
