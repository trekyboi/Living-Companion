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
    private int skillPoints = 1;
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

    /** Spends one point after validating class, level gate, and maximum rank. */
    public boolean unlockSkillRank(CompanionSkill skill, CompanionClass archetype) {
        if (skill == null || archetype == null || skill.getArchetype() != archetype
                || level < skill.getUnlockLevel() || skillPoints < 1) return false;
        int current = getSkillRank(skill.getId());
        if (current >= skill.getMaximumRank()) return false;
        skillRanks.put(skill.getId(), current + 1);
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

    public void load(CompoundTag tag, CompanionClass archetype) {
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level")));
        experience = Math.max(0, tag.getInt("Experience"));
        skillPoints = Math.max(0, tag.getInt("SkillPoints"));
        skillRanks.clear();
        if (tag.contains("Skills", 10)) {
            CompoundTag skills = tag.getCompound("Skills");
            for (String key : skills.getAllKeys()) {
                int rank = skills.getInt(key);
                CompanionSkill skill = CompanionSkill.byId(key);
                if (skill != null && archetype != null && skill.getArchetype() == archetype
                        && rank > 0 && rank <= skill.getMaximumRank()) skillRanks.put(key, rank);
            }
        }
        // Migration: a fresh level-1 companion starts with its first level's skill point.
        if (level == 1 && skillPoints == 0 && skillRanks.isEmpty()) skillPoints = 1;
        if (level >= MAX_LEVEL) experience = 0;
        else experience = Math.min(experience, getExperienceToNextLevel() - 1);
    }
}
