package com.livingcompanion.client;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.client.renderer.CompanionPlayerRenderer;
import com.livingcompanion.client.renderer.MageCompanionRenderer;
import com.livingcompanion.registry.ModEntities;
import com.livingcompanion.registry.ModMenus;
import com.livingcompanion.client.screen.CompanionInventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.COMPANION.get(), CompanionPlayerRenderer::new);
        event.registerEntityRenderer(ModEntities.MAGE_COMPANION.get(), MageCompanionRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.COMPANION,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
        event.registerLayerDefinition(ModModelLayers.MAGE_COMPANION,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
                MenuScreens.register(ModMenus.COMPANION_INVENTORY.get(), CompanionInventoryScreen::new));
    }

    private ClientModEvents() { }
}
