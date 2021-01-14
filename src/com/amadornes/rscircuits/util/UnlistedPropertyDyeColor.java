package com.amadornes.rscircuits.util;

import net.minecraft.item.EnumDyeColor;
import net.minecraftforge.common.property.IUnlistedProperty;

public class UnlistedPropertyDyeColor implements IUnlistedProperty<EnumDyeColor> {

    @Override
    public String getName() {

        return "color";
    }

    @Override
    public boolean isValid(EnumDyeColor value) {

        return true;
    }

    @Override
    public Class<EnumDyeColor> getType() {

        return EnumDyeColor.class;
    }

    @Override
    public String valueToString(EnumDyeColor value) {

        return value.name().toLowerCase();
    }

}
