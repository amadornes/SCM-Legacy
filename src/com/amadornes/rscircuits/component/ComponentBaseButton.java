package com.amadornes.rscircuits.component;

import static com.amadornes.rscircuits.api.component.EnumCircuitSide.*;

import java.util.EnumSet;
import java.util.List;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;

import net.minecraft.util.math.AxisAlignedBB;

public abstract class ComponentBaseButton extends ComponentBaseFace {

    public static final EnumCircuitSide[] INPUT_SIDES = { BOTTOM, BACK, FRONT, RIGHT, LEFT };

    public ComponentBaseButton(ICircuit circuit, EnumComponentSlot slot) {

        super(circuit, slot);
    }

    public ComponentBaseButton(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.of(slot, EnumComponentSlot.CENTER);
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 6 / 16D, 1));
        boxes.add(new AxisAlignedBB(1 / 16D, 0, 1 / 16D, 15 / 16D, 7 / 16D, 15 / 16D));
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return new AxisAlignedBB(0, 0, 0, 1, 6 / 16D, 1);
    }

    protected byte getInput() {

        int input = 0;
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            input = Math.max(input, getInputAny(EnumComponentSlot.BOTTOM, s) & 0xFF);
        }
        IComponent c = getCircuit().getComponent(getPos().down(), EnumComponentSlot.TOP);
        if (c != null) {
            input = Math.max(input, c.getOutputSignalAny(EnumComponentSlot.TOP, EnumCircuitSide.TOP) & 0xFF);
        }
        return (byte) input;
    }

}
