package com.livingcompanion.progression;

import java.util.List;

/** Authoritative definitions for the five skills available to each companion archetype. */
public enum CompanionSkill {
    PALADIN_DIVINE_GRACE("paladin_divine_grace", CompanionClass.PALADIN, SkillType.PASSIVE, "Divine Grace",
            "Improves the Paladin's natural recovery and resilience.", 5, 1),
    PALADIN_HOLY_STRIKE("paladin_holy_strike", CompanionClass.PALADIN, SkillType.ACTIVE, "Holy Strike",
            "Empowers the next melee strike with bonus holy damage.", 5, 3),
    PALADIN_DIVINE_PROTECTION("paladin_divine_protection", CompanionClass.PALADIN, SkillType.ACTIVE, "Divine Protection",
            "Raises a protective ward around the Paladin and nearby owner.", 5, 8),
    PALADIN_PRAYER("paladin_prayer", CompanionClass.PALADIN, SkillType.ACTIVE, "Prayer",
            "Restores health to the Paladin and nearby owner.", 5, 15),
    PALADIN_AEGIS_OF_DAWN("paladin_aegis_of_dawn", CompanionClass.PALADIN, SkillType.ULTIMATE, "Aegis of Dawn",
            "Creates a powerful aura that greatly reduces incoming damage.", 3, 25),

    TANK_UNYIELDING("tank_unyielding", CompanionClass.TANK, SkillType.PASSIVE, "Unyielding",
            "Increases defensive strength as the Tank's health falls.", 5, 1),
    TANK_TAUNT("tank_taunt", CompanionClass.TANK, SkillType.ACTIVE, "Taunt",
            "Draws nearby hostile mobs toward the Tank.", 5, 3),
    TANK_SHIELD_WALL("tank_shield_wall", CompanionClass.TANK, SkillType.ACTIVE, "Shield Wall",
            "Greatly reduces incoming damage for a short duration.", 5, 8),
    TANK_INTERCEPT("tank_intercept", CompanionClass.TANK, SkillType.ACTIVE, "Intercept",
            "Moves to protect the owner and absorbs part of an incoming attack.", 5, 15),
    TANK_LAST_BASTION("tank_last_bastion", CompanionClass.TANK, SkillType.ULTIMATE, "Last Bastion",
            "Enters a fortified stance and draws nearby enemies' aggression.", 3, 25);

    public enum SkillType { PASSIVE, ACTIVE, ULTIMATE }

    private final String id;
    private final CompanionClass archetype;
    private final SkillType type;
    private final String displayName;
    private final String description;
    private final int maximumRank;
    private final int unlockLevel;

    CompanionSkill(String id, CompanionClass archetype, SkillType type, String displayName,
                   String description, int maximumRank, int unlockLevel) {
        this.id = id;
        this.archetype = archetype;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.maximumRank = maximumRank;
        this.unlockLevel = unlockLevel;
    }

    public String getId() { return id; }
    public CompanionClass getArchetype() { return archetype; }
    public SkillType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMaximumRank() { return maximumRank; }
    public int getUnlockLevel() { return unlockLevel; }

    public static CompanionSkill byId(String id) {
        if (id == null) return null;
        for (CompanionSkill skill : values()) {
            if (skill.id.equals(id)) return skill;
        }
        return null;
    }

    public static List<CompanionSkill> forClass(CompanionClass archetype) {
        return java.util.Arrays.stream(values())
                .filter(skill -> skill.archetype == archetype)
                .toList();
    }
}
