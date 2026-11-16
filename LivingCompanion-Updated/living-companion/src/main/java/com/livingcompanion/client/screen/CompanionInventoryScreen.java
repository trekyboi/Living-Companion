package com.livingcompanion.client.screen;

import com.livingcompanion.menu.CompanionInventoryMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;

public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 222;
        this.inventoryLabelY = 120;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
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

        // Separate the companion equipment and storage sections.
        graphics.fill(left + 52, top + 14, left + 53, top + 104, 0xFF51483C);
        graphics.fill(left + 6, top + 112, left + this.imageWidth - 6, top + 113, 0xFF51483C);
        graphics.drawString(this.font, "Companion", left + 62, top + 6, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Equipment", left + 8, top + 106, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Your Inventory", left + 8, top + 120, 0xFFE6D3B0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
