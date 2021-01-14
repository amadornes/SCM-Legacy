package com.amadornes.rscircuits.component.wire2;

import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.google.common.collect.Multimap;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.Rotation;

public abstract class ComponentWireFlatBase extends ComponentBaseFace implements IRedstoneConductor, IRedstoneConductorContainer {

    protected final SimpleRedstoneConductor<ComponentWireFlatBase> cond = new SimpleRedstoneConductor<ComponentWireFlatBase>(this);

    public ComponentWireFlatBase(ICircuit circuit, EnumComponentSlot slot) {
        super(circuit, slot);
    }

    public ComponentWireFlatBase(ICircuit circuit) {
        super(circuit);
    }

    @Override
    public EnumComponentSlot getSlot() {

        return slot;
    }

    @Override
    public void onAdded() {

        cond.onAdded();
    }

    @Override
    public void onRemoved() {

        cond.onRemoved();
    }

    @Override
    public void onLoaded() {

        cond.onLoaded();
    }

    @Override
    public void onCircuitAdded() {

        cond.onCircuitAdded();
    }

    @Override
    public void rotatePre(Rotation rotation) {

        cond.rotate(rotation);
    }

    @Override
    public void rotatePost() {

        cond.rotatePost();
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        cond.onNeighborChange(side, slot, component, updateType);
    }

    @Override
    public void propagate(EnumDyeColor color) {

        cond.propagate(color);
    }

    @Override
    public void propagate(IRedstoneConductor from, EnumDyeColor color, Multimap<IRedstoneConductor, EnumDyeColor> wires,
            Set<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled) {

        cond.propagate(from, color, wires, scheduled);
    }

    @Override
    public void onPropagated(Set<EnumDyeColor> colors, byte power) {

        cond.onPropagated(colors, power);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IConnectionInfo[] getNeighbors() {

        return cond.getNeighbors();
    }

    @Override
    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated) {

        return cond.connect(side, conductor, type, simulated);
    }

    @Override
    public void disconnect(EnumCircuitSide side) {

        cond.disconnect(side);
    }

}
