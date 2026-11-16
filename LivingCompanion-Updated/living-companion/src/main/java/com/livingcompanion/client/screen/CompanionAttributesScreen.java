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
        int panelWidth = Math.min(520, Math.max(220, this.width - 16));
        int panelHeight = Math.min(300, Math.max(220, this.height - 16));
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int rowGap = Math.max(27, (panelHeight - 112) / CompanionAttribute.values().length);
        int buttonWidth = Math.min(96, Math.max(70, panelWidth / 5));
        int buttonX = left + panelWidth - buttonWidth - 12;
        int rowY = top + 78;
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            Button button = Button.builder(Component.literal("Upgrade"), b ->
                    CompanionNetwork.requestAttributeUpgrade(data.entityId, attribute.getId()))
                    .bounds(buttonX, rowY, buttonWidth, 22).build();
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
        int panelWidth = Math.min(520, Math.max(220, this.width - 16));
        int panelHeight = Math.min(300, Math.max(220, this.height - 16));
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181B20);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF9A784A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, 0xFFFFE6BE);
        graphics.drawCenteredString(this.font, "Unspent attribute points: " + data.attributePoints,
                this.width / 2, top + 38, 0xFF83E6A0);

        int y = top + 78;
        int rowGap = Math.max(27, (panelHeight - 112) / CompanionAttribute.values().length);
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            int value = getValue(attribute);
            graphics.drawString(this.font, attribute.getDisplayName() + "  " + value + "/20",
                    left + 18, y, 0xFFFFE6BE, false);
            int barX = left + 18;
            int barY = y + 13;
            int barWidth = Math.max(72, Math.min(150, panelWidth - 150));
            graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF090B0E);
            graphics.fill(barX, barY, barX + (barWidth * value / 20), barY + 6, 0xFFD6A85F);
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
            graphics.drawString(this.font, scaling, barX + barWidth + 10, y + 12, 0xFFBFC5CC, false);
            y += rowGap;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
