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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Responsive, scrollable client view of server-authoritative companion skills. */
public final class CompanionSkillsScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 600;
    private static final int PANEL_MAX_HEIGHT = 430;
    private static final int ROW_HEIGHT = 68;
    private static final int ROW_GAP = 6;
    private static final int HEADER_HEIGHT = 126;

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
        int previousSpecialization = this.data.specializationId;
        this.data = data;
        if (previousSpecialization != data.specializationId && this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
        } else {
            updateButtonPositions();
        }
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
        if (this.panelWidth >= 400 && this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.getAbilities().instabuild) {
            this.addRenderableWidget(Button.builder(Component.literal("MAX LEVEL (CREATIVE)"), button ->
                    CompanionNetwork.requestCreativeMaxLevel(this.data.entityId))
                    .bounds(this.panelLeft + this.panelWidth - 244, this.panelTop + 27, 132, 24).build());
        }
        this.listBottom = this.panelTop + this.panelHeight - 12;

        CompanionClass[] classes = CompanionClass.values();
        if (data.classId <= 0 || data.classId >= classes.length) return;

        int buttonWidth = Math.min(96, Math.max(76, this.panelWidth / 6));
        int buttonX = this.panelLeft + this.panelWidth - buttonWidth - 14;
        if (data.level >= 30 && data.specializationId < 0) {
            CompanionSpecialization[] choices = CompanionSpecialization.forClass(classes[data.classId]);
            int choiceWidth = Math.max(54, Math.min(120,
                    (this.panelWidth - 48 - 8 * (choices.length - 1)) / choices.length));
            for (int i = 0; i < choices.length; i++) {
                CompanionSpecialization choice = choices[i];
                Button select = Button.builder(Component.literal(choice.getDisplayName()), button ->
                        CompanionNetwork.requestSpecialization(data.entityId, choice.getId()))
                        .bounds(this.panelLeft + 16 + i * (choiceWidth + 8), this.panelTop + 98,
                                choiceWidth, 20).build();
                this.specializationButtons.add(select);
                this.addRenderableWidget(select);
            }
        }
        for (CompanionSkill skill : CompanionSkill.forClassAndSpecialization(
                classes[data.classId], getSelectedSpecialization())) {
            int rank = skill.ordinal() < data.ranks.length ? data.ranks[skill.ordinal()] : 0;
            boolean[] confirmationPending = {false};
            Button upgrade = Button.builder(Component.literal(rank >= skill.getMaximumRank()
                            ? "MAXED" : "Upgrade"), button -> {
                    if (!confirmationPending[0]) {
                        confirmationPending[0] = true;
                        button.setMessage(Component.literal("Confirm?"));
                    } else {
                        confirmationPending[0] = false;
                        CompanionNetwork.requestSkillUpgrade(data.entityId, skill.getId());
                        button.setMessage(Component.literal("Sent"));
                    }
                })
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

        CompanionUiTheme.drawPanel(graphics, left, top, right - left, bottom - top);
        graphics.fill(left, bottom - 2, right, bottom, 0xFF9A784A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 10, 0xFFFFE6BE);

        CompanionClass[] classes = CompanionClass.values();
        if (data.classId > 0 && data.classId < classes.length) {
            graphics.drawString(this.font, classes[data.classId].getDisplayName().toUpperCase(java.util.Locale.ROOT)
                    + "  •  LEVEL " + data.level,
                    left + 16, top + 32, CompanionUiTheme.GOLD_BRIGHT, false);
            CompanionUiTheme.drawPill(graphics, this.font, "SKILL POINTS: " + data.skillPoints,
                    left + 16, top + 47, data.skillPoints > 0
                            ? CompanionUiTheme.SUCCESS : CompanionUiTheme.TEXT_MUTED);
            String xp = data.level >= CompanionProgressionMaxLevel.VALUE
                    ? "Maximum level reached" : "XP: " + data.experience + " / " + data.experienceToNext;
            graphics.drawString(this.font, xp, left + 16, top + 65, 0xFFB9B9B9, false);
            if (getSelectedSpecialization() != null) {
                graphics.drawString(this.font, "Path: " + getSelectedSpecialization().getDisplayName(),
                        left + 16, top + 83, 0xFFFFD58A, false);
            } else if (data.level < 30) {
                graphics.drawString(this.font, "Specialization unlocks at level 30",
                        left + 16, top + 83, 0xFF9BA5AF, false);
            } else {
                graphics.drawString(this.font, "Choose your permanent class specialization:",
                        left + 16, top + 83, 0xFFFFD58A, false);
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
                CompanionUiTheme.drawCard(graphics, left + 8, rowY,
                    right - left - 16, ROW_HEIGHT, false);
                boolean locked = data.level < skill.getUnlockLevel();
                if (locked) graphics.fill(left + 8, rowY, right - 8, rowBottom, 0x884B2525);

                graphics.renderItem(skillIcon(skill), left + 16, rowY + 24);
                if (locked) graphics.drawString(this.font, "L", left + 30, rowY + 39, 0xFFFFA070, false);
                int textX = left + 44;
                int textRight = textX + descriptionWidth;
                graphics.drawString(this.font, skill.getDisplayName(),
                        textX, rowY + 8, CompanionUiTheme.TEXT_PRIMARY, false);
                String skillMeta = skill.getType().name() + "  •  RANK "
                        + rank + "/" + skill.getMaximumRank();
                graphics.drawString(this.font, skillMeta,
                        textRight - this.font.width(skillMeta),
                        rowY + 8, CompanionUiTheme.GOLD_BRIGHT, false);
                for (int pip = 0; pip < skill.getMaximumRank(); pip++) {
                    int pipX = textX + pip * 9;
                    graphics.fill(pipX, rowY + 19, pipX + 7, rowY + 23,
                            pip < rank ? 0xFFFFD06E : 0xFF555B64);
                }

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
                if (cooldownTicks > 0 && skill.getCooldownTicks() > 0) {
                    int barWidth = Math.max(36, descriptionWidth - 8);
                    int remaining = Math.min(skill.getCooldownTicks(), cooldownTicks);
                    int filled = barWidth * remaining / skill.getCooldownTicks();
                    CompanionUiTheme.drawProgressBar(graphics, textX, rowY + 63,
                            barWidth, 3, filled / (float) barWidth, CompanionUiTheme.GOLD);
                }
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

    private static ItemStack skillIcon(CompanionSkill skill) {
        return switch (skill) {
            case PALADIN_DIVINE_GRACE -> new ItemStack(Items.GOLDEN_APPLE);
            case PALADIN_HOLY_STRIKE, PALADIN_JUDGMENT, TANK_VANGUARD_FURY -> new ItemStack(Items.GOLDEN_SWORD);
            case PALADIN_DIVINE_PROTECTION, TANK_SHIELD_WALL, TANK_BULWARK_OATH -> new ItemStack(Items.SHIELD);
            case PALADIN_PRAYER, PALADIN_SANCTUARY -> new ItemStack(Items.GHAST_TEAR);
            case PALADIN_AEGIS_OF_DAWN, TANK_LAST_BASTION -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case TANK_UNYIELDING -> new ItemStack(Items.IRON_CHESTPLATE);
            case TANK_TAUNT -> new ItemStack(Items.BELL);
            case TANK_INTERCEPT -> new ItemStack(Items.IRON_INGOT);
            case PALADIN_TEMPLAR_VIGIL -> new ItemStack(Items.IRON_CHESTPLATE);
            case TANK_JUGGERNAUT_MOMENTUM -> new ItemStack(Items.ANVIL);
            case PALADIN_WARDEN_SANCTUARY_PULSE -> new ItemStack(Items.GHAST_TEAR);
            case PALADIN_WARDEN_CIRCLE_OF_RENEWAL, PALADIN_TEMPLAR_HOLY_STANDARD -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case PALADIN_CRUSADER_RADIANT_ASSAULT, PALADIN_CRUSADER_JUDGMENT_OF_DAWN -> new ItemStack(Items.GOLDEN_SWORD);
            case PALADIN_TEMPLAR_AEGIS_LINK -> new ItemStack(Items.SHIELD);
            case TANK_BULWARK_FORTRESS_STANCE, TANK_BULWARK_UNBREAKABLE_LINE -> new ItemStack(Items.SHIELD);
            case TANK_VANGUARD_WARCRY -> new ItemStack(Items.BELL);
            case TANK_VANGUARD_WARPATH -> new ItemStack(Items.IRON_AXE);
            case TANK_JUGGERNAUT_CRUSHING_BLOW, TANK_JUGGERNAUT_UNSTOPPABLE_RAMPAGE -> new ItemStack(Items.ANVIL);
        };
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
