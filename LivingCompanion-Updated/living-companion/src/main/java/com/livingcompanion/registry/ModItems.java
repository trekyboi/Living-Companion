package com.livingcompanion.registry;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.item.CompanionSpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration. Entity types register before items (vanilla registry
 * order), so {@code ModEntities.COMPANION.get()} is safe inside item suppliers.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LivingCompanion.MOD_ID);

    /** Adventurer brown & gold. Vanilla auto-registers spawn-egg tint colors. */
    public static final RegistryObject<CompanionSpawnEggItem> COMPANION_SPAWN_EGG =
            ITEMS.register("companion_spawn_egg", () -> new CompanionSpawnEggItem(
                    ModEntities.COMPANION.get(),
                    0x6B4423,   // base: leather brown
                    0xD9A441,   // spots: adventurer gold
                    new Item.Properties()));

    private ModItems() {
    }
}
