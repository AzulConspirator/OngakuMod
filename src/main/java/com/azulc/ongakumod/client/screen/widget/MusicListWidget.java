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

public class MusicListWidget extends ObjectSelectionList<MusicListWidget.MusicEntry> 
{
    private final AutoplayScreen screen;
    public record CollapsedMusicEntry(ItemStack stack, int count, int originalIndex) {}
    
    public MusicListWidget(AutoplayScreen screen, int width, int height, int top, int itemHeight) 
    {
        super(screen.getMinecraft(), width, height, top, itemHeight);
        this.screen = screen;
    }

    public void refreshList(List<ItemStack> discs) {
        this.clearEntries();
        
        // 1. Group the discs
        // Key: The Item, Value: The wrapper containing count and the first index found
        java.util.Map<net.minecraft.world.item.Item, CollapsedMusicEntry> groupedDiscs = new java.util.LinkedHashMap<>();

        for (int i = 0; i < discs.size(); i++) {
            ItemStack stack = discs.get(i);
            if (stack.isEmpty()) continue;

            net.minecraft.world.item.Item item = stack.getItem();
            if (groupedDiscs.containsKey(item)) {
                CollapsedMusicEntry existing = groupedDiscs.get(item);
                groupedDiscs.put(item, new CollapsedMusicEntry(stack, existing.count() + 1, existing.originalIndex()));
            } else {
                groupedDiscs.put(item, new CollapsedMusicEntry(stack, 1, i));
            }
        }

        // 2. Add the grouped results to the UI list
        for (CollapsedMusicEntry collapsed : groupedDiscs.values()) {
            this.addEntry(new MusicEntry(collapsed.stack(), collapsed.originalIndex(), collapsed.count()));
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
        public final int count;

        public MusicEntry(ItemStack _disc, int _index ,int _count) {
            this.disc = _disc;
            this.index = _index;
            this.count = _count;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            // Render Item
            graphics.renderFakeItem(disc, x + 2, y + 1);
            // ( Title / Author ) ...
            List<Component> tooltip = getDiscDescription(disc, Minecraft.getInstance().level, Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
            if (tooltip.size() >= 2) {
                String fullText = tooltip.get(1).getString();
                String[] parts = fullText.split(" - ");
                if (parts.length == 2) {
                    String countText = this.count > 1? " x" + this.count : "";
                    graphics.drawString(Minecraft.getInstance().font, parts[1] + countText, x + 24, y + 1, 0xFFFFFFFF, false);
                    graphics.drawString(Minecraft.getInstance().font, parts[0], x + 24, y + 11, 0xFFAAAAAA, false);
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