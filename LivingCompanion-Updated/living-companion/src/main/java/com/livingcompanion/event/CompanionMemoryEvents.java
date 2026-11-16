package com.livingcompanion.event;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;
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
            if (companion != null) {
                companion.remember("you fell in battle");
                companion.addRelationshipExperience(5);
            }
        }
    }

    /** A quiet acknowledgement when the owner returns after a death. */
    @SubscribeEvent
    public static void onOwnerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompanionEntity companion = findCompanion(player);
        if (companion == null) return;
        companion.remember("you came back after falling in battle");
        companion.addRelationshipExperience(20);
        if (companion.getCompanionClass() == com.livingcompanion.progression.CompanionClass.PALADIN) {
            companion.commentOnEvent(
                    "You're back. Take your time; I'll keep watch.",
                    "I'm glad to see you standing again. We continue when you're ready.",
                    "That was a hard fall. You don't have to face the next one alone."
            );
        } else if (companion.getCompanionClass() == com.livingcompanion.progression.CompanionClass.TANK) {
            companion.commentOnEvent(
                    "There you are. I was beginning to run out of things to hit.",
                    "Back on your feet? Good. Stay close this time.",
                    "We'll get your things back. Then we settle the score."
            );
        } else {
            companion.commentOnEvent("There you are. Take a moment; I'll keep watch.");
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
            if (companion != null) {
                companion.remember(memory);
                companion.addRelationshipExperience(30);
                if (event.getEntity() instanceof EnderDragon) {
                    companion.commentOnEvent("Well. That's the biggest problem dealt with.",
                            "I was expecting a longer argument.",
                            "Remind me never to do that twice in one afternoon.");
                } else {
                    companion.commentOnEvent("That thing was a proper nuisance.",
                            "I would like to file a complaint about that fight.",
                            "Good. I was running out of patience.");
                }
            }
        } else if (attacker instanceof CompanionEntity companion) {
            companion.remember(memory);
            companion.addRelationshipExperience(30);
            companion.commentOnEvent("Well. That's the biggest problem dealt with.",
                    "I was expecting a longer argument.",
                    "Remind me never to do that twice in one afternoon.");
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
        if (companion != null) {
            companion.remember(memory);
            companion.addRelationshipExperience(8);
            if (event.getState().is(Blocks.DIAMOND_ORE)
                    || event.getState().is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                companion.commentOnEvent("Diamonds. Excellent. Try not to spend them all at once.",
                        "That's a good find. Suddenly this trip feels worthwhile.",
                        "I'll pretend I spotted those first.");
            } else {
                companion.commentOnEvent("Ancient debris. That should make the trip worthwhile.",
                        "Careful with that. It looks valuable and unpleasant.",
                        "Now that's a reason to keep exploring.");
            }
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String memory = null;
        if (event.getTo() == Level.NETHER) memory = "we entered the Nether together";
        else if (event.getTo() == Level.END) memory = "we entered the End together";
        CompanionEntity companion = findCompanion(player);
        if (companion == null) return;

        if (memory != null) {
            companion.remember(memory);
            companion.addRelationshipExperience(10);
        }

        // FOLLOW and DEFEND companions travel with their owner. STAY companions
        // deliberately remain in the dimension where they were ordered to wait.
        if (companion.getCommand() != CompanionEntity.Command.STAY
                && companion.level() != player.level()
                && player.serverLevel() != null) {
            ServerLevel destination = player.serverLevel();
            ITeleporter teleporter = new ITeleporter() {
                @Override
                public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                                Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                    // Place the companion beside the owner without requiring a portal
                    // in the destination dimension.
                    Vec3 landing = new Vec3(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D);
                    return new PortalInfo(landing, Vec3.ZERO, player.getYRot(), player.getXRot());
                }
            };
            Entity transferred = companion.changeDimension(destination, teleporter);
            if (transferred instanceof CompanionEntity movedCompanion) {
                movedCompanion.getNavigation().stop();
                if (event.getTo() == Level.NETHER) {
                    movedCompanion.commentOnEvent("The Nether. Lovely. Everything here wants to kill us.",
                            "Try not to touch anything. Especially the things that are on fire.",
                            "I preferred the weather back home.");
                } else if (event.getTo() == Level.END) {
                    movedCompanion.commentOnEvent("The End. Subtle name. Very welcoming.",
                            "Right. Let's deal with the dragon and leave.",
                            "I have a bad feeling about this place. Several, actually.");
                }
            }
        }
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
