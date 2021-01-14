package com.amadornes.rscircuits.util;

import net.minecraftforge.common.property.IUnlistedProperty;

public class UnlistedPropertyName implements IUnlistedProperty<String> {

    @Override
    public String getName() {

        return "color";
    }

    @Override
    public boolean isValid(String value) {

        return true;
    }

    @Override
    public Class<String> getType() {

        return String.class;
    }

    @Override
    public String valueToString(String value) {

        return value == null ? "" : value;
    }

}
