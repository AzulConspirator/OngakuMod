package com.azulc.ongakumod.client.screen;

import java.util.List;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.client.screen.widget.MusicListWidget;
import com.azulc.ongakumod.container.AutoplayMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.PacketDistributor;

import com.azulc.ongakumod.network.ManagePlaylistPayload;
import com.azulc.ongakumod.network.PlayDiscPayload;
import com.azulc.ongakumod.network.StopDiscPayload;

public class AutoplayScreen extends AbstractContainerScreen<AutoplayMenu> 
{
    public static final ResourceLocation BUTTON_ICONS = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    private MusicListWidget musicList;

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
        // STOP (Index 2: Stop/Pause)
        int startX = this.leftPos + 4;
        int startY = this.topPos + 130;
        int spacing = 21; // 20px button + 1px gap

        // 1. STOP
        this.addRenderableWidget(new IconButton(startX, startY, 20, 20, 2, false, 
            Component.literal("Stop"), (b) -> {
            PacketDistributor.sendToServer(new StopDiscPayload(this.menu.getBlockPos()));
        }));

        // 2. PLAY
        this.addRenderableWidget(new IconButton(startX + spacing, startY, 20, 20, 0, false, 
            Component.literal("Play"), (b) -> {
                MusicListWidget.MusicEntry selected = this.musicList.getSelected();
                if (selected != null) {
                    // This is where the actual packet is sent
                    this.setSelectedDisc(selected.index); 
                } 
/*             if (this.musicList.getSelected() != null) {
                this.setSelectedDisc(this.musicList.getSelected().index);
            } */
        }));

        // 3. SKIP
        this.addRenderableWidget(new IconButton(startX + (spacing * 2), startY, 20, 20, 1, false, 
            Component.literal("Skip"), (b) -> {
            PacketDistributor.sendToServer(new ManagePlaylistPayload(this.menu.getBlockPos(), "", ManagePlaylistPayload.Action.SKIP));
        }));

        // 4. AUTOPLAY (Indicator in corner)
        this.addRenderableWidget(new IconButton(startX + (spacing * 3), startY, 20, 20, 3, true, 
            Component.literal("Autoplay"), (b) -> {
            PacketDistributor.sendToServer(new ManagePlaylistPayload(this.menu.getBlockPos(), "", ManagePlaylistPayload.Action.TOGGLE_AUTOPLAY));
        }));
    }

    public AutoplayMenu getMenu() 
    {
        return this.menu;
    }
    
    public void setSelectedDisc(int index) 
    {
        BlockPos pos = this.menu.getBlockPos();
        PacketDistributor.sendToServer(new PlayDiscPayload(pos, index));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) 
    {
        // Left Pane (Player)
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 90, this.topPos + this.imageHeight, 0x66000000);
        // Right Pane (List)
        graphics.fill(this.leftPos + 90, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x66000000);
        // Divider
        graphics.fill(this.leftPos + 90, this.topPos, this.leftPos + 91, this.topPos + this.imageHeight, 0x66FFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick); 
        this.renderBg(graphics, partialTick, mouseX, mouseY);

        // 1. Indicator
        int jukeStatus = this.menu.getData().get(2);
        int indicatorColor = switch (jukeStatus) {
            case -1 -> 0xFFFF5555;
            case 0 -> 0xFF55FF55;
            case 1 -> 0xFF5555FF;
            default -> 0xFF888888;
        };
        graphics.fill(this.leftPos + 10, this.topPos + 10, this.leftPos + 18, this.topPos + 18, indicatorColor);
        graphics.drawString(this.font, "Sound Box", this.leftPos + 22, this.topPos + 10, 0xFFFFFFFF, false);

        // 2. Now Playing Logic
        int currentVisualIndex = this.menu.getData().get(1); // This is the Collapsed Index 
        if (currentVisualIndex >= 0 && this.musicList != null) 
        {
            // Instead of looping and comparing mismatched indices, directly grab the exact row
            if (currentVisualIndex < this.musicList.children().size()) 
            {
                MusicListWidget.MusicEntry entry = this.musicList.children().get(currentVisualIndex);
                this.musicList.setSelected(entry);
                
                // Render the big "Now Playing" icon and text
                ItemStack playingStack = entry.disc;
                graphics.renderItem(playingStack, this.leftPos + 37, this.topPos + 40);
                
                // Draw Tooltip info...
                List<Component> tooltip = getDiscDescription(playingStack, this.minecraft.level, this.minecraft.player, TooltipFlag.Default.NORMAL);
                if (tooltip.size() >= 2) 
                {
                    String fullText = tooltip.get(1).getString();
                    String[] parts = fullText.split(" - ");
                    if (parts.length == 2) 
                    {
                        graphics.drawCenteredString(this.font, parts[1], this.leftPos + 45, this.topPos + 65, 0xFFFFFFFF);
                        graphics.drawCenteredString(this.font, parts[0], this.leftPos + 45, this.topPos + 75, 0xFFAAAAAA);
                    }
                    else 
                    {
                        graphics.drawCenteredString(this.font, fullText, this.leftPos + 45, this.topPos + 65, 0xFFFFFFFF);
                    }
                }
            }
        }
        else 
        {
            graphics.drawCenteredString(this.font, "No Disc", this.leftPos + 45, this.topPos + 65, 0xFFAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partialTick); 
    }
    
    public static class IconButton extends Button {
        protected final int iconIndex;
        protected final boolean isAutoplayButton;

        public IconButton(int x, int y, int width, int height, int iconIndex, boolean isAutoplay, Component tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.iconIndex = iconIndex;
            this.isAutoplayButton = isAutoplay;
            if (tooltip != null) {
                this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(tooltip));
            }
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = this.isHoveredOrFocused() ? 0x66FFFFFF : 0x66000000;
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // Draw Icon
            int u = this.iconIndex * 16;
            graphics.blit(BUTTON_ICONS, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2, u, 0, 16, 16, 128, 16);

            // Autoplay Corner Indicator (Top Right)
            if (isAutoplayButton) {
                // Check state from menu data
                boolean enabled = ((AutoplayScreen)Minecraft.getInstance().screen).getMenu().getData().get(4) == 1;
                int color = enabled ? 0xFF55FF55 : 0xFFFF5555; // Green / Red
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, color);
            }
        }
    }
    public static class FlatButton extends Button 
    {

        public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) 
        {
            super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) 
        {
            // 0x66000000 = default semi-transparent. 0x99FFFFFF = white-ish when hovered.
            int bgColor = this.isHoveredOrFocused() ? 0x66FFFFFF : 0x66000000;
            int textColor = this.isHoveredOrFocused() ? 0xFF000000 : 0xFFFFFFFF; // Invert text on hover
            // Draw flat background
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            // Draw centered text
            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
        }
    }
}