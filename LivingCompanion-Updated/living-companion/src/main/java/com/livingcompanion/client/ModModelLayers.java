package com.livingcompanion.client;

import com.livingcompanion.LivingCompanion;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** Vanilla player-model layers for the standard knight and slim-arm mage. */
public final class ModModelLayers {
    public static final ModelLayerLocation COMPANION =
            new ModelLayerLocation(new ResourceLocation(LivingCompanion.MOD_ID, "companion"), "main");
    public static final ModelLayerLocation MAGE_COMPANION =
            new ModelLayerLocation(new ResourceLocation(LivingCompanion.MOD_ID, "mage_companion"), "main");

    private ModModelLayers() { }
}
