package com.livingcompanion.registry;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.menu.CompanionInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registered container type for the companion's inventory screen. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, LivingCompanion.MOD_ID);

    public static final RegistryObject<MenuType<CompanionInventoryMenu>> COMPANION_INVENTORY =
            MENUS.register("companion_inventory",
                    () -> IForgeMenuType.create(CompanionInventoryMenu::new));

    private ModMenus() { }
}
