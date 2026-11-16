package com.livingcompanion.progression;

import net.minecraft.nbt.CompoundTag;

/**
 * Persistent behavioral development. Traits are derived from recorded actions,
 * keeping the system deterministic and inexpensive to tick.
 */
public final class CompanionPersonality {
    private int combatExperience;
    private int protectionEvents;
    private int discoveries;
    private int peacefulInteractions;

    public int getCombatExperience() { return combatExperience; }
    public int getProtectionEvents() { return protectionEvents; }
    public int getDiscoveries() { return discoveries; }
    public int getPeacefulInteractions() { return peacefulInteractions; }

    public void recordCombat() { combatExperience = Math.min(10000, combatExperience + 1); }
    public void recordProtection() { protectionEvents = Math.min(10000, protectionEvents + 1); }
    public void recordDiscovery() { discoveries = Math.min(10000, discoveries + 1); }
    public void recordPeacefulInteraction() { peacefulInteractions = Math.min(10000, peacefulInteractions + 1); }

    /** 0.0–1.0 confidence bonus earned through repeated combat experience. */
    public double getCombatConfidence() {
        return Math.min(1.0D, combatExperience / 250.0D);
    }

    /** Protective behavior grows through moments when the owner needed help. */
    public double getProtectiveInstinct() {
        return Math.min(1.0D, protectionEvents / 40.0D);
    }

    /** Curiosity grows through exploration and meaningful discoveries. */
    public double getCuriosity() {
        return Math.min(1.0D, discoveries / 60.0D);
    }

    public String getCombatDisposition() {
        if (combatExperience >= 150) return "Seasoned";
        if (combatExperience >= 50) return "Confident";
        if (combatExperience >= 10) return "Steady";
        return "Unproven";
    }

    public String getTemperament() {
        if (protectionEvents >= 20) return "Protective";
        if (discoveries >= 30) return "Curious";
        if (peacefulInteractions >= 20) return "Gentle";
        return "Reserved";
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CombatExperience", combatExperience);
        tag.putInt("ProtectionEvents", protectionEvents);
        tag.putInt("Discoveries", discoveries);
        tag.putInt("PeacefulInteractions", peacefulInteractions);
        return tag;
    }

    public void load(CompoundTag tag) {
        combatExperience = Math.max(0, Math.min(10000, tag.getInt("CombatExperience")));
        protectionEvents = Math.max(0, Math.min(10000, tag.getInt("ProtectionEvents")));
        discoveries = Math.max(0, Math.min(10000, tag.getInt("Discoveries")));
        peacefulInteractions = Math.max(0, Math.min(10000, tag.getInt("PeacefulInteractions")));
    }
}
