package com.amadornes.rscircuits.client.gui;

import java.io.IOException;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.network.NetworkHandler;
import com.amadornes.rscircuits.network.PacketPunchcard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

public class GuiPunchcard extends GuiScreen {

    private static final ResourceLocation resLoc = new ResourceLocation(SCM.MODID, "textures/gui/punchcard.png");
    private static final ResourceLocation resLocEmpty = new ResourceLocation(SCM.MODID, "textures/gui/punchcard_empty.png");

    private static final int xSize = 12 * 16, ySize = 256;

    private boolean[][] holes;

    public GuiPunchcard(boolean[][] holes) {
        this.holes = holes;
    }

    @Override
    public boolean doesGuiPauseGame() {

        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

        int guiLeft = (width - xSize) / 2;
        int guiTop = (height - ySize) / 2;

        Minecraft.getMinecraft().renderEngine.bindTexture(resLoc);

        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, 2 * 16);
        drawTexturedModalRect(guiLeft, guiTop + 256 - 2 * 16, 0, 256 - 2 * 16, xSize, 2 * 16);
        drawTexturedModalRect(guiLeft, guiTop + 2 * 16, 0, 2 * 16, 2 * 16, ySize - 4 * 16);
        drawTexturedModalRect(guiLeft + 256 - 6 * 16, guiTop + 2 * 16, 256 - 6 * 16, 2 * 16, 2 * 16, ySize - 4 * 16);

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 12; y++) {
                if (!holes[x][y]) {
                    drawTexturedModalRect(guiLeft + (2 + x) * 16, guiTop + (2 + y) * 16, (2 + x) * 16, (2 + y) * 16, 16, 16);
                }
            }
        }

        Minecraft.getMinecraft().renderEngine.bindTexture(resLocEmpty);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 12; y++) {
                if (holes[x][y]) {
                    drawTexturedModalRect(guiLeft + (2 + x) * 16, guiTop + (2 + y) * 16, (2 + x) * 16, (2 + y) * 16, 16, 16);
                }
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {

        int guiLeft = (width - xSize) / 2;
        int guiTop = (height - ySize) / 2;
        mouseX -= guiLeft;
        mouseY -= guiTop;

        int x = mouseX - 32;
        int y = mouseY - 32;
        int pX = (int) Math.floor(x / 16D);
        int pY = (int) Math.floor(y / 16D);

        if (pX >= 0 && pX < 8 && pY >= 0 && pY < 12 && (x % 16) >= 3 && (x % 16) < 13 && (y % 16) >= 3 && (y % 16) < 13) {
            if (!holes[pX][pY]) {
                // TODO: Play sound!
                holes[pX][pY] = true;
                NetworkHandler.instance.sendToServer(new PacketPunchcard(pX, pY));
            }
        }
    }

}
