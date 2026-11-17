package com.livingcompanion.client.screen;

import com.livingcompanion.network.CompanionNetwork;
import com.livingcompanion.progression.CompanionClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

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

    private int panelWidth() {
        return Math.min(440, Math.max(220, width - 24));
    }

    private int panelHeight() {
        return Math.min(340, Math.max(280, height - 20));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int innerWidth = panelWidth - 56;
        int center = this.width / 2;
        int buttonGap = 12;
        int classButtonWidth = (innerWidth - buttonGap) / 2;

        this.nameField = new EditBox(this.font, left + 28, top + 80,
                innerWidth, 22, Component.literal("Companion name"));
        this.nameField.setMaxLength(24);
        this.nameField.setValue("Aldric");
        this.addRenderableWidget(this.nameField);

        this.paladinButton = this.addRenderableWidget(Button.builder(
                Component.literal("Paladin"), button -> selectClass(CompanionClass.PALADIN))
                .bounds(left + 28, top + 126, classButtonWidth, 24).build());
        this.tankButton = this.addRenderableWidget(Button.builder(
                Component.literal("Tank"), button -> selectClass(CompanionClass.TANK))
                .bounds(left + 28 + classButtonWidth + buttonGap, top + 126,
                        classButtonWidth, 24).build());

        this.addRenderableWidget(Button.builder(Component.literal("Begin Adventure"), button -> confirm())
                .bounds(center - Math.min(180, innerWidth / 2), top + 258,
                        Math.min(360, innerWidth), 24).build());
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
                    (selectedClass == CompanionClass.PALADIN ? "✓  " : "") + "Paladin"));
        }
        if (this.tankButton != null) {
            this.tankButton.setMessage(Component.literal(
                    (selectedClass == CompanionClass.TANK ? "✓  " : "") + "Tank"));
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
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int innerLeft = left + 28;
        int innerWidth = panelWidth - 56;
        int center = this.width / 2;

        CompanionUiTheme.drawPanel(graphics, left, top, panelWidth, panelHeight());
        CompanionUiTheme.drawTitle(graphics, this.font, "A COMPANION FOR THE JOURNEY",
                "Choose his name and calling", center, top + 14);

        CompanionUiTheme.drawSectionHeader(graphics, this.font, "COMPANION NAME",
                innerLeft, top + 61, innerWidth);

        int buttonGap = 12;
        int classButtonWidth = (innerWidth - buttonGap) / 2;
        CompanionUiTheme.drawCard(graphics, innerLeft - 3, top + 123,
                classButtonWidth + 6, 30, selectedClass == CompanionClass.PALADIN);
        CompanionUiTheme.drawCard(graphics, innerLeft + classButtonWidth + buttonGap - 3,
                top + 123, classButtonWidth + 6, 30,
                selectedClass == CompanionClass.TANK);

        int descriptionTop = top + 164;
        int descriptionHeight = 72;
        CompanionUiTheme.drawCard(graphics, innerLeft - 3, descriptionTop,
                innerWidth + 6, descriptionHeight, true);

        String description = selectedClass.getDescription();
        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(description), innerWidth - 24);
        int lineY = descriptionTop + 10;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(this.font, line, center, lineY,
                    CompanionUiTheme.TEXT_PRIMARY);
            lineY += 12;
        }

        String traits = selectedClass == CompanionClass.PALADIN
                ? "Healing  •  Protection  •  Holy strikes"
                : "Taunts  •  Resilience  •  Frontline defense";
        List<FormattedCharSequence> traitLines = this.font.split(
                Component.literal(traits), innerWidth - 24);
        int traitY = Math.max(descriptionTop + 43, lineY + 3);
        for (FormattedCharSequence line : traitLines) {
            graphics.drawCenteredString(this.font, line, center, traitY,
                    CompanionUiTheme.GOLD_BRIGHT);
            traitY += 11;
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
