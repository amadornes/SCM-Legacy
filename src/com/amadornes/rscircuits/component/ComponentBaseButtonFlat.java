package com.amadornes.rscircuits.component;

import java.util.EnumSet;
import java.util.List;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;

import net.minecraft.util.math.AxisAlignedBB;

public abstract class ComponentBaseButtonFlat extends ComponentBaseButton {

    public ComponentBaseButtonFlat(ICircuit circuit, EnumComponentSlot slot) {

        super(circuit, slot);
    }

    public ComponentBaseButtonFlat(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.of(slot);
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 4 / 16D, 1));
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return new AxisAlignedBB(0, 0, 0, 1, 4 / 16D, 1);
    }

}
