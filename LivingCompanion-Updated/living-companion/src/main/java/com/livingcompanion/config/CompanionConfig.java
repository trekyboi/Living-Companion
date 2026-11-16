package com.livingcompanion.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/** Simple server-authoritative settings for the companion's core systems. */
public final class CompanionConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue MOVEMENT_SPEED;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_START_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue FOLLOW_STOP_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue HOSTILE_SCAN_RANGE;
    public static final ForgeConfigSpec.IntValue RESURRECTION_DELAY_SECONDS;
    public static final ForgeConfigSpec.BooleanValue DIALOGUE_ENABLED;
    public static final ForgeConfigSpec.IntValue DIALOGUE_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.BooleanValue LOOT_PICKUP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LOOT_PICKUP_WHITELIST;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.push("companion");
        MAX_HEALTH = b.comment("Companion maximum health.")
                .defineInRange("maxHealth", 40.0D, 10.0D, 200.0D);
        ATTACK_DAMAGE = b.comment("Base melee damage before weapon bonuses.")
                .defineInRange("attackDamage", 6.0D, 1.0D, 40.0D);
        MOVEMENT_SPEED = b.comment("Movement speed.")
                .defineInRange("movementSpeed", 0.28D, 0.10D, 1.0D);
        b.pop();

        b.push("following");
        FOLLOW_START_DISTANCE = b.comment("Start following beyond this distance.")
                .defineInRange("startDistance", 2.5D, 1.0D, 32.0D);
        FOLLOW_STOP_DISTANCE = b.comment("Stop moving toward the owner inside this distance.")
                .defineInRange("stopDistance", 1.5D, 0.5D, 16.0D);
        TELEPORT_DISTANCE = b.comment("Catch-up teleport distance. Set to 0 to disable.")
                .defineInRange("teleportDistance", 24.0D, 0.0D, 128.0D);
        b.pop();

        b.push("combat");
        HOSTILE_SCAN_RANGE = b.comment("Range used to find hostile monsters.")
                .defineInRange("hostileScanRange", 16.0D, 4.0D, 48.0D);
        LOOT_PICKUP_ENABLED = b.comment("Allow companions to collect explicitly approved nearby item drops.")
                .define("lootPickupEnabled", false);
        LOOT_PICKUP_WHITELIST = b.comment("Item registry IDs the companion may collect. Empty means collect nothing.")
                .defineList("lootPickupWhitelist", List.of(), value -> value instanceof String itemId
                        && itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"));
        b.pop();

        b.push("resurrection");
        RESURRECTION_DELAY_SECONDS = b.comment("Survival respawn delay in seconds (0-10). Creative-mode owners respawn the companion instantly.")
                .defineInRange("delaySeconds", 10, 0, 10);
        b.pop();

        b.push("dialogue");
        DIALOGUE_ENABLED = b.comment("Enable occasional companion dialogue.")
                .define("enabled", true);
        DIALOGUE_COOLDOWN_SECONDS = b.comment("Minimum delay between random dialogue lines.")
                .defineInRange("cooldownSeconds", 90, 15, 3600);
        b.pop();

        SPEC = b.build();
    }

    private CompanionConfig() { }
}
