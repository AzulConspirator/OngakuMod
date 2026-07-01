package com.azulc.ongakumod.util;

import com.azulc.ongakumod.OngakuModClient;
import com.azulc.ongakumod.client.screen.AutoplayScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

public class UIHelper {
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
            
            graphics.blitSprite(OngakuModClient.BUTTON_SPRITE,this.getX(), this.getY(),this.width,this.height);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            // Draw Icon
            int u = this.iconIndex * 16;
            graphics.blit(OngakuModClient.BUTTON_ICONS, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2, u, 0, 16, 16, 128, 16);

            // Autoplay Corner Indicator (Top Right)
            if (isAutoplayButton) {
                // Check state from menu data
                boolean enabled = ((AutoplayScreen)Minecraft.getInstance().screen).getMenu().getData().get(3) == 1;
                int color = enabled ? 0xFF55FF55 : 0xFFFF5555; // Green / Red
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, color);
            }
        }
    }

    public static class TerminalIconButton extends Button {
        private final int iconIndex;
        private final BooleanSupplier activeStateSupplier;

        public TerminalIconButton(int x, int y, int iconIndex, Component tooltip, BooleanSupplier activeStateSupplier, OnPress onPress) {
            super(x, y, 20, 20, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.iconIndex = iconIndex;
            this.activeStateSupplier = activeStateSupplier;
            if (tooltip != null) {
                this.setTooltip(Tooltip.create(tooltip));
            }
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int bgColor = this.isHoveredOrFocused() ? 0x66FFFFFF : 0x66000000;
            graphics.blitSprite(OngakuModClient.BUTTON_SPRITE,this.getX(), this.getY(),this.width,this.height);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            int u = this.iconIndex * 16;
            graphics.blit(OngakuModClient.BUTTON_ICONS, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2, u, 0, 16, 16, 128, 16);

            if (activeStateSupplier.getAsBoolean()) {
                graphics.fill(this.getX() + this.width - 5, this.getY() + 1, this.getX() + this.width - 1, this.getY() + 5, 0xFF55FF55);
            }
        }
    }
}
