package com.amadornes.rscircuits.api.component;

import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;

import com.google.common.collect.Multimap;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.IStringSerializable;

@Deprecated
public interface IRedstoneConductor {

    public <T extends IConnectionInfo> T[] getNeighbors();

    public byte getInput(EnumDyeColor color);

    public void propagate(EnumDyeColor color);

    public void propagate(IRedstoneConductor from, EnumDyeColor color, Multimap<IRedstoneConductor, EnumDyeColor> wires,
            Set<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled);

    public void onPropagated(Set<EnumDyeColor> colors, byte power);

    public EnumDyeColor getColor();

    public boolean isColorBiased();

    public boolean isBundled();

    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated);

    public void disconnect(EnumCircuitSide side);

    @Deprecated
    public enum EnumConnectionType implements IStringSerializable {
        ANODE(true, false, true),
        CATHODE(true, true, false),
        NONE(false, false, false),
        @Deprecated
        DISABLED(false, false, false),
        BIDIRECTIONAL(true, true, true);

        public static final EnumConnectionType[] VALUES = values();

        public final boolean canConnect, isInput, isOutput;

        private EnumConnectionType(boolean canConnect, boolean isInput, boolean isOutput) {
            this.canConnect = canConnect;
            this.isInput = isInput;
            this.isOutput = isOutput;
        }

        @Override
        public String getName() {

            return name().toLowerCase();
        }

    }

    @Deprecated
    public interface IConnectionInfo {

        public EnumCircuitSide getSide();

        public EnumConnectionType getType();

        public IRedstoneConductor getNeighbor();

    }

}
