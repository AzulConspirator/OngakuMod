package com.azulc.ongakumod.client.screen;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.client.screen.widget.MusicListWidget;
import com.azulc.ongakumod.container.AutoplayMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

    import net.neoforged.neoforge.network.PacketDistributor;
import com.azulc.ongakumod.network.PlayDiscPayload;
import com.azulc.ongakumod.network.StopDiscPayload;

public class AutoplayScreen extends AbstractContainerScreen<AutoplayMenu> 
{
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OngakuMod.MODID, "textures/gui/controller.png");
    private MusicListWidget musicList;
    public AutoplayScreen(AutoplayMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();
        this.musicList = new MusicListWidget(this, 150, 100, this.topPos + 30, 20);
        this.musicList.setX(this.leftPos + 10);
        this.musicList.setY(this.topPos + 30);
        
        this.addRenderableWidget(this.musicList);
        this.musicList.refreshList(this.menu.getSyncedDiscs());

        // Add Stop Button
        this.addRenderableWidget(Button.builder(Component.literal("Stop"), (button) -> {
            PacketDistributor.sendToServer(new StopDiscPayload(this.menu.getBlockPos()));
        }).bounds(this.leftPos + 50, this.topPos + 140, 40, 20).build());
        // Add Play (Replay) Button
        this.addRenderableWidget(Button.builder(Component.literal("Play"), (button) -> {
        // If a disc is selected in the list, play it
        if (this.musicList.getSelected() != null) {
            this.setSelectedDisc(this.musicList.getSelected().index);
        }}).bounds(this.leftPos + 95, this.topPos + 140, 40, 20).build());
    }

    public void setSelectedDisc(int index) {
        // Local feedback
        BlockPos pos = this.menu.getBlockPos();
        PacketDistributor.sendToServer(new PlayDiscPayload(pos, index));
        this.minecraft.player.displayClientMessage(
            Component.literal("Attempting to play disc from slot: " + index), 
            true
        );
    }

     @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 1. Jukebox Status
        boolean hasJukebox = this.menu.getData().get(2) == 1;
        graphics.drawString(this.font, hasJukebox ? "§2JUKEBOX: OK" : "§4JUKEBOX: MISSING", 8, 6, 0xFFFFFF, false);
        int status = this.menu.getData().get(2);
        String statusText = switch(status) {
            case 1 -> "STATUS: Playing Music";
            case 0 -> "STATUS: Jukebox Empty";
            default -> "STATUS: Jukebox Missing";
        };
        graphics.drawString(this.font, statusText, 10, 150, status == 1 ? 0x00FF00 : 0xFF0000, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick); // 1.21.1 specific
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

}