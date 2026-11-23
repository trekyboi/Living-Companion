package com.livingcompanion.client.screen;

import com.livingcompanion.entity.CompanionEntity;
import com.livingcompanion.network.CompanionNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Direct command selector for the owner's companion. */
public final class CompanionCommandScreen extends Screen {
    private static final CompanionEntity.Command[] COMMANDS = CompanionEntity.Command.values();
    private static final String[] LABELS = {"Follow", "Stay", "Defend", "Retreat", "Patrol"};
    private static final String[] DESCRIPTIONS = {
            "Stay close and travel with you.",
            "Hold this exact position.",
            "Guard you and respond to nearby threats.",
            "Fall back toward your position.",
            "Watch the area where the order is given."
    };

    private final Screen parent;
    private final int companionEntityId;
    private CompanionEntity.Command selectedCommand;
    private final Button[] commandButtons = new Button[COMMANDS.length];

    // Shared geometry keeps the buttons and rendered panel aligned, including
    // after the player resizes the game window.
    private int panelWidth;
    private int panelHeight;
    private int panelLeft;
    private int panelTop;

    public CompanionCommandScreen(Screen parent, int companionEntityId,
                                  CompanionEntity.Command selectedCommand) {
        super(Component.literal("Companion Commands"));
        this.parent = parent;
        this.companionEntityId = companionEntityId;
        this.selectedCommand = selectedCommand == null
                ? CompanionEntity.Command.FOLLOW : selectedCommand;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(230, this.width - 16);
        this.panelHeight = Math.min(220, this.height - 16);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        for (int i = 0; i < COMMANDS.length; i++) {
            final CompanionEntity.Command command = COMMANDS[i];
            this.commandButtons[i] = this.addRenderableWidget(Button.builder(
                    commandLabel(command), button -> {
                        CompanionNetwork.requestSetCommand(this.companionEntityId, command.ordinal());
                        this.selectedCommand = command;
                        updateCommandLabels();
                    }).bounds(this.panelLeft + 10, this.panelTop + 43 + i * 22,
                    this.panelWidth - 20, 19).build());
        }

        this.addRenderableWidget(Button.builder(Component.literal("Back to Inventory"),
                button -> returnToParent())
                .bounds(this.panelLeft + 10, this.panelTop + this.panelHeight - 28,
                        this.panelWidth - 20, 18).build());
    }

    private Component commandLabel(CompanionEntity.Command command) {
        String label = LABELS[command.ordinal()];
        return Component.literal((this.selectedCommand == command ? "✓  " : "    ") + label
                + (this.selectedCommand == command ? "  •  ACTIVE" : ""));
    }

    private void updateCommandLabels() {
        for (int i = 0; i < COMMANDS.length; i++) {
            this.commandButtons[i].setMessage(commandLabel(COMMANDS[i]));
        }
    }

    private void returnToParent() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        CompanionUiTheme.drawPanel(graphics, this.panelLeft, this.panelTop,
                this.panelWidth, this.panelHeight);
        CompanionUiTheme.drawTitle(graphics, this.font, "COMMAND CENTER",
                "Choose how your companion behaves", this.width / 2, this.panelTop + 13);
        graphics.drawCenteredString(this.font,
                "CURRENT ORDER: " + LABELS[this.selectedCommand.ordinal()].toUpperCase(java.util.Locale.ROOT),
                this.width / 2, this.panelTop + 29, CompanionUiTheme.GOLD_BRIGHT);
        graphics.drawCenteredString(this.font, DESCRIPTIONS[this.selectedCommand.ordinal()],
                this.width / 2, this.panelTop + this.panelHeight - 42,
                CompanionUiTheme.TEXT_SECONDARY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
