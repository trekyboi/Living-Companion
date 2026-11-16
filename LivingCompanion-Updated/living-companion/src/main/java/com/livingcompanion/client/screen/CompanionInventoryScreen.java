package com.livingcompanion.client.screen;

import com.livingcompanion.menu.CompanionInventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;

/** Player-style companion inventory with explicit slot wells and section labels. */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_FILL = 0xFF101216;
    private static final int SLOT_EDGE_LIGHT = 0xFF55565A;
    private static final int SLOT_EDGE_DARK = 0xFF08090B;

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 222;
        this.inventoryLabelY = 120;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_FILL);
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, SLOT_EDGE_LIGHT);
        graphics.fill(x, y, x + 1, y + SLOT_SIZE, SLOT_EDGE_LIGHT);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_EDGE_DARK);
        graphics.fill(x + SLOT_SIZE - 1, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_EDGE_DARK);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xE0181B20);
        graphics.fill(left, top, left + this.imageWidth, top + 1, 0xFF9A784A);
        graphics.fill(left, top + this.imageHeight - 1, left + this.imageWidth, top + this.imageHeight, 0xFF9A784A);
        graphics.fill(left, top, left + 1, top + this.imageHeight, 0xFF9A784A);
        graphics.fill(left + this.imageWidth - 1, top, left + this.imageWidth, top + this.imageHeight, 0xFF9A784A);

        // Equipment column: helmet, chest, legs, feet, main hand, offhand.
        drawSlot(graphics, left + 8, top + 20);
        drawSlot(graphics, left + 8, top + 38);
        drawSlot(graphics, left + 8, top + 56);
        drawSlot(graphics, left + 8, top + 74);
        drawSlot(graphics, left + 26, top + 20);
        drawSlot(graphics, left + 26, top + 56);

        // Companion storage: three rows plus its hotbar.
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, left + 62 + col * SLOT_SIZE, top + 20 + row * SLOT_SIZE);
            }
        }

        // Player inventory and hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, left + 8 + col * SLOT_SIZE, top + 132 + row * SLOT_SIZE);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, left + 8 + col * SLOT_SIZE, top + 190);
        }

        graphics.fill(left + 52, top + 14, left + 53, top + 104, 0xFF51483C);
        graphics.fill(left + 6, top + 112, left + this.imageWidth - 6, top + 113, 0xFF51483C);
        graphics.drawString(this.font, "Equipment", left + 8, top + 106, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Companion Storage", left + 62, top + 106, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Your Inventory", left + 8, top + 120, 0xFFE6D3B0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
