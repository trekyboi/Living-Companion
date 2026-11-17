package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple owner-facing screen for saving and swapping three equipment presets. */
public final class CompanionLoadoutsScreen extends Screen {
    private final Screen parent;
    private final int companionEntityId;

    public CompanionLoadoutsScreen(Screen parent, int companionEntityId) {
        super(Component.literal("Companion Equipment Loadouts"));
        this.parent = parent;
        this.companionEntityId = companionEntityId;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 110;
        int top = this.height / 2 - 58;
        for (int preset = 0; preset < 3; preset++) {
            final int index = preset;
            int y = top + preset * 34;
            this.addRenderableWidget(Button.builder(Component.literal("Save"), button ->
                    CompanionNetwork.requestEquipmentLoadout(companionEntityId, index, true))
                    .bounds(left + 112, y, 48, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Equip / Swap"), button ->
                    CompanionNetwork.requestEquipmentLoadout(companionEntityId, index, false))
                    .bounds(left + 164, y, 76, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
                .bounds(this.width / 2 - 40, top + 112, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int left = this.width / 2 - 110;
        int top = this.height / 2 - 58;
        CompanionUiTheme.drawPanel(graphics, left - 10, top - 24, 260, 166);
        CompanionUiTheme.drawTitle(graphics, this.font, "EQUIPMENT LOADOUTS",
                "Save and swap complete gear sets", this.width / 2, top - 16);
        for (int preset = 0; preset < 3; preset++) {
            int rowY = top + preset * 34 - 3;
            CompanionUiTheme.drawCard(graphics, left - 3, rowY, 246, 29, false);
            graphics.drawString(this.font, "PRESET 0" + (preset + 1),
                    left + 5, rowY + 10, CompanionUiTheme.TEXT_PRIMARY, false);
        }
        CompanionUiTheme.drawDivider(graphics, left - 2, top + 91, 244);
        graphics.drawCenteredString(this.font, "SAVE captures the six equipped slots.",
                this.width / 2, top + 96, CompanionUiTheme.TEXT_SECONDARY);
        graphics.drawCenteredString(this.font, "SWAP exchanges the current gear with a preset.",
                this.width / 2, top + 108, CompanionUiTheme.TEXT_MUTED);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
