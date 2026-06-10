package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.network.TerminalActionPayload;
import com.azulc.ongakumod.util.TerminalControlHandler;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> 
{    
    public static final ResourceLocation BUTTON_ICONS = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    ControllerSnapshot snapshot;

    public TerminalScreen(TerminalMenu menu, Inventory inv, Component title) 
    {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 120; 
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    protected void init()
    {
        super.init();
        int startX = this.leftPos + ((this.imageWidth - 62) / 2);
        int startY = this.topPos + 85;
        int spacing = 21;
        this.addRenderableWidget(
            new TerminalIconButton(startX,startY,2,
                Component.translatable("general.ongakumod.stop"),() -> false, b -> sendAction( TerminalControlHandler.ACTION_STOP,0,this.menu.IsblockMode()))
        );
        this.addRenderableWidget(
            new TerminalIconButton(startX + spacing,startY,0,
                Component.literal(Component.translatable("general.ongakumod.play").getString()+" / "+Component.translatable("general.ongakumod.skip").getString()),() -> false, b -> sendAction(TerminalControlHandler.ACTION_PLAY_TRACK,this.menu.getSnapshot().playlistIndex(),this.menu.IsblockMode()))
        );
        this.addRenderableWidget(
            new TerminalIconButton(startX + (spacing * 2),startY,3,
                Component.translatable("general.ongakumod.autoplay"),() -> this.menu.getSnapshot() != null&& this.menu.getSnapshot().autoplay(), b -> sendAction(TerminalControlHandler.ACTION_TOGGLE_AP,0,this.menu.IsblockMode())
            )
        );
    }
    private void sendAction(int action, int index, boolean isBlockMode)
    {
        this.snapshot = this.menu.getSnapshot();
        BlockPos targetPos = this.snapshot != null ? this.snapshot.pos() : BlockPos.ZERO;
        Optional<BlockPos> blockPosOpt = Optional.ofNullable(this.menu.getTerminalBlockPos()).flatMap(opt -> opt);
        PacketDistributor.sendToServer(new TerminalActionPayload(this.menu.getNetworkId(),targetPos,this.menu.IsControllerLoaded(),action, index, isBlockMode, blockPosOpt));
    }   

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Suppress default inventory layout strings
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Flat UI panels background fills
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66000000);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0x66FFFFFF);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66FFFFFF);

        this.snapshot = this.menu.getSnapshot();
        if (this.snapshot == null) {
            graphics.drawCenteredString(this.font,(Component.translatable("terminal.ongakumod.pendingconnection").getString()), this.leftPos + (this.imageWidth / 2), this.topPos + (this.imageHeight / 2) - 4, 0xFFAAAAAA);
            return;
        }

        graphics.fill(this.leftPos + 10, this.topPos + 10, this.leftPos + 16, this.topPos + 16, 0xFF55FF55);

        int textY = this.topPos + 32;
        int lineSpacing = 12;

        if(this.snapshot.currentDisc() != null && !this.snapshot.currentDisc().isEmpty())
        {
            List<Component> discName = this.snapshot.currentDisc().getTooltipLines(TooltipContext.of(this.minecraft.level),this.minecraft.player,TooltipFlag.Default.NORMAL);
            if (discName.size() >= 2) 
            {
                String fullText = discName.get(1).getString();
                String[] parts = fullText.split(" - ");
                if (parts.length == 2) 
                {
                    graphics.drawString(this.font, parts[1], this.leftPos + 15, textY, 0xFFFFFFFF);
                    graphics.drawString(this.font, parts[0], this.leftPos + 15, textY+ lineSpacing, 0xFFAAAAAA);
                }
                else 
                {
                    graphics.drawString(this.font, fullText, this.leftPos + 15, textY, 0xFFFFFFFF);
                }
            }
        }
        else
        {
            graphics.drawString(this.font, (Component.translatable("general.ongakumod.nodisc").getString()) , this.leftPos + 15, textY, 0xFFE0E0E0, false);
        }
        boolean isPhysicallyLinked = this.menu.IsControllerLoaded(); 
        String mode = isPhysicallyLinked ? (Component.translatable("terminal.ongakumod.direct_connection").getString()) : (Component.translatable("terminal.ongakumod.remote_connection").getString());
        int modeColor = isPhysicallyLinked ? 0xFF55FF55 : 0xFFFFAA55;
        graphics.drawString(this.font,mode,this.leftPos + 15,textY + (lineSpacing * 2),modeColor,false);
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