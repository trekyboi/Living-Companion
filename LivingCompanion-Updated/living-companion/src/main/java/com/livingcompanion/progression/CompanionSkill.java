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

    SHAMAN_SPIRIT_ATTUNEMENT("shaman_spirit_attunement", CompanionClass.SHAMAN, SkillType.PASSIVE, "Spirit Attunement",
            "The Shaman's bond with the spirit world slowly restores her health.", 3, 1, 0),
    SHAMAN_SPIRIT_BOLT("shaman_spirit_bolt", CompanionClass.SHAMAN, SkillType.ACTIVE, "Spirit Bolt",
            "Fires a focused bolt of spiritual energy at a hostile target.", 3, 1, 100),
    SHAMAN_RESTORATION_TIDE("shaman_restoration_tide", CompanionClass.SHAMAN, SkillType.ACTIVE, "Restoration Tide",
            "Calls restorative spirits to heal the Shaman and her owner.", 3, 10, 300),
    SHAMAN_ANCESTRAL_WRATH("shaman_ancestral_wrath", CompanionClass.SHAMAN, SkillType.ULTIMATE, "Ancestral Wrath",
            "Unleashes a wave of ancestral energy against nearby hostiles and restores nearby allies.", 1, 25, 1200),

    ELEMENTALIST_ELEMENTAL_FOCUS("elementalist_elemental_focus", CompanionClass.ELEMENTALIST, SkillType.PASSIVE, "Elemental Focus",
            "Elemental discipline improves the force of the Elementalist's spells.", 3, 1, 0),
    ELEMENTALIST_ARCANE_BOLT("elementalist_arcane_bolt", CompanionClass.ELEMENTALIST, SkillType.ACTIVE, "Arcane Bolt",
            "Launches a concentrated elemental projectile at a hostile target.", 3, 1, 90),
    ELEMENTALIST_PRIMAL_SURGE("elementalist_primal_surge", CompanionClass.ELEMENTALIST, SkillType.ACTIVE, "Primal Surge",
            "Detonates elemental energy around the target, damaging nearby hostiles.", 3, 10, 260),
    ELEMENTALIST_CATACLYSM("elementalist_cataclysm", CompanionClass.ELEMENTALIST, SkillType.ULTIMATE, "Cataclysm",
            "Calls down a devastating elemental eruption around the Elementalist's target.", 1, 25, 1200),

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
            "Warden active: restores nearby allies and reinforces the Paladin's ward.", 3, 30, 240,
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
            CompanionSpecialization.TANK_JUGGERNAUT),

    ELEMENTALIST_PYROMANCER_EMBER_HEART("elementalist_pyromancer_ember_heart", CompanionClass.ELEMENTALIST, SkillType.PASSIVE, "Ember Heart",
            "Pyromancer passive: increases elemental spell damage and adds burning to Arcane Bolt.", 3, 30, 0,
            CompanionSpecialization.ELEMENTALIST_PYROMANCER),
    ELEMENTALIST_PYROMANCER_FLAMEBURST("elementalist_pyromancer_flameburst", CompanionClass.ELEMENTALIST, SkillType.ACTIVE, "Flameburst",
            "Pyromancer active: blasts nearby hostiles with fire and sets them ablaze.", 3, 30, 220,
            CompanionSpecialization.ELEMENTALIST_PYROMANCER),
    ELEMENTALIST_PYROMANCER_INFERNO("elementalist_pyromancer_inferno", CompanionClass.ELEMENTALIST, SkillType.ULTIMATE, "Inferno",
            "Pyromancer ultimate: unleashes a devastating ring of fire around the Elementalist.", 1, 30, 2400,
            CompanionSpecialization.ELEMENTALIST_PYROMANCER),

    ELEMENTALIST_CRYOMANCER_FROSTBIND("elementalist_cryomancer_frostbind", CompanionClass.ELEMENTALIST, SkillType.PASSIVE, "Frostbind",
            "Cryomancer passive: Arcane Bolt chills its target, slowing its movement.", 3, 30, 0,
            CompanionSpecialization.ELEMENTALIST_CRYOMANCER),
    ELEMENTALIST_CRYOMANCER_FROST_NOVA("elementalist_cryomancer_frost_nova", CompanionClass.ELEMENTALIST, SkillType.ACTIVE, "Frost Nova",
            "Cryomancer active: releases a freezing pulse that slows nearby hostiles.", 3, 30, 240,
            CompanionSpecialization.ELEMENTALIST_CRYOMANCER),
    ELEMENTALIST_CRYOMANCER_ABSOLUTE_ZERO("elementalist_cryomancer_absolute_zero", CompanionClass.ELEMENTALIST, SkillType.ULTIMATE, "Absolute Zero",
            "Cryomancer ultimate: freezes nearby hostiles in place and deals heavy frost damage.", 1, 30, 2400,
            CompanionSpecialization.ELEMENTALIST_CRYOMANCER),

    ELEMENTALIST_STORMCALLER_STATIC_CHARGE("elementalist_stormcaller_static_charge", CompanionClass.ELEMENTALIST, SkillType.PASSIVE, "Static Charge",
            "Stormcaller passive: Arcane Bolt deals increased damage and briefly jolts its target.", 3, 30, 0,
            CompanionSpecialization.ELEMENTALIST_STORMCALLER),
    ELEMENTALIST_STORMCALLER_CHAIN_LIGHTNING("elementalist_stormcaller_chain_lightning", CompanionClass.ELEMENTALIST, SkillType.ACTIVE, "Chain Lightning",
            "Stormcaller active: strikes the current target and arcs through nearby hostiles.", 3, 30, 220,
            CompanionSpecialization.ELEMENTALIST_STORMCALLER),
    ELEMENTALIST_STORMCALLER_TEMPEST("elementalist_stormcaller_tempest", CompanionClass.ELEMENTALIST, SkillType.ULTIMATE, "Tempest",
            "Stormcaller ultimate: calls a violent storm that damages and knocks back nearby hostiles.", 1, 30, 2400,
            CompanionSpecialization.ELEMENTALIST_STORMCALLER);

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
