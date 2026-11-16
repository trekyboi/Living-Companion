package com.livingcompanion.resurrection;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Hooks companion death and advances the persistent resurrection queue. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionResurrectionEvents {
    @SubscribeEvent
    public static void onCompanionDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CompanionEntity companion)) return;
        if (companion.level().isClientSide || companion.getServer() == null) return;
        CompanionResurrectionData.get(companion.getServer()).enqueue(companion, companion.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        CompanionResurrectionData.get(server).tick(server);
    }

    private CompanionResurrectionEvents() { }
}
