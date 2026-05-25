package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.DiscContainer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DiscRackScreen extends AbstractContainerScreen<DiscContainer> {
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/disc_rack.png");
    private final int containerRows = 1;

    public DiscRackScreen(DiscContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + this.containerRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CONTAINER_BACKGROUND, i, j, 0, 0, this.imageWidth, this.containerRows * 18 + 17);
        guiGraphics.blit(CONTAINER_BACKGROUND, i, j + this.containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }
}