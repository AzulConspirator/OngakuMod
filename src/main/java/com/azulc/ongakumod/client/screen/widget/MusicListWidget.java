package com.azulc.ongakumod.client.screen.widget;

import java.util.List;

import com.azulc.ongakumod.client.screen.AutoplayScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class MusicListWidget extends ObjectSelectionList<MusicListWidget.MusicEntry> {
    private final AutoplayScreen screen;

    public MusicListWidget(AutoplayScreen screen, int width, int height, int top, int itemHeight) {
        // In 1.21.1, the constructor is (Minecraft, width, height, top, itemHeight)
        super(screen.getMinecraft(), width, height, top, itemHeight);
        this.screen = screen;
    }

    public void refreshList(List<ItemStack> discs) {
        this.clearEntries();
        for (int i = 0; i < discs.size(); i++) {
            this.addEntry(new MusicEntry(discs.get(i), i));
        }
    }

    @Override
    public int getRowWidth() {
        return 140; // Adjust to fit your GUI background
    }

    protected boolean isSelectedItem(int index) {
        return this.getSelected() != null && this.getSelected().index == index;
    }

    // This is the individual row in your scroll list
    public class MusicEntry extends ObjectSelectionList.Entry<MusicEntry> {
        public final ItemStack disc;
        public final int index;

        public MusicEntry(ItemStack disc, int index) {
            this.disc = disc;
            this.index = index;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            graphics.renderFakeItem(disc, x + 2, y + 1);
            graphics.drawString(Minecraft.getInstance().font, disc.getHoverName(), x + 22, y + 5, 0xFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            screen.setSelectedDisc(this.index);
            return true;
        }

        @Override
        public Component getNarration() {
            return disc.getHoverName();
        }
    }
}