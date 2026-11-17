package com.livingcompanion.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/** Persistent RPG progression for one companion. All mutations are server-authoritative. */
public final class CompanionProgression {
    public static final int MAX_LEVEL = 50;
    public static final int ATTRIBUTE_POINT_CAP = 20;
    private int level = 1;
    private int experience = 0;
    private int skillPoints = 1;
    private int attributePoints = 0;
    private final Map<String, Integer> skillRanks = new HashMap<>();
    private final Map<String, Integer> attributes = new HashMap<>();
    private final Set<CompanionAchievement> achievements = EnumSet.noneOf(CompanionAchievement.class);
    private CompanionSpecialization specialization;

    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getSkillPoints() { return skillPoints; }
    public int getAttributePoints() { return attributePoints; }
    public CompanionSpecialization getSpecialization() { return specialization; }

    /** Specialization is a permanent level-15 choice validated against the base class. */
    public boolean chooseSpecialization(CompanionSpecialization choice, CompanionClass archetype) {
        if (choice == null || archetype == null || specialization != null || level < 15
                || choice.getArchetype() != archetype) return false;
        specialization = choice;
        return true;
    }
    public Set<CompanionAchievement> getAchievements() { return Collections.unmodifiableSet(achievements); }
    public boolean hasAchievement(CompanionAchievement achievement) { return achievements.contains(achievement); }

    public boolean awardAchievement(CompanionAchievement achievement) {
        return achievement != null && achievements.add(achievement);
    }

    /** Highest earned achievement title, with a clear default for new companions. */
    public String getTitle() {
        CompanionAchievement[] priority = {
                CompanionAchievement.UNBREAKABLE, CompanionAchievement.DRAGON_SLAYER,
                CompanionAchievement.WITHER_SLAYER, CompanionAchievement.DEVOTED,
                CompanionAchievement.LEGEND, CompanionAchievement.TRUSTED,
                CompanionAchievement.ELITE, CompanionAchievement.VETERAN,
                CompanionAchievement.END_EXPLORER, CompanionAchievement.NETHER_EXPLORER,
                CompanionAchievement.DIAMOND_SEEKER, CompanionAchievement.FIRST_BLOOD
        };
        for (CompanionAchievement achievement : priority) {
            if (achievements.contains(achievement)) return achievement.getTitle();
        }
        return "Adventurer";
    }
    public int getAttribute(CompanionAttribute attribute) {
        return attribute == null ? 0 : attributes.getOrDefault(attribute.getId(), 0);
    }
    public Map<String, Integer> getAttributes() { return Collections.unmodifiableMap(attributes); }
    public int getExperienceToNextLevel() {
        return level >= MAX_LEVEL ? 0 : 100 + level * 35;
    }
    public Map<String, Integer> getSkillRanks() { return Collections.unmodifiableMap(skillRanks); }
    public int getSkillRank(String skillId) { return skillRanks.getOrDefault(skillId, 0); }

    /** Adds XP and returns the number of levels gained. Each level grants one skill and two attribute points. */
    /** Creative-only testing shortcut. Preserves already-spent progression points. */
    public void setMaxLevelForCreative() {
        level = MAX_LEVEL;
        experience = 0;
        int spentSkillPoints = skillRanks.values().stream().mapToInt(Integer::intValue).sum();
        int spentAttributePoints = attributes.values().stream().mapToInt(Integer::intValue).sum();
        skillPoints = Math.max(0, MAX_LEVEL - spentSkillPoints);
        attributePoints = Math.max(0, (MAX_LEVEL - 1) * 2 - spentAttributePoints);
        awardAchievement(CompanionAchievement.VETERAN);
        awardAchievement(CompanionAchievement.ELITE);
        awardAchievement(CompanionAchievement.LEGEND);
    }

    public int addExperience(int amount) {
        if (amount <= 0 || level >= MAX_LEVEL) return 0;
        experience += amount;
        int gained = 0;
        while (level < MAX_LEVEL && experience >= getExperienceToNextLevel()) {
            experience -= getExperienceToNextLevel();
            level++;
            skillPoints++;
            attributePoints += 2;
            gained++;
        }
        if (level >= MAX_LEVEL) experience = 0;
        return gained;
    }

