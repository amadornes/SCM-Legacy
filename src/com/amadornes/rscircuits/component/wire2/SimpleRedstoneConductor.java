package com.amadornes.rscircuits.component.wire2;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.ComponentBase;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.IConnectionInfo;
import com.amadornes.rscircuits.api.component.IWirePassthroughComponent;
import com.amadornes.rscircuits.component.ComponentBaseInt;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

public class SimpleRedstoneConductor<T extends ComponentBaseInt & IRedstoneConductor & IRedstoneConductorContainer> {

    private final T parent;
    private final ConnectionInfo[] neighbors = new ConnectionInfo[6];

    public SimpleRedstoneConductor(T parent) {

        this.parent = parent;
        for (EnumCircuitSide side : EnumCircuitSide.VALUES) {
            neighbors[side.ordinal()] = new ConnectionInfo(side);
        }
    }

    public IConnectionInfo[] getNeighbors() {

        return neighbors;
    }

    public ConnectionInfo[] getNeighborsRAW() {

        return neighbors;
    }

    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated) {

        ConnectionInfo info = neighbors[side.ordinal()];
        if (!simulated) {
            info.type = type;
            info.neighbor = conductor;
            parent.onConnect(side, conductor, type);
        }
        return true;
    }

    public void disconnect(EnumCircuitSide side) {

        ConnectionInfo info = neighbors[side.ordinal()];
        info.type = EnumConnectionType.NONE;
        info.neighbor = null;
        parent.onDisconnect(side);
    }

    public void propagate(EnumDyeColor color) {

        if (!parent.isBundled() && color != parent.getColor()) {
            return;
        }

        Multimap<IRedstoneConductor, EnumDyeColor> wires = MultimapBuilder.hashKeys().hashSetValues().build();
        LinkedHashSet<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled = new LinkedHashSet<>();
        LinkedHashSet<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduledNext = new LinkedHashSet<>();

        scheduledNext.add(Triple.of(parent, parent, color));

        while (!scheduledNext.isEmpty()) {
            scheduled.addAll(scheduledNext);
            scheduledNext.clear();
            scheduled.forEach(p -> p.getMiddle().propagate(p.getLeft(), p.getRight(), wires, scheduledNext));
            scheduled.clear();
        }

        AtomicInteger power = new AtomicInteger(0);
        wires.asMap().forEach((w, cols) -> cols.forEach(c -> power.set(Math.max(power.get(), w.getInput(c) & 0xFF))));
        byte pow = (byte) power.get();

        wires.asMap().forEach((w, cols) -> w.onPropagated((Set<EnumDyeColor>) cols, pow));
    }

    public void propagate(IRedstoneConductor from, EnumDyeColor color, Multimap<IRedstoneConductor, EnumDyeColor> wires,
            Set<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled) {

        if (parent.getPos() != null && !wires.containsEntry(parent, color)) {
            wires.put(parent, color);
            for (ConnectionInfo info : neighbors) {
                if (info.neighbor != null && (info.type == EnumConnectionType.ANODE || info.type == EnumConnectionType.BIDIRECTIONAL)
                        && info.neighbor.getNeighbors()[info.side.ordinal() ^ 1].getType() != EnumConnectionType.CATHODE) {
                    scheduled.add(Triple.of(parent, info.neighbor, parent.isBundled() ? color : parent.getColor()));
                }
            }
        }
    }

    public void onPropagated(Set<EnumDyeColor> colors, byte power) {

        for (ConnectionInfo info : neighbors) {
            if (info.getType().isOutput && (info.neighbor == null
                    || info.neighbor.getNeighbors()[info.side.ordinal() ^ 1].getType() == EnumConnectionType.CATHODE)) {
                if (info.neighbor == null) {
                    IRedstoneConductor wire = getWireNeighbor(parent.getCircuit(), parent.getPos(), info.side, parent.getSlot());
                    if (wire == null || wire.getNeighbors()[info.side.ordinal() ^ 1].getType() == EnumConnectionType.CATHODE) {
                        parent.getCircuit().notifyUpdate(parent.getPos(), parent.getSlot(), info.side);
                    }
                } else if (info.neighbor.getNeighbors()[info.side.ordinal() ^ 1].getType() == EnumConnectionType.CATHODE) {
                    parent.getCircuit().notifyUpdate(parent.getPos(), parent.getSlot(), info.side);
                }
            }
        }

    }

    public void onAdded() {

        if (isClient()) {
            return;
        }

        for (EnumCircuitSide side : EnumCircuitSide.VALUES) {
            computeConnection(side);
        }
        if (parent.isBundled()) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                propagate(c);
            }
        } else {
            propagate(parent.getColor());
        }
    }

    public void computeConnection(EnumCircuitSide side) {

        IRedstoneConductor neighbor = getWireNeighbor(parent.getCircuit(), parent.getPos(), side, parent.getSlot());
        ConnectionInfo info = neighbors[side.ordinal()];
        EnumConnectionType prevType = info.type;
        if (!info.type.canConnect) {
            if (info.type == EnumConnectionType.NONE) {
                if (neighbor != null) {
                    if ((parent.isBundled() || neighbor.isBundled()
                            || (!parent.isColorBiased() || !neighbor.isColorBiased() || parent.getColor() == neighbor.getColor()))
                            && parent.connect(side, neighbor, EnumConnectionType.ANODE, true)
                            && neighbor.connect(side.getOpposite(), parent, EnumConnectionType.ANODE, true)) {
                        parent.connect(side, neighbor, EnumConnectionType.ANODE, false);
                        neighbor.connect(side.getOpposite(), parent, EnumConnectionType.ANODE, false);
                    }
                } else {
                    Pair<IComponent, EnumComponentSlot> n = parent.getNeighborPair(parent.getSlot(), side);
                    if (n != null) {
                        if (!parent.isBundled()) {
                            if (n.getKey().isInput(n.getValue(), side.getOpposite())) {
                                info.type = EnumConnectionType.ANODE;
                            } else if (n.getKey().isOutput(n.getValue(), side.getOpposite())) {
                                info.type = EnumConnectionType.CATHODE;
                            }
                        } else {
                            if (n.getKey().getBundledConnection(n.getValue(), side.getOpposite()).canConnect) {
                                info.type = EnumConnectionType.BIDIRECTIONAL;
                            }
                        }
                        if (info.type != prevType) {
                            parent.onConnect(side, null, info.type);
                        }
                    } else {
                        info.type = EnumConnectionType.NONE;
                        if (info.type != prevType) {
                            parent.onDisconnect(side);
                        }
                    }
                }
            }
        } else {
            if (neighbor != null) {
                info.neighbor = neighbor;
                return;
            }
            Pair<IComponent, EnumComponentSlot> n = parent.getNeighborPair(parent.getSlot(), side);
            if (n == null) {
                info.type = EnumConnectionType.NONE;
                if (info.type != prevType) {
                    parent.onDisconnect(side);
                }
            } else if ((info.type == EnumConnectionType.ANODE && !n.getKey().isInput(n.getValue(), side.getOpposite()))
                    || (info.type == EnumConnectionType.CATHODE && !n.getKey().isOutput(n.getValue(), side.getOpposite()))
                    || (info.type == EnumConnectionType.BIDIRECTIONAL
                            && !n.getKey().getBundledConnection(n.getValue(), side.getOpposite()).canConnect)) {
                info.type = EnumConnectionType.NONE;
                computeConnection(side);
            }
        }
    }

    public void onRemoved() {

        if (isClient()) {
            return;
        }

        for (ConnectionInfo info : neighbors) {
            if (info.neighbor != null) {
                info.neighbor.disconnect(info.side.getOpposite());
                if (parent.isBundled() && info.neighbor.isBundled()) {
                    for (EnumDyeColor c : EnumDyeColor.values()) {
                        info.neighbor.propagate(c);
                    }
                } else if (parent.isBundled()) {
                    info.neighbor.propagate(info.neighbor.getColor());
                } else {
                    info.neighbor.propagate(parent.getColor());
                }
                parent.disconnect(info.side);
            }
        }
    }

    public void onLoaded() {

        if (isClient()) {
            return;
        }

        for (EnumCircuitSide side : EnumCircuitSide.VALUES) {
            ConnectionInfo info = neighbors[side.ordinal()];
            if (info.type.canConnect) {
                IRedstoneConductor neighbor = getWireNeighbor(parent.getCircuit(), parent.getPos(), side, parent.getSlot());
                if (neighbor != null) {
                    info.neighbor = neighbor;
                }
            } else {
                info.neighbor = null;
            }
        }
    }

    public void onCircuitAdded() {

        if (isClient()) {
            return;
        }

        if (parent.isBundled()) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                propagate(c);
            }
        } else {
            propagate(parent.getColor());
        }
    }

    public void rotate(Rotation rotation) {

        if (isClient()) {
            return;
        }

        ConnectionInfo[] neighbors2 = new ConnectionInfo[neighbors.length];
        for (ConnectionInfo info : neighbors) {
            EnumCircuitSide side = info.side.rotate(rotation);
            ConnectionInfo nInfo = neighbors2[side.ordinal()] = new ConnectionInfo(side);
            nInfo.type = info.type;
        }
        System.arraycopy(neighbors2, 0, neighbors, 0, neighbors.length);
    }

    public void rotatePost() {

        if (isClient()) {
            return;
        }

        onLoaded();
        if (parent.isBundled()) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                propagate(c);
            }
        } else {
            propagate(parent.getColor());
        }
    }

    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (isClient()) {
            return;
        }

        ConnectionInfo info = neighbors[side.ordinal()];
        EnumConnectionType prevType = info.type;
        computeConnection(side);
        if (prevType.canConnect || info.type.canConnect) {
            if (parent.isBundled()) {
                for (EnumDyeColor c : EnumDyeColor.values()) {
                    propagate(c);
                }
            } else {
                propagate(parent.getColor());
            }
        }
    }

    private boolean isClient() {

        return parent.getCircuit().getWorld() != null && parent.getCircuit().getWorld().isRemote;
    }

    public static IRedstoneConductor getWireNeighbor(ICircuit circuit, BlockPos pos, EnumCircuitSide side, EnumComponentSlot slot) {

        Pair<IComponent, EnumComponentSlot> c = ComponentBase.getNeighborPairConditioned(circuit, pos, slot, side,
                (comp, s) -> comp instanceof IRedstoneConductor || !(comp instanceof IWirePassthroughComponent)
                        || ((IWirePassthroughComponent) comp).blocksWire());
        return c != null && c.getKey() instanceof IRedstoneConductor ? (IRedstoneConductor) c.getKey() : null;
    }

    public static class ConnectionInfo implements IConnectionInfo {

        public final EnumCircuitSide side;
        public EnumConnectionType type = EnumConnectionType.NONE;
        public IRedstoneConductor neighbor;

        public ConnectionInfo(EnumCircuitSide side) {

            this.side = side;
        }

        @Override
        public EnumCircuitSide getSide() {

            return side;
        }

        @Override
        public EnumConnectionType getType() {

            return type;
        }

        @Override
        public IRedstoneConductor getNeighbor() {

            return neighbor;
        }

    }

}
