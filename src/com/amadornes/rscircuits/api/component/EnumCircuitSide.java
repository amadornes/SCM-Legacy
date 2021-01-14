package com.amadornes.rscircuits.api.component;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;

public enum EnumCircuitSide {

    BOTTOM(EnumFacing.DOWN),
    TOP(EnumFacing.UP),
    BACK(EnumFacing.NORTH),
    FRONT(EnumFacing.SOUTH),
    RIGHT(EnumFacing.WEST),
    LEFT(EnumFacing.EAST),
    UNKNOWN(null);

    public static final EnumCircuitSide[] VALUES = { BOTTOM, TOP, BACK, FRONT, RIGHT, LEFT };
    public static final EnumCircuitSide[] HORIZONTALS = { BACK, FRONT, RIGHT, LEFT };
    public static final EnumCircuitSide[] HORIZONTALS_ROT = { BACK, LEFT, FRONT, RIGHT };
    public static final EnumCircuitSide[] VERTICALS = { BOTTOM, TOP };

    public final EnumFacing face;

    private EnumCircuitSide(EnumFacing face) {
        this.face = face;
    }

    public EnumCircuitSide getOpposite() {

        return VALUES[ordinal() ^ 1];
    }

    public EnumComponentSlot getSlot() {

        return EnumComponentSlot.VALUES[ordinal()];
    }

    public EnumCircuitSide rotate(Rotation rotation) {

        return VALUES[rotation.rotate(face).ordinal()];
    }

}
