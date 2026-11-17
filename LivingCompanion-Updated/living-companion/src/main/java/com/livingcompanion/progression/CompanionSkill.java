package com.livingcompanion.progression;

import java.util.Arrays;
import java.util.List;

/** Authoritative definitions for base-class and specialization skills. */
public enum CompanionSkill {
    PALADIN_DIVINE_GRACE("paladin_divine_grace", CompanionClass.PALADIN, SkillType.PASSIVE, "Divine Grace",
            "Improves the Paladin's natural recovery and resilience.", 5, 1, 0),
    PALADIN_HOLY_STRIKE("paladin_holy_strike", CompanionClass.PALADIN, SkillType.ACTIVE, "Holy Strike",
            "Empowers the next melee strike with bonus holy damage.", 3, 1, 100),
    PALADIN_DIVINE_PROTECTION("paladin_divine_protection", CompanionClass.PALADIN, SkillType.ACTIVE, "Divine Protection",
            "Raises a protective ward around the Paladin and nearby owner.", 3, 5, 240),
    PALADIN_PRAYER("paladin_prayer", CompanionClass.PALADIN, SkillType.ACTIVE, "Prayer",
            "Restores health to the Paladin and nearby owner.", 3, 10, 300),
    PALADIN_AEGIS_OF_DAWN("paladin_aegis_of_dawn", CompanionClass.PALADIN, SkillType.ULTIMATE, "Aegis of Dawn",
            "Creates a powerful aura that greatly reduces incoming damage.", 1, 25, 1200),

    TANK_UNYIELDING("tank_unyielding", CompanionClass.TANK, SkillType.PASSIVE, "Unyielding",
            "Increases defensive strength as the Tank's health falls.", 5, 1, 0),
    TANK_TAUNT("tank_taunt", CompanionClass.TANK, SkillType.ACTIVE, "Taunt",
            "Draws nearby hostile mobs toward the Tank.", 3, 1, 160),
    TANK_SHIELD_WALL("tank_shield_wall", CompanionClass.TANK, SkillType.ACTIVE, "Shield Wall",
            "Greatly reduces incoming damage for a short duration.", 3, 5, 300),
    TANK_INTERCEPT("tank_intercept", CompanionClass.TANK, SkillType.ACTIVE, "Intercept",
            "Intercepts nearby attacks, redirecting part of the owner's damage to the Tank.", 3, 10, 360),
    TANK_LAST_BASTION("tank_last_bastion", CompanionClass.TANK, SkillType.ULTIMATE, "Last Bastion",
            "Enters a fortified stance and draws nearby enemies' aggression.", 1, 25, 1200),

    PALADIN_SANCTUARY("paladin_sanctuary", CompanionClass.PALADIN, SkillType.PASSIVE, "Sanctuary",
            "Warden branch: strengthens Prayer healing and protective wards.", 3, 30, 0, CompanionSpecialization.PALADIN_WARDEN),
    PALADIN_JUDGMENT("paladin_judgment", CompanionClass.PALADIN, SkillType.PASSIVE, "Judgment",
            "Crusader branch: empowers Holy Strike with additional radiant damage.", 3, 30, 0, CompanionSpecialization.PALADIN_CRUSADER),
    TANK_BULWARK_OATH("tank_bulwark_oath", CompanionClass.TANK, SkillType.PASSIVE, "Bulwark's Oath",
            "Bulwark branch: reduces damage suffered by the owner while nearby.", 3, 30, 0, CompanionSpecialization.TANK_BULWARK),
    TANK_VANGUARD_FURY("tank_vanguard_fury", CompanionClass.TANK, SkillType.PASSIVE, "Vanguard's Fury",
            "Vanguard branch: increases the Tank's melee damage.", 3, 30, 0, CompanionSpecialization.TANK_VANGUARD),
    PALADIN_TEMPLAR_VIGIL("paladin_templar_vigil", CompanionClass.PALADIN, SkillType.PASSIVE, "Templar's Vigil",
            "Templar branch: reduces damage suffered by the owner while the Paladin is nearby.", 3, 30, 0,
            CompanionSpecialization.PALADIN_TEMPLAR),
    TANK_JUGGERNAUT_MOMENTUM("tank_juggernaut_momentum", CompanionClass.TANK, SkillType.PASSIVE, "Juggernaut Momentum",
            "Juggernaut branch: adds crushing force to successful melee hits.", 3, 30, 0,
            CompanionSpecialization.TANK_JUGGERNAUT),

