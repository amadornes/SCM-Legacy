package com.amadornes.rscircuits.client.gui;

import java.util.function.IntConsumer;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.EnumDyeColor;

public class GuiColorPalette extends GuiScreen {

    private int active;
    private IntConsumer callback;

    public GuiColorPalette(int active, IntConsumer callback) {

        this.active = active;
        this.callback = callback;
    }

    @Override
    public boolean doesGuiPauseGame() {

        return false;
    }

    @Override
    public void onGuiClosed() {

        callback.accept(active);
    }

    @Override
    public void updateScreen() {

        if (!Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        int size = 100;
        int b = 2;
        int cOff = 4;
        int cSize = (size - b * 2 - cOff * 5) / 4;

        int selInactive = 0xA010100F;

        int x = (this.width - size) / 2, y = (this.height - size) / 2;

        String colorName = I18n.format("color." + EnumDyeColor.byMetadata(active).getName().toLowerCase());
        drawCenteredString(fontRendererObj, colorName, x + (size / 2), y - 12, 0xFFFFFF);

        drawGradientRect(x, y, x + size, y + size, 0x7010100F, 0x7010100F);
        drawGradientRect(x, y, x + size, y + b, 0xA010100F, 0xA010100F);
        drawGradientRect(x, y + size - b, x + size, y + size, 0xA010100F, 0xA010100F);
        drawGradientRect(x, y + b, x + b, y + size - b, 0xA010100F, 0xA010100F);
        drawGradientRect(x + size - b, y + b, x + size, y + size - b, 0xA010100F, 0xA010100F);

        for (int x_ = 0; x_ < 4; x_++) {
            int xOff = x + b + cOff * (x_ + 1) + cSize * x_;
            for (int y_ = 0; y_ < 4; y_++) {
                int yOff = y + b + cOff * (y_ + 1) + cSize * y_;
                EnumDyeColor dye = EnumDyeColor.byMetadata(y_ * 4 + x_);
                int color = dye.getMapColor().colorValue | 0xFF000000;

                if (mouseX >= xOff && mouseX < xOff + cSize && mouseY >= yOff && mouseY < yOff + cSize) {
                    active = y_ * 4 + x_;
                }

                if (active == y_ * 4 + x_) {
                    drawGradientRect(xOff, yOff, xOff + cSize, yOff + cSize, color, color);

                    drawGradientRect(xOff - b, yOff - b, xOff + cSize + b, yOff, selInactive, selInactive);
                    drawGradientRect(xOff - b, yOff + cSize, xOff + cSize + b, yOff + cSize + b, selInactive, selInactive);
                    drawGradientRect(xOff - b, yOff, xOff, yOff + cSize, selInactive, selInactive);
                    drawGradientRect(xOff + cSize, yOff, xOff + cSize + b, yOff + cSize, selInactive, selInactive);
                } else {
                    drawGradientRect(xOff + b, yOff + b, xOff + cSize - b, yOff + cSize - b, color, color);

                    drawGradientRect(xOff, yOff, xOff + cSize, yOff + b, selInactive, selInactive);
                    drawGradientRect(xOff, yOff + cSize - b, xOff + cSize, yOff + cSize, selInactive, selInactive);
                    drawGradientRect(xOff, yOff + b, xOff + b, yOff + cSize - b, selInactive, selInactive);
                    drawGradientRect(xOff + cSize - b, yOff + b, xOff + cSize, yOff + cSize - b, selInactive, selInactive);
                }
            }
        }
    }

}
