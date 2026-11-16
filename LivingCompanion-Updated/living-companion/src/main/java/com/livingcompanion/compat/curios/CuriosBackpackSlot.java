package com.livingcompanion.compat.curios;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Menu slot backed by the companion's Curios back slot. */
public final class CuriosBackpackSlot {
    private CuriosBackpackSlot() {}

    public static Slot create(LivingEntity companion, int x, int y) {
        ICurioStacksHandler backHandler = CuriosApi.getCuriosInventory(companion)
                .resolve()
                .flatMap(handler -> handler.getStacksHandler("back"))
                .orElse(null);
        if (backHandler != null && backHandler.getStacks().getSlots() > 0) {
            return new SlotItemHandler(backHandler.getStacks(), 0, x, y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return CuriosApi.isStackValid(new SlotContext("back", companion, 0, false, true), stack);
                }

                @Override
                public int getMaxStackSize() { return 1; }
            };
        }

        // Keep client/server menu indices identical while Curios capability data
        // is still synchronizing. The client may predict a placement; the server
        // rejects it until the real Curios handler exists.
        return new Slot(new SimpleContainer(1), 0, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return companion.level().isClientSide && !stack.isEmpty();
            }

            @Override
            public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
                return companion.level().isClientSide;
            }

            @Override
            public int getMaxStackSize() { return 1; }
        };
    }
}
