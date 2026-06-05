package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.TerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

// Import your custom payload and action references
import com.azulc.ongakumod.network.ManagePlaylistPayload; 

import java.util.Optional;
import java.util.function.BooleanSupplier;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    // Shared icon atlas reference
    public static final ResourceLocation BUTTON_ICONS = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    
    public TerminalScreen(TerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 120; 
    }

    @Override
    protected void init() {
        super.init();

        // Center 4 buttons of 20px width each, spaced by 21px (Total span: 83px)
        int startX = this.leftPos + ((this.imageWidth - 83) / 2);
        int startY = this.topPos + 85;
        int spacing = 21;
        // 1. STOP (Icon Index 2)
        this.addRenderableWidget(new TerminalIconButton(startX, startY, 2, 
            Component.literal("Stop"), () -> false, (b) -> {
                // WARNING: If this menu was opened from an item, blockPos may be invalid.
                // Ensure your network payload layer can handle networkId fallbacks.
                PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.empty(), Optional.of(this.menu.getNetworkId()), "", ManagePlaylistPayload.Action.STOP, Optional.empty()));
        }));

        // 2. PLAY (Icon Index 0)
        this.addRenderableWidget(new TerminalIconButton(startX + spacing, startY, 0, 
            Component.literal("Play"), () -> false, (b) -> {
                PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.empty(), Optional.of(this.menu.getNetworkId()), "", ManagePlaylistPayload.Action.PLAY, Optional.empty()));
        }));

        // 3. SKIP (Icon Index 1)
        this.addRenderableWidget(new TerminalIconButton(startX + (spacing * 2), startY, 1, 
            Component.literal("Skip"), () -> false, (b) -> {
                PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.empty(), Optional.of(this.menu.getNetworkId()), "", ManagePlaylistPayload.Action.SKIP, Optional.empty()));
        }));

        // 4. AUTOPLAY (Icon Index 3) - Dynamically checks state from the live snapshot object
        this.addRenderableWidget(new TerminalIconButton(startX + (spacing * 3), startY, 3, 
            Component.literal("Autoplay Loop"), 
            () -> this.menu.getSnapshot() != null && this.menu.getSnapshot().autoplay(), 
            (b) -> {
                PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.empty(), Optional.of(this.menu.getNetworkId()), "", ManagePlaylistPayload.Action.TOGGLE_AUTOPLAY, Optional.empty()));
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default text placements
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Flat panels
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xAA000000);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0x66FFFFFF);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66FFFFFF);

        var snapshot = this.menu.getSnapshot();
        if (snapshot == null) {
            graphics.drawCenteredString(this.font, "Connecting to Controller...", this.leftPos + (this.imageWidth / 2), this.topPos + (this.imageHeight / 2) - 4, 0xFFAAAAAA);
            return;
        }

        // Green terminal status light
        graphics.fill(this.leftPos + 10, this.topPos + 10, this.leftPos + 16, this.topPos + 16, 0xFF55FF55);
        graphics.drawString(this.font, "Remote Vinyl Station", this.leftPos + 22, this.topPos + 9, 0xFFFFFFFF, false);

        int textY = this.topPos + 32;
        int lineSpacing = 12;

        graphics.drawString(this.font, "Disc: " + snapshot.currentDisc(), this.leftPos + 15, textY, 0xFFE0E0E0, false);
        graphics.drawString(this.font, "Track Number: #" + (snapshot.playlistIndex() + 1), this.leftPos + 15, textY + lineSpacing, 0xFFE0E0E0, false);
        
        String autoplayText = snapshot.autoplay() ? "Loop Engine: Connected" : "Loop Engine: Idle";
        int autoplayColor = snapshot.autoplay() ? 0xFF55FF55 : 0xFFFF5555;
        graphics.drawString(this.font, autoplayText, this.leftPos + 15, textY + (lineSpacing * 2), autoplayColor, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        this.renderBg(graphics, delta, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Decoupled Icon Button that relies entirely on a clean state functional predicate.
     */
    public static class TerminalIconButton extends Button {
        private final int iconIndex;
        private final BooleanSupplier activeStateSupplier;

        public TerminalIconButton(int x, int y, int iconIndex, Component tooltip, BooleanSupplier activeStateSupplier, OnPress onPress) {
            super(x, y, 20, 20, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.iconIndex = iconIndex;
            this.activeStateSupplier = activeStateSupplier;
            if (tooltip != null) {
                this.setTooltip(Tooltip.create(tooltip));
            }
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = this.isHoveredOrFocused() ? 0x66FFFFFF : 0x66000000;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // Draw Icon sequentially from atlas grid layout
            int u = this.iconIndex * 16;
            graphics.blit(BUTTON_ICONS, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2, u, 0, 16, 16, 128, 16);

            // Functional corner light execution
            if (activeStateSupplier.getAsBoolean()) {
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, 0xFF55FF55);
            }
        }
    }
}