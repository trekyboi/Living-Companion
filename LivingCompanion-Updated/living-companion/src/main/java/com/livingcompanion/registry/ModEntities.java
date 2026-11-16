package com.livingcompanion.registry;

import com.livingcompanion.LivingCompanion;
import com.livingcompanion.entity.CompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entity registration.
 *
 * <p>No {@code SpawnPlacements} are registered on purpose: the companion never
 * spawns naturally. He enters the world only via the spawn egg or
 * {@code /summon livingcompanion:companion}.</p>
 *
 * <p>Attributes are registered here with SAFE DEFAULTS (registry time runs
 * before configs load). Live values from {@code CompanionConfig} are applied
 * per-instance on the first server tick — see {@link CompanionEntity}.</p>
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LivingCompanion.MOD_ID);

    public static final RegistryObject<EntityType<CompanionEntity>> COMPANION =
            ENTITIES.register("companion", () -> EntityType.Builder
                    .of(CompanionEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)          // humanoid hitbox, slightly taller than a player
                    .clientTrackingRange(10)     // chunks; generous so he renders at follow distances
                    .updateInterval(3)           // ticks between position sync packets
                    .build("companion"));

    /** Mod-bus listener, wired in {@code LivingCompanion}'s constructor. */
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(COMPANION.get(), CompanionEntity.createAttributes().build());
    }

    private ModEntities() {
    }
}
