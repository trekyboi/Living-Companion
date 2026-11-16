package com.livingcompanion.progression;

/** Spendable companion attributes. Values are capped to keep bonuses predictable. */
public enum CompanionAttribute {
    VIGOR("vigor", "Vigor", "Increases maximum health."),
    MIGHT("might", "Might", "Increases melee damage."),
    FORTITUDE("fortitude", "Fortitude", "Increases armor."),
    AGILITY("agility", "Agility", "Increases movement speed."),
    WISDOM("wisdom", "Wisdom", "Improves skill cooldowns and healing.");

    private final String id;
    private final String displayName;
    private final String description;
    CompanionAttribute(String id, String displayName, String description) {
        this.id = id; this.displayName = displayName; this.description = description;
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public static CompanionAttribute byId(String id) {
        if (id == null) return null;
        for (CompanionAttribute attribute : values()) if (attribute.id.equals(id)) return attribute;
        return null;
    }
}
