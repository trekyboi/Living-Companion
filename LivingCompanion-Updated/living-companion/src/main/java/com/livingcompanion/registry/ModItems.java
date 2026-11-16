package com.livingcompanion.registry;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.item.CompanionSpawnEggItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registration. The spawn egg receives the entity RegistryObject as a
 * supplier. Forge resolves it lazily after the entity type has been registered.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LivingCompanion.MOD_ID);

    /** Adventurer brown & gold. ForgeSpawnEggItem handles deferred entity lookup. */
    public static final RegistryObject<CompanionSpawnEggItem> COMPANION_SPAWN_EGG =
            ITEMS.register("companion_spawn_egg", () -> new CompanionSpawnEggItem(
                    ModEntities.COMPANION,
                    0x6B4423,
                    0xD9A441,
                    new Item.Properties()));

    private ModItems() {
    }
}
