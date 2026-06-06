package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.network.TerminalActionPayload;
import com.azulc.ongakumod.util.TerminalControlHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.BooleanSupplier;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> {
    
    public static final ResourceLocation BUTTON_ICONS = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    
    public TerminalScreen(TerminalMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 120; 
    }

    @Override
    protected void init() {
        super.init();

        int startX = this.leftPos + ((this.imageWidth - 83) / 2);
        int startY = this.topPos + 85;
        int spacing = 21;

        // 1. STOP (Action ID: 1)
        this.addRenderableWidget(new TerminalIconButton(startX, startY, 2, 
            Component.literal("Stop"), () -> false, (b) -> {
                var snapshot = this.menu.getSnapshot();
                BlockPos targetPos = snapshot != null ? snapshot.pos() : BlockPos.ZERO;
                Boolean isBlockMode = this.menu.getTerminalBlockPos() != null ? true : false;
                Optional<BlockPos> blockPosOpt = Optional.ofNullable(this.menu.getTerminalBlockPos())
                .flatMap(opt -> opt);
                PacketDistributor.sendToServer(new TerminalActionPayload(
                        this.menu.getNetworkId(),
                        targetPos,
                        TerminalControlHandler.ACTION_STOP,
                        0, // Index ignored for stop actions
                        isBlockMode,
                        blockPosOpt
                ));
        }));

        // 2. PLAY (Action ID: 0)
        this.addRenderableWidget(new TerminalIconButton(startX + spacing, startY, 0, 
            Component.literal("Play"), () -> false, (b) -> {
                var snapshot = this.menu.getSnapshot();
                BlockPos targetPos = snapshot != null ? snapshot.pos() : BlockPos.ZERO;
                int currentIndex = snapshot != null ? snapshot.playlistIndex() : 0;
                Boolean isBlockMode = this.menu.getTerminalBlockPos() != null ? true : false;
                Optional<BlockPos> blockPosOpt = Optional.ofNullable(this.menu.getTerminalBlockPos())
                .flatMap(opt -> opt);
                PacketDistributor.sendToServer(new TerminalActionPayload(
                        this.menu.getNetworkId(),
                        targetPos,
                        TerminalControlHandler.ACTION_PLAY_TRACK,
                        currentIndex,
                        isBlockMode,
                        blockPosOpt
                ));
        }));

        // 3. SKIP (Action ID: 2)
        this.addRenderableWidget(new TerminalIconButton(startX + (spacing * 2), startY, 1, 
            Component.literal("Skip"), () -> false, (b) -> {
                var snapshot = this.menu.getSnapshot();
                BlockPos targetPos = snapshot != null ? snapshot.pos() : BlockPos.ZERO;
                Boolean isBlockMode = this.menu.getTerminalBlockPos() != null ? true : false;
                Optional<BlockPos> blockPosOpt = Optional.ofNullable(this.menu.getTerminalBlockPos()).flatMap(opt -> opt);
                PacketDistributor.sendToServer(new TerminalActionPayload(
                        this.menu.getNetworkId(),
                        targetPos,
                        TerminalControlHandler.ACTION_SKIP,
                        0, // Index ignored for context skips
                        isBlockMode,
                        blockPosOpt
                ));
        }));

        // 4. AUTOPLAY (Action ID: 3)
        this.addRenderableWidget(new TerminalIconButton(startX + (spacing * 3), startY, 3, 
            Component.literal("Autoplay Loop"), 
            () -> this.menu.getSnapshot() != null && this.menu.getSnapshot().autoplay(), 
            (b) -> {
                var snapshot = this.menu.getSnapshot();
                BlockPos targetPos = snapshot != null ? snapshot.pos() : BlockPos.ZERO;
                Boolean isBlockMode = this.menu.getTerminalBlockPos() != null ? true : false;
                Optional<BlockPos> blockPosOpt = Optional.ofNullable(this.menu.getTerminalBlockPos()).flatMap(opt -> opt);
                PacketDistributor.sendToServer(new TerminalActionPayload(
                        this.menu.getNetworkId(),
                        targetPos,
                        TerminalControlHandler.ACTION_TOGGLE_AP,
                        0,
                        isBlockMode,
                        blockPosOpt
                ));
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default inventory layout strings
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Flat UI panels background fills
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xAA000000);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0x66FFFFFF);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66FFFFFF);

        var snapshot = this.menu.getSnapshot();
        if (snapshot == null) {
            graphics.drawCenteredString(this.font, "Connecting to Controller...", this.leftPos + (this.imageWidth / 2), this.topPos + (this.imageHeight / 2) - 4, 0xFFAAAAAA);
            return;
        }

        graphics.fill(this.leftPos + 10, this.topPos + 10, this.leftPos + 16, this.topPos + 16, 0xFF55FF55);
        graphics.drawString(this.font, "Remote Vinyl Station", this.leftPos + 22, this.topPos + 9, 0xFFFFFFFF, false);

        int textY = this.topPos + 32;
        int lineSpacing = 12;

        // FIX: Handle Nullable currentDisc variants gracefully instead of drawing "null"
        String discName = (snapshot.currentDisc() == null || snapshot.currentDisc().isEmpty()) ? "None" : snapshot.currentDisc();
        graphics.drawString(this.font, "Disc: " + discName, this.leftPos + 15, textY, 0xFFE0E0E0, false);
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

            int u = this.iconIndex * 16;
            graphics.blit(BUTTON_ICONS, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2, u, 0, 16, 16, 128, 16);

            if (activeStateSupplier.getAsBoolean()) {
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, 0xFF55FF55);
            }
        }
    }
}