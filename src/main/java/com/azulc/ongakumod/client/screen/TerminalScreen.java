package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.container.TerminalMenu;
import com.azulc.ongakumod.network.TerminalActionPayload;
import com.azulc.ongakumod.network.TerminalControlHandler;
import com.azulc.ongakumod.util.UIHelper;
import com.azulc.ongakumod.util.ControllerRegistry.ControllerSnapshot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class TerminalScreen extends AbstractContainerScreen<TerminalMenu> 
{    
    ControllerSnapshot snapshot;
    private long lastActionTime = 0;

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
        int iconSize = 20;
        this.addRenderableWidget(
            new UIHelper.IconButton(startX, startY, iconSize, iconSize, UIHelper.ICON_STOP, Component.translatable("general.ongakumod.stop"),b -> sendAction( TerminalControlHandler.ACTION_STOP,0,this.menu.IsblockMode()))
        );
        this.addRenderableWidget(
            new UIHelper.IconButton(startX + (spacing), startY, iconSize, iconSize, UIHelper.ICON_PLAY, Component.literal(Component.translatable("general.ongakumod.play").getString()+" / "+Component.translatable("general.ongakumod.skip").getString()),b -> sendAction(TerminalControlHandler.ACTION_PLAY_TRACK,this.menu.getSnapshot().playlistIndex(),this.menu.IsblockMode()))
        );
        this.addRenderableWidget(
            new UIHelper.IconButton(startX + (spacing * 2), startY, iconSize, iconSize, UIHelper.ICON_AUTOPLAY,() -> this.menu.getSnapshot() != null&& this.menu.getSnapshot().autoplay(), Component.translatable("general.ongakumod.autoplay"), b -> sendAction(TerminalControlHandler.ACTION_TOGGLE_AP,0,this.menu.IsblockMode()))
        );
    }
    private void sendAction(int action, int index, boolean isBlockMode)
    {
        long now = net.minecraft.Util.getMillis();
        if (now - this.lastActionTime < 250) return;
        this.lastActionTime = now;
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
        // BG
        graphics.blitSprite(OngakuModClient.BG_SPRITE,this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        //
        this.snapshot = this.menu.getSnapshot();
        if (this.snapshot == null) {
            graphics.drawCenteredString(this.font,(Component.translatable("terminal.ongakumod.pendingconnection").getString()), this.leftPos + (this.imageWidth / 2), this.topPos + (this.imageHeight / 2) - 4, 0xFFAAAAAA);
            return;
        }
 
        int textCenterX = this.leftPos + (this.imageWidth / 2);
        int textY = this.topPos + 32;
        int lineSpacing = 12;
        int maxTextWidth = this.imageWidth - 20;
 
        int indicatorColor = 0;
        if(this.snapshot.currentDisc() != null && !this.snapshot.currentDisc().isEmpty())
        {
            indicatorColor = 6;
            List<Component> discName = this.snapshot.currentDisc().getTooltipLines(TooltipContext.of(this.minecraft.level),this.minecraft.player,TooltipFlag.Default.NORMAL);
            if (discName.size() >= 2) 
            {
                String fullText = discName.get(1).getString();
                String[] parts = fullText.split(" - ");
                if (parts.length == 2) 
                {
                    String song = UIHelper.truncate(this.font, parts[1], maxTextWidth);
                    String artist = UIHelper.truncate(this.font, parts[0], maxTextWidth);
                    graphics.drawCenteredString(this.font, song, textCenterX, textY, 0xFFFFFFFF);
                    graphics.drawCenteredString(this.font, artist, textCenterX, textY + lineSpacing, 0xFFAAAAAA);
                }
                else 
                {
                    String text = UIHelper.truncate(this.font, fullText, maxTextWidth);
                    graphics.drawCenteredString(this.font, text, textCenterX, textY, 0xFFFFFFFF);
                }
            }
        }
        else
        {
            indicatorColor = 2;
            graphics.drawCenteredString(this.font, (Component.translatable("general.ongakumod.nodisc").getString()), textCenterX, textY, 0xFFE0E0E0);
        }
        graphics.blit(OngakuModClient.BOX_STATUS, this.leftPos + 10, this.topPos + 10,8,8, 0, indicatorColor,2,2, 2, 8);
        boolean isPhysicallyLinked = this.menu.IsControllerLoaded(); 
        String mode = isPhysicallyLinked ? (Component.translatable("terminal.ongakumod.direct_connection").getString()) : (Component.translatable("terminal.ongakumod.remote_connection").getString());
        int modeColor = isPhysicallyLinked ? 0xFF55FF55 : 0xFFFFAA55;
        float modeScale = 0.7f;
        int modeWidth = this.font.width(mode);
        int modeX = this.leftPos + this.imageWidth - 10 - Math.round(modeWidth * modeScale);
        int modeY = this.topPos + 11;
        graphics.pose().pushPose();
        graphics.pose().translate(modeX, modeY, 0);
        graphics.pose().scale(modeScale, modeScale, 1.0f);
        graphics.drawString(this.font, mode, 0, 0, modeColor, false);
        graphics.pose().popPose();
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        this.renderBg(graphics, delta, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, delta);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}