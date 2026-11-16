package com.livingcompanion.entity;

import com.livingcompanion.LivingCompanion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge combat hooks for owner-protection abilities. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID)
public final class CompanionCombatEvents {
    private CompanionCombatEvents() { }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof CompanionEntity attacker) {
            attacker.recordCombatExperience();
        }
        if (event.getEntity() instanceof CompanionEntity companion) {
            companion.commentOnEvent(
                    "That one got through. I'll be more careful.",
                    "Armor's holding. Keep your attention on the fight.",
                    "Still standing. Let's finish this."
            );
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer owner)) return;
        if (owner.level().isClientSide) return;

        var nearbyCompanions = owner.serverLevel().getEntitiesOfClass(
                CompanionEntity.class, owner.getBoundingBox().inflate(12.0D),
                companion -> companion.isAlive() && companion.isOwnedBy(owner)
                        && companion.getCompanionClass()
                        != com.livingcompanion.progression.CompanionClass.UNASSIGNED);

        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity livingAttacker
                ? livingAttacker : null;
        for (CompanionEntity companion : nearbyCompanions) {
            companion.assistOwnerAgainst(attacker);
            if (attacker instanceof net.minecraft.world.entity.monster.Monster || attacker instanceof EnderDragon) {
                companion.recordProtectionEvent();
            }
            if (attacker instanceof EnderDragon) {
                companion.commentOnEvent("Dragon incoming. Watch the sky and keep moving.",
                        "The dragon has turned its attention to us. Stay sharp.",
                        "That thing hits hard. I'll draw its attention where I can.");
            } else if (attacker instanceof WitherBoss) {
                companion.commentOnEvent("The Wither is targeting us. Keep clear of its skulls.",
                        "Wither fire incoming. Move before it finds its mark.",
                        "I'll keep it occupied. Watch for the next blast.");
            }
            float remaining = companion.interceptOwnerDamage(owner, event.getSource(), event.getAmount());
            companion.commentOnEvent(
                    "You're hurt. Fall back a step and let me handle this.",
                    "Stay with me. I'll keep them off you.",
                    "You're taking too many hits. Get behind me."
            );
            if (remaining < event.getAmount()) {
                event.setAmount(Math.max(0.0F, remaining));
                break;
            }
        }
    }
}
