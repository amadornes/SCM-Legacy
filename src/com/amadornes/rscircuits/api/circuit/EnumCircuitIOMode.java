package com.amadornes.rscircuits.api.circuit;

import java.util.Locale;

import net.minecraft.util.IStringSerializable;

public enum EnumCircuitIOMode implements IStringSerializable {

    REGULAR,
    BUNDLED;

    @Override
    public String getName() {

        return name().toLowerCase(Locale.ENGLISH);
    }

}
