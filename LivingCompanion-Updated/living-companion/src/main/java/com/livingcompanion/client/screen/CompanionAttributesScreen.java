package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.network.CompanionNetwork.SkillDataMessage;
import com.livingcompanion.progression.CompanionAttribute;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative companion attribute allocation screen. */
public final class CompanionAttributesScreen extends Screen {
    private final Screen parent;
    private SkillDataMessage data;
    private final List<Button> upgradeButtons = new ArrayList<>();

    private CompanionAttributesScreen(Screen parent, SkillDataMessage data) {
        super(Component.literal("Companion Attributes"));
        this.parent = parent;
        this.data = data;
    }

    public static void open(Screen parent, SkillDataMessage data) {
        net.minecraft.client.Minecraft.getInstance().setScreen(new CompanionAttributesScreen(parent, data));
    }

    public void updateData(SkillDataMessage data) {
        if (data.entityId != this.data.entityId) return;
        this.data = data;
        for (int i = 0; i < upgradeButtons.size(); i++) {
            CompanionAttribute attribute = CompanionAttribute.values()[i];
            int value = getValue(attribute);
            upgradeButtons.get(i).active = data.attributePoints > 0
                    && value < com.livingcompanion.progression.CompanionProgression.ATTRIBUTE_POINT_CAP;
            upgradeButtons.get(i).setMessage(Component.literal(value >= 20 ? "MAX" : "Upgrade"));
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.upgradeButtons.clear();
        int panelWidth = Math.max(180, Math.min(520, this.width - 16));
        int panelHeight = Math.max(150, Math.min(300, this.height - 16));
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int rowGap = Math.max(20, Math.min(34, (panelHeight - 100) / CompanionAttribute.values().length));
        int buttonWidth = Math.min(96, Math.max(58, panelWidth / 5));
        int buttonX = left + panelWidth - buttonWidth - 12;
        int rowY = top + 76;
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            Button button = Button.builder(Component.literal("Upgrade"), b ->
                    CompanionNetwork.requestAttributeUpgrade(data.entityId, attribute.getId()))
                    .bounds(buttonX, rowY, buttonWidth, 18).build();
            this.upgradeButtons.add(button);
            this.addRenderableWidget(button);
            rowY += rowGap;
        }
        updateData(this.data);
    }

    private int getValue(CompanionAttribute attribute) {
        return attribute.ordinal() < data.attributes.length ? data.attributes[attribute.ordinal()] : 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTicks >= 20) {
            refreshTicks = 0;
            CompanionNetwork.requestSkillData(data.entityId);
        }
    }
    private int refreshTicks;

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.max(180, Math.min(520, this.width - 16));
        int panelHeight = Math.max(150, Math.min(300, this.height - 16));
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        CompanionUiTheme.drawPanel(graphics, left, top, panelWidth, panelHeight);
        CompanionUiTheme.drawTitle(graphics, this.font, "COMPANION ATTRIBUTES",
                "Shape your companion's combat role", this.width / 2, top + 10);
        CompanionUiTheme.drawPill(graphics, this.font,
                "UNSPENT POINTS: " + data.attributePoints,
                left + 18, top + 42, data.attributePoints > 0
                        ? CompanionUiTheme.SUCCESS : CompanionUiTheme.TEXT_MUTED);
        CompanionUiTheme.drawSectionHeader(graphics, this.font, "ATTRIBUTE ALLOCATION",
                left + 18, top + 64, panelWidth - 36);

        int y = top + 76;
        int rowGap = Math.max(20, Math.min(34, (panelHeight - 100) / CompanionAttribute.values().length));
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            int value = getValue(attribute);
            CompanionUiTheme.drawCard(graphics, left + 12, y - 5,
                    panelWidth - 24, rowGap - 2, false);
            graphics.drawString(this.font, attribute.getDisplayName().toUpperCase(java.util.Locale.ROOT)
                            + "  " + value + "/20",
                    left + 22, y, CompanionUiTheme.TEXT_PRIMARY, false);
            int barX = left + 22;
            int barY = y + 13;
            int barWidth = Math.max(28, Math.min(150, panelWidth - 190));
            CompanionUiTheme.drawProgressBar(graphics, barX, barY, barWidth, 6,
                    value / 20.0F, CompanionUiTheme.GOLD);
            for (int segment = 1; segment < 20; segment++) {
                int dividerX = barX + barWidth * segment / 20;
                graphics.fill(dividerX, barY, dividerX + 1, barY + 6, 0xFF252A31);
            }
            String scaling = switch (attribute) {
                case VIGOR -> "+1 max health per point";
                case MIGHT -> "+0.15 melee damage per point";
                case FORTITUDE -> "+0.4 armor per point";
                case AGILITY -> "+0.002 movement speed per point";
                case WISDOM -> "-0.5% skill cooldown and +1% Prayer healing per point";
            };
            graphics.drawString(this.font, scaling, barX + barWidth + 10, y + 12,
                    CompanionUiTheme.TEXT_SECONDARY, false);
            y += rowGap;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
