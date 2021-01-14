package com.amadornes.rscircuits.component.wire2;

import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.component.misc.ComponentPost;
import com.google.common.collect.Multimap;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;

public abstract class ComponentWirePostBase extends ComponentPost implements IRedstoneConductor, IRedstoneConductorContainer {

    protected final SimpleRedstoneConductor<ComponentWirePostBase> cond = new SimpleRedstoneConductor<ComponentWirePostBase>(this);
    private boolean needsReconnect = false;

    public ComponentWirePostBase(ICircuit circuit) {
        super(circuit);
    }

    @Override
    public EnumComponentSlot getSlot() {

        return EnumComponentSlot.CENTER;
    }

    @Override
    public void onAdded() {

        if (needsReconnect) {
            needsReconnect = false;
            for (EnumCircuitSide side : EnumCircuitSide.VALUES) {
                cond.computeConnection(side);
            }
        }

        cond.onAdded();
    }

    @Override
    public void onRemoved() {

        cond.onRemoved();
    }

    @Override
    public void onLoaded() {

        if (needsReconnect) {
            needsReconnect = false;
            for (EnumCircuitSide side : EnumCircuitSide.VALUES) {
                cond.computeConnection(side);
            }
        }

        cond.onLoaded();
    }

    @Override
    public void onCircuitAdded() {

        cond.onCircuitAdded();
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

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

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = super.writeToNBT(tag);

        int connections = (cond.getNeighbors()[0].getType().ordinal() & 0b11)
                | ((cond.getNeighbors()[0].getType().ordinal() & 0b11) << 0b11);
        tag.setInteger("connections", connections);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);

        if (tag.hasKey("connections")) {
            int connections = tag.getInteger("connections");
            cond.getNeighborsRAW()[0].type = EnumConnectionType.VALUES[connections & 0b11];
            cond.getNeighborsRAW()[1].type = EnumConnectionType.VALUES[(connections >> 0b11) & 0b11];
        } else {
            needsReconnect = true;
        }
    }

}
