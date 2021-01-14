package com.amadornes.rscircuits.client.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.TextFormatting;

public class GuiTutorial extends GuiScreen {

    private boolean stayOnRelease = false;

    @Override
    public boolean doesGuiPauseGame() {

        return false;
    }

    @Override
    public void initGui() {

        Mouse.setGrabbed(true);
    }

    @Override
    public void onGuiClosed() {

        Mouse.setGrabbed(false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {

        stayOnRelease = true;
        Mouse.setGrabbed(false);
    }

    @Override
    public void updateScreen() {

        if (!stayOnRelease && !Keyboard.isKeyDown(Keyboard.KEY_LMENU)) {
            Mouse.setGrabbed(false);
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        int height = 275;
        int width = 225;

        int midX = this.width / 2, midY = this.height / 2;

        GlStateManager.pushMatrix();
        if (!stayOnRelease) {
            drawCenteredString(fontRendererObj, "Click to stay open", midX, midY - (height / 2) - 12, 0xFFFFFF);
        }
        GlStateManager.translate(midX - (width / 2), midY - (height / 2), 0);
        {
            // Header
            {
                drawGradientRect(0, 0, width, 44, 0x7010100F, 0x7010100F);
                GlStateManager.scale(2, 2, 0);
                drawCenteredString(fontRendererObj, "Super Circuit Maker", width / 4, 4, 0xFFFFFF);
                GlStateManager.scale(1 / 2D, 1 / 2D, 0);
                drawCenteredString(fontRendererObj, "Basic in-game manual", width / 2, 30, 0xFFFFFF);
            }
            // Content
            {
                drawGradientRect(0, 47, width, height, 0x7010100F, 0x8010100F);
                GlStateManager.translate(0, 47, 0);

                drawString(fontRendererObj, "Screwdriver:", 8, 8, 0xFFFFFF);

                drawString(fontRendererObj, " The screwdriver is like your average", 12, 20 + 10 * 0, 0xEEEEEE);
                drawString(fontRendererObj, "wrench. It lets you rotate components", 12, 20 + 10 * 1, 0xEEEEEE);
                drawString(fontRendererObj, "and circuits by right-clicking them, but", 12, 20 + 10 * 2, 0xEEEEEE);
                drawString(fontRendererObj, "it also has some unique uses.", 12, 20 + 10 * 3, 0xEEEEEE);
                drawString(fontRendererObj,
                        String.format(" You can %sclick%s the connection of two",
                                TextFormatting.YELLOW.toString() + TextFormatting.UNDERLINE, TextFormatting.RESET),
                        12, 20 + 10 * 4, 0xEEEEEE);
                drawString(fontRendererObj,
                        String.format("wires to %sdisconnect%s them, or do it while", TextFormatting.AQUA, TextFormatting.RESET), 12,
                        20 + 10 * 5, 0xEEEEEE);
                drawString(fontRendererObj,
                        String.format("holding %sshift%s to %stoggle%s that side's mode.",
                                TextFormatting.YELLOW.toString() + TextFormatting.UNDERLINE, TextFormatting.RESET, TextFormatting.AQUA,
                                TextFormatting.RESET),
                        12, 20 + 10 * 6, 0xEEEEEE);
                drawString(fontRendererObj, " Due to some issues in MC's block brea-", 12, 20 + 10 * 7, 0xEEEEEE);
                drawString(fontRendererObj, "king logic, this may not always work, but", 12, 20 + 10 * 8, 0xEEEEEE);
                drawString(fontRendererObj,
                        String.format("you can also %sleft-click and drag%s to",
                                TextFormatting.YELLOW.toString() + TextFormatting.UNDERLINE, TextFormatting.RESET),
                        12, 20 + 10 * 9, 0xEEEEEE);
                drawString(fontRendererObj, String.format("%sremove components%s more easily.", TextFormatting.AQUA, TextFormatting.RESET),
                        12, 20 + 10 * 10, 0xEEEEEE);

            }
        }
        GlStateManager.popMatrix();
    }

}
