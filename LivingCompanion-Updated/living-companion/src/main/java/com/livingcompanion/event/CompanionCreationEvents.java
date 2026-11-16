package com.livingcompanion.event;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.resurrection.CompanionResurrectionData;
import com.livingcompanion.network.CompanionNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opens the one-time companion creation flow when a player first joins a world. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionCreationEvents {
    private static final String CREATION_COMPLETE = "LivingCompanionCreationComplete";

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getServer() == null) return;
        if (player.getPersistentData().getBoolean(CREATION_COMPLETE)) return;

        if (findCompanion(player) != null
                || CompanionResurrectionData.get(player.getServer()).hasPendingResurrection(player.getUUID())) {
            player.getPersistentData().putBoolean(CREATION_COMPLETE, true);
            return;
        }
        CompanionNetwork.openCreation(player);
    }

    private static CompanionEntity findCompanion(ServerPlayer owner) {
        for (ServerLevel level : owner.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion && companion.isOwnedBy(owner)) return companion;
            }
        }
        return null;
    }

    private CompanionCreationEvents() { }
}
