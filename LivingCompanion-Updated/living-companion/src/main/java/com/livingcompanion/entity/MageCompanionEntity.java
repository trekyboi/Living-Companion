package com.livingcompanion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Female mage companion. Extends the established companion systems so she
 * receives the same ownership, inventory, progression, relationship, and AI
 * behavior while using a dedicated entity type and client renderer.
 */
public final class MageCompanionEntity extends CompanionEntity {
    public MageCompanionEntity(EntityType<? extends MageCompanionEntity> type, Level level) {
        super(type, level);
        this.setCustomNameVisible(true);
    }
}
