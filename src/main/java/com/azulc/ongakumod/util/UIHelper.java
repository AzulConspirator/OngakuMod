package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuModClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

public class UIHelper 
{
    public static final int ICON_SIZE = 16;
    public static final int ICON_SHEET_COLUMNS = 8;
    public static final int ICON_SHEET_ROWS = 2;
    public static final int ICON_SHEET_WIDTH = ICON_SIZE * ICON_SHEET_COLUMNS;   // 128
    public static final int ICON_SHEET_HEIGHT = ICON_SIZE * ICON_SHEET_ROWS;     // 32

    public static final int ICON_PLAY = 0;
    public static final int ICON_SKIP = 1;
    public static final int ICON_STOP = 2;
    public static final int ICON_AUTOPLAY = 3;
    public static final int ICON_QUEUE_UP = 4;
    public static final int ICON_QUEUE_DOWN = 5;
    public static final int ICON_EXCLUDE = 6;
    public static final int ICON_INCLUDE = 7;
    public static final int ICON_PREV_TRACK = 9;
    public static final int ICON_NEXT_TRACK = 1;

    public static void DrawIcon(GuiGraphics graphics, int x, int y, int renderSize, int iconIndex) {
        int u = (iconIndex % ICON_SHEET_COLUMNS) * ICON_SIZE;
        int v = (iconIndex / ICON_SHEET_COLUMNS) * ICON_SIZE;
        graphics.blit(OngakuModClient.BUTTON_ICONS, x, y, renderSize, renderSize, u, v, ICON_SIZE, ICON_SIZE, ICON_SHEET_WIDTH, ICON_SHEET_HEIGHT);
    }

    public static class IconButton extends Button {
        protected final int iconIndex;
        protected final BooleanSupplier activeStateSupplier;

        public IconButton(int x, int y, int width, int height, int iconIndex, Component tooltip, OnPress onPress) {
            this(x, y, width, height, iconIndex, null, tooltip, onPress);
        }
        public IconButton(int x, int y, int width, int height, int iconIndex, BooleanSupplier activeStateSupplier, Component tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.iconIndex = iconIndex;
            this.activeStateSupplier = activeStateSupplier;
            if (tooltip != null) {
                this.setTooltip(Tooltip.create(tooltip));
            }
        }
 
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = this.isHoveredOrFocused() ? 0x66FFFFFF : 0x66000000;
 
            graphics.blitSprite(OngakuModClient.BUTTON_SPRITE, this.getX(), this.getY(), this.width, this.height);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
 
            DrawIcon(graphics, this.getX() + (this.width - ICON_SIZE) / 2, this.getY() + (this.height - ICON_SIZE) / 2, ICON_SIZE, this.iconIndex);
 
            if (this.activeStateSupplier != null && this.activeStateSupplier.getAsBoolean()) {
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, 0xFF55FF55);
            }
        }
    }
}
