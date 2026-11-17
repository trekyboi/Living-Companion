package com.livingcompanion.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared visual language for Living Companion screens.
 * Dark slate surfaces, antique-gold framing, and restrained status colors.
 */
public final class CompanionUiTheme {
    public static final int PANEL = 0xF0181B20;
    public static final int PANEL_INSET = 0xFF22272E;
    public static final int PANEL_RAISED = 0xFF2B3038;
    public static final int BORDER = 0xFF51483C;
    public static final int GOLD = 0xFFB18A55;
    public static final int GOLD_BRIGHT = 0xFFFFD58A;
    public static final int GOLD_MUTED = 0xFF9A784A;
    public static final int TEXT_PRIMARY = 0xFFE8DCC7;
    public static final int TEXT_SECONDARY = 0xFFBFC5CC;
    public static final int TEXT_MUTED = 0xFF858B94;
    public static final int SUCCESS = 0xFF78B887;
    public static final int WARNING = 0xFFE0B45E;
    public static final int DANGER = 0xFFB94D4D;
    public static final int BAR_TRACK = 0xFF090B0E;

    private CompanionUiTheme() {}

    public static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + width, y + 2, GOLD);
        graphics.fill(x, y + height - 2, x + width, y + height, GOLD_MUTED);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
    }

    public static void drawDivider(GuiGraphics graphics, int x, int y, int width) {
        if (width <= 0) return;
        graphics.fill(x, y, x + width, y + 1, BORDER);
    }

    public static void drawSectionHeader(GuiGraphics graphics, Font font, String label,
                                         int x, int y, int width) {
        graphics.drawString(font, label, x, y, GOLD_BRIGHT, false);
        int lineStart = x + font.width(label) + 10;
        if (lineStart < x + width) {
            graphics.fill(lineStart, y + 5, x + width, y + 6, BORDER);
        }
    }

    public static void drawProgressBar(GuiGraphics graphics, int x, int y, int width,
                                       int height, float progress, int fillColor) {
        if (width <= 0 || height <= 0) return;
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        graphics.fill(x, y, x + width, y + height, BAR_TRACK);
        int fillWidth = Math.round(width * clamped);
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }
        graphics.fill(x, y, x + width, y + 1, 0x55FFFFFF);
    }

    public static void drawTab(GuiGraphics graphics, Font font, int x, int y, int width,
                               int height, String label, boolean selected, boolean hovered) {
        int background = selected ? PANEL_RAISED : hovered ? 0xFF30353D : PANEL_INSET;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1, selected ? GOLD : BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height,
                selected ? GOLD_BRIGHT : BORDER);
        graphics.drawCenteredString(font, label, x + width / 2, y + (height - 8) / 2,
                selected ? GOLD_BRIGHT : TEXT_SECONDARY);
    }
}
