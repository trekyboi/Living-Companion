package com.livingcompanion.client.renderer;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.client.ModModelLayers;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla player-shaped renderer with a knight skin, held items, and armor layers. */
public final class CompanionPlayerRenderer
        extends HumanoidMobRenderer<CompanionEntity, CompanionKnightModel> {

    private static final ResourceLocation KNIGHT_TEXTURE =
            new ResourceLocation(LivingCompanion.MOD_ID, "textures/entity/companion_knight.png");

    public CompanionPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new CompanionKnightModel(context.bakeLayer(ModModelLayers.COMPANION)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return KNIGHT_TEXTURE;
    }
}
