package com.azulc.ongakumod.client.screen.widget;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.azulc.ongakumod.OngakuMod;
import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.client.screen.AutoplayScreen;
import com.azulc.ongakumod.network.ManagePlaylistPayload;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentity;
import com.azulc.ongakumod.util.PlaylistHelper.DiscIdentityHelper;
import com.azulc.ongakumod.util.UIHelper;

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

    public void refreshList(List<ItemStack> discs)
    {
        this.clearEntries();
        Map<DiscIdentity, MusicEntry> ordered = new LinkedHashMap<>();

        for (int i = 0; i < discs.size(); i++)
        {
            ItemStack stack = discs.get(i);
            if (stack.isEmpty()) continue;

            DiscIdentity identity = DiscIdentityHelper.get(stack, BlockPos.ZERO, i);
            MusicEntry existing = ordered.get(identity);
            if (existing != null) {
                ordered.put(identity, new MusicEntry(existing.disc, existing.index, existing.count + 1, identity));
            } else {
                ordered.put(identity, new MusicEntry(stack, i, 1, identity));
            }
        }

        for (MusicEntry entry : ordered.values())
        {
            this.addEntry(entry);
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

    @Override
    protected void renderItem(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int index, int left, int top, int width, int height) {
        // Vanilla passes (itemHeight - 4) here; override with the real row height
        // so backgrounds/sprites fill the full row with no gap.
        int gapheight = this.itemHeight - 2;
        super.renderItem(guiGraphics, mouseX, mouseY, partialTick, index, left, top, width, gapheight);
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
            int SpriteWidth = rowWidth - 4;
            int SpriteHeight = height;
            int bgRight = x + SpriteWidth;
            int bgBottom = y + SpriteHeight;
 
            boolean isExcluded = screen.getMenu().getBlockEntity().excludedTracks.contains(this.identity);
            boolean isSelected = MusicListWidget.this.getSelected() == this;

            int mainColor = isExcluded ? 0xFF666666 : 0xFFFFFFFF;
            int subColor = isExcluded ? 0xFF444444 : 0xFFAAAAAA;
 
            int jukeboxStatus = screen.getMenu().getData().get(2);
            boolean isNowPlaying = (jukeboxStatus == 2 && this.index == screen.getMenu().getData().get(1));
            
            // Draw backgrounds
            if (isExcluded) {
                graphics.blitSprite(OngakuModClient.ENTRY_INACTIVE, x, y, SpriteWidth, SpriteHeight);
            } else {
                graphics.blitSprite(OngakuModClient.ENTRY_ACTIVE, x, y, SpriteWidth, SpriteHeight);
            }
            
            if (isNowPlaying) {
                graphics.fill(x, y, bgRight, bgBottom, 0x4455FF55);
            } else if (isSelected) {
                graphics.fill(x, y, bgRight, bgBottom, 0x33FFFFFF);
            } else if (isHovered) {
                graphics.fill(x, y, bgRight, bgBottom, 0x22FFFFFF);
            }
 
            int itemTargetY = y + ((height - 16) / 2);
            graphics.renderFakeItem(disc, x + 4, itemTargetY);
            graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
 
            List<Component> tooltip = getDiscDescription(disc, Minecraft.getInstance().level, Minecraft.getInstance().player, TooltipFlag.Default.NORMAL);
            int textX = x + 26;
            int availableTextWidth = (x + SpriteWidth) - textX - 4;
            Boolean ShouldMove = isHovered || isSelected || isNowPlaying;
            if (tooltip.size() >= 2)
            {
                String fullText = tooltip.get(1).getString();
                String[] parts = fullText.split(" - ");
                if (parts.length == 2)
                {
                    String songTitle = parts[1];
                    String artistName = parts[0];
                    String countText = this.count > 1 ? " x" + this.count : "";
 
                    UIHelper.drawScrollingText(graphics, Minecraft.getInstance().font, songTitle + countText, textX, y + 3, availableTextWidth, 0.8f, mainColor,ShouldMove);
                    UIHelper.drawScrollingText(graphics, Minecraft.getInstance().font, artistName, textX, y + 11, availableTextWidth, 0.6f, subColor,ShouldMove);
                }
                else
                {
                    UIHelper.drawScrollingText(graphics, Minecraft.getInstance().font, fullText, textX, y + ((height - 9) / 2), availableTextWidth, mainColor,ShouldMove);
                }
            }
            else
            {
                UIHelper.drawScrollingText(graphics, Minecraft.getInstance().font, disc.getHoverName().getString(), textX, y + ((height - 9) / 2), availableTextWidth, mainColor,ShouldMove);
            }
 
            if (isHovered)
            {
                int rightEdge = x + rowWidth - 10;
                int iconTargetY = y + ((height - 12) / 2);
                UIHelper.DrawIcon(graphics, rightEdge - 42, iconTargetY, 12, UIHelper.ICON_QUEUE_UP);
                UIHelper.DrawIcon(graphics, rightEdge - 27, iconTargetY, 12, UIHelper.ICON_QUEUE_DOWN);
                UIHelper.DrawIcon(graphics, rightEdge - 12, iconTargetY, 12, isExcluded ? UIHelper.ICON_INCLUDE : UIHelper.ICON_EXCLUDE);
            }
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
                if (jukeStatus == 2)
                {
                    OngakuMod.LOGGER.info("Clicked :" + this.index+","+this.identity.toString());
                    if (MusicListWidget.this.checkAndUseCooldown()) screen.setSelectedDisc(this.index);
                }
                return true;
            }

            return false;
        }


        private void sendAction(BlockPos pos, ManagePlaylistPayload.Action action) {
            PacketDistributor.sendToServer(new ManagePlaylistPayload(Optional.of(pos), Optional.empty(), Optional.empty(), action, Optional.of(this.index)));
        }

        @Override
        public Component getNarration()
        {
            return disc.getHoverName();
        }
    }
}  