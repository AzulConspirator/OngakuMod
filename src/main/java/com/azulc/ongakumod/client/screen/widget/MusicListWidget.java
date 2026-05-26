package com.azulc.ongakumod.client.screen.widget;

import java.util.List;

import com.azulc.ongakumod.client.screen.AutoplayScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

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

    public static List<Component> getDiscDescription(ItemStack stack, ClientLevel level,LocalPlayer player,TooltipFlag Tooltip) 
    {
        return stack.getTooltipLines(Item.TooltipContext.of(level),  player,Tooltip) ;
    }
    
    public class MusicEntry extends ObjectSelectionList.Entry<MusicEntry> 
    {
        public final ItemStack disc;
        public final int index;

        public MusicEntry(ItemStack _disc, int _index) {
            this.disc = _disc;
            this.index = _index;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            
            graphics.renderFakeItem(disc, x + 2, y + 1);
            List<Component> tooltip = getDiscDescription(disc,Minecraft.getInstance().level,Minecraft.getInstance().player,TooltipFlag.Default.NORMAL);
            // Logic: If there is a second line (the song info), use it. 
            // Otherwise, fallback to the item name.
            if (tooltip.size() >= 2) {
                String fullText = tooltip.get(1).getString();
                String[] parts = fullText.split(" - ");
                
                if (parts.length == 2) {
                    graphics.drawString(Minecraft.getInstance().font, parts[1], x + 24, y + 1, 0xFFFFFFFF, false); // Title
                    graphics.drawString(Minecraft.getInstance().font, parts[0], x + 24, y + 11, 0xFFAAAAAA, false); // Author
                } else {
                    graphics.drawString(Minecraft.getInstance().font, fullText, x + 24, y + 5, 0xFFFFFFFF, false);
                }
            } else {
                graphics.drawString(Minecraft.getInstance().font, disc.getHoverName(), x + 24, y + 5, 0xFFFFFFFF, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) 
        {
            MusicListWidget.this.setSelected(this);
            screen.setSelectedDisc(this.index);
            return true;
        }

        @Override
        public Component getNarration() {
            return disc.getHoverName();
        }
    }


}