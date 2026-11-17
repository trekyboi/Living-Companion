package com.livingcompanion.entity;

import net.minecraft.nbt.CompoundTag;

/** A world-stamped record of a meaningful shared event. */
public final class JournalEntry {
    private final long worldTime;
    private final String dimension;
    private final String biome;
    private final int x, y, z;
    private final String text;

    public JournalEntry(long worldTime, String dimension, String biome, int x, int y, int z, String text) {
        this.worldTime = Math.max(0L, worldTime);
        this.dimension = dimension == null ? "unknown" : dimension;
        this.biome = biome == null ? "unknown" : biome;
        this.x = x; this.y = y; this.z = z;
        this.text = text == null ? "" : text;
    }

    public long getWorldTime() { return worldTime; }
    public String getDimension() { return dimension; }
    public String getBiome() { return biome; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getText() { return text; }

    public static JournalEntry capture(CompanionEntity companion, String text) {
        String dimension = companion.level().dimension().location().toString();
        String biome = companion.level().getBiome(companion.blockPosition()).unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown");
        return new JournalEntry(companion.level().getDayTime(), dimension, biome,
                companion.blockPosition().getX(), companion.blockPosition().getY(),
                companion.blockPosition().getZ(), text);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("WorldTime", worldTime);
        tag.putString("Dimension", dimension);
        tag.putString("Biome", biome);
        tag.putInt("X", x); tag.putInt("Y", y); tag.putInt("Z", z);
        tag.putString("Text", text);
        return tag;
    }

    public static JournalEntry load(CompoundTag tag) {
        return new JournalEntry(tag.getLong("WorldTime"), tag.getString("Dimension"),
                tag.getString("Biome"), tag.getInt("X"), tag.getInt("Y"),
                tag.getInt("Z"), tag.getString("Text"));
    }
}