    public boolean spendAttributePoint(CompanionAttribute attribute) {
        if (attribute == null || attributePoints <= 0
                || getAttribute(attribute) >= ATTRIBUTE_POINT_CAP) return false;
        attributes.put(attribute.getId(), getAttribute(attribute) + 1);
        attributePoints--;
        return true;
    }

    /** Spends one point after validating class, level gate, and maximum rank. */
    public boolean unlockSkillRank(CompanionSkill skill, CompanionClass archetype) {
        if (skill == null || archetype == null || skill.getArchetype() != archetype
                || (skill.getRequiredSpecialization() != null
                && skill.getRequiredSpecialization() != specialization)
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
        tag.putInt("AttributePoints", attributePoints);
        if (specialization != null) tag.putString("Specialization", specialization.getId());
        CompoundTag skills = new CompoundTag();
        skillRanks.forEach(skills::putInt);
        tag.put("Skills", skills);
        CompoundTag savedAttributes = new CompoundTag();
        attributes.forEach(savedAttributes::putInt);
        tag.put("Attributes", savedAttributes);
        ListTag savedAchievements = new ListTag();
        for (CompanionAchievement achievement : achievements) {
            savedAchievements.add(StringTag.valueOf(achievement.getId()));
        }
        tag.put("Achievements", savedAchievements);
        return tag;
    }

    public void load(CompoundTag tag, CompanionClass archetype) {
        level = Math.max(1, Math.min(MAX_LEVEL, tag.getInt("Level")));
        experience = Math.max(0, tag.getInt("Experience"));
        skillPoints = Math.max(0, tag.getInt("SkillPoints"));
        attributePoints = Math.max(0, tag.getInt("AttributePoints"));
        specialization = CompanionSpecialization.byId(tag.getString("Specialization"));
        if (specialization != null && specialization.getArchetype() != archetype) specialization = null;
        skillRanks.clear();
        if (tag.contains("Skills", 10)) {
            CompoundTag skills = tag.getCompound("Skills");
            for (String key : skills.getAllKeys()) {
                int rank = skills.getInt(key);
                CompanionSkill skill = CompanionSkill.byId(key);
                if (skill != null && archetype != null && skill.getArchetype() == archetype
                        && (skill.getRequiredSpecialization() == null
                        || skill.getRequiredSpecialization() == specialization)
                        && rank > 0 && rank <= skill.getMaximumRank()) skillRanks.put(key, rank);
            }
        }
        achievements.clear();
        if (tag.contains("Achievements", Tag.TAG_LIST)) {
            ListTag savedAchievements = tag.getList("Achievements", Tag.TAG_STRING);
            for (int i = 0; i < savedAchievements.size(); i++) {
                CompanionAchievement achievement = CompanionAchievement.byId(savedAchievements.getString(i));
                if (achievement != null) achievements.add(achievement);
            }
        }
        attributes.clear();
        if (tag.contains("Attributes", 10)) {
            CompoundTag savedAttributes = tag.getCompound("Attributes");
            for (CompanionAttribute attribute : CompanionAttribute.values()) {
                int value = Math.max(0, Math.min(ATTRIBUTE_POINT_CAP,
                        savedAttributes.getInt(attribute.getId())));
                if (value > 0) attributes.put(attribute.getId(), value);
            }
        }
        // Migration for companions created before attribute points existed.
        if (!tag.contains("AttributePoints")) {
            attributePoints = Math.max(0, (level - 1) * 2);
        }
        if (level == 1 && skillPoints == 0 && skillRanks.isEmpty()) skillPoints = 1;
        if (level >= 10) awardAchievement(CompanionAchievement.VETERAN);
        if (level >= 25) awardAchievement(CompanionAchievement.ELITE);
        if (level >= 50) awardAchievement(CompanionAchievement.LEGEND);
        if (level >= MAX_LEVEL) experience = 0;
        else experience = Math.min(experience, getExperienceToNextLevel() - 1);
    }
}
