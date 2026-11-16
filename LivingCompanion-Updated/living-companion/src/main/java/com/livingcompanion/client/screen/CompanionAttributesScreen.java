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
        int panelWidth = Math.min(520, this.width - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - 300) / 2;
        int buttonX = left + panelWidth - 112;
        int rowY = top + 86;
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            Button button = Button.builder(Component.literal("Upgrade"), b ->
                    CompanionNetwork.requestAttributeUpgrade(data.entityId, attribute.getId()))
                    .bounds(buttonX, rowY, 96, 22).build();
            this.upgradeButtons.add(button);
            this.addRenderableWidget(button);
            rowY += 39;
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
        int panelWidth = Math.min(520, this.width - 24);
        int panelHeight = 300;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0181B20);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF9A784A);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top + 14, 0xFFFFE6BE);
        graphics.drawCenteredString(this.font, "Unspent attribute points: " + data.attributePoints,
                this.width / 2, top + 38, 0xFF83E6A0);

        int y = top + 87;
        for (CompanionAttribute attribute : CompanionAttribute.values()) {
            int value = getValue(attribute);
            graphics.drawString(this.font, attribute.getDisplayName() + "  " + value + "/20",
                    left + 18, y, 0xFFFFE6BE, false);
            graphics.drawString(this.font, attribute.getDescription(),
                    left + 18, y + 13, 0xFFBFC5CC, false);
            y += 39;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
