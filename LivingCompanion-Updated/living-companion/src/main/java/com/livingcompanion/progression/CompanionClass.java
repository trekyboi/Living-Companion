package com.livingcompanion.progression;

/** Starting archetypes available during companion creation. */
public enum CompanionClass {
    UNASSIGNED("Unassigned", "Choose an archetype to begin your journey."),
    PALADIN("Paladin", "A guardian who protects, restores, and smites hostile foes."),
    TANK("Tank", "A frontline defender built to endure punishment and draw enemy attention."),
    SHAMAN("Shaman", "A spiritual guide who channels nature, restoration, and protective elemental forces."),
    ELEMENTALIST("Elementalist", "A spellcaster who commands fire, water, air, and earth to control the battlefield.");

    private final String displayName;
    private final String description;

    CompanionClass(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
