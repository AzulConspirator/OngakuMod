package com.azulc.ongakumod.client.screen.widget;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.network.ManagePlaylistPayload;
import com.azulc.ongakumod.util.LinkHelper;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentity;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentityHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

public class MusicListWidget extends ObjectSelectionList<MusicListWidget.MusicEntry>
{
    private final AutoplayScreen screen;
    private long lastClickTime = 0;
    public record CollapsedMusicEntry(ItemStack stack, int count, int originalIndex, DiscIdentity identity) {}

    public MusicListWidget(AutoplayScreen screen, int width, int height, int top, int itemHeight)
    {
        super(screen.getMinecraft(), width, height, top, itemHeight);
        this.screen = screen;
    }

    public boolean checkAndUseCooldown() {
        long now = net.minecraft.Util.getMillis();
        if (now - this.lastClickTime > 250) { // 250ms cooldown
            this.lastClickTime = now;
            return true;
        }
        return false;
    }

    @Override
    protected void renderSelection(GuiGraphics graphics, int top, int width, int height, int outerColor, int innerColor) {
        // Suppress vanilla outline.
    }

    public void refreshList(List<ItemStack> discs) {
    MusicEntry lastSelected = this.getSelected();
    // Track the absolute original index of the selected row, not just its object reference
    int savedOriginalIndex = (lastSelected != null) ? lastSelected.index : -1;
    
    this.clearEntries();

    List<MusicEntry> finalEntries = new java.util.ArrayList<>();
    Map<DiscIdentity, CollapsedMusicEntry> bundledVanilla = new java.util.LinkedHashMap<>();

    for (int i = 0; i < discs.size(); i++) {
        ItemStack stack = discs.get(i);
        if (stack.isEmpty()) continue;

        // Obtain your new identity object
        DiscIdentity identity = DiscIdentityHelper.get(stack); 
        boolean isEtchedMusic = LinkHelper.hasComponentByString(stack, "etched:music");

        if (isEtchedMusic) {
            // Etched discs bypass bundling entirely; keep their true absolute list index
            finalEntries.add(new MusicEntry(stack, i, 1,identity));
        } else {
            // Standard records compile into groups based on identity
            if (bundledVanilla.containsKey(identity)) {
                CollapsedMusicEntry existing = bundledVanilla.get(identity);
                bundledVanilla.put(identity, new CollapsedMusicEntry(stack, existing.count() + 1, existing.originalIndex(),identity));
            } else {
                bundledVanilla.put(identity, new CollapsedMusicEntry(stack, 1, i,identity));
            }
        }
    }

    // Append standard compiled groups
    for (CollapsedMusicEntry bundled : bundledVanilla.values()) {
        finalEntries.add(new MusicEntry(bundled.stack(), bundled.originalIndex(), bundled.count(),bundled.identity()));
    }

    // Populate the widget and re-evaluate selection state cleanly
    for (MusicEntry entry : finalEntries) {
        this.addEntry(entry);
        
        // Exact position matching prevents duplicate identity snapping
        if (entry.index == savedOriginalIndex) {
            this.setSelected(entry);
        }
    }
}

    @Override
    public int getRowWidth() {
        return 140;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getX() + this.width + 10;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX <= this.getX() + this.width + 30 &&
               mouseY >= this.getY() && mouseY <= this.getY() + this.height;
    }

    protected boolean isSelectedItem(int index) {
        return this.getSelected() != null && this.getSelected().index == index;
    }

    public static List<Component> getDiscDescription(ItemStack stack, ClientLevel level, LocalPlayer player, TooltipFlag tooltip)
    {
        return stack.getTooltipLines(Item.TooltipContext.of(level), player, tooltip);
    }

    public class MusicEntry extends ObjectSelectionList.Entry<MusicEntry>
    {
        public final ItemStack disc;
        public final int index;
        public final int count;
        public final DiscIdentity identity;

