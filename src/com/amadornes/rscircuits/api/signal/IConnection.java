package com.amadornes.rscircuits.api.signal;

import net.minecraft.util.IStringSerializable;

public interface IConnection {

    public static enum EnumRedstoneConnection implements IConnection, IStringSerializable {
        NONE(false, false),
        INPUT(true, false),
        OUTPUT(false, true),
        BIDIRECTIONAL(true, true);

        public static final EnumRedstoneConnection[] VALUES = values();

        private final boolean input, output;

        private EnumRedstoneConnection(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public String getName() {

            return name().toLowerCase();
        }

        public boolean isInput() {

            return input;
        }

        public boolean isOutput() {

            return output;
        }

        public EnumRedstoneConnection getOpposite() {

            return this == INPUT ? OUTPUT : this == OUTPUT ? INPUT : this;
        }

        public EnumRedstoneConnection getState() {

            return this == EnumRedstoneConnection.BIDIRECTIONAL ? OUTPUT : this;
        }

        public static EnumRedstoneConnection of(boolean in, boolean out) {

            return !in && !out ? NONE : in && !out ? INPUT : !in && out ? OUTPUT : BIDIRECTIONAL;
        }

    }

    public static interface IBundledConnection extends IConnection {

    }

}
