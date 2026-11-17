package com.livingcompanion.client.renderer;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.client.ModModelLayers;
import com.livingcompanion.entity.MageCompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/** Slim-arm Alex-style player renderer with a dedicated mage skin. */
public final class MageCompanionRenderer
        extends HumanoidMobRenderer<MageCompanionEntity, PlayerModel<MageCompanionEntity>> {

    private static final ResourceLocation MAGE_TEXTURE =
            new ResourceLocation(LivingCompanion.MOD_ID, "textures/entity/companion_mage.png");

    public MageCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModModelLayers.MAGE_COMPANION), true), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(MageCompanionEntity entity) {
        return MAGE_TEXTURE;
    }
}
