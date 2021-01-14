package com.amadornes.rscircuits.component;

import java.util.EnumSet;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public abstract class ComponentBaseFace extends ComponentBaseInt {

    protected EnumComponentSlot slot = EnumComponentSlot.BOTTOM;

    public ComponentBaseFace(ICircuit circuit, EnumComponentSlot slot) {

        super(circuit);
        this.slot = slot;
    }

    public ComponentBaseFace(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.of(slot);
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        super.onNeighborChange(side, slot, component, updateType);

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }
    }

    public boolean dropIfNeeded() {

        if (!getCircuit().isSideSolid(getPos().down(), EnumCircuitSide.TOP)) {
            getCircuit().removeComponent(this);
            getDrops().forEach(getCircuit()::spawnStack);
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag.setInteger("slot", slot.ordinal());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        slot = EnumComponentSlot.VALUES[tag.getInteger("slot")];
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        buf.writeEnumValue(slot);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        slot = buf.readEnumValue(EnumComponentSlot.class);
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        buf.writeEnumValue(slot);
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        slot = buf.readEnumValue(EnumComponentSlot.class);
    }

}
