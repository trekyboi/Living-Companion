package com.livingcompanion.menu;

import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.inventory.CompanionInventory;
import net.minecraftforge.fml.ModList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;

public final class CompanionInventoryMenu extends AbstractContainerMenu {
    private final CompanionEntity companion;
    private final CompanionInventory companionInventory;
    private final boolean curiosBackSlot;

    /** Client constructor. The entity id is sent by NetworkHooks.openScreen. */
    public CompanionInventoryMenu(int id, Inventory playerInventory, FriendlyByteBuf data) {
        this(id, playerInventory, (CompanionEntity) playerInventory.player.level().getEntity(data.readVarInt()));
    }

    /** Server constructor. */
    public CompanionInventoryMenu(int id, Inventory playerInventory, CompanionEntity companion) {
        super(com.livingcompanion.registry.ModMenus.COMPANION_INVENTORY.get(), id);
        this.companion = companion;
        this.companionInventory = companion.getCompanionInventory();
        this.curiosBackSlot = ModList.get().isLoaded("curios");

        // Companion armor and hand slots, arranged like the equipment column
        // in the vanilla player inventory.
        this.addSlot(new EquipmentSlotMenuSlot(companionInventory, CompanionInventory.HEAD, 8, 20, EquipmentSlot.HEAD));
        this.addSlot(new EquipmentSlotMenuSlot(companionInventory, CompanionInventory.CHEST, 8, 38, EquipmentSlot.CHEST));
        this.addSlot(new EquipmentSlotMenuSlot(companionInventory, CompanionInventory.LEGS, 8, 56, EquipmentSlot.LEGS));
        this.addSlot(new EquipmentSlotMenuSlot(companionInventory, CompanionInventory.FEET, 8, 74, EquipmentSlot.FEET));
        this.addSlot(new Slot(companionInventory, CompanionInventory.MAIN_HAND, 26, 20));
        this.addSlot(new Slot(companionInventory, CompanionInventory.OFF_HAND, 26, 56));

        // Optional Curios back slot; Curios owns its saved equipment data.
        if (this.curiosBackSlot) {
            Slot backpackSlot = com.livingcompanion.compat.curios.CuriosBackpackSlot.create(companion, 26, 38);
            if (backpackSlot != null) this.addSlot(backpackSlot);
        }

        // Companion's 27 storage slots and 9-slot hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(companionInventory, col + row * 9, 62 + col * 18, 20 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(companionInventory, 27 + col, 62 + col * 18, 78));
        }

        // Player inventory and hotbar, as in a standard container screen.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 142 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 200));
        }
    }

    public boolean hasCuriosBackSlot() { return curiosBackSlot; }

    public CompanionEntity getCompanion() {
        return companion;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || companion == null || !companion.isOwnedBy(player) || !companion.isAlive()) return false;
        companion.setProtectedFromOwner(!companion.isProtectedFromOwner());
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isOwnedBy(player)
                && companion.isAlive() && companion.distanceToSqr(player) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return empty;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();

        // Without Curios: 0-5 equipment, 6-41 storage, 42-77 player.
        // With Curios: slot 6 is the back accessory, 7-42 storage, 43-78 player.
        int storageStart = curiosBackSlot ? 7 : 6;
        int playerStart = curiosBackSlot ? 43 : 42;
        int playerEnd = playerStart + 36;
        if (index < playerStart) {
            if (!this.moveItemStackTo(source, playerStart, playerEnd, true)) return empty;
        } else {
            boolean moved = this.moveItemStackTo(source, storageStart, storageStart + 36, false);
            if (!moved && curiosBackSlot) moved = this.moveItemStackTo(source, 6, 7, false);
            if (!moved) return empty;
        }

        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return empty;
        slot.onTake(player, source);
        return original;
    }

    private static final class EquipmentSlotMenuSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        private EquipmentSlotMenuSlot(CompanionInventory inventory, int index,
                                      int x, int y, EquipmentSlot equipmentSlot) {
            super(inventory, index, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armor
                    && armor.getEquipmentSlot() == equipmentSlot;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
