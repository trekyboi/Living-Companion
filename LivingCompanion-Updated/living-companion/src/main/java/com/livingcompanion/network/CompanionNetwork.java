package com.livingcompanion.network;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import com.livingcompanion.progression.CompanionAttribute;
import com.livingcompanion.progression.CompanionSpecialization;
import com.livingcompanion.resurrection.CompanionResurrectionData;
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
        CHANNEL.registerMessage(5, UpgradeAttributeMessage.class,
                UpgradeAttributeMessage::encode, UpgradeAttributeMessage::decode,
                UpgradeAttributeMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(6, RequestStatsMessage.class,
                RequestStatsMessage::encode, RequestStatsMessage::decode,
                RequestStatsMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, StatsDataMessage.class,
                StatsDataMessage::encode, StatsDataMessage::decode,
                StatsDataMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(8, ChooseSpecializationMessage.class,
                ChooseSpecializationMessage::encode, ChooseSpecializationMessage::decode,
                ChooseSpecializationMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(9, OpenBackpackMessage.class,
                OpenBackpackMessage::encode, OpenBackpackMessage::decode,
                OpenBackpackMessage::handle, java.util.Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void openCreation(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCreationMessage());
    }

    /** Sends a skill-rank purchase request; the server validates all requirements. */
    public static void requestSkillUpgrade(int companionEntityId, String skillId) {
        CHANNEL.sendToServer(new UpgradeSkillMessage(companionEntityId, skillId));
    }

    public static void requestSpecialization(int companionEntityId, String specializationId) {
        CHANNEL.sendToServer(new ChooseSpecializationMessage(companionEntityId, specializationId));
    }

    public static void requestAttributeUpgrade(int companionEntityId, String attributeId) {
        CHANNEL.sendToServer(new UpgradeAttributeMessage(companionEntityId, attributeId));
    }

    public static void requestSkillData(int companionEntityId) {
        CHANNEL.sendToServer(new RequestSkillDataMessage(companionEntityId));
    }

    public static void requestOpenBackpack(int companionEntityId) {
        CHANNEL.sendToServer(new OpenBackpackMessage(companionEntityId));
    }

    public static void requestStats(int companionEntityId) {
        CHANNEL.sendToServer(new RequestStatsMessage(companionEntityId));
    }

    private static void sendStatsData(ServerPlayer player, CompanionEntity companion) {
        net.minecraft.world.entity.ai.attributes.AttributeInstance maxHealth =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        net.minecraft.world.entity.ai.attributes.AttributeInstance attack =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        net.minecraft.world.entity.ai.attributes.AttributeInstance armor =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        net.minecraft.world.entity.ai.attributes.AttributeInstance toughness =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        net.minecraft.world.entity.ai.attributes.AttributeInstance movement =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        net.minecraft.world.entity.ai.attributes.AttributeInstance knockback =
                companion.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
        double[] stats = {
                companion.getHealth(), companion.getMaxHealth(),
                companion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                companion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),
                companion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS),
                companion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED),
                companion.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE),
                maxHealth == null ? companion.getMaxHealth() : maxHealth.getBaseValue(),
                attack == null ? 0.0D : attack.getBaseValue(),
                armor == null ? 0.0D : armor.getBaseValue(),
                toughness == null ? 0.0D : toughness.getBaseValue(),
                movement == null ? 0.0D : movement.getBaseValue(),
                knockback == null ? 0.0D : knockback.getBaseValue()
        };
        int[] attributes = new int[CompanionAttribute.values().length];
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            attributes[attribute.ordinal()] = companion.getProgression().getAttribute(attribute);
        }
        int[] ranks = new int[CompanionSkill.values().length];
        for (CompanionSkill skill : CompanionSkill.values()) {
            ranks[skill.ordinal()] = companion.getProgression().getSkillRank(skill.getId());
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new StatsDataMessage(companion.getId(), companion.getCompanionClass().ordinal(),
                        companion.getProgression().getLevel(), companion.getProgression().getExperience(),
                        companion.getProgression().getExperienceToNextLevel(), stats, attributes, ranks,
                        companion.getRelationship().getLevel(), companion.getRelationship().getExperience(),
                        companion.getRelationship().getExperienceToNextLevel(), companion.getRelationship().getTitle(),
                        companion.getProgression().getTitle(), companion.getProgression().getAchievements().size(),
                        companion.getProgression().getAchievements().stream()
                                .map(achievement -> achievement.getTitle()).toArray(String[]::new)));
    }

    public static final class OpenBackpackMessage {
        private final int companionEntityId;
        public OpenBackpackMessage(int companionEntityId) { this.companionEntityId = companionEntityId; }
        public static void encode(OpenBackpackMessage message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.companionEntityId); }
        public static OpenBackpackMessage decode(FriendlyByteBuf buffer) { return new OpenBackpackMessage(buffer.readVarInt()); }
        public static void handle(OpenBackpackMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !net.minecraftforge.fml.ModList.get().isLoaded("curios")
                        || !net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) return;
                Entity entity = player.serverLevel().getEntity(message.companionEntityId);
                if (!(entity instanceof CompanionEntity companion) || !companion.isOwnedBy(player)
                        || !companion.isAlive() || companion.distanceToSqr(player) > 64.0D) return;
                com.livingcompanion.compat.sophisticated.SophisticatedBackpacksCompat.openBackpack(player, companion);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class RequestStatsMessage {
        private final int entityId;
        public RequestStatsMessage(int entityId) { this.entityId = entityId; }
        public static void encode(RequestStatsMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId);
        }
        public static RequestStatsMessage decode(FriendlyByteBuf buffer) {
            return new RequestStatsMessage(buffer.readVarInt());
        }
        public static void handle(RequestStatsMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(message.entityId);
                if (entity instanceof CompanionEntity companion && companion.isOwnedBy(player)
                        && companion.distanceToSqr(player) <= 64.0D) sendStatsData(player, companion);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class StatsDataMessage {
        public final int entityId, classId, level, experience, experienceToNext;
        public final int relationshipLevel, relationshipExperience, relationshipExperienceToNext;
        public final String relationshipTitle, companionTitle;
        public final int achievementCount;
        public final String[] achievementTitles;
        public final double[] stats;
        public final int[] attributes, ranks;
        public StatsDataMessage(int entityId, int classId, int level, int experience,
                                int experienceToNext, double[] stats, int[] attributes, int[] ranks,
                                int relationshipLevel, int relationshipExperience,
                                int relationshipExperienceToNext, String relationshipTitle,
                                String companionTitle, int achievementCount, String[] achievementTitles) {
            this.entityId=entityId; this.classId=classId; this.level=level;
            this.experience=experience; this.experienceToNext=experienceToNext;
            this.stats=stats; this.attributes=attributes; this.ranks=ranks;
            this.relationshipLevel=relationshipLevel; this.relationshipExperience=relationshipExperience;
            this.relationshipExperienceToNext=relationshipExperienceToNext;
            this.relationshipTitle=relationshipTitle;
            this.companionTitle=companionTitle; this.achievementCount=achievementCount;
            this.achievementTitles=achievementTitles;
        }
        public static void encode(StatsDataMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId); buffer.writeVarInt(message.classId);
            buffer.writeVarInt(message.level); buffer.writeVarInt(message.experience);
            buffer.writeVarInt(message.experienceToNext);
            buffer.writeVarInt(message.stats.length);
            for (double value : message.stats) buffer.writeDouble(value);
            buffer.writeVarIntArray(message.attributes);
            buffer.writeVarIntArray(message.ranks);
            buffer.writeVarInt(message.relationshipLevel);
            buffer.writeVarInt(message.relationshipExperience);
            buffer.writeVarInt(message.relationshipExperienceToNext);
            buffer.writeUtf(message.relationshipTitle, 64);
            buffer.writeUtf(message.companionTitle, 64);
            buffer.writeVarInt(message.achievementCount);
            buffer.writeVarInt(message.achievementTitles.length);
            for (String achievement : message.achievementTitles) buffer.writeUtf(achievement, 64);
        }
        public static StatsDataMessage decode(FriendlyByteBuf buffer) {
            int entityId=buffer.readVarInt(), classId=buffer.readVarInt(), level=buffer.readVarInt();
            int experience=buffer.readVarInt(), next=buffer.readVarInt();
            int count=Math.min(16, buffer.readVarInt());
            double[] stats=new double[count];
            for(int i=0;i<count;i++) stats[i]=buffer.readDouble();
            int[] attributes=buffer.readVarIntArray(CompanionAttribute.values().length);
            int[] ranks=buffer.readVarIntArray(CompanionSkill.values().length);
            int relationshipLevel=buffer.readVarInt();
            int relationshipExperience=buffer.readVarInt();
            int relationshipNext=buffer.readVarInt();
            String relationshipTitle=buffer.readUtf(64);
            String companionTitle=buffer.readUtf(64);
            int achievementCount=buffer.readVarInt();
            int achievementTitleCount=Math.min(32, buffer.readVarInt());
            String[] achievementTitles=new String[achievementTitleCount];
            for(int i=0;i<achievementTitleCount;i++) achievementTitles[i]=buffer.readUtf(64);
            return new StatsDataMessage(entityId,classId,level,experience,next,stats,
                    attributes,ranks,relationshipLevel,relationshipExperience,relationshipNext,
                    relationshipTitle,companionTitle,achievementCount,achievementTitles);
        }
        public static void handle(StatsDataMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context=supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft=Minecraft.getInstance();
                net.minecraft.client.gui.screens.Screen parent=minecraft.screen;
                if(parent instanceof com.livingcompanion.client.screen.CompanionStatsScreen statsScreen) {
                    statsScreen.updateData(message);
                } else {
                    com.livingcompanion.client.screen.CompanionStatsScreen.open(parent,message);
                }
            }));
            context.setPacketHandled(true);
        }
    }

    private static void sendSkillData(ServerPlayer player, CompanionEntity companion) {
        int[] ranks = new int[CompanionSkill.values().length];
        for (CompanionSkill skill : CompanionSkill.values()) {
            ranks[skill.ordinal()] = companion.getProgression().getSkillRank(skill.getId());
        }
        int[] cooldowns = new int[CompanionSkill.values().length];
        int[] activeTicks = new int[CompanionSkill.values().length];
        for (CompanionSkill skill : CompanionSkill.values()) {
            cooldowns[skill.ordinal()] = companion.getSkillRuntime().getCooldownRemaining(skill);
            activeTicks[skill.ordinal()] = companion.getSkillRuntime().getActiveTicksRemaining(skill);
        }
        int[] attributes = new int[CompanionAttribute.values().length];
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            attributes[attribute.ordinal()] = companion.getProgression().getAttribute(attribute);
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkillDataMessage(companion.getId(), companion.getCompanionClass().ordinal(),
                        companion.getProgression().getSpecialization() == null ? -1
                                : companion.getProgression().getSpecialization().ordinal(),
                        companion.getProgression().getLevel(), companion.getProgression().getExperience(),
                        companion.getProgression().getExperienceToNextLevel(),
                        companion.getProgression().getSkillPoints(), companion.getProgression().getAttributePoints(),
                        ranks, cooldowns, activeTicks, attributes));
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
        public final int entityId, classId, specializationId, level, experience, experienceToNext, skillPoints, attributePoints;
        public final int[] ranks;
        public final int[] cooldowns;
        public final int[] activeTicks;
        public final int[] attributes;
        public SkillDataMessage(int entityId, int classId, int specializationId, int level, int experience,
                                int experienceToNext, int skillPoints, int attributePoints, int[] ranks,
                                int[] cooldowns, int[] activeTicks, int[] attributes) {
            this.entityId = entityId; this.classId = classId; this.specializationId = specializationId; this.level = level;
            this.experience = experience; this.experienceToNext = experienceToNext;
            this.skillPoints = skillPoints; this.attributePoints = attributePoints; this.ranks = ranks;
            this.cooldowns = cooldowns; this.activeTicks = activeTicks; this.attributes = attributes;
        }
        public static void encode(SkillDataMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.entityId); buffer.writeVarInt(message.classId);
            buffer.writeVarInt(message.specializationId);
            buffer.writeVarInt(message.level); buffer.writeVarInt(message.experience);
            buffer.writeVarInt(message.experienceToNext); buffer.writeVarInt(message.skillPoints);
            buffer.writeVarInt(message.attributePoints);
            buffer.writeVarIntArray(message.ranks);
            buffer.writeVarIntArray(message.cooldowns);
            buffer.writeVarIntArray(message.activeTicks);
            buffer.writeVarIntArray(message.attributes);
        }
        public static SkillDataMessage decode(FriendlyByteBuf buffer) {
            return new SkillDataMessage(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarIntArray(CompanionSkill.values().length),
                    buffer.readVarIntArray(CompanionSkill.values().length),
                    buffer.readVarIntArray(CompanionSkill.values().length),
                    buffer.readVarIntArray(CompanionAttribute.values().length));
        }
        public static void handle(SkillDataMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft minecraft = Minecraft.getInstance();
                net.minecraft.client.gui.screens.Screen parent = minecraft.screen;
                if (parent instanceof com.livingcompanion.client.screen.CompanionAttributesScreen attributesScreen) {
                    attributesScreen.updateData(message);
                } else if (parent instanceof com.livingcompanion.client.screen.CompanionSkillsScreen skillsScreen) {
                    skillsScreen.updateData(message);
                } else {
                    com.livingcompanion.client.screen.CompanionSkillsScreen.open(parent, message);
                }
            }));
            context.setPacketHandled(true);
        }
    }

    public static final class ChooseSpecializationMessage {
        private final int companionEntityId;
        private final String specializationId;

        public ChooseSpecializationMessage(int companionEntityId, String specializationId) {
            this.companionEntityId = companionEntityId;
            this.specializationId = specializationId;
        }

        public static void encode(ChooseSpecializationMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.companionEntityId);
            buffer.writeUtf(message.specializationId, 32);
        }

        public static ChooseSpecializationMessage decode(FriendlyByteBuf buffer) {
            return new ChooseSpecializationMessage(buffer.readVarInt(), buffer.readUtf(32));
        }

        public static void handle(ChooseSpecializationMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(message.companionEntityId);
                if (!(entity instanceof CompanionEntity companion) || !companion.isOwnedBy(player)
                        || companion.distanceToSqr(player) > 64.0D) return;
                CompanionSpecialization choice = CompanionSpecialization.byId(message.specializationId);
                if (!companion.getProgression().chooseSpecialization(choice, companion.getCompanionClass())) {
                    player.sendSystemMessage(Component.literal("That specialization is unavailable. Reach level 15 and choose a path for your class."));
                    return;
                }
                companion.refreshProgressionAttributes();
                player.sendSystemMessage(Component.literal(companion.getName().getString()
                        + " has chosen the " + choice.getDisplayName() + " path."));
                sendSkillData(player, companion);
            });
            context.setPacketHandled(true);
        }
    }

    public static final class UpgradeAttributeMessage {
        private final int companionEntityId;
        private final String attributeId;
        public UpgradeAttributeMessage(int companionEntityId, String attributeId) {
            this.companionEntityId = companionEntityId; this.attributeId = attributeId;
        }
        public static void encode(UpgradeAttributeMessage message, FriendlyByteBuf buffer) {
            buffer.writeVarInt(message.companionEntityId); buffer.writeUtf(message.attributeId, 32);
        }
        public static UpgradeAttributeMessage decode(FriendlyByteBuf buffer) {
            return new UpgradeAttributeMessage(buffer.readVarInt(), buffer.readUtf(32));
        }
        public static void handle(UpgradeAttributeMessage message, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                Entity entity = player.serverLevel().getEntity(message.companionEntityId);
                if (!(entity instanceof CompanionEntity companion) || !companion.isOwnedBy(player)
                        || companion.distanceToSqr(player) > 64.0D) return;
                CompanionAttribute attribute = CompanionAttribute.byId(message.attributeId);
                if (attribute == null || !companion.getProgression().spendAttributePoint(attribute)) {
                    player.sendSystemMessage(Component.literal("That attribute upgrade is unavailable."));
                    return;
                }
                companion.refreshProgressionAttributes();
                player.sendSystemMessage(Component.literal(attribute.getDisplayName() + " increased to "
                        + companion.getProgression().getAttribute(attribute) + "."));
                sendSkillData(player, companion);
            });
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
                if (findCompanion(player) != null
                        || CompanionResurrectionData.get(player.getServer()).hasPendingResurrection(player.getUUID())) {
                    player.getPersistentData().putBoolean("LivingCompanionCreationComplete", true);
                    player.sendSystemMessage(Component.literal("Your companion is already here or waiting to return."));
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
