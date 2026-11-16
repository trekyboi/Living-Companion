package com.livingcompanion.inventory;

import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.ContainerHelper;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * Player-style companion inventory: 36 carried slots plus the six live
 * equipment slots used by the companion's combat/rendering systems.
 */
public final class CompanionInventory implements Container {
    public static final int STORAGE_SIZE = 36;
    public static final int MAIN_HAND = 36;
    public static final int OFF_HAND = 37;
    public static final int HEAD = 38;
    public static final int CHEST = 39;
    public static final int LEGS = 40;
    public static final int FEET = 41;
    public static final int SIZE = 42;

    private static final EquipmentSlot[] EQUIPMENT = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final CompanionEntity companion;
    private final NonNullList<ItemStack> storage =
            NonNullList.withSize(STORAGE_SIZE, ItemStack.EMPTY);

    public CompanionInventory(CompanionEntity companion) {
        this.companion = companion;
    }

    @Override public int getContainerSize() { return SIZE; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : storage) if (!stack.isEmpty()) return false;
        for (EquipmentSlot slot : EQUIPMENT) {
            if (!companion.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= SIZE) return ItemStack.EMPTY;
        return index < STORAGE_SIZE ? storage.get(index)
                : companion.getItemBySlot(EQUIPMENT[index - STORAGE_SIZE]);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index < 0 || index >= SIZE || count <= 0) return ItemStack.EMPTY;
        if (index < STORAGE_SIZE) {
            ItemStack result = ContainerHelper.removeItem(storage, index, count);
            if (!result.isEmpty()) setChanged();
            return result;
        }
        ItemStack current = getItem(index);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = current.split(count);
        if (current.isEmpty()) setItem(index, ItemStack.EMPTY);
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= SIZE) return ItemStack.EMPTY;
        if (index < STORAGE_SIZE) return ContainerHelper.takeItem(storage, index);
        ItemStack current = getItem(index);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = current.copy();
        setItem(index, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE) return;
        if (index < STORAGE_SIZE) {
            storage.set(index, stack);
            if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        } else {
            companion.setItemSlot(EQUIPMENT[index - STORAGE_SIZE], stack);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        companion.inventoryChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return companion.isAlive() && companion.distanceToSqr(player) <= 64.0D;
    }

    @Override
    public void clearContent() {
        storage.clear();
        for (EquipmentSlot slot : EQUIPMENT) companion.setItemSlot(slot, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index < 0 || index >= SIZE || stack.isEmpty()) return false;
        if (index < STORAGE_SIZE) return true;
        EquipmentSlot slot = EQUIPMENT[index - STORAGE_SIZE];
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) return true;
        return stack.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == slot;
    }

    public void save(CompoundTag tag) {
        CompoundTag items = new CompoundTag();
        ContainerHelper.saveAllItems(items, storage);
        tag.put("CompanionStorage", items);
    }

    public void load(CompoundTag tag) {
        ContainerHelper.loadAllItems(tag.getCompound("CompanionStorage"), storage);
    }

    public List<ItemStack> getStorageItems() {
        return List.copyOf(storage);
    }
}
