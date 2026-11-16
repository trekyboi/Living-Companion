package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.progression.CompanionClass;
import com.livingcompanion.progression.CompanionSkill;
import com.livingcompanion.progression.CompanionSpecialization;
import com.livingcompanion.network.CompanionNetwork.SkillDataMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Responsive, scrollable client view of server-authoritative companion skills. */
public final class CompanionSkillsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 600;
    private static final int PANEL_MAX_HEIGHT = 430;
    private static final int ROW_HEIGHT = 68;
    private static final int ROW_GAP = 6;
    private static final int HEADER_HEIGHT = 102;

    private final Screen parent;
    private SkillDataMessage data;
    private final List<Button> upgradeButtons = new ArrayList<>();
    private final List<Button> specializationButtons = new ArrayList<>();
    private int scrollOffset;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;
    private int refreshTicks;

    private CompanionSkillsScreen(Screen parent, SkillDataMessage data) {
        super(Component.literal("Companion Skills"));
        this.parent = parent;
        this.data = data;
    }

    public static void open(Screen parent, SkillDataMessage data) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.screen instanceof CompanionSkillsScreen current) {
            current.updateData(data);
            minecraft.setScreen(current);
        } else {
            minecraft.setScreen(new CompanionSkillsScreen(parent, data));
        }
    }

    public void updateData(SkillDataMessage data) {
        if (data.entityId != this.data.entityId) return;
        this.data = data;
        if (this.minecraft != null) this.init(this.minecraft, this.width, this.height);
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.refreshTicks >= 20) {
            this.refreshTicks = 0;
            CompanionNetwork.requestSkillData(this.data.entityId);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.upgradeButtons.clear();
        this.specializationButtons.clear();

        this.panelWidth = Math.min(PANEL_MAX_WIDTH, Math.max(220, this.width - 16));
        this.panelHeight = Math.min(PANEL_MAX_HEIGHT, Math.max(200, this.height - 16));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
        this.listTop = this.panelTop + HEADER_HEIGHT;
        this.addRenderableWidget(Button.builder(Component.literal("Attributes"), button ->
                CompanionAttributesScreen.open(this, this.data))
                .bounds(this.panelLeft + this.panelWidth - 108, this.panelTop + 29, 94, 22).build());
        this.listBottom = this.panelTop + this.panelHeight - 12;

        CompanionClass[] classes = CompanionClass.values();
        if (data.classId <= 0 || data.classId >= classes.length) return;

        int buttonWidth = Math.min(96, Math.max(76, this.panelWidth / 6));
        int buttonX = this.panelLeft + this.panelWidth - buttonWidth - 14;
        if (data.level >= 15 && data.specializationId < 0) {
            CompanionSpecialization[] choices = CompanionSpecialization.forClass(classes[data.classId]);
            int choiceWidth = Math.min(120, Math.max(80, (this.panelWidth - 48) / 2));
            for (int i = 0; i < choices.length; i++) {
                CompanionSpecialization choice = choices[i];
                Button select = Button.builder(Component.literal(choice.getDisplayName()), button ->
                        CompanionNetwork.requestSpecialization(data.entityId, choice.getId()))
                        .bounds(this.panelLeft + 16 + i * (choiceWidth + 8), this.panelTop + 72,
                                choiceWidth, 20).build();
                this.specializationButtons.add(select);
                this.addRenderableWidget(select);
            }
        }
        for (CompanionSkill skill : CompanionSkill.forClassAndSpecialization(
                classes[data.classId], getSelectedSpecialization())) {
            int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
            Button upgrade = Button.builder(Component.literal(rank >= skill.getMaximumRank()
                            ? "MAXED" : "Upgrade"), button ->
                    CompanionNetwork.requestSkillUpgrade(data.entityId, skill.getId()))
                    .bounds(buttonX, this.listTop, buttonWidth, 22).build();
            upgrade.active = canUpgrade(skill, rank);
            this.upgradeButtons.add(upgrade);
            this.addRenderableWidget(upgrade);
        }
        updateButtonPositions();
    }

    private CompanionSpecialization getSelectedSpecialization() {
        CompanionSpecialization[] values = CompanionSpecialization.values();
        return data.specializationId >= 0 && data.specializationId < values.length
                ? values[data.specializationId] : null;
    }

    private boolean canUpgrade(CompanionSkill skill, int rank) {
        return data.level >= skill.getUnlockLevel()
                && data.skillPoints > 0 && rank < skill.getMaximumRank();
    }

    private void updateButtonPositions() {
        int rowStep = ROW_HEIGHT + ROW_GAP;
        int buttonWidth = Math.min(96, Math.max(76, this.panelWidth / 6));
        int buttonX = this.panelLeft + this.panelWidth - buttonWidth - 14;
        CompanionClass[] classes = CompanionClass.values();
        if (data.classId <= 0 || data.classId >= classes.length) return;
        List<CompanionSkill> skills = CompanionSkill.forClassAndSpecialization(
                classes[data.classId], getSelectedSpecialization());
        for (int i = 0; i < this.upgradeButtons.size(); i++) {
            Button button = this.upgradeButtons.get(i);
            int rowY = this.listTop + i * rowStep - this.scrollOffset;
            button.setX(buttonX);
            button.setY(rowY + (ROW_HEIGHT - 22) / 2);
            boolean visible = rowY >= this.listTop && rowY + ROW_HEIGHT <= this.listBottom;
            button.visible = visible;
            if (i < skills.size()) {
                CompanionSkill skill = skills.get(i);
                int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
                button.active = visible && canUpgrade(skill, rank);
            } else {
                button.active = false;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= this.panelLeft && mouseX <= this.panelLeft + this.panelWidth
                && mouseY >= this.listTop && mouseY <= this.listBottom) {
            int contentHeight = this.upgradeButtons.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
            int viewportHeight = this.listBottom - this.listTop;
            int maxScroll = Math.max(0, contentHeight - viewportHeight);
            this.scrollOffset = MthClamp.clamp(this.scrollOffset - (int) (delta * 24), 0, maxScroll);
            updateButtonPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.panelLeft;
        int top = this.panelTop;
        int right = left + this.panelWidth;
        int bottom = top + this.panelHeight;

        graphics.fill(left, top, right, bottom, 0xF0181B20);
        graphics.fill(left, top, right, top + 2, 0xFF9A784A);
        graphics.fill(left, bottom - 2, right, bottom, 0xFF9A784A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFE6BE);

        CompanionClass[] classes = CompanionClass.values();
        if (data.classId > 0 && data.classId < classes.length) {
            graphics.drawString(this.font, classes[data.classId].getDisplayName()
                    + "  |  Level " + data.level + "  |  Skill points: " + data.skillPoints,
                    left + 16, top + 32, 0xFFE6D3B0, false);
            String xp = data.level >= CompanionProgressionMaxLevel.VALUE
                    ? "Maximum level reached" : "XP: " + data.experience + " / " + data.experienceToNext;
            graphics.drawString(this.font, xp, left + 16, top + 49, 0xFFB9B9B9, false);
            if (getSelectedSpecialization() != null) {
                graphics.drawString(this.font, "Path: " + getSelectedSpecialization().getDisplayName(),
                        left + 16, top + 70, 0xFFFFD58A, false);
            } else if (data.level < 15) {
                graphics.drawString(this.font, "Specialization unlocks at level 15",
                        left + 16, top + 70, 0xFF9BA5AF, false);
            } else {
                graphics.drawString(this.font, "Choose your permanent class specialization:",
                        left + 16, top + 70, 0xFFFFD58A, false);
            }

            List<CompanionSkill> skills = CompanionSkill.forClassAndSpecialization(
                    classes[data.classId], getSelectedSpecialization());
            int descriptionWidth = Math.max(90, this.panelWidth - Math.min(96, Math.max(76, this.panelWidth / 6)) - 54);
            graphics.enableScissor(left + 8, this.listTop, right - 8, this.listBottom);
            for (int i = 0; i < skills.size(); i++) {
                CompanionSkill skill = skills.get(i);
                int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
                int rowY = this.listTop + i * (ROW_HEIGHT + ROW_GAP) - this.scrollOffset;
                int rowBottom = rowY + ROW_HEIGHT;
                graphics.fill(left + 8, rowY, right - 8, rowBottom, 0xFF252A31);

                int textX = left + 18;
                int textRight = textX + descriptionWidth;
                graphics.drawString(this.font, skill.getDisplayName() + "  [" + skill.getType().name()
                                + "]  Rank " + rank + "/" + skill.getMaximumRank(),
                        textX, rowY + 8, 0xFFFFE6BE, false);

                List<net.minecraft.util.FormattedCharSequence> description =
                        this.font.split(Component.literal(skill.getDescription()), descriptionWidth);
                int maxLines = 2;
                for (int line = 0; line < Math.min(maxLines, description.size()); line++) {
                    graphics.drawString(this.font, description.get(line), textX,
                            rowY + 27 + line * 11, 0xFFBFC5CC, false);
                }

                int activeTicks = skill.ordinal() < data.activeTicks.length
                        ? data.activeTicks[skill.ordinal()] : 0;
                int cooldownTicks = skill.ordinal() < data.cooldowns.length
                        ? data.cooldowns[skill.ordinal()] : 0;
                String status;
                int statusColor;
                if (data.level < skill.getUnlockLevel()) {
                    status = "Unlocks at level " + skill.getUnlockLevel();
                    statusColor = 0xFFFFA070;
                } else if (activeTicks > 0) {
                    status = "ACTIVE  " + String.format(java.util.Locale.ROOT, "%.1fs", activeTicks / 20.0D);
                    statusColor = 0xFF83E6A0;
                } else if (cooldownTicks > 0) {
                    status = "Cooldown  " + String.format(java.util.Locale.ROOT, "%.1fs", cooldownTicks / 20.0D);
                    statusColor = 0xFFFFC078;
                } else if (skill.getType() == CompanionSkill.SkillType.PASSIVE) {
                    status = "Passive";
                    statusColor = 0xFF9BA5AF;
                } else if (rank >= skill.getMaximumRank()) {
                    status = "Fully upgraded";
                    statusColor = 0xFF9BA5AF;
                } else if (data.skillPoints <= 0) {
                    status = "Requires 1 skill point";
                    statusColor = 0xFF9BA5AF;
                } else {
                    status = "Ready";
                    statusColor = 0xFF83E6A0;
                }
                graphics.drawString(this.font, status, textX, rowY + 53, statusColor, false);
            }
            graphics.disableScissor();

            int contentHeight = skills.size() * (ROW_HEIGHT + ROW_GAP) - ROW_GAP;
            int viewportHeight = this.listBottom - this.listTop;
            if (contentHeight > viewportHeight) {
                graphics.drawCenteredString(this.font, "Scroll to view more skills",
                        this.width / 2, bottom - 10, 0xFF9BA5AF);
            }
        }
        updateButtonPositions();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static final class MthClamp {
        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class CompanionProgressionMaxLevel {
        private static final int VALUE = 50;
    }
}