    PALADIN_WARDEN_SANCTUARY_PULSE("paladin_warden_sanctuary_pulse", CompanionClass.PALADIN, SkillType.ACTIVE, "Sanctuary Pulse",
            "Warden active: restores nearby allies and reinforces the Paladin's ward.", 3, 15, 240,
            CompanionSpecialization.PALADIN_WARDEN),
    PALADIN_WARDEN_CIRCLE_OF_RENEWAL("paladin_warden_circle_of_renewal", CompanionClass.PALADIN, SkillType.ULTIMATE, "Circle of Renewal",
            "Warden ultimate: delivers a major group heal and a powerful protective ward.", 1, 30, 2400,
            CompanionSpecialization.PALADIN_WARDEN),
    PALADIN_CRUSADER_RADIANT_ASSAULT("paladin_crusader_radiant_assault", CompanionClass.PALADIN, SkillType.ACTIVE, "Radiant Assault",
            "Crusader active: strikes the current foe with concentrated holy force.", 3, 30, 180,
            CompanionSpecialization.PALADIN_CRUSADER),
    PALADIN_CRUSADER_JUDGMENT_OF_DAWN("paladin_crusader_judgment_of_dawn", CompanionClass.PALADIN, SkillType.ULTIMATE, "Judgment of Dawn",
            "Crusader ultimate: unleashes a radiant burst against nearby hostiles.", 1, 30, 2400,
            CompanionSpecialization.PALADIN_CRUSADER),
    PALADIN_TEMPLAR_AEGIS_LINK("paladin_templar_aegis_link", CompanionClass.PALADIN, SkillType.ACTIVE, "Aegis Link",
            "Templar active: shields the owner through a sustained protective bond.", 3, 30, 220,
            CompanionSpecialization.PALADIN_TEMPLAR),
    PALADIN_TEMPLAR_HOLY_STANDARD("paladin_templar_holy_standard", CompanionClass.PALADIN, SkillType.ULTIMATE, "Holy Standard",
            "Templar ultimate: grants a powerful defensive blessing and restores the owner.", 1, 30, 2400,
            CompanionSpecialization.PALADIN_TEMPLAR),
    TANK_BULWARK_FORTRESS_STANCE("tank_bulwark_fortress_stance", CompanionClass.TANK, SkillType.ACTIVE, "Fortress Stance",
            "Bulwark active: enters a reinforced stance and draws nearby enemies.", 3, 30, 220,
            CompanionSpecialization.TANK_BULWARK),
    TANK_BULWARK_UNBREAKABLE_LINE("tank_bulwark_unbreakable_line", CompanionClass.TANK, SkillType.ULTIMATE, "Unbreakable Line",
            "Bulwark ultimate: raises an immense defensive barrier and forces enemies to engage.", 1, 30, 2400,
            CompanionSpecialization.TANK_BULWARK),
    TANK_VANGUARD_WARCRY("tank_vanguard_warcry", CompanionClass.TANK, SkillType.ACTIVE, "Warcry",
            "Vanguard active: damages nearby foes and forces their attention onto the Tank.", 3, 30, 180,
            CompanionSpecialization.TANK_VANGUARD),
    TANK_VANGUARD_WARPATH("tank_vanguard_warpath", CompanionClass.TANK, SkillType.ULTIMATE, "Warpath",
            "Vanguard ultimate: unleashes a devastating shockwave and extended taunt.", 1, 30, 2400,
            CompanionSpecialization.TANK_VANGUARD),
    TANK_JUGGERNAUT_CRUSHING_BLOW("tank_juggernaut_crushing_blow", CompanionClass.TANK, SkillType.ACTIVE, "Crushing Blow",
            "Juggernaut active: delivers a heavy empowered strike and restores a measure of health.", 3, 30, 180,
            CompanionSpecialization.TANK_JUGGERNAUT),
    TANK_JUGGERNAUT_UNSTOPPABLE_RAMPAGE("tank_juggernaut_unstoppable_rampage", CompanionClass.TANK, SkillType.ULTIMATE, "Unstoppable Rampage",
            "Juggernaut ultimate: tears through nearby enemies and converts the assault into healing.", 1, 30, 2400,
            CompanionSpecialization.TANK_JUGGERNAUT);

    public enum SkillType { PASSIVE, ACTIVE, ULTIMATE }

    private final String id;
    private final CompanionClass archetype;
    private final SkillType type;
    private final String displayName;
    private final String description;
    private final int maximumRank;
    private final int unlockLevel;
    private final int cooldownTicks;
    private final CompanionSpecialization requiredSpecialization;

    CompanionSkill(String id, CompanionClass archetype, SkillType type, String displayName,
                   String description, int maximumRank, int unlockLevel, int cooldownTicks) {
        this(id, archetype, type, displayName, description, maximumRank, unlockLevel, cooldownTicks, null);
    }

    CompanionSkill(String id, CompanionClass archetype, SkillType type, String displayName,
                   String description, int maximumRank, int unlockLevel, int cooldownTicks,
                   CompanionSpecialization requiredSpecialization) {
        this.id = id;
        this.archetype = archetype;
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.maximumRank = maximumRank;
        this.unlockLevel = unlockLevel;
        this.cooldownTicks = cooldownTicks;
        this.requiredSpecialization = requiredSpecialization;
    }

    public String getId() { return id; }
    public CompanionClass getArchetype() { return archetype; }
    public SkillType getType() { return type; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getMaximumRank() { return maximumRank; }
    public int getUnlockLevel() { return unlockLevel; }
    public int getCooldownTicks() { return cooldownTicks; }
    public CompanionSpecialization getRequiredSpecialization() { return requiredSpecialization; }

    public static CompanionSkill byId(String id) {
        if (id == null) return null;
        for (CompanionSkill skill : values()) {
            if (skill.id.equals(id)) return skill;
        }
        return null;
    }

    public static List<CompanionSkill> forClass(CompanionClass archetype) {
        return Arrays.stream(values()).filter(skill -> skill.archetype == archetype).toList();
    }

    public static List<CompanionSkill> forClassAndSpecialization(
            CompanionClass archetype, CompanionSpecialization specialization) {
        return Arrays.stream(values()).filter(skill -> skill.archetype == archetype
                && (skill.requiredSpecialization == null
                || skill.requiredSpecialization == specialization)).toList();
    }
}
