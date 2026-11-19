package com.livingcompanion.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared medieval-fantasy visual language for Living Companion.
 * Warm walnut panels, beveled antique-gold trim, restrained wood grain,
 * parchment text surfaces, and readable status colors.
 */
public final class CompanionUiTheme {
    public static final int BACKDROP = 0xD8080706;
    public static final int PANEL = 0xFF3A1D12;
    public static final int PANEL_INSET = 0xFF512719;
    public static final int PANEL_RAISED = 0xFF67351F;
    public static final int PANEL_HOVER = 0xFF794326;
    public static final int BORDER = 0xFF28130D;
    public static final int GOLD = 0xFFD59A36;
    public static final int GOLD_BRIGHT = 0xFFFFD96A;
    public static final int GOLD_MUTED = 0xFF956020;
    public static final int TEXT_PRIMARY = 0xFFFFE6B0;
    public static final int TEXT_SECONDARY = 0xFFE0C18A;
    public static final int TEXT_MUTED = 0xFFB18A5B;
    public static final int SUCCESS = 0xFF8FC66B;
    public static final int WARNING = 0xFFE8B64F;
    public static final int DANGER = 0xFFD45B45;
    public static final int BAR_TRACK = 0xFF1B0D09;
    public static final int PARCHMENT = 0xFFF0D5A5;
    public static final int PARCHMENT_TEXT = 0xFF57321C;

    private CompanionUiTheme() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;

        // Deep shadow and layered wooden frame.
        graphics.fill(x + 3, y + 4, x + width + 3, y + height + 4, 0x99000000);
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, GOLD_MUTED);
        graphics.fill(x + 4, y + 4, x + width - 4, y + height - 4, PANEL);

        // Warm horizontal grain gives flat GUI surfaces a wooden identity.
        for (int line = 0; line < height - 10; line += 13) {
            int yy = y + 6 + line;
            graphics.fill(x + 5, yy, x + width - 5, yy + 1, 0x183B1005);
            if (width > 100) {
                graphics.fill(x + 10 + (line * 7 % Math.max(1, width - 24)), yy + 2,
                        Math.min(x + width - 8, x + 28 + (line * 7 % Math.max(1, width - 24))),
                        yy + 3, 0x224A1D0D);
            }
        }

        // Inner bevel and fine gold pinstripe.
        graphics.fill(x + 4, y + 4, x + width - 4, y + 5, 0xFFFFD96A);
        graphics.fill(x + 4, y + 4, x + 5, y + height - 4, 0xFFFFD96A);
        graphics.fill(x + 4, y + height - 5, x + width - 4, y + height - 4, 0xFF754414);
        graphics.fill(x + width - 5, y + 4, x + width - 4, y + height - 4, 0xFF754414);

        // Pixel-style corner studs.
        drawStud(graphics, x + 7, y + 7);
        drawStud(graphics, x + width - 10, y + 7);
        drawStud(graphics, x + 7, y + height - 10);
        drawStud(graphics, x + width - 10, y + height - 10);
    }

    private static void drawStud(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 3, y + 3, GOLD_BRIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + 2, 0xFFFFF0B0);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, GOLD_MUTED);
    }

    public static void drawCard(GuiGraphics graphics, int x, int y, int width, int height,
                                boolean highlighted) {
        if (width <= 0 || height <= 0) return;
        int surface = highlighted ? PANEL_RAISED : PANEL_INSET;
        int edge = highlighted ? GOLD_BRIGHT : GOLD_MUTED;

        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, surface);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, edge);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF28130D);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, edge);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF28130D);

        if (height >= 24 && width >= 60) {
            graphics.fill(x + 5, y + 5, x + width - 5, y + 6, 0x183B1005);
            graphics.fill(x + 8, y + height - 7, x + width - 8, y + height - 6, 0x183B1005);
        }
    }

    /** Light parchment surface for readable journal notes and descriptive text. */
    public static void drawParchment(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x88000000);
        graphics.fill(x, y, x + width, y + height, 0xFF5A321B);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, PARCHMENT);
        graphics.fill(x + 4, y + 4, x + width - 4, y + 5, 0xFFD8B77D);
        graphics.fill(x + 4, y + height - 5, x + width - 4, y + height - 4, 0xFFB88A50);
        for (int line = 0; line < height - 12; line += 11) {
            int yy = y + 7 + line;
            graphics.fill(x + 6, yy, x + width - 6, yy + 1, 0x18A17A48);
        }
    }

    public static void drawDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        graphics.fill(x, y, x + width, y + 2, GOLD_MUTED);
        graphics.fill(x, y, x + width, y + 1, GOLD_BRIGHT);
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, String label,
                                         int x, int y, int width) {
        graphics.drawString(font, label, x, y, GOLD_BRIGHT, false);
        int lineStart = x + font.width(label) + 10;
        if (lineStart < x + width) {
            graphics.fill(lineStart, y + 5, x + width, y + 6, GOLD_MUTED);
            graphics.fill(lineStart, y + 5, Math.min(x + width, lineStart + 12), y + 6, GOLD);
        }
    }

    public static void drawTitle(GuiGraphics graphics, Font font, String title,
                                 String subtitle, int centerX, int y) {
        graphics.drawCenteredString(font, title, centerX + 1, y + 1, 0xFF241008);
        graphics.drawCenteredString(font, title, centerX, y, GOLD_BRIGHT);
        if (subtitle != null && !subtitle.isBlank()) {
            graphics.drawCenteredString(font, subtitle, centerX, y + 14, TEXT_SECONDARY);
        }
    }

    public static void drawPill(GuiGraphics graphics, Font font, String label,
                                int x, int y, int color) {
        int width = font.width(label) + 10;
        graphics.fill(x, y, x + width, y + 13, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 12, PANEL_RAISED);
        graphics.fill(x + 1, y + 1, x + 2, y + 12, color);
        graphics.drawString(font, label, x + 6, y + 3, color, false);
    }

    public static void drawProgressBar(GuiGraphics graphics, int x, int y, int width,
                                       int height, float progress, int fillColor) {
        if (width <= 0 || height <= 0) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BAR_TRACK);
        int fillWidth = Math.round((width - 2) * clamped);
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + height - 1, fillColor);
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + 2, 0x55FFFFFF);
        }
    }

    public static void drawTab(GuiGraphics graphics, Font font, int x, int y, int width,
                               int height, String label, boolean selected, boolean hovered) {
        int background = selected ? PANEL_RAISED : hovered ? PANEL_HOVER : PANEL_INSET;
        graphics.fill(x, y, x + width, y + height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, background);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, selected ? GOLD_BRIGHT : GOLD_MUTED);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, GOLD_MUTED);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2,
                selected ? GOLD_BRIGHT : TEXT_SECONDARY);
    }
}
