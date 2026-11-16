package com.livingcompanion;

import com.livingcompanion.config.CompanionConfig;
import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.registry.ModCreativeTabs;
import com.livingcompanion.registry.ModEntities;
import com.livingcompanion.registry.ModItems;
import com.livingcompanion.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Mod entry point.
 *
 * <p>Uses Forge 47.4.x constructor injection of {@link FMLJavaModLoadingContext}
 * (the static {@code FMLJavaModLoadingContext.get()} / {@code ModLoadingContext.get()}
 * accessors are deprecated for removal on this Forge line).</p>
 *
 * <p>Architecture rule for the whole project: the mod bus carries
 * <em>lifecycle &amp; registration</em> events, while
 * {@link MinecraftForge#EVENT_BUS} carries <em>gameplay</em> events. All
 * companion AI, combat, dialogue and resurrection logic is
 * server-authoritative; client classes live strictly under
 * {@code com.livingcompanion.client} and are never referenced from common code
 * except through client-only extension points.</p>
 */
@Mod(LivingCompanion.MOD_ID)
public final class LivingCompanion {

    public static final String MOD_ID = "livingcompanion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LivingCompanion(FMLJavaModLoadingContext context) {
        final IEventBus modBus = context.getModEventBus();

        // --- Registration (DeferredRegisters fire in vanilla registry order) ---
        CompanionNetwork.register();
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);

        // --- Lifecycle listeners ---
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(ModEntities::onAttributeCreation);

        // Common config: loaded once at startup, server-authoritative values.
        // NOTE: config values must only be READ after config load (i.e. never
        // from registry/attribute suppliers, which run before configs exist).
        context.registerConfig(
                ModConfig.Type.COMMON,
                CompanionConfig.SPEC,
                "livingcompanion-common.toml");

        // Gameplay event handlers are registered per-system in later phases.
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[LivingCompanion] Constructed. Knight companion entity and player-style renderer are registered.");
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("curios")
                    && net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")) {
                com.livingcompanion.compat.sophisticated.SophisticatedBackpacksCompat.register();
            }
            LOGGER.info("[LivingCompanion] Common setup complete. Mod id '{}', targeting MC 1.20.1 / Forge 47.x.", MOD_ID);
        });
    }
}
