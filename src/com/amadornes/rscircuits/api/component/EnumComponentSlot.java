package com.amadornes.rscircuits.api.component;

public enum EnumComponentSlot {

    BOTTOM(EnumCircuitSide.BOTTOM),
    TOP(EnumCircuitSide.TOP),
    BACK(EnumCircuitSide.BACK),
    FRONT(EnumCircuitSide.FRONT),
    RIGHT(EnumCircuitSide.RIGHT),
    LEFT(EnumCircuitSide.LEFT),
    CENTER(null);

    public static final EnumComponentSlot[] VALUES = values();

    public final EnumCircuitSide side;

    private EnumComponentSlot(EnumCircuitSide direction) {
        this.side = direction;
    }

    public EnumComponentSlot getOpposite() {

        return VALUES[ordinal() ^ 1];
    }

}
