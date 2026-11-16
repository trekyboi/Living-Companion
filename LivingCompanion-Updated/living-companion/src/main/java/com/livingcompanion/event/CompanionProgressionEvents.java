package com.livingcompanion.event;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Awards RPG experience when the companion personally defeats a hostile creature. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionProgressionEvents {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof CompanionEntity companion)) return;
        LivingEntity defeated = event.getEntity();
        if (defeated == companion || defeated.getMaxHealth() <= 0.0F) return;

        int experience;
        if (defeated instanceof EnderDragon || defeated instanceof WitherBoss) {
            experience = 250;
        } else {
            experience = Math.max(5, Math.min(40, 5 + Math.round(defeated.getMaxHealth() / 2.0F)));
        }
        companion.awardExperience(experience);
    }

    private CompanionProgressionEvents() { }
}
