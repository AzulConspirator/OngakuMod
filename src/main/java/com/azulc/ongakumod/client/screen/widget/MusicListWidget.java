package com.azulc.ongakumod.client.screen.widget;

import java.util.List;

import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.network.ManagePlaylistPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

public class MusicListWidget extends ObjectSelectionList<MusicListWidget.MusicEntry> 
{
    private final AutoplayScreen screen;
    public record CollapsedMusicEntry(ItemStack stack, int count, int originalIndex) {}
    
    public MusicListWidget(AutoplayScreen screen, int width, int height, int top, int itemHeight) 
    {
        super(screen.getMinecraft(), width, height, top, itemHeight);
        this.screen = screen;
    }

    @Override
    protected void renderSelection(GuiGraphics graphics, int top, int width, int height, int outerColor, int innerColor) {
        // Leave empty to suppress vanilla outline.
    }
    
    public void refreshList(List<ItemStack> discs) 
    {
       // 1. Remember what was selected by its index or Item type
        MusicEntry lastSelected = this.getSelected();
        int savedIndex = (lastSelected != null) ? lastSelected.index : -1;

        this.clearEntries();
        // 1. Group the discs
        java.util.Map<net.minecraft.world.item.Item, CollapsedMusicEntry> groupedDiscs = new java.util.LinkedHashMap<>();

        for (int i = 0; i < discs.size(); i++) 
        {
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
        for (CollapsedMusicEntry collapsed : groupedDiscs.values()) 
        {
            MusicEntry newEntry = new MusicEntry(collapsed.stack(), collapsed.originalIndex(), collapsed.count());
            this.addEntry(newEntry);
            // 2. Restore the selection if it matches the saved index
            if (collapsed.originalIndex() == savedIndex) {
                this.setSelected(newEntry);
            }
        }
    }

    @Override
    public int getRowWidth() {
        return 140; // Adjust to fit your GUI background
    }
    @Override
    protected int getScrollbarPosition() {
        // This pushes the scrollbar 10 pixels to the right of the list's edge
        return this.getX() + this.width + 10; 
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Expand the "active" area to the right by 30px to catch scrollbar drags
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width + 30 && 
            mouseY >= this.getY() && mouseY <= this.getY() + this.height;
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

        public MusicEntry(ItemStack _disc, int _index, int _count) {
            this.disc = _disc;
            this.index = _index;
            this.count = _count;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick) {
            // 1. Calculate Bounds to prevent overflow
            // We use getRowWidth() instead of the 'width' parameter to ensure it stays within the visible pane
            int rowWidth = getRowWidth();
            int bgRight = x + rowWidth - 4;
            int bgBottom = y + height - 1 ; // Subtracting 2 for equal padding/spacing between entries
            boolean isExcluded = screen.getMenu().getBlockEntity().isItemExcluded(this.disc.getItem());
            boolean isSelected = MusicListWidget.this.getSelected() == this;
            int mainColor = isExcluded ? 0xFF666666 : 0xFFFFFFFF;
            int subColor = isExcluded ? 0xFF444444 : 0xFFAAAAAA;
            // 2. Highlight Logic
            int currentPlayingVisualIndex = screen.getMenu().getData().get(1);
            boolean isNowPlaying = false;
            
            if (currentPlayingVisualIndex >= 0 && currentPlayingVisualIndex < MusicListWidget.this.children().size()) 
            {
                MusicEntry playingEntry = MusicListWidget.this.children().get(currentPlayingVisualIndex);
                if (ItemStack.isSameItem(this.disc, playingEntry.disc)) 
                {
                    isNowPlaying = true;
                }
            }

            // Draw Background (using calculated rowWidth to prevent overflow)
            if (isNowPlaying) {
                graphics.fill(x, y, bgRight, bgBottom, 0x4455FF55); // Subtle Green
                graphics.fill(x, y, x + 2, bgBottom, 0xFF55FF55);   // Left Accent bar
            }else if (isSelected) {
                graphics.fill(x, y, bgRight, bgBottom, 0x33FFFFFF); // Light Blue/White selection
                graphics.fill(x, y, x + 2, bgBottom, 0xFFFFFFFF);   // Left bar
            } else if (isHovered) {
                graphics.fill(x, y, bgRight, bgBottom, 0x22FFFFFF); // Subtle Hover
            }
            // 3. Render Item
            // 2. DIM THE ITEM ICON
            if (isExcluded) {
                graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f); // Set rendering to 30% brightness
            }
            graphics.renderFakeItem(disc, x + 4, y);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            // 4. Render Text with Scaling
            List<Component> tooltip = getDiscDescription(disc, Minecraft.getInstance().level, Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
            int textX = x + 26;
            if (tooltip.size() >= 2) 
            {
                String fullText = tooltip.get(1).getString();
                String[] parts = fullText.split(" - ");
                if (parts.length == 2)
                {
                    String songTitle = parts[1];
                    String artistName = parts[0];
                    String countText = this.count > 1 ? " x" + this.count : "";
                    // Main Title (Normal Size)
                    graphics.pose().pushPose();
                    graphics.pose().translate(textX, y + 2, 0);
                    graphics.pose().scale(0.8f, 0.8f, 1.0f);
                    graphics.drawString(Minecraft.getInstance().font, songTitle + countText, 0,0, mainColor, false);
                    graphics.pose().popPose();
                    // Artist Name (Scaled Down)
                    graphics.pose().pushPose();
                    graphics.pose().translate(textX, y + 10, 0);
                    graphics.pose().scale(0.6f, 0.6f, 1.0f); // Scale to 80%
                    graphics.drawString(Minecraft.getInstance().font, artistName, 0, 0, subColor, false);
                    graphics.pose().popPose();
                } 
                else 
                {
                    graphics.drawString(Minecraft.getInstance().font, fullText, textX, y + 6, mainColor, false);
                }
            } 
            else 
            {
                graphics.drawString(Minecraft.getInstance().font, disc.getHoverName(), textX, y + 6, mainColor, false);
            }
        // 3. Render Shrunken Control Buttons (12x12 icons)
            if (isHovered) {
                int rightEdge = x + rowWidth - 10;
                int iconY = y+2; // Centered vertically in a 20px row
                
                // UP (Index 4)
                renderScaledIcon(graphics, rightEdge - 42, iconY, 4);
                // DOWN (Index 5)
                renderScaledIcon(graphics, rightEdge - 27, iconY, 5);
                // EXCLUDE (Index 6/7)
                renderScaledIcon(graphics, rightEdge - 12, iconY, isExcluded ? 7 : 6);
            }
        }
        private void renderScaledIcon(GuiGraphics graphics, int x, int y, int iconIndex) {
            // Parameters: texture, destX, destY, destWidth, destHeight, sourceU, sourceV, sourceWidth, sourceHeight, texWidth, texHeight
            graphics.blit(AutoplayScreen.BUTTON_ICONS, x, y, 12, 12, iconIndex * 16, 0, 16, 16, 128, 16);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int listIndex = MusicListWidget.this.children().indexOf(this);
            if (listIndex == -1) return false;

            int rowTop = MusicListWidget.this.getRowTop(listIndex);
            int rowLeft = MusicListWidget.this.getRowLeft();
            int rightEdge = MusicListWidget.this.getRowLeft() + MusicListWidget.this.getRowWidth() - 10;

            if (mouseY < rowTop || mouseY > rowTop + MusicListWidget.this.itemHeight) return false;
            // Tightened hitboxes for the 12x12 buttons
            String registryName = BuiltInRegistries.ITEM.getKey(this.disc.getItem()).toString();
            BlockPos pos = screen.getMenu().getBlockPos();

            if (mouseX >= rightEdge - 12 && mouseX <= rightEdge) {
                sendAction(pos, registryName, ManagePlaylistPayload.Action.EXCLUDE);
                return true;
            } 
            else if (mouseX >= rightEdge - 27 && mouseX <= rightEdge - 15) {
                sendAction(pos, registryName, ManagePlaylistPayload.Action.MOVE_DOWN);
                return true;
            } 
            else if (mouseX >= rightEdge - 42 && mouseX <= rightEdge - 30) {
                sendAction(pos, registryName, ManagePlaylistPayload.Action.MOVE_UP);
                return true;
            }
            if (mouseX >= rowLeft && mouseX < rightEdge - 45) {
                MusicListWidget.this.setSelected(this);
                int jukeStatus = screen.getMenu().getData().get(2);
                if (jukeStatus == 1) {
                    screen.setSelectedDisc(this.index);
                }
                return true;
            }
            return false;
        }

        private void sendAction(BlockPos pos, String name, ManagePlaylistPayload.Action action) {
            PacketDistributor.sendToServer(new ManagePlaylistPayload(pos, name, action));
        }

        @Override
        public Component getNarration() {
            return disc.getHoverName();
        }
    }
}