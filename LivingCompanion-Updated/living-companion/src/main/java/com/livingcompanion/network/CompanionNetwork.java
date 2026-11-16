package com.livingcompanion.network;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.progression.CompanionClass;
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
    }

    public static void openCreation(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenCreationMessage());
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
