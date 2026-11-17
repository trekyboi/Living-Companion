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
import net.minecraftforge.fml.ModList;

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
        this.imageHeight = 272;
        this.inventoryLabelY = -1000;
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
        if (ModList.get().isLoaded("curios") && ModList.get().isLoaded("sophisticatedbackpacks")) {
            this.addRenderableWidget(Button.builder(Component.literal("Backpack"), button -> {
                CompanionNetwork.requestOpenBackpack(this.menu.getCompanion().getId());
            }).bounds(this.leftPos + 70, this.topPos + 2, 58, 18).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Loadouts"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CompanionLoadoutsScreen(this, this.menu.getCompanion().getId()));
            }
        }).bounds(this.leftPos + 8, this.topPos + 2, 58, 18).build());
        this.skillsButton = this.addRenderableWidget(Button.builder(Component.literal("Skills"), button -> {
            CompanionNetwork.requestSkillData(this.menu.getCompanion().getId());
        }).bounds(this.leftPos + 132, this.topPos + 2, 46, 18).build());
        this.statsButton = this.addRenderableWidget(Button.builder(Component.literal("Stats"), button -> {
            CompanionNetwork.requestStats(this.menu.getCompanion().getId());
        }).bounds(this.leftPos + 180, this.topPos + 2, 48, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("Companion Commands"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new CompanionCommandScreen(this, this.menu.getCompanion().getId(),
                        this.menu.getCompanion().getCommand()));
            }
        }).bounds(this.leftPos + 62, this.topPos + 248, 166, 18).build());
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
        CompanionUiTheme.drawPanel(graphics, left, top, this.imageWidth, this.imageHeight);
        CompanionUiTheme.drawCard(graphics, left + 5, top + 22, 44, 80, false);
        CompanionUiTheme.drawCard(graphics, left + 58, top + 22, 170, 80, false);
        CompanionUiTheme.drawCard(graphics, left + 5, top + 140, 170, 78, false);
        CompanionUiTheme.drawCard(graphics, left + 5, top + 198, 170, 24, true);

        // Equipment column: helmet, chest, legs, feet, main hand, offhand.
        drawSlot(graphics, left + 8, top + 26);
        drawSlot(graphics, left + 8, top + 44);
        drawSlot(graphics, left + 8, top + 62);
        drawSlot(graphics, left + 8, top + 80);
        drawSlot(graphics, left + 26, top + 26);
        drawSlot(graphics, left + 26, top + 62);
        if (this.menu.hasCuriosBackSlot()) drawSlot(graphics, left + 26, top + 44);

        // Companion storage: three rows plus its hotbar.
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, left + 62 + col * SLOT_SIZE, top + 26 + row * SLOT_SIZE);
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

        graphics.fill(left + 52, top + 22, left + 53, top + 104, 0xFF51483C);
        graphics.fill(left + 6, top + 126, left + this.imageWidth - 6, top + 127, 0xFF51483C);
        CompanionUiTheme.drawSectionHeader(graphics, this.font, "GEAR",
                left + 8, top + 108, 40);
        CompanionUiTheme.drawSectionHeader(graphics, this.font, "COMPANION STORAGE",
                left + 62, top + 108, 166);
        CompanionUiTheme.drawSectionHeader(graphics, this.font, "YOUR INVENTORY",
                left + 8, top + 132, 166);

        // Recognizable vanilla item silhouettes identify each empty equipment slot.
        ItemStack[] equipmentIcons = {
                new ItemStack(Items.IRON_HELMET),
                new ItemStack(Items.IRON_CHESTPLATE),
                new ItemStack(Items.IRON_LEGGINGS),
                new ItemStack(Items.IRON_BOOTS),
                new ItemStack(Items.IRON_SWORD),
                new ItemStack(Items.SHIELD)
        };
        int[][] equipmentPositions = {{8, 26}, {8, 44}, {8, 62}, {8, 80}, {26, 26}, {26, 62}};
        for (int i = 0; i < equipmentPositions.length; i++) {
            int sx = left + equipmentPositions[i][0];
            int sy = top + equipmentPositions[i][1];
            if (!this.menu.getSlot(i).hasItem()) {
                graphics.renderItem(equipmentIcons[i], sx + 1, sy + 1);
                graphics.fill(sx + 2, sy + 15, sx + 16, sy + 16, 0x88777D86);
            }
        }
        if (this.menu.hasCuriosBackSlot() && !this.menu.getSlot(6).hasItem()) {
            graphics.renderItem(new ItemStack(Items.CHEST), left + 27, top + 45);
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
        int slot = -1;
        String description = null;
        if (x >= 8 && x < 26 && y >= 26 && y < 44) {
            slot = 0; description = "Helmet slot — head armor";
        } else if (x >= 8 && x < 26 && y >= 44 && y < 62) {
            slot = 1; description = "Chest slot — chest armor";
        } else if (x >= 8 && x < 26 && y >= 62 && y < 80) {
            slot = 2; description = "Leg slot — leg armor";
        } else if (x >= 8 && x < 26 && y >= 80 && y < 98) {
            slot = 3; description = "Boot slot — foot armor";
        } else if (x >= 26 && x < 44 && y >= 26 && y < 44) {
            slot = 4; description = "Main hand — weapon or utility item";
        } else if (x >= 26 && x < 44 && y >= 62 && y < 80) {
            slot = 5; description = "Off hand — shield or utility item";
        }
        if (this.menu.hasCuriosBackSlot() && x >= 26 && x < 44 && y >= 44 && y < 62) {
            return this.menu.getSlot(6).hasItem() ? null : "Back slot — equip a Curios-compatible backpack";
        }
        // Preserve vanilla item tooltips whenever the slot contains an item.
        return slot >= 0 && !this.menu.getSlot(slot).hasItem() ? description : null;
    }
}
