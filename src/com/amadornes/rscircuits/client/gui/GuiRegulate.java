package com.amadornes.rscircuits.client.gui;

import java.util.function.Consumer;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.util.BoolFunction;
import com.amadornes.rscircuits.util.IntBoolFunction;
import com.google.common.base.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

public class GuiRegulate extends GuiScreen {

    private static final ResourceLocation resLoc = new ResourceLocation(SCM.MODID, "textures/gui/time.png");
    private static final String[] buttonTexts = { "-20", "-5", "-1", "+1", "+5", "+20" };
    private static final int[] buttonActions = { -20, -5, -1, +1, +5, +20 };

    private static final int xSize = 228, ySize = 50;

    private IntBoolFunction<String> titleSupplier;
    private BoolFunction<String> shortSupplier;
    private Supplier<Integer> getter;
    private Consumer<Integer> setter;
    private int min, max;
    private double shiftMul;

    public GuiRegulate(IntBoolFunction<String> titleSupplier, BoolFunction<String> shortSupplier, Supplier<Integer> getter,
            Consumer<Integer> setter, int min, int max, double shiftMul) {

        this.titleSupplier = titleSupplier;
        this.shortSupplier = shortSupplier;
        this.getter = getter;
        this.setter = setter;
        this.min = min;
        this.max = max;
        this.shiftMul = shiftMul;
    }

    @Override
    public boolean doesGuiPauseGame() {

        return false;
    }

    @Override
    public void initGui() {

        super.initGui();

        int guiLeft = (width - xSize) / 2;
        int guiTop = (height - ySize) / 2;

        int buttonWidth = 35;
        for (int i = 0; i < buttonTexts.length; i++) {
            buttonList.add(new GuiButton(0 * buttonTexts.length + i, guiLeft + 4 + i * (buttonWidth + 2), guiTop + 25, buttonWidth, 20,
                    buttonTexts[i] + shortSupplier.apply(isShiftKeyDown())));
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {

        int newVal = (int) (getter.get() + (buttonActions[button.id] * (isShiftKeyDown() ? shiftMul : 1)));
        newVal = Math.max(min, Math.min(newVal, max));
        setter.accept(newVal);
    }

    @Override
    public void drawScreen(int x, int y, float partialTicks) {

        int guiLeft = (width - xSize) / 2;
        int guiTop = (height - ySize) / 2;

        Minecraft.getMinecraft().renderEngine.bindTexture(resLoc);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        super.drawScreen(x, y, partialTicks);

        drawCenteredString(fontRendererObj, titleSupplier.apply((int) (getter.get() / (isShiftKeyDown() ? shiftMul : 1)), isShiftKeyDown()),
                guiLeft + xSize / 2, guiTop + 10, 0xFFFFFF);
    }

    @Override
    public void updateScreen() {

        for (int i = 0; i < buttonTexts.length; i++) {
            buttonList.get(i).displayString = buttonTexts[i] + shortSupplier.apply(isShiftKeyDown());
        }
    }

}
