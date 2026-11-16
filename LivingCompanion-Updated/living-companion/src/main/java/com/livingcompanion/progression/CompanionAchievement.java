package com.livingcompanion.progression;

/** Milestone achievements that unlock companion titles. */
public enum CompanionAchievement {
    FIRST_BLOOD("first_blood", "First Blood", "Defeat your first enemy together."),
    DIAMOND_SEEKER("diamond_seeker", "Diamond Seeker", "Discover diamonds together."),
    NETHER_EXPLORER("nether_explorer", "Netherwalker", "Enter the Nether together."),
    END_EXPLORER("end_explorer", "Far Traveler", "Enter the End together."),
    WITHER_SLAYER("wither_slayer", "Witherbreaker", "Defeat the Wither together."),
    DRAGON_SLAYER("dragon_slayer", "Dragon's Bane", "Defeat the Ender Dragon together."),
    VETERAN("veteran", "Veteran", "Reach companion level 10."),
    ELITE("elite", "Elite", "Reach companion level 25."),
    LEGEND("legend", "Living Legend", "Reach companion level 50."),
    TRUSTED("trusted", "Trusted Friend", "Reach relationship level 5."),
    DEVOTED("devoted", "Devoted Companion", "Reach relationship level 8."),
    UNBREAKABLE("unbreakable", "Unbreakable Bond", "Reach relationship level 10.");

    private final String id;
    private final String title;
    private final String description;

    CompanionAchievement(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public static CompanionAchievement byId(String id) {
        if (id == null) return null;
        for (CompanionAchievement achievement : values()) {
            if (achievement.id.equals(id)) return achievement;
        }
        return null;
    }
}
