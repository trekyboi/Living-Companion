package com.livingcompanion.client;

import com.livingcompanion.LivingCompanion;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Vanilla player-model layer used by the companion renderer. */
public final class ModModelLayers {
    public static final ModelLayerLocation COMPANION =
            new ModelLayerLocation(new ResourceLocation(LivingCompanion.MOD_ID, "companion"), "main");

    private ModModelLayers() { }
}
