package com.livingcompanion.network;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import net.minecraft.client.Minecraft;
import com.livingcompanion.registry.ModEntities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Server-authoritative packets for the first-time companion creation flow. */
public final class CompanionNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LivingCompanion.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, OpenCreationMessage.class,
                OpenCreationMessage::encode, OpenCreationMessage::decode,
                OpenCreationMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1, CreateCompanionMessage.class,
                CreateCompanionMessage::encode, CreateCompanionMessage::decode,
                CreateCompanionMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, UpgradeSkillMessage.class,
                UpgradeSkillMessage::encode, UpgradeSkillMessage::decode,
                UpgradeSkillMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, RequestSkillDataMessage.class,
                RequestSkillDataMessage::encode, RequestSkillDataMessage::decode,
                RequestSkillDataMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, SkillDataMessage.class,
                SkillDataMessage::encode, SkillDataMessage::decode,
                SkillDataMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void openCreation(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCreationMessage());
    }

    /** Sends a skill-rank purchase request; the server validates all requirements. */
    public static void requestSkillUpgrade(int companionEntityId, String skillId) {
        CHANNEL.sendToServer(new UpgradeSkillMessage(companionEntityId, skillId));
    }

    public static void requestSkillData(int companionEntityId) {
        CHANNEL.sendToServer(new RequestSkillDataMessage(companionEntityId));
    }

    private static void sendSkillData(ServerPlayer player, CompanionEntity companion) {
        int[] ranks = new int[CompanionSkill.values().length];
        for (CompanionSkill skill : CompanionSkill.values()) {
            ranks[skill.ordinal()] = companion.getProgression().getSkillRank(skill.getId());
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkillDataMessage(companion.getId(), companion.getCompanionClass().ordinal(),
                        companion.getProgression().getLevel(), companion.getProgression().getExperience(),
                        companion.getProgression().getExperienceToNextLevel(),
                        companion.getProgression().getSkillPoints(), ranks));
    }

    public static final class RequestSkillDataMessage {
        private final int companionEntityId;
        public RequestSkillDataMessage(int id) { this.companionEntityId = id; }
        public static void encode(RequestSkillDataMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.companionEntityId);
        }
        public static RequestSkillDataMessage decode(FriendlyByteBuf buffer) {
            return new RequestSkillDataMessage(buffer.readVarInt());
        }
        public static void handle(RequestSkillDataMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(message.companionEntityId);
                if (entity instanceof CompanionEntity companion && companion.isOwnedBy(player)
                        && companion.distanceToSqr(player) <= 64.0D) sendSkillData(player, companion);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class SkillDataMessage {
        private final int entityId, classId, level, experience, experienceToNext, skillPoints;
        private final int[] ranks;
        public SkillDataMessage(int entityId, int classId, int level, int experience,
                                int experienceToNext, int skillPoints, int[] ranks) {
            this.entityId = entityId; this.classId = classId; this.level = level;
            this.experience = experience; this.experienceToNext = experienceToNext;
            this.skillPoints = skillPoints; this.ranks = ranks;
        }
        public static void encode(SkillDataMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId); buffer.writeVarInt(message.classId);
            buffer.writeVarInt(message.level); buffer.writeVarInt(message.experience);
            buffer.writeVarInt(message.experienceToNext); buffer.writeVarInt(message.skillPoints);
            buffer.writeVarIntArray(message.ranks);
        }
        public static SkillDataMessage decode(FriendlyByteBuf buffer) {
            return new SkillDataMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarIntArray(CompanionSkill.values().length));
        }
        public static void handle(SkillDataMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft = Minecraft.getInstance();
                net.minecraft.client.gui.screens.Screen parent = minecraft.screen;
                com.livingcompanion.client.screen.CompanionSkillsScreen.open(parent, message);
            }));
            context.setPacketHandled(true);
        }
    }

    public static final class UpgradeSkillMessage {
        private final int companionEntityId;
        private final String skillId;

        public UpgradeSkillMessage(int companionEntityId, String skillId) {
            this.companionEntityId = companionEntityId;
            this.skillId = skillId;
        }

        public static void encode(UpgradeSkillMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.companionEntityId);
            buffer.writeUtf(message.skillId, 64);
        }

        public static UpgradeSkillMessage decode(FriendlyByteBuf buffer) {
            return new UpgradeSkillMessage(buffer.readVarInt(), buffer.readUtf(64));
        }

        public static void handle(UpgradeSkillMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(message.companionEntityId);
                if (!(entity instanceof CompanionEntity companion)
                        || !companion.isOwnedBy(player)
                        || companion.distanceToSqr(player) > 64.0D) return;

                CompanionSkill skill = CompanionSkill.byId(message.skillId);
                if (skill == null || !companion.getProgression().unlockSkillRank(
                        skill, companion.getCompanionClass())) {
                    player.sendSystemMessage(Component.literal("That skill rank is unavailable. Check its class, level, and skill-point requirements."));
                    return;
                }

                companion.inventoryChanged();
                player.sendSystemMessage(Component.literal(skill.getDisplayName() + " upgraded to rank "
                        + companion.getProgression().getSkillRank(skill.getId()) + "."));
                sendSkillData(player, companion);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class OpenCreationMessage {
        public static void encode(OpenCreationMessage message, FriendlyByteBuf buffer) { }
        public static OpenCreationMessage decode(FriendlyByteBuf buffer) { return new OpenCreationMessage(); }
        public static void handle(OpenCreationMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.livingcompanion.client.screen.CompanionCreationScreen.open()));
            context.setPacketHandled(true);
        }
    }

    public static final class CreateCompanionMessage {
        private final String name;
        private final int classId;

        public CreateCompanionMessage(String name, int classId) {
            this.name = name;
            this.classId = classId;
        }

        public static void encode(CreateCompanionMessage message, FriendlyByteBuf buffer) {
            buffer.writeUtf(message.name, 24);
            buffer.writeVarInt(message.classId);
        }

        public static CreateCompanionMessage decode(FriendlyByteBuf buffer) {
            return new CreateCompanionMessage(buffer.readUtf(24), buffer.readVarInt());
        }

        public static void handle(CreateCompanionMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || player.getServer() == null) return;
                if (message.classId <= CompanionClass.UNASSIGNED.ordinal()
                        || message.classId >= CompanionClass.values().length) return;
                String cleanName = message.name == null ? "" : message.name.replaceAll("[\\p{Cntrl}]", "").trim();
                if (cleanName.isEmpty() || cleanName.length() > 24) {
                    player.sendSystemMessage(Component.literal("Choose a name between 1 and 24 characters."));
                    return;
                }
                if (findCompanion(player) != null) {
                    player.getPersistentData().putBoolean("LivingCompanionCreationComplete", true);
                    player.sendSystemMessage(Component.literal("You already have a companion."));
                    return;
                }

                CompanionClass archetype = CompanionClass.values()[message.classId];
                ServerLevel level = player.serverLevel();
                CompanionEntity companion = ModEntities.COMPANION.get().create(level);
                if (companion == null) return;
                companion.moveTo(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D,
                        player.getYRot(), 0.0F);
                companion.bindOwner(player);
                companion.setCompanionClass(archetype);
                companion.setCompanionName(cleanName);
                if (level.addFreshEntity(companion)) {
                    player.getPersistentData().putBoolean("LivingCompanionCreationComplete", true);
                    player.sendSystemMessage(Component.literal(cleanName + " has joined your adventure as a "
                            + archetype.getDisplayName() + "."));
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static CompanionEntity findCompanion(ServerPlayer owner) {
        if (owner.getServer() == null) return null;
        for (ServerLevel level : owner.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion && companion.isOwnedBy(owner)) return companion;
            }
        }
        return null;
    }

    private CompanionNetwork() { }
}
