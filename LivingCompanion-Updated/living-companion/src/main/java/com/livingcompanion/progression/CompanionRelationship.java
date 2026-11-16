package com.livingcompanion.progression;

import net.minecraft.nbt.CompoundTag;

/** Persistent familiarity progression between a companion and its owner. */
public final class CompanionRelationship {
    public static final int MAX_LEVEL = 10;

    private int level = 1;
    private int experience;
    private int proximityTicks;

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getExperienceToNextLevel() {
        return level >= MAX_LEVEL ? 0 : 100 + (level - 1) * 75;
    }
    public int getProximityTicks() { return proximityTicks; }

    public String getTitle() {
        if (level >= 10) return "Unbreakable Bond";
        if (level >= 8) return "Devoted Companion";
        if (level >= 6) return "Close Companion";
        if (level >= 4) return "Trusted Friend";
        if (level >= 2) return "Familiar";
        return "New Acquaintance";
    }

    /** Adds bond experience and returns the number of relationship levels gained. */
    public int addExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < MAX_LEVEL && experience >= getExperienceToNextLevel()) {
            experience -= getExperienceToNextLevel();
            level++;
            gained++;
        }
        if (level >= MAX_LEVEL) experience = 0;
        return gained;
    }

    /** Accumulates time spent together; every minute nearby grants one bond XP. */
    public int tickNearOwner() {
        if (level >= MAX_LEVEL) return 0;
        proximityTicks++;
        if (proximityTicks >= 1200) {
            proximityTicks = 0;
            return addExperience(1);
        }
        return 0;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("ProximityTicks", proximityTicks);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level")));
        experience = Math.max(0, tag.getInt("Experience"));
        proximityTicks = Math.max(0, Math.min(1199, tag.getInt("ProximityTicks")));
        if (level >= MAX_LEVEL) experience = 0;
        else experience = Math.min(experience, getExperienceToNextLevel() - 1);
    }
}
