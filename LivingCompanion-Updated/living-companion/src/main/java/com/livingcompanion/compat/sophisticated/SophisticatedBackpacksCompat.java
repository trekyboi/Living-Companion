package com.livingcompanion.compat.sophisticated;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.NetworkHooks;
import top.theillusivec4.curios.api.CuriosApi;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.util.PlayerInventoryProvider;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Bridges a nearby owned companion's Curios back slot into Sophisticated Backpacks. */
public final class SophisticatedBackpacksCompat {
    public static final String HANDLER_ID = "livingcompanion";
    private static boolean registered;

    private SophisticatedBackpacksCompat() {}

    public static void register() {
        if (registered) return;
        PlayerInventoryProvider.get().addPlayerInventoryHandler(
                HANDLER_ID,
                SophisticatedBackpacksCompat::getNearbyBackpackCompanions,
                (player, identifier) -> getBackpackStack(player, identifier).isEmpty() ? 0 : 1,
                (player, identifier, slot) -> slot == 0 ? getBackpackStack(player, identifier) : ItemStack.EMPTY,
                false, false, false, false);
        registered = true;
    }

    private static Set<String> getNearbyBackpackCompanions(Player player) {
        Set<String> identifiers = new LinkedHashSet<>();
        for (CompanionEntity companion : player.level().getEntitiesOfClass(
                CompanionEntity.class, player.getBoundingBox().inflate(8.0D),
                entity -> entity.isAlive() && entity.isOwnedBy(player))) {
            if (!getBackpackStack(companion).isEmpty()) identifiers.add(companion.getUUID().toString());
        }
        return identifiers;
    }

    private static CompanionEntity findNearbyOwnedCompanion(Player player, String identifier) {
        final UUID companionId;
        try { companionId = UUID.fromString(identifier); }
        catch (IllegalArgumentException exception) { return null; }
        return player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(8.0D),
                entity -> entity.getUUID().equals(companionId) && entity.isAlive() && entity.isOwnedBy(player)
                        && entity.distanceToSqr(player) <= 64.0D)
                .stream().findFirst().orElse(null);
    }

    private static ItemStack getBackpackStack(Player player, String identifier) {
        CompanionEntity companion = findNearbyOwnedCompanion(player, identifier);
        return companion == null ? ItemStack.EMPTY : getBackpackStack(companion);
    }

    public static ItemStack getBackpackStack(CompanionEntity companion) {
        return CuriosApi.getCuriosInventory(companion).resolve()
                .flatMap(handler -> handler.getStacksHandler("back"))
                .filter(handler -> handler.getStacks().getSlots() > 0)
                .map(handler -> handler.getStacks().getStackInSlot(0))
                .filter(stack -> {
                    var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    return !stack.isEmpty() && key != null && key.getNamespace().equals("sophisticatedbackpacks");
                })
                .orElse(ItemStack.EMPTY);
    }

    public static void openBackpack(ServerPlayer player, CompanionEntity companion) {
        if (!companion.isOwnedBy(player) || !companion.isAlive() || companion.distanceToSqr(player) > 64.0D) return;
        ItemStack backpack = getBackpackStack(companion);
        if (backpack.isEmpty()) {
            player.sendSystemMessage(Component.literal("Equip a Sophisticated Backpack in the companion's back slot first."));
            return;
        }

        BackpackContext.Item backpackContext = new BackpackContext.Item(HANDLER_ID, companion.getUUID().toString(), 0);
        NetworkHooks.openScreen(player,
                new SimpleMenuProvider((windowId, inventory, user) -> new BackpackContainer(windowId, user, backpackContext),
                        backpack.getHoverName()),
                buffer -> backpackContext.toBuffer(buffer, player));
    }
}
