package com.livingcompanion.progression;

/** Permanent level-30 class paths that shape the companion's combat role. */
public enum CompanionSpecialization {
    PALADIN_WARDEN(CompanionClass.PALADIN, "warden", "Warden", "A protective healer who strengthens wards and restorative skills."),
    PALADIN_CRUSADER(CompanionClass.PALADIN, "crusader", "Crusader", "An aggressive holy warrior who amplifies melee damage and Holy Strike."),
    TANK_BULWARK(CompanionClass.TANK, "bulwark", "Bulwark", "A steadfast protector who improves mitigation and bodyguard skills."),
    TANK_VANGUARD(CompanionClass.TANK, "vanguard", "Vanguard", "An aggressive frontline fighter who improves pressure, taunts, and damage."),
    PALADIN_TEMPLAR(CompanionClass.PALADIN, "templar", "Templar", "A battle-priest who protects allies through disciplined defensive auras."),
    TANK_JUGGERNAUT(CompanionClass.TANK, "juggernaut", "Juggernaut", "An unstoppable bruiser who converts endurance into crushing counterattacks."),
    ELEMENTALIST_PYROMANCER(CompanionClass.ELEMENTALIST, "pyromancer", "Pyromancer", "A fire specialist focused on burning damage and explosive area attacks."),
    ELEMENTALIST_CRYOMANCER(CompanionClass.ELEMENTALIST, "cryomancer", "Cryomancer", "A frost specialist who slows and freezes threats to control the battlefield."),
    ELEMENTALIST_STORMCALLER(CompanionClass.ELEMENTALIST, "stormcaller", "Stormcaller", "A lightning specialist who chains damage across groups of enemies.");

    private final CompanionClass archetype;
    private final String id;
    private final String displayName;
    private final String description;

    CompanionSpecialization(CompanionClass archetype, String id, String displayName, String description) {
        this.archetype = archetype;
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }

    public CompanionClass getArchetype() { return archetype; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static CompanionSpecialization byId(String id) {
        if (id == null || id.isBlank()) return null;
        for (CompanionSpecialization specialization : values()) {
            if (specialization.id.equals(id)) return specialization;
        }
        return null;
    }

    public static CompanionSpecialization[] forClass(CompanionClass archetype) {
        return java.util.Arrays.stream(values())
                .filter(specialization -> specialization.archetype == archetype)
                .toArray(CompanionSpecialization[]::new);
    }
}
