package com.amadornes.rscircuits.api.component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.api.SCMAPI;
import com.amadornes.rscircuits.api.circuit.ICircuit;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public abstract class ComponentBase implements IComponent {

    private final ICircuit circuit;
    private BlockPos pos;

    public ComponentBase(ICircuit circuit) {

        this.circuit = circuit;
    }

    @Override
    public ICircuit getCircuit() {

        return this.circuit;
    }

    @Override
    public BlockPos getPos() {

        return this.pos;
    }

    @Override
    public void setPos(BlockPos pos) {

        this.pos = pos;
    }

    @Override
    public IBlockState getActualState() {

        return SCMAPI.getInstance().getComponentRegistry().getState(getName()).getBaseState();
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.noneOf(EnumComponentSlot.class);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

    }

    @Override
    public void writeDescription(PacketBuffer buf) {

    }

    @Override
    public void readDescription(PacketBuffer buf) {

    }

    public IComponent getNeighbor(EnumComponentSlot slot, EnumCircuitSide side) {

        return getNeighbor(getCircuit(), getPos(), slot, side);
    }

    public Pair<IComponent, EnumComponentSlot> getNeighborPair(EnumComponentSlot slot, EnumCircuitSide side) {

        return getNeighborPairConditioned(getCircuit(), getPos(), slot, side, (c, s) -> c != this);
    }

    public static IComponent getNeighbor(ICircuit circuit, BlockPos pos, EnumComponentSlot slot, EnumCircuitSide side) {

        Pair<IComponent, EnumComponentSlot> p = getNeighborPair(circuit, pos, slot, side);
        return p == null ? null : p.getKey();
    }

    public static Pair<IComponent, EnumComponentSlot> getNeighborPair(ICircuit circuit, BlockPos pos, EnumComponentSlot slot,
            EnumCircuitSide side) {

        return getNeighborPairConditioned(circuit, pos, slot, side, (c, s) -> true);
    }

    public static Pair<IComponent, EnumComponentSlot> getNeighborPairConditioned(ICircuit circuit, BlockPos pos, EnumComponentSlot slot,
            EnumCircuitSide side, BiPredicate<IComponent, EnumComponentSlot> predicate) {

        if (slot.side != side) {
            if (slot.side == side.getOpposite()) {
                IComponent c = circuit.getComponent(pos, EnumComponentSlot.CENTER);
                if (c != null && predicate.test(c, EnumComponentSlot.CENTER)) {
                    return Pair.of(c, EnumComponentSlot.CENTER);
                }
                return null;
            }
            IComponent c = circuit.getComponent(pos, EnumComponentSlot.VALUES[side.ordinal()]);
            if (c != null && predicate.test(c, EnumComponentSlot.VALUES[side.ordinal()])) {
                return Pair.of(c, EnumComponentSlot.VALUES[side.ordinal()]);
            }
        }
        IComponent c = circuit.getComponent(pos.offset(side.face), EnumComponentSlot.VALUES[side.getOpposite().ordinal()]);
        if (c != null && predicate.test(c, EnumComponentSlot.VALUES[side.getOpposite().ordinal()])) {
            return Pair.of(c, EnumComponentSlot.VALUES[side.getOpposite().ordinal()]);
        }
        if (slot.side == side) {
            slot = EnumComponentSlot.CENTER;
        }
        c = circuit.getComponent(pos.offset(side.face), slot);
        if (c != null && predicate.test(c, slot)) {
            return Pair.of(c, slot);
        }
        return null;
    }

    public byte getInput(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color) {

        Pair<IComponent, EnumComponentSlot> c = getNeighborPair(slot, side);
        if (c != null && c.getKey().isStrongOutput(c.getValue(), side.getOpposite())) {
            return c.getKey().getOutputSignal(c.getValue(), side.getOpposite(), color, false);
        }
        return 0;
    }

    public byte getInputAny(EnumComponentSlot slot, EnumCircuitSide side) {

        int power = 0;

        Pair<IComponent, EnumComponentSlot> c = getNeighborPair(slot, side);
        if (c != null && c.getKey().isStrongOutput(c.getValue(), side.getOpposite())) {
            for (EnumDyeColor color : EnumDyeColor.values()) {
                power = Math.max(power, c.getKey().getOutputSignal(c.getValue(), side.getOpposite(), color, false) & 0xFF);
            }
        }

        return (byte) power;
    }

    public boolean isOnEdge(EnumCircuitSide edge) {

        return edge == null ? false : getCircuit().getCircuit(pos.offset(edge.face)) == null;
    }

    public EnumCircuitSide getEdgeOn() {

        BlockPos pos = getPos();
        if (pos.getX() == 0) {
            return EnumCircuitSide.RIGHT;
        } else if (pos.getX() == 6) {
            return EnumCircuitSide.LEFT;
        } else if (pos.getZ() == 0) {
            return EnumCircuitSide.BACK;
        } else if (pos.getZ() == 6) {
            return EnumCircuitSide.FRONT;
        }
        return null;
    }

    public List<EnumCircuitSide> getEdgesOn() {

        List<EnumCircuitSide> edges = new ArrayList<>();
        BlockPos pos = getPos();
        if (pos.getX() == 0) {
            edges.add(EnumCircuitSide.RIGHT);
        }
        if (pos.getX() == 6) {
            edges.add(EnumCircuitSide.LEFT);
        }
        if (pos.getZ() == 0) {
            edges.add(EnumCircuitSide.BACK);
        }
        if (pos.getZ() == 6) {
            edges.add(EnumCircuitSide.FRONT);
        }
        return edges;
    }

    public boolean isOnSeam() {

        BlockPos pos = getPos();
        return pos.getX() == 8 || pos.getZ() == 8;
    }

}
