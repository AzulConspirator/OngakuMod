package com.azulc.ongakumod.client.screen;

import java.util.List;
import java.util.Optional;

import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.client.screen.widget.MusicListWidget;
import com.azulc.ongakumod.container.AutoplayMenu;
import com.azulc.ongakumod.network.ManagePlaylistPayload;
import com.azulc.ongakumod.util.UIHelper;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;


public class AutoplayScreen extends AbstractContainerScreen<AutoplayMenu> 
{
    private MusicListWidget musicList;
    private long lastActionTime = 0;

    public AutoplayScreen(AutoplayMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        // FIX: Tell the parent class our exact dimensions immediately
        this.imageWidth = 240;
        this.imageHeight = 160;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Leave this completely empty to stop Vanilla from drawing default titles
    }

    // Add this method to AutoplayScreen.java
    public void refreshLivePlaylist(List<ItemStack> newDiscs) {
        // 1. Update the menu's internal memory
        this.menu.getSyncedDiscs().clear();
        this.menu.getSyncedDiscs().addAll(newDiscs);
        
        // 2. Force the widget to redraw the rows
        if (this.musicList != null) {
            this.musicList.refreshList(newDiscs);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }
    
    public static List<Component> getDiscDescription(ItemStack stack, ClientLevel level,LocalPlayer player,TooltipFlag Tooltip) 
    {
        return stack.getTooltipLines(Item.TooltipContext.of(level),  player,Tooltip) ;
    }
    
    @Override
    protected void init() 
    {
        super.init();
        // Right Pane: Playlist (Starts 90px in)
        this.musicList = new MusicListWidget(this, 140, 140, this.topPos + 10, 20);
        this.musicList.setRectangle(140, 140, this.leftPos + 95, this.topPos + 10);
        this.addRenderableWidget(this.musicList);
        this.musicList.refreshList(this.menu.getSyncedDiscs());

        // Left Pane: Custom Flat Buttons
        int startX = this.leftPos + 3;
        int startY = this.topPos + 130;
        int spacing = 21;
        int iconSize = 20;
        // 1. STOP
        this.addRenderableWidget(
            new UIHelper.IconButton(startX, startY, iconSize, iconSize, UIHelper.ICON_STOP, 
            Component.translatable("general.ongakumod.stop"), (b) -> {
                if (Util.getMillis() - this.lastActionTime < UIHelper.COOLDOWN_MS) return;
                this.lastActionTime = Util.getMillis();
                PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(this.menu.getBlockPos()),Optional.empty(), Optional.empty(), ManagePlaylistPayload.Action.STOP,Optional.empty()));
                this.musicList.refreshList(this.menu.getSyncedDiscs());
            }).withCooldown(() -> UIHelper.cooldownProgress(this.lastActionTime)));

