package com.livingcompanion.progression;

import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Persistent RPG progression for one companion. All mutations are server-authoritative. */
public final class CompanionProgression {
    public static final int MAX_LEVEL = 50;
    private int level = 1;
    private int experience = 0;
    private int skillPoints = 0;
    private final Map<String, Integer> skillRanks = new HashMap<>();

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getSkillPoints() { return skillPoints; }
    public int getExperienceToNextLevel() {
        return level >= MAX_LEVEL ? 0 : 100 + level * 35;
    }
    public Map<String, Integer> getSkillRanks() {
        return Collections.unmodifiableMap(skillRanks);
    }
    public int getSkillRank(String skillId) {
        return skillRanks.getOrDefault(skillId, 0);
    }

    /** Adds XP and returns the number of levels gained. XP carries across level thresholds. */
    public int addExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < MAX_LEVEL && experience >= getExperienceToNextLevel()) {
            experience -= getExperienceToNextLevel();
            level++;
            skillPoints++;
            gained++;
        }
        if (level >= MAX_LEVEL) experience = 0;
        return gained;
    }

    /** Spends one point on a skill. Skill IDs and maximum ranks are defined by the skill registry/UI. */
    public boolean unlockSkillRank(String skillId, int maximumRank) {
        if (skillId == null || skillId.isBlank() || maximumRank < 1 || skillPoints < 1) return false;
        int current = getSkillRank(skillId);
        if (current >= maximumRank) return false;
        skillRanks.put(skillId, current + 1);
        skillPoints--;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putInt("Experience", experience);
        tag.putInt("SkillPoints", skillPoints);
        CompoundTag skills = new CompoundTag();
        skillRanks.forEach(skills::putInt);
        tag.put("Skills", skills);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level")));
        experience = Math.max(0, tag.getInt("Experience"));
        skillPoints = Math.max(0, tag.getInt("SkillPoints"));
        skillRanks.clear();
        if (tag.contains("Skills", 10)) {
            CompoundTag skills = tag.getCompound("Skills");
            for (String key : skills.getAllKeys()) {
                int rank = skills.getInt(key);
                if (rank > 0 && rank <= 10) skillRanks.put(key, rank);
            }
        }
        if (level >= MAX_LEVEL) experience = 0;
        else experience = Math.min(experience, getExperienceToNextLevel() - 1);
    }
}
