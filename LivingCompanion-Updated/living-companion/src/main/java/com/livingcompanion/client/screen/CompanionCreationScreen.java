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

/** Sequential first-world companion introduction: Knight first, then Mage. */
public final class CompanionCreationScreen extends Screen {
    private EditBox nameField;
    private int step = 0;
    private String knightName = "Aldric";
    private CompanionClass knightClass = CompanionClass.PALADIN;
    private CompanionClass mageClass = CompanionClass.SHAMAN;
    private Button firstClassButton;
    private Button secondClassButton;
    private Button backButton;
    private Button continueButton;

    public CompanionCreationScreen() {
        super(Component.literal("Meet Your Companions"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new CompanionCreationScreen());
    }

    private int panelWidth() { return Math.min(460, Math.max(260, width - 24)); }
    private int panelHeight() { return Math.min(360, Math.max(320, height - 20)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return (height - panelHeight()) / 2; }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int innerWidth = panelWidth() - 56;
        int gap = 12;
        int classWidth = (innerWidth - gap) / 2;
        nameField = new EditBox(font, left + 28, top + 82, innerWidth, 22,
                Component.literal("Companion name"));
        nameField.setMaxLength(24);
        nameField.setValue(step == 0 ? knightName : "Lyra");
        addRenderableWidget(nameField);

        firstClassButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> selectClass(step == 0 ? CompanionClass.PALADIN : CompanionClass.SHAMAN))
                .bounds(left + 28, top + 130, classWidth, 24).build());
        secondClassButton = addRenderableWidget(Button.builder(Component.empty(),
                button -> selectClass(step == 0 ? CompanionClass.TANK : CompanionClass.ELEMENTALIST))
                .bounds(left + 28 + classWidth + gap, top + 130, classWidth, 24).build());

        backButton = addRenderableWidget(Button.builder(Component.literal("Back"), button -> goBack())
                .bounds(left + 28, top + 278, 90, 24).build());
        continueButton = addRenderableWidget(Button.builder(Component.literal("Next: Meet the Mage"), button -> advance())
                .bounds(left + 128, top + 278, innerWidth - 100, 24).build());
        backButton.visible = step == 1;
        refreshButtons();
        setInitialFocus(nameField);
    }

    private void selectClass(CompanionClass selected) {
        if (step == 0 && (selected == CompanionClass.PALADIN || selected == CompanionClass.TANK)) {
            knightClass = selected;
        } else if (step == 1 && (selected == CompanionClass.SHAMAN || selected == CompanionClass.ELEMENTALIST)) {
            mageClass = selected;
        }
        refreshButtons();
    }

    private void refreshButtons() {
        if (firstClassButton == null) return;
        CompanionClass first = step == 0 ? CompanionClass.PALADIN : CompanionClass.SHAMAN;
        CompanionClass second = step == 0 ? CompanionClass.TANK : CompanionClass.ELEMENTALIST;
        CompanionClass selected = step == 0 ? knightClass : mageClass;
        firstClassButton.setMessage(Component.literal((selected == first ? "✓  " : "") + first.getDisplayName()));
        secondClassButton.setMessage(Component.literal((selected == second ? "✓  " : "") + second.getDisplayName()));
        continueButton.setMessage(Component.literal(step == 0 ? "Next: Meet the Mage" : "Begin Adventure"));
        backButton.visible = step == 1;
    }

    private void advance() {
        String entered = nameField.getValue().trim();
        if (entered.isEmpty() || entered.length() > 24) {
            nameField.setFocused(true);
            return;
        }
        if (step == 0) {
            knightName = entered;
            step = 1;
            rebuildWidgets();
            return;
        }
        String mageName = entered;
        CompanionNetwork.CHANNEL.sendToServer(new CompanionNetwork.CreateCompanionsMessage(
                knightName, knightClass.ordinal(), mageName, mageClass.ordinal()));
        Minecraft.getInstance().setScreen(null);
    }

    private void goBack() {
        if (step != 1) return;
        step = 0;
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = panelLeft(), top = panelTop(), panelWidth = panelWidth();
        int innerLeft = left + 28, innerWidth = panelWidth - 56, center = width / 2;
        CompanionUiTheme.drawPanel(graphics, left, top, panelWidth, panelHeight());
        String character = step == 0 ? "THE KNIGHT" : "THE MAGE";
        String subtitle = step == 0 ? "Your steadfast blade on the road ahead"
                : "A wielder of ancient and elemental magic";
        CompanionUiTheme.drawTitle(graphics, font, "A COMPANION FOR THE JOURNEY",
                character + "  •  " + subtitle, center, top + 14);
        CompanionUiTheme.drawSectionHeader(graphics, font, step == 0 ? "KNIGHT'S NAME" : "MAGE'S NAME",
                innerLeft, top + 62, innerWidth);

        int gap = 12, classWidth = (innerWidth - gap) / 2;
        CompanionUiTheme.drawCard(graphics, innerLeft - 3, top + 127,
                classWidth + 6, 30, (step == 0 ? knightClass : mageClass)
                        == (step == 0 ? CompanionClass.PALADIN : CompanionClass.SHAMAN));
        CompanionUiTheme.drawCard(graphics, innerLeft + classWidth + gap - 3, top + 127,
                classWidth + 6, 30, (step == 0 ? knightClass : mageClass)
                        == (step == 0 ? CompanionClass.TANK : CompanionClass.ELEMENTALIST));

        CompanionClass selected = step == 0 ? knightClass : mageClass;
        CompanionUiTheme.drawCard(graphics, innerLeft - 3, top + 168, innerWidth + 6, 82, true);
        List<FormattedCharSequence> lines = font.split(Component.literal(selected.getDescription()), innerWidth - 24);
        int y = top + 179;
        for (FormattedCharSequence line : lines) {
            graphics.drawCenteredString(font, line, center, y, CompanionUiTheme.TEXT_PRIMARY);
            y += 12;
        }
        String traits = step == 0
                ? (knightClass == CompanionClass.PALADIN ? "Healing  •  Protection  •  Holy strikes"
                : "Taunts  •  Resilience  •  Frontline defense")
                : (mageClass == CompanionClass.SHAMAN ? "Nature  •  Spirit magic  •  Restoration"
                : "Fire  •  Water  •  Air  •  Earth");
        graphics.drawCenteredString(font, traits, center, top + 229, CompanionUiTheme.GOLD_BRIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
