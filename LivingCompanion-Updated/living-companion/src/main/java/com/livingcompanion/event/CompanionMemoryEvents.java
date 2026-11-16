package com.livingcompanion.event;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Records a small set of meaningful world events on the owner's companion. */
@Mod.EventBusSubscriber(modid = LivingCompanion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompanionMemoryEvents {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompanionEntity companion = findCompanion(player);
            if (companion != null) companion.remember("you fell in battle");
        }
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        String memory = null;
        if (event.getEntity() instanceof EnderDragon) memory = "we defeated the Ender Dragon";
        if (event.getEntity() instanceof WitherBoss) memory = "we defeated the Wither";
        if (memory == null) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer player) {
            CompanionEntity companion = findCompanion(player);
            if (companion != null) companion.remember(memory);
        } else if (attacker instanceof CompanionEntity companion) {
            companion.remember(memory);
        }
    }

    @SubscribeEvent
    public static void onOreBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        String memory = null;
        if (event.getState().is(Blocks.DIAMOND_ORE) || event.getState().is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            memory = "we found diamonds deep underground";
        } else if (event.getState().is(Blocks.ANCIENT_DEBRIS)) {
            memory = "we uncovered ancient debris in the Nether";
        }
        if (memory == null) return;
        CompanionEntity companion = findCompanion(player);
        if (companion != null) companion.remember(memory);
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String memory = null;
        if (event.getTo() == Level.NETHER) memory = "we entered the Nether together";
        else if (event.getTo() == Level.END) memory = "we entered the End together";
        if (memory == null) return;
        CompanionEntity companion = findCompanion(player);
        if (companion != null) companion.remember(memory);
    }

    private static CompanionEntity findCompanion(ServerPlayer owner) {
        if (owner.getServer() == null) return null;
        for (ServerLevel level : owner.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof CompanionEntity companion && companion.isOwnedBy(owner)) {
                    return companion;
                }
            }
        }
        return null;
    }

    private CompanionMemoryEvents() { }
}
