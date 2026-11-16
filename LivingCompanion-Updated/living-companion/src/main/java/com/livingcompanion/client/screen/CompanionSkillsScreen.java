package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import com.livingcompanion.network.CompanionNetwork.SkillDataMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Client view of server-authoritative companion progression and skill ranks. */
public final class CompanionSkillsScreen extends Screen {
    private final Screen parent;
    private SkillDataMessage data;

    private CompanionSkillsScreen(Screen parent, SkillDataMessage data) {
        super(Component.literal("Companion Skills"));
        this.parent = parent;
        this.data = data;
    }

    public static void open(Screen parent, SkillDataMessage data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CompanionSkillsScreen(parent, data));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        CompanionClass[] classes = CompanionClass.values();
        if (data.classId <= 0 || data.classId >= classes.length) return;
        CompanionClass archetype = classes[data.classId];
        int panelLeft = (this.width - 420) / 2;
        int panelTop = (this.height - 286) / 2;
        int rowY = panelTop + 76;
        for (CompanionSkill skill : CompanionSkill.forClass(archetype)) {
            int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
            boolean available = data.level >= skill.getUnlockLevel()
                    && data.skillPoints > 0 && rank < skill.getMaximumRank();
            Button upgrade = Button.builder(Component.literal(rank >= skill.getMaximumRank()
                            ? "MAXED" : "Upgrade"), button ->
                    CompanionNetwork.requestSkillUpgrade(data.entityId, skill.getId()))
                    .bounds(panelLeft + 326, rowY + 5, 82, 20).build();
            upgrade.active = available;
            this.addRenderableWidget(upgrade);
            rowY += 40;
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = (this.width - 420) / 2;
        int top = (this.height - 286) / 2;
        graphics.fill(left, top, left + 420, top + 286, 0xF0181B20);
        graphics.fill(left, top, left + 420, top + 2, 0xFF9A784A);
        graphics.fill(left, top + 284, left + 420, top + 286, 0xFF9A784A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFE6BE);

        CompanionClass[] classes = CompanionClass.values();
        if (data.classId > 0 && data.classId < classes.length) {
            graphics.drawString(this.font, classes[data.classId].getDisplayName()
                    + "  |  Level " + data.level + "  |  Skill points: " + data.skillPoints,
                    left + 12, top + 30, 0xFFE6D3B0, false);
            String xp = data.level >= CompanionProgressionMaxLevel.VALUE
                    ? "Maximum level reached" : "XP: " + data.experience + " / " + data.experienceToNext;
            graphics.drawString(this.font, xp, left + 12, top + 44, 0xFFB9B9B9, false);

            int rowY = top + 68;
            for (CompanionSkill skill : CompanionSkill.forClass(classes[data.classId])) {
                int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
                graphics.fill(left + 8, rowY, left + 412, rowY + 37, 0xFF252A31);
                graphics.drawString(this.font, skill.getDisplayName() + "  [" + skill.getType().name()
                        + "]  Rank " + rank + "/" + skill.getMaximumRank(),
                        left + 16, rowY + 4, 0xFFFFE6BE, false);
                graphics.drawString(this.font, skill.getDescription(), left + 16, rowY + 19,
                        0xFFBFC5CC, false);
                if (data.level < skill.getUnlockLevel()) {
                    graphics.drawString(this.font, "Unlocks at level " + skill.getUnlockLevel(),
                            left + 326, rowY + 27, 0xFFFFA070, false);
                }
                rowY += 40;
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static final class CompanionProgressionMaxLevel {
        private static final int VALUE = 50;
    }
}
