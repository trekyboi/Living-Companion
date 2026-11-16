package com.livingcompanion.entity;

import com.livingcompanion.LivingCompanion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge combat hooks for owner-protection abilities. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID)
public final class CompanionCombatEvents {
    private CompanionCombatEvents() { }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || event.getAmount() <= 0.0F) return;
        if (owner.level().isClientSide) return;

        var nearbyCompanions = owner.serverLevel().getEntitiesOfClass(
                CompanionEntity.class, owner.getBoundingBox().inflate(12.0D),
                companion -> companion.isAlive() && companion.isOwnedBy(owner)
                        && companion.getCompanionClass()
                        != com.livingcompanion.progression.CompanionClass.UNASSIGNED);

        for (CompanionEntity companion : nearbyCompanions) {
            float remaining = companion.interceptOwnerDamage(owner, event.getSource(), event.getAmount());
            if (remaining < event.getAmount()) {
                event.setAmount(Math.max(0.0F, remaining));
                break;
            }
        }
    }
}
