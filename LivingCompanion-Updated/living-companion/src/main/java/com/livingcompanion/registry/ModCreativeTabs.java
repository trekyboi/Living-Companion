package com.livingcompanion.registry;

import com.livingcompanion.LivingCompanion;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Dedicated creative tab. Future phases add the recall whistle, companion
 * contract, etc. here.
 */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LivingCompanion.MOD_ID);

    public static final RegistryObject<CreativeModeTab> COMPANION_TAB =
            TABS.register("companion", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.livingcompanion"))
                    .icon(() -> new ItemStack(ModItems.COMPANION_SPAWN_EGG.get()))
                    .displayItems((parameters, output) -> output.accept(ModItems.COMPANION_SPAWN_EGG.get()))
                    .build());

    private ModCreativeTabs() {
    }
}
