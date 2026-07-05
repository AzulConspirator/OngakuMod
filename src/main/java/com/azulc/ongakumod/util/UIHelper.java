package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuModClient;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
    public static final long COOLDOWN_MS = 1000;

    public static void DrawIcon(GuiGraphics graphics, int x, int y, int renderSize, int iconIndex) {
        int u = (iconIndex % ICON_SHEET_COLUMNS) * ICON_SIZE;
        int v = (iconIndex / ICON_SHEET_COLUMNS) * ICON_SIZE;
        graphics.blit(OngakuModClient.BUTTON_ICONS, x, y, renderSize, renderSize, u, v, ICON_SIZE, ICON_SIZE, ICON_SHEET_WIDTH, ICON_SHEET_HEIGHT);
    }

     
    // ---------------------------------------------------------------------
    // Text overflow handling
    // ---------------------------------------------------------------------
 
    /**
     * Truncates text to fit maxWidth pixels, appending "..." when it doesn't fit.
     * Returns the text unchanged if it already fits.
     */
    public static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        String trimmed = font.plainSubstrByWidth(text, Math.max(maxWidth - ellipsisWidth, 0));
        return trimmed + ellipsis;
    }
 
    public static void drawScrollingText(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, int color, boolean shouldMove) {
        drawScrollingText(graphics, font, text, x, y, maxWidth, 1.0f, color, true,shouldMove);
    }
 
    public static void drawScrollingText(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, float scale, int color, boolean shouldMove) {
        drawScrollingText(graphics, font, text, x, y, maxWidth, scale, color, false,shouldMove);
    }
 
    /**
     * Draws text that fits within maxWidth normally.it scrolls back and forth ("marquee") to reveal the full string over time
     */
    public static void drawScrollingText(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, float scale, int color, boolean centerWhenFits, boolean shouldMove) {
        int textWidth = font.width(text);
        int scaledMaxWidth = Math.round(maxWidth / scale);
 
        if (textWidth <= scaledMaxWidth) {
            int localX = centerWhenFits ? (scaledMaxWidth - textWidth) / 2 : 0;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.drawString(font, text, localX, 0, color, false);
            graphics.pose().popPose();
            return;
        }
 
        long holdMs = 800L;
        long travelMs = Math.max(1200L, (long) (textWidth - scaledMaxWidth) * 15L);
        long cycle = (travelMs + holdMs) * 2;
        long t = Util.getMillis() % cycle;
        int offset;
        if (shouldMove)
        {        
            float progress;
            if (t < holdMs) {
                progress = 0f; // paused at the start
            } else if (t < holdMs + travelMs) {
                progress = (float) (t - holdMs) / travelMs; // scrolling forward
            } else if (t < holdMs * 2 + travelMs) {
                progress = 1f; // paused at the end
            } else {
                progress = 1f - (float) (t - holdMs * 2 - travelMs) / travelMs; // scrolling back
            }
            offset = Math.round(progress * (textWidth - scaledMaxWidth));
        }
        else
        {
            offset = 0;
        }
        graphics.enableScissor(x, y, x + maxWidth, y + Math.round(font.lineHeight * scale) + 1);
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(font, text, -offset, 0, color, false);
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    
    public static float cooldownProgress(Long lastActionTime) {
        return (Util.getMillis() - lastActionTime) / (float) COOLDOWN_MS;
    }

    public static class IconButton extends Button {
        protected final int iconIndex;
        protected final BooleanSupplier activeStateSupplier;
        protected Supplier<Float> cooldownProgressSupplier;

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

        public IconButton withCooldown(Supplier<Float> cooldownProgressSupplier) {
            this.cooldownProgressSupplier = cooldownProgressSupplier;
            return this;
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

            if (this.cooldownProgressSupplier != null) {
            float progress = Math.max(0f, Math.min(1f, this.cooldownProgressSupplier.get()));
            if (progress < 1f) {
                int overlayTop = this.getY() + Math.round(progress * this.height);
                graphics.fill(this.getX(), overlayTop, this.getX() + this.width, this.getY() + this.height, 0x55FFFFFF);
            }
        }
        }
    }
}