        public MusicEntry(ItemStack disc, int index, int count, DiscIdentity identity)
        {
            this.disc = disc;
            this.index = index;
            this.count = count;
            this.identity = identity;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isHovered, float partialTick)
        {
            int rowWidth = getRowWidth();
            int bgRight = x + rowWidth - 4;
            int bgBottom = y + height - 1;

            boolean isExcluded = screen.getMenu().getBlockEntity().isExcluded(this.disc);
            boolean isSelected = MusicListWidget.this.getSelected() == this;

            int mainColor = isExcluded ? 0xFF666666 : 0xFFFFFFFF;
            int subColor = isExcluded ? 0xFF444444 : 0xFFAAAAAA;


            int playingIndexFromServer = screen.getMenu().getData().get(1); 
            int jukeboxStatus = screen.getMenu().getData().get(2); // 1 = playing, 0 = idle

            boolean isNowPlaying = false;
            if (jukeboxStatus == 1 && playingIndexFromServer == this.index) {
                isNowPlaying = true;
            }

            // 2. Render visual highlights
            if (isNowPlaying) {
                graphics.fill(x, y, bgRight, bgBottom, 0x4455FF55);
                graphics.fill(x, y, x + 2, bgBottom, 0xFF55FF55);
            } else if (isSelected) {
                graphics.fill(x, y, bgRight, bgBottom, 0x33FFFFFF);
                graphics.fill(x, y, x + 2, bgBottom, 0xFFFFFFFF);
            } else if (isHovered) {
                graphics.fill(x, y, bgRight, bgBottom, 0x22FFFFFF);
            }
            if (isExcluded) {
                graphics.setColor(0.3f, 0.3f, 0.3f, 1.0f);
            }
            graphics.renderFakeItem(disc, x + 4, y);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);

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

                    graphics.pose().pushPose();
                    graphics.pose().translate(textX, y + 2, 0);
                    graphics.pose().scale(0.8f, 0.8f, 1.0f);
                    graphics.drawString(Minecraft.getInstance().font, songTitle + countText, 0, 0, mainColor, false);
                    graphics.pose().popPose();

                    graphics.pose().pushPose();
                    graphics.pose().translate(textX, y + 10, 0);
                    graphics.pose().scale(0.6f, 0.6f, 1.0f);
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

            if (isHovered)
            {
                int rightEdge = x + rowWidth - 10;
                int iconY = y + 2;

                renderScaledIcon(graphics, rightEdge - 42, iconY, 4);
                renderScaledIcon(graphics, rightEdge - 27, iconY, 5);
                renderScaledIcon(graphics, rightEdge - 12, iconY, isExcluded ? 7 : 6);
            }
        }

        private void renderScaledIcon(GuiGraphics graphics, int x, int y, int iconIndex)
        {
            graphics.blit(AutoplayScreen.BUTTON_ICONS, x, y, 12, 12, iconIndex * 16, 0, 16, 16, 128, 16);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            int listIndex = MusicListWidget.this.children().indexOf(this);
            if (listIndex == -1) return false;
            int rowTop = MusicListWidget.this.getRowTop(listIndex);
            int rowLeft = MusicListWidget.this.getRowLeft();
            int rightEdge = MusicListWidget.this.getRowLeft() + MusicListWidget.this.getRowWidth() - 10;

            if (mouseY < rowTop || mouseY > rowTop + MusicListWidget.this.itemHeight) return false;

            BlockPos pos = screen.getMenu().getBlockPos();

            if (mouseX >= rightEdge - 12 && mouseX <= rightEdge)
            {
                if (MusicListWidget.this.checkAndUseCooldown())sendAction(pos, ManagePlaylistPayload.Action.EXCLUDE);
                return true;
            }
            else if (mouseX >= rightEdge - 27 && mouseX <= rightEdge - 15)
            {
                if (MusicListWidget.this.checkAndUseCooldown())sendAction(pos, ManagePlaylistPayload.Action.MOVE_DOWN);
                return true;
            }
            else if (mouseX >= rightEdge - 42 && mouseX <= rightEdge - 30)
            {
                if (MusicListWidget.this.checkAndUseCooldown())sendAction(pos, ManagePlaylistPayload.Action.MOVE_UP);
                return true;
            }

            if (mouseX >= rowLeft && mouseX < rightEdge - 45)
            {
                MusicListWidget.this.setSelected(this);
                int jukeStatus = screen.getMenu().getData().get(2);
                if (jukeStatus == 1)
                {
                    if (MusicListWidget.this.checkAndUseCooldown()) screen.setSelectedDisc(this.index);
                }
                return true;
            }

            return false;
        }


        private void sendAction(BlockPos pos, ManagePlaylistPayload.Action action) {
             PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(pos),Optional.empty() ,Optional.of(this.disc), action, Optional.empty()));
        }

        @Override
        public Component getNarration()
        {
            return disc.getHoverName();
        }
    }
}  