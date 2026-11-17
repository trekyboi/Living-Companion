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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, top - 15, 0xFFE6D3B0);
        for (int preset = 0; preset < 3; preset++) {
            graphics.drawString(this.font, "Preset " + (preset + 1), left, top + 6 + preset * 34, 0xFFFFFFFF);
        }
        graphics.drawCenteredString(this.font, "Save captures the six equipped slots.", this.width / 2,
                top + 91, 0xFFB9B2A7);
        graphics.drawCenteredString(this.font, "Equip / Swap exchanges current gear with the preset.",
                this.width / 2, top + 102, 0xFFB9B2A7);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
