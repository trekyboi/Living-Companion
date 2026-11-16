package com.livingcompanion.event;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/**
 * Awards companion XP for meaningful combat participation.
 * Companion kills grant full XP; the owner's nearby kills grant shared XP,
 * keeping progression active when the player is doing most of the fighting.
 */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionProgressionEvents {

    private static final double XP_SHARE_RANGE = 32.0D;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity defeated = event.getEntity();
        if (defeated.getMaxHealth() <= 0.0F) return;

        int baseExperience = calculateExperience(defeated);
        Entity attacker = event.getSource().getEntity();

        if (attacker instanceof CompanionEntity companion) {
            if (companion.isAlive()) companion.awardExperience(scaleExperience(baseExperience, companion, defeated));
            return;
        }

        if (!(attacker instanceof ServerPlayer player)) return;

        CompanionEntity companion = player.level().getEntitiesOfClass(
                        CompanionEntity.class,
                        player.getBoundingBox().inflate(XP_SHARE_RANGE),
                        candidate -> candidate.isAlive() && candidate.isOwnedBy(player))
                .stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);

        if (companion != null) {
            // The companion receives a meaningful share while the owner remains
            // the primary combatant. Boss rewards remain substantial.
            int sharedExperience = Math.max(1, Math.round(scaleExperience(baseExperience, companion, defeated) * 0.75F));
            companion.awardExperience(sharedExperience);
        }
    }

    /** Weak enemies lose value as the companion outlevels them; dangerous enemies retain better rewards. */
    private static int scaleExperience(int baseExperience, CompanionEntity companion, LivingEntity defeated) {
        int companionLevel = companion.getProgression().getLevel();
        int threatLevel = Math.max(1, (int) Math.ceil(defeated.getMaxHealth() / 4.0D));
        int levelGap = Math.max(0, companionLevel - threatLevel);
        double multiplier = Math.max(0.25D, 1.0D - levelGap * 0.025D);
        return Math.max(1, (int) Math.round(baseExperience * multiplier));
    }

    private static int calculateExperience(LivingEntity defeated) {
        if (defeated instanceof EnderDragon || defeated instanceof WitherBoss) {
            return 250;
        }
        return Math.max(5, Math.min(40,
                5 + Math.round(defeated.getMaxHealth() / 2.0F)));
    }

    private CompanionProgressionEvents() { }
}
