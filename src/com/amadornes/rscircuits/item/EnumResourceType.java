package com.amadornes.rscircuits.item;

public enum EnumResourceType {

    PILE_OF_REDSTONE(64, 1),
    QUARTZ_RESONATOR(64, 1),
    ENDER_PULSAR(64, 1),
    ENERGIZED_GLOWSTONE(64, 1),
    TINY_LAMP(64, 1),
    TINY_PLATE(64, 1),
    PILE_OF_GLOWSTONE(64, 1),
    PILE_OF_GLOWING_REDSTONE(64, 1),
    REDSTONE_STICK(64, 1),
    PAINT_BRUSH(1, 1),
    PALETTE(1, 1),
    TINY_PLATE_NONCOND(64, 1),
    TINY_BUNDLED_WIRE(64, 1),
    BUNDLED_STICK(64, 1),
    TINY_LAMP_SEGMENTED(64, 1),
    ADDER(64, 1),
    SUBTRACTOR(64, 1),
    MULTIPLIER(64, 1),
    DIVIDER(64, 1),
    CONSTANT(64, 1),
    INV_SCANNER(64, 1),
    PUNCHCARD_READER(64, 1),
    LIGHT_SENSOR(64, 1),
    LAMP_DRIVER(64, 1),
    TINY_NOTE_BLOCK(64, 1);

    public static final EnumResourceType[] VALUES = values();

    public final int maxStackSize, metaValues;

    private EnumResourceType(int maxStackSize, int metaValues) {

        this.maxStackSize = maxStackSize;
        this.metaValues = metaValues;
    }

}
