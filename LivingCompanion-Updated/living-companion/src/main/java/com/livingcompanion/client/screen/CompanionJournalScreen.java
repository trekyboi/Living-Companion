package com.livingcompanion.client.screen;

import com.livingcompanion.entity.JournalEntry;
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
    private final JournalEntry[] entries;
    private int scrollOffset;

    public CompanionJournalScreen(String companionName, String className, int level,
                                  int bondLevel, String bondTitle, JournalEntry[] entries) {
        super(Component.literal("Companion Journal"));
        this.companionName = companionName == null ? "Companion" : companionName;
        this.className = className == null ? "Adventurer" : className;
        this.level = level;
        this.bondLevel = bondLevel;
        this.bondTitle = bondTitle == null ? "New Acquaintance" : bondTitle;
        this.entries = entries == null ? new JournalEntry[0] : entries.clone();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int panelWidth = Math.min(350, this.width - 24);
        int panelHeight = Math.min(310, this.height - 24);
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
        int visibleRows = Math.max(1, Math.min(5, (panelHeight - 145) / 35));
        int startY = top + 105;
        if (this.entries.length == 0) {
            graphics.drawCenteredString(this.font, "The journey is young.",
                    this.width / 2, startY + 20, CompanionUiTheme.TEXT_SECONDARY);
            graphics.drawCenteredString(this.font, "Shared moments will be written here.",
                    this.width / 2, startY + 34, CompanionUiTheme.TEXT_MUTED);
        } else {
            for (int row = 0; row < visibleRows; row++) {
                int index = this.entries.length - 1 - (this.scrollOffset + row);
                if (index < 0) break;
                JournalEntry entry = this.entries[index];
                int y = startY + row * 35;
                CompanionUiTheme.drawCard(graphics, innerLeft, y, innerWidth, 32, row == 0);
                graphics.drawString(this.font, String.format(java.util.Locale.ROOT, "%02d", index + 1),
                        innerLeft + 7, y + 5, CompanionUiTheme.GOLD);
                String title = fit(entry.getText(), innerWidth - 38);
                graphics.drawString(this.font, title, innerLeft + 29, y + 4,
                        CompanionUiTheme.TEXT_PRIMARY, false);
                String details;
                if (entry.getWorldTime() <= 0L || "unknown".equals(entry.getDimension())) {
                    details = "DATE / LOCATION UNKNOWN  •  LEGACY MEMORY";
                } else {
                    String biome = humanize(entry.getBiome());
                    long day = entry.getWorldTime() / 24000L + 1L;
                    details = "DAY " + day + "  •  " + biome + "  •  "
                            + entry.getX() + ", " + entry.getY() + ", " + entry.getZ();
                }
                graphics.drawString(this.font, fit(details, innerWidth - 38),
                        innerLeft + 29, y + 17, CompanionUiTheme.TEXT_MUTED, false);
            }
        }
        graphics.drawCenteredString(this.font, "Mouse wheel / arrows  •  " + this.entries.length + " records",
                this.width / 2, top + panelHeight - 26, CompanionUiTheme.TEXT_MUTED);
        graphics.drawCenteredString(this.font, "ESC  •  CLOSE",
                this.width / 2, top + panelHeight - 14, CompanionUiTheme.GOLD_BRIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String fit(String value, int maxWidth) {
        String result = value == null ? "" : value;
        while (this.font.width(result) > maxWidth && result.length() > 4) {
            result = result.substring(0, result.length() - 4);
        }
        return result.length() < (value == null ? 0 : value.length()) ? result + "..." : result;
    }

    private static String humanize(String id) {
        if (id == null || id.isBlank()) return "Unknown place";
        int colon = id.indexOf(':');
        String value = colon >= 0 ? id.substring(colon + 1) : id;
        return value.replace('_', ' ');
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxOffset = Math.max(0, this.entries.length - 1);
        this.scrollOffset = Math.max(0, Math.min(maxOffset,
                this.scrollOffset + (delta < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.scrollOffset = Math.min(Math.max(0, this.entries.length - 1), this.scrollOffset + 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.scrollOffset = Math.max(0, this.scrollOffset - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
