package com.livingcompanion.client.screen;

import com.livingcompanion.menu.CompanionInventoryMenu;
import com.livingcompanion.network.CompanionNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.components.Button;

/** Player-style companion inventory with explicit slot wells and section labels. */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionInventoryMenu> {
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_FILL = 0xFF101216;
    private static final int SLOT_EDGE_LIGHT = 0xFF55565A;
    private static final int SLOT_EDGE_DARK = 0xFF08090B;
    private Button protectionButton;
    private Button skillsButton;
    private Button statsButton;

    public CompanionInventoryScreen(CompanionInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 236;
        this.imageHeight = 250;
        this.inventoryLabelY = 130;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    private Component protectionLabel() {
        return Component.literal("Owner damage blocked: " + (this.menu.getCompanion().isProtectedFromOwner() ? "ON" : "OFF"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.skillsButton = this.addRenderableWidget(Button.builder(Component.literal("Skills"), button -> {
            CompanionNetwork.requestSkillData(this.menu.getCompanion().getId());
        }).bounds(this.leftPos + 132, this.topPos + 2, 46, 18).build());
        this.statsButton = this.addRenderableWidget(Button.builder(Component.literal("Stats"), button -> {
            CompanionNetwork.requestStats(this.menu.getCompanion().getId());
        }).bounds(this.leftPos + 180, this.topPos + 2, 48, 18).build());
        this.protectionButton = this.addRenderableWidget(Button.builder(protectionLabel(), button -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(this.leftPos + 62, this.topPos + 226, 166, 18).build());
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
                drawSlot(graphics, left + 8 + col * SLOT_SIZE, top + 142 + row * SLOT_SIZE);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, left + 8 + col * SLOT_SIZE, top + 200);
        }

        graphics.fill(left + 52, top + 14, left + 53, top + 104, 0xFF51483C);
        graphics.fill(left + 6, top + 126, left + this.imageWidth - 6, top + 127, 0xFF51483C);
        graphics.drawString(this.font, "Equipment", left + 8, top + 116, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Companion Storage", left + 62, top + 116, 0xFFE6D3B0, false);
        graphics.drawString(this.font, "Your Inventory", left + 8, top + 132, 0xFFE6D3B0, false);

        // Recognizable vanilla item silhouettes identify each empty equipment slot.
        ItemStack[] equipmentIcons = {
                new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.SHIELD)
        };
        int[][] equipmentPositions = {{8, 20}, {8, 38}, {8, 56}, {8, 74}, {26, 20}, {26, 56}};
        for (int i = 0; i < equipmentPositions.length; i++) {
            int sx = left + equipmentPositions[i][0];
            int sy = top + equipmentPositions[i][1];
            if (!this.menu.getSlot(i).hasItem()) {
                graphics.renderItem(equipmentIcons[i], sx + 1, sy + 1);
                graphics.fill(sx + 2, sy + 15, sx + 16, sy + 16, 0x88777D86);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        if (this.protectionButton != null) this.protectionButton.setMessage(protectionLabel());
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
        String equipmentTip = equipmentTooltip(mouseX - this.leftPos, mouseY - this.topPos);
        if (equipmentTip != null) {
            graphics.renderTooltip(this.font, Component.literal(equipmentTip), mouseX, mouseY);
        }
    }

    private String equipmentTooltip(int x, int y) {
        if (x >= 8 && x < 26 && y >= 20 && y < 38) return "Helmet slot — head protection";
        if (x >= 8 && x < 26 && y >= 38 && y < 56) return "Chest slot — chest armor";
        if (x >= 8 && x < 26 && y >= 56 && y < 74) return "Leg slot — leg armor";
        if (x >= 8 && x < 26 && y >= 74 && y < 92) return "Boot slot — movement and fall protection";
        if (x >= 26 && x < 44 && y >= 20 && y < 38) return "Main hand — weapon or utility item";
        if (x >= 26 && x < 44 && y >= 56 && y < 74) return "Off hand — shield or utility item";
        return null;
    }
}
