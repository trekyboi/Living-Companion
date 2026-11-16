package com.livingcompanion.resurrection;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.registry.ModEntities;
import net.minecraft.core.BlockPos;
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

        long delay = CompanionConfig.RESURRECTION_DELAY_SECONDS.get() * 20L;
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
            if (now < entry.resurrectAt) continue;
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
                if (owner != null) owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Knight: I'm back. That bed was a welcome sight."));
                iterator.remove();
                changed = true;
            }
        }
        if (changed) setDirty();
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
        if (level.dimension().equals(entry.dimension) && level.hasChunkAt(requested)) {
            BlockPos candidate = level.getBlockState(requested).getBlock() instanceof BedBlock
                    ? requested.above() : requested;
            if (level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()) {
                return candidate;
            }
        }
        return level.getSharedSpawnPos().above();
    }

    private static final class PendingResurrection {
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private final BlockPos position;
        private final long resurrectAt;
        private final CompoundTag entityData;

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
