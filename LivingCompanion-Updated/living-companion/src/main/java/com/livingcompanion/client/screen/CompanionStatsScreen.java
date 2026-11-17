package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.network.CompanionNetwork.StatsDataMessage;
import com.livingcompanion.progression.CompanionAttribute;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import com.livingcompanion.progression.CompanionSpecialization;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only, server-fed combat and progression summary for the companion. */
public final class CompanionStatsScreen extends Screen {
    private final Screen parent;
    private StatsDataMessage data;
    private int refreshTicks;

    private CompanionStatsScreen(Screen parent, StatsDataMessage data) {
        super(Component.literal("Companion Stats"));
        this.parent = parent;
        this.data = data;
    }

    public static void open(Screen parent, StatsDataMessage data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CompanionStatsScreen(parent, data));
    }

    @Override
    protected void init() {
        super.init();
        if (minecraft != null && minecraft.player != null
                && minecraft.player.getAbilities().instabuild) {
            addRenderableWidget(Button.builder(Component.literal("MAX LEVEL (CREATIVE)"), button ->
                    CompanionNetwork.requestCreativeMaxLevel(data.entityId))
                    .bounds(width / 2 - 76, height - 30, 152, 20).build());
        }
    }

    public void updateData(StatsDataMessage next) {
        if (next.entityId == data.entityId) data = next;
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            CompanionNetwork.requestStats(data.entityId);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private double stat(int index) {
        return index >= 0 && index < data.stats.length ? data.stats[index] : 0.0D;
    }

    private String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String statValue(double total, double base) {
        return number(total) + " (" + (total - base >= 0 ? "+" : "") + number(total - base) + ")";
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelWidth = Math.min(600, Math.max(300, width - 16));
        int panelHeight = Math.min(400, Math.max(250, height - 48));
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2 - 8;
        int innerLeft = left + 16;
        int innerRight = left + panelWidth - 16;
        int columnGap = 18;
        int columnWidth = (panelWidth - 32 - columnGap) / 2;
        int rightColumn = innerLeft + columnWidth + columnGap;

        CompanionUiTheme.drawPanel(graphics, left, top, panelWidth, panelHeight);
        CompanionUiTheme.drawTitle(graphics, font, "COMPANION OVERVIEW",
                "Combat performance • progression • bond", width / 2, top + 8);

        String className = data.classId >= 0 && data.classId < CompanionClass.values().length
                ? CompanionClass.values()[data.classId].getDisplayName() : "Unknown";
        String specializationName = data.specializationId >= 0
                && data.specializationId < CompanionSpecialization.values().length
                ? CompanionSpecialization.values()[data.specializationId].getDisplayName() : "Unchosen";
        CompanionUiTheme.drawPill(graphics, font, className.toUpperCase(java.util.Locale.ROOT),
                innerLeft, top + 36, CompanionUiTheme.GOLD_BRIGHT);
        CompanionUiTheme.drawPill(graphics, font, "LEVEL " + data.level,
                innerLeft + 92, top + 36, CompanionUiTheme.TEXT_PRIMARY);
        CompanionUiTheme.drawPill(graphics, font, specializationName.toUpperCase(java.util.Locale.ROOT),
                innerLeft + 164, top + 36, CompanionUiTheme.SUCCESS);

        String xp = data.experienceToNext > 0
                ? data.experience + " / " + data.experienceToNext + " XP" : "MAX LEVEL";
        graphics.drawString(font, xp, innerRight - font.width(xp), top + 39,
                CompanionUiTheme.TEXT_SECONDARY, false);

        String bond = "BOND  " + data.relationshipTitle + "  •  Lv. " + data.relationshipLevel;
        graphics.drawString(font, bond, innerLeft, top + 57, CompanionUiTheme.TEXT_PRIMARY, false);
        String titleLine = "Title: " + data.companionTitle + "   |   Achievements: "
                + data.achievementCount + "/12";
        graphics.drawString(font, titleLine, innerLeft, top + 71,
                CompanionUiTheme.TEXT_SECONDARY, false);

        int healthY = top + 94;
        double maxHealth = Math.max(0.0D, stat(1));
        double currentHealth = Math.max(0.0D, stat(0));
        String healthLabel = "HEALTH  " + number(currentHealth) + " / " + number(maxHealth);
        graphics.drawString(font, healthLabel, innerLeft, healthY - 11,
                CompanionUiTheme.TEXT_PRIMARY, false);
        CompanionUiTheme.drawProgressBar(graphics, innerLeft, healthY, panelWidth - 32, 7,
                maxHealth > 0.0D ? (float) (currentHealth / maxHealth) : 0.0F,
                CompanionUiTheme.DANGER);

        int sectionY = top + 116;
        CompanionUiTheme.drawSectionHeader(graphics, font, "COMBAT PROFILE",
                innerLeft, sectionY, columnWidth);
        CompanionUiTheme.drawSectionHeader(graphics, font, "ATTRIBUTE ALLOCATION",
                rightColumn, sectionY, columnWidth);

        String[] labels = {"Health", "Attack", "Armor", "Toughness", "Move Speed", "Knockback"};
        String[] values = {
                statValue(stat(1), stat(7)),
                statValue(stat(2), stat(8)),
                statValue(stat(3), stat(9)),
                statValue(stat(4), stat(10)),
                statValue(stat(5), stat(11)),
                number(stat(6) * 100.0D) + "% (" + (stat(6) - stat(12) >= 0 ? "+" : "")
                        + number((stat(6) - stat(12)) * 100.0D) + "%)"
        };
        for (int i = 0; i < labels.length; i++) {
            int y = sectionY + 17 + i * 15;
            CompanionUiTheme.drawCard(graphics, innerLeft, y - 3, columnWidth, 14, false);
            graphics.drawString(font, labels[i], innerLeft + 5, y,
                    CompanionUiTheme.TEXT_SECONDARY, false);
            String value = values[i];
            graphics.drawString(font, value,
                    innerLeft + columnWidth - 5 - font.width(value), y,
                    CompanionUiTheme.TEXT_PRIMARY, false);
        }

        int attributeY = sectionY + 17;
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            int value = attribute.ordinal() < data.attributes.length
                    ? data.attributes[attribute.ordinal()] : 0;
            CompanionUiTheme.drawCard(graphics, rightColumn, attributeY - 3,
                    columnWidth, 25, false);
            graphics.drawString(font, attribute.getDisplayName(),
                    rightColumn + 5, attributeY, CompanionUiTheme.TEXT_PRIMARY, false);
            String points = value + "/20";
            graphics.drawString(font, points,
                    rightColumn + columnWidth - 5 - font.width(points), attributeY,
                    CompanionUiTheme.GOLD_BRIGHT, false);
            CompanionUiTheme.drawProgressBar(graphics, rightColumn + 5, attributeY + 11,
                    columnWidth - 10, 5, value / 20.0F, CompanionUiTheme.GOLD);
            attributeY += 29;
        }

        int lowerY = sectionY + 119;
        CompanionUiTheme.drawSectionHeader(graphics, font, "CLASS & SPECIALIZATION SKILLS",
                innerLeft, lowerY, columnWidth);
        CompanionUiTheme.drawSectionHeader(graphics, font, "ACHIEVEMENTS",
                rightColumn, lowerY, columnWidth);

        int shown = 0;
        if (data.classId >= 0 && data.classId < CompanionClass.values().length) {
            CompanionSpecialization selected = data.specializationId >= 0
                    && data.specializationId < CompanionSpecialization.values().length
                    ? CompanionSpecialization.values()[data.specializationId] : null;
            for (CompanionSkill skill : CompanionSkill.forClassAndSpecialization(
                    CompanionClass.values()[data.classId], selected)) {
                if (shown >= 8) break;
                int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
                int y = lowerY + 16 + shown * 11;
                graphics.drawString(font, skill.getDisplayName(),
                        innerLeft + 5, y, CompanionUiTheme.TEXT_SECONDARY, false);
                String rankText = rank + "/" + skill.getMaximumRank();
                graphics.drawString(font, rankText,
                        innerLeft + columnWidth - 5 - font.width(rankText), y,
                        rank > 0 ? CompanionUiTheme.GOLD_BRIGHT : CompanionUiTheme.TEXT_MUTED, false);
                shown++;
            }
        }

        int achievementRows = Math.min(6, data.achievementTitles.length);
        for (int i = 0; i < achievementRows; i++) {
            graphics.drawString(font, data.achievementTitles[i],
                    rightColumn + 5, lowerY + 16 + i * 12,
                    CompanionUiTheme.TEXT_SECONDARY, false);
        }
        if (data.achievementTitles.length > achievementRows) {
            graphics.drawString(font, "+" + (data.achievementTitles.length - achievementRows) + " more",
                    rightColumn + 5, lowerY + 16 + achievementRows * 12,
                    CompanionUiTheme.TEXT_MUTED, false);
        }

        String footer = "Combat values include progression and equipped gear bonuses.";
        graphics.drawCenteredString(font, footer, width / 2,
                top + panelHeight - 14, CompanionUiTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
