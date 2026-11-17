package com.livingcompanion.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** A persistent, owner-only chronicle of the companion's remembered adventures. */
public final class CompanionJournalScreen extends Screen {
    private final String companionName;
    private final String className;
    private final int level;
    private final int bondLevel;
    private final String bondTitle;
    private final String[] memories;
    private int scrollOffset;

    public CompanionJournalScreen(String companionName, String className, int level,
                                  int bondLevel, String bondTitle, String[] memories) {
        super(Component.literal("Companion Journal"));
        this.companionName = companionName == null ? "Companion" : companionName;
        this.className = className == null ? "Adventurer" : className;
        this.level = level;
        this.bondLevel = bondLevel;
        this.bondTitle = bondTitle == null ? "New Acquaintance" : bondTitle;
        this.memories = memories == null ? new String[0] : memories.clone();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(330, this.width - 24);
        int panelHeight = Math.min(286, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        CompanionUiTheme.drawPanel(graphics, left, top, panelWidth, panelHeight);
        CompanionUiTheme.drawTitle(graphics, this.font, "THE JOURNAL",
                this.companionName + "  •  " + this.className + "  •  LEVEL " + this.level,
                this.width / 2, top + 12);

        int innerLeft = left + 14;
        int innerWidth = panelWidth - 28;
        CompanionUiTheme.drawCard(graphics, innerLeft, top + 48, innerWidth, 30, true);
        graphics.drawString(this.font, "BOND  " + this.bondLevel + " / 10",
                innerLeft + 9, top + 54, CompanionUiTheme.GOLD_BRIGHT, false);
        graphics.drawString(this.font, this.bondTitle,
                innerLeft + 9, top + 66, CompanionUiTheme.TEXT_SECONDARY, false);

        CompanionUiTheme.drawSectionHeader(graphics, this.font, "CHRONICLE",
                innerLeft, top + 88, innerWidth);
        int visibleRows = Math.max(1, Math.min(7, (panelHeight - 132) / 23));
        int startY = top + 105;
        if (this.memories.length == 0) {
            graphics.drawCenteredString(this.font, "The journey is young.",
                    this.width / 2, startY + 20, CompanionUiTheme.TEXT_SECONDARY);
            graphics.drawCenteredString(this.font, "Shared moments will be written here.",
                    this.width / 2, startY + 34, CompanionUiTheme.TEXT_MUTED);
        } else {
            for (int row = 0; row < visibleRows; row++) {
                int index = this.memories.length - 1 - (this.scrollOffset + row);
                if (index < 0) break;
                int y = startY + row * 23;
                CompanionUiTheme.drawCard(graphics, innerLeft, y, innerWidth, 20, row == 0);
                graphics.drawString(this.font, String.format(java.util.Locale.ROOT,
                                "%02d", index + 1),
                        innerLeft + 7, y + 6, CompanionUiTheme.GOLD);
                String entry = this.memories[index];
                int maxWidth = innerWidth - 35;
                while (this.font.width(entry) > maxWidth && entry.length() > 4) {
                    entry = entry.substring(0, entry.length() - 4);
                }
                if (!entry.equals(this.memories[index])) entry += "...";
                graphics.drawString(this.font, entry, innerLeft + 29, y + 6,
                        CompanionUiTheme.TEXT_PRIMARY, false);
            }
        }
        graphics.drawCenteredString(this.font, "Use mouse wheel to browse memories",
                this.width / 2, top + panelHeight - 26, CompanionUiTheme.TEXT_MUTED);
        graphics.drawCenteredString(this.font, "ESC  •  CLOSE",
                this.width / 2, top + panelHeight - 14, CompanionUiTheme.GOLD_BRIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, this.memories.length - 1);
        this.scrollOffset = Math.max(0, Math.min(maxOffset,
                this.scrollOffset + (delta < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.scrollOffset = Math.min(Math.max(0, this.memories.length - 1), this.scrollOffset + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