        // 2. PLAY
        this.addRenderableWidget(
            new UIHelper.IconButton(startX + spacing, startY, iconSize, iconSize, UIHelper.ICON_PLAY, 
            Component.translatable("general.ongakumod.play"), (b) -> {
            if (Util.getMillis() - this.lastActionTime < UIHelper.COOLDOWN_MS) return;
            this.lastActionTime = Util.getMillis();
            MusicListWidget.MusicEntry selected = this.musicList.getSelected();
            if (selected != null) {
                this.setSelectedDisc(selected.index); 
                this.musicList.refreshList(this.menu.getSyncedDiscs());
            }
        }).withCooldown(() -> UIHelper.cooldownProgress(this.lastActionTime)));
        // 3. SKIP
        this.addRenderableWidget(
            new UIHelper.IconButton(startX + (spacing * 2), startY, iconSize, iconSize, UIHelper.ICON_SKIP, 
            Component.translatable("general.ongakumod.skip"), (b) -> {
            if (Util.getMillis() - this.lastActionTime < UIHelper.COOLDOWN_MS) return;
            this.lastActionTime = Util.getMillis();
            PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(this.menu.getBlockPos()),Optional.empty(), Optional.empty(), ManagePlaylistPayload.Action.SKIP,Optional.empty()));
            this.musicList.refreshList(this.menu.getSyncedDiscs());
        }).withCooldown(() -> UIHelper.cooldownProgress(this.lastActionTime)));

        // 4. AUTOPLAY (Indicator in corner)
        this.addRenderableWidget(new UIHelper.IconButton(startX + (spacing * 3), startY, iconSize, iconSize, UIHelper.ICON_AUTOPLAY, () -> this.menu.getData().get(3) == 1, 
            Component.translatable("general.ongakumod.autoplay"), (b) -> {
            if (Util.getMillis() - this.lastActionTime < UIHelper.COOLDOWN_MS) return;
            this.lastActionTime = Util.getMillis();
            PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(this.menu.getBlockPos()),Optional.empty(), Optional.empty(), ManagePlaylistPayload.Action.TOGGLE_AUTOPLAY,Optional.empty()));
            this.musicList.refreshList(this.menu.getSyncedDiscs());
        }).withCooldown(() -> UIHelper.cooldownProgress(this.lastActionTime)));
    }

    public AutoplayMenu getMenu() 
    {
        return this.menu;
    }

    public void setSelectedDisc(int index) 
    {
        PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(this.menu.getBlockPos()),Optional.empty(), Optional.empty(), ManagePlaylistPayload.Action.PLAY,Optional.of(index)));         
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) 
    {
        int leftPaneWidth = 90;
        int rightPaneWidth = this.imageWidth - leftPaneWidth; // 240 - 90 = 150
        // Left Pane (Player)
        graphics.blitSprite(OngakuModClient.BG_SPRITE,this.leftPos, this.topPos, leftPaneWidth, this.imageHeight);
        // Right Pane (List)
         graphics.blitSprite(OngakuModClient.BG_SPRITE,this.leftPos + 90, this.topPos, rightPaneWidth,this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) 
    {
        super.renderBackground(graphics, mouseX, mouseY, partialTick); 
        this.renderBg(graphics, partialTick, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick); 
        // 1. Status Indicator
        int jukeStatus = this.menu.getData().get(2);
        int indicatorColor = switch (jukeStatus) {
            case -1 -> 0;
            case 0 -> 2;
            case 1 -> 4;
            case 2 -> 6;
            default -> 0;
        };
        graphics.blit(OngakuModClient.BOX_STATUS, this.leftPos + 10, this.topPos + 10, 8, 8, (float) 0, (float) indicatorColor, 2, 2, 2, 8);
        graphics.drawString(this.font, Component.translatable("block.ongakumod.autoplay_controller"), this.leftPos + 22, this.topPos + 10, 0xFFFFFFFF, false);
        int rawPlayingIndex = this.menu.getData().get(1); 
        List<ItemStack> masterList = this.menu.getSyncedDiscs();

        int maxTextWidth = 80;
        int textCenterX = this.leftPos + 45;
        int textViewportX = textCenterX - (maxTextWidth / 2);


        if (rawPlayingIndex >= 0 && rawPlayingIndex < masterList.size()) 
        {
            ItemStack playingStack = masterList.get(rawPlayingIndex);
            
            if (!playingStack.isEmpty()) 
            {
                // Render Timeline
                int elapsed = this.menu.getData().get(4);
                int total = this.menu.getData().get(5);
                if (total > 0) 
                {
                    elapsed = elapsed >= total? total : elapsed;
                    float progress = Math.min(1.0f, (float) elapsed / total);
                    int barX = this.leftPos + 10;
                    int barY = this.topPos + 115;
                    int barWidth = 70;
                    int barHeight = 4;

                    graphics.blit(OngakuModClient.PROGRESS_BAR, barX, barY, 0, 4, barWidth, barHeight, 70, 8);
                    int filledWidth = (int) (barWidth * progress);
                    if (filledWidth > 0) {
                        graphics.blit(OngakuModClient.PROGRESS_BAR, barX, barY, 0, 0, filledWidth, barHeight, 70, 8);
                    }
                    
                    String timeStr = String.format("%d:%02d / %d:%02d", (elapsed/20)/60, (elapsed/20)%60, (total/20)/60, (total/20)%60);
                    graphics.pose().pushPose();
                    graphics.pose().translate(barX, barY + 6, 0);
                    graphics.pose().scale(0.5f, 0.5f, 1.0f);
                    graphics.drawString(this.font, timeStr, 0, 0, 0xFFAAAAAA, false);
                    graphics.pose().popPose();
                }

                graphics.renderItem(playingStack, this.leftPos + 37, this.topPos + 40);
                List<Component> tooltip = getDiscDescription(playingStack, this.minecraft.level, this.minecraft.player, TooltipFlag.Default.NORMAL);
                if (tooltip.size() >= 2) 
                {
                    String fullText = tooltip.get(1).getString();
                    String[] parts = fullText.split(" - ");
                    if (parts.length == 2) 
                    {
                        UIHelper.drawScrollingText(graphics, this.font, parts[1], textViewportX, this.topPos + 65, maxTextWidth, 0xFFFFFFFF,true);
                        UIHelper.drawScrollingText(graphics, this.font, parts[0], textViewportX, this.topPos + 75, maxTextWidth, 0xFFAAAAAA,true);
                    }
                    else 
                    {
                        UIHelper.drawScrollingText(graphics, this.font, fullText, textViewportX, this.topPos + 65, maxTextWidth, 0xFFFFFFFF,true);
                    }

                }
            }
            else 
            {
                graphics.drawCenteredString(this.font, Component.translatable("general.ongakumod.nodisc").getString(), this.leftPos + 45, this.topPos + 65, 0xFFAAAAAA);
            }
        }
        else 
        {
            graphics.drawCenteredString(this.font, Component.translatable("general.ongakumod.nodisc").getString(), this.leftPos + 45, this.topPos + 65, 0xFFAAAAAA);
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.musicList != null && this.musicList.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.musicList != null && this.musicList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}