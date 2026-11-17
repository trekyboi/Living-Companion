package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.progression.CompanionClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** First-join creation screen for the player's one persistent knight companion. */
public final class CompanionCreationScreen extends Screen {
    private EditBox nameField;
    private CompanionClass selectedClass = CompanionClass.PALADIN;
    private Button paladinButton;
    private Button tankButton;

    public CompanionCreationScreen() {
        super(Component.literal("Create Your Companion"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CompanionCreationScreen());
    }

    @Override
    protected void init() {
        int center = this.width / 2;
        int panelWidth = Math.min(252, Math.max(220, this.width - 16));
        int contentWidth = Math.min(200, panelWidth - 32);
        this.nameField = new EditBox(this.font, center - contentWidth / 2, this.height / 2 - 62,
                contentWidth, 20, Component.literal("Companion name"));
        this.nameField.setMaxLength(24);
        this.nameField.setValue("Aldric");
        this.addRenderableWidget(this.nameField);

        this.paladinButton = this.addRenderableWidget(Button.builder(
                Component.literal("Paladin"), button -> selectClass(CompanionClass.PALADIN))
                .bounds(center - 102, this.height / 2 - 22, 98, 20).build());
        this.tankButton = this.addRenderableWidget(Button.builder(
                Component.literal("Tank"), button -> selectClass(CompanionClass.TANK))
                .bounds(center + 4, this.height / 2 - 22, 98, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Begin Adventure"), button -> confirm())
                .bounds(center - 100, this.height / 2 + 65, 200, 20).build());
        refreshClassButtons();
        this.setInitialFocus(this.nameField);
    }

    private void selectClass(CompanionClass type) {
        this.selectedClass = type;
        refreshClassButtons();
    }

    private void refreshClassButtons() {
        if (this.paladinButton != null) {
            this.paladinButton.setMessage(Component.literal(
                    (selectedClass == CompanionClass.PALADIN ? "✓ " : "") + "Paladin"));
        }
        if (this.tankButton != null) {
            this.tankButton.setMessage(Component.literal(
                    (selectedClass == CompanionClass.TANK ? "✓ " : "") + "Tank"));
        }
    }

    private void confirm() {
        String name = this.nameField.getValue().trim();
        if (name.isEmpty() || name.length() > 24) {
            this.nameField.setFocused(true);
            return;
        }
        CompanionNetwork.CHANNEL.sendToServer(
                new CompanionNetwork.CreateCompanionMessage(name, selectedClass.ordinal()));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int center = this.width / 2;
        int panelWidth = Math.min(252, Math.max(220, this.width - 16));
        int panelLeft = center - panelWidth / 2;
        int panelHeight = Math.min(226, Math.max(210, this.height - 16));
        int top = (this.height - panelHeight) / 2;
        CompanionUiTheme.drawPanel(graphics, panelLeft, top, panelWidth, panelHeight);
        CompanionUiTheme.drawTitle(graphics, this.font, "A COMPANION FOR THE JOURNEY",
                "Choose his name and calling", center, top + 10);
        CompanionUiTheme.drawCard(graphics, center - 104, this.height / 2 - 25,
                100, 26, selectedClass == CompanionClass.PALADIN);
        CompanionUiTheme.drawCard(graphics, center + 2, this.height / 2 - 25,
                100, 26, selectedClass == CompanionClass.TANK);
        CompanionUiTheme.drawSectionHeader(graphics, this.font, "IDENTITY",
                center - 100, this.height / 2 - 76, 200);
        graphics.drawString(this.font, "NAME", center - 100, this.height / 2 - 76, 0xFFE8D5B5);
        CompanionUiTheme.drawCard(graphics, center - 104, this.height / 2 + 2,
                208, 42, true);
        graphics.drawCenteredString(this.font, selectedClass.getDescription(),
                center, this.height / 2 + 8, CompanionUiTheme.TEXT_PRIMARY);
        if (selectedClass == CompanionClass.PALADIN) {
            graphics.drawCenteredString(this.font, "Healing • protection • holy strikes",
                    center, this.height / 2 + 23, CompanionUiTheme.GOLD_BRIGHT);
        } else {
            graphics.drawCenteredString(this.font, "Taunts • resilience • frontline defense",
                    center, this.height / 2 + 23, 0xFFD9C38F);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
