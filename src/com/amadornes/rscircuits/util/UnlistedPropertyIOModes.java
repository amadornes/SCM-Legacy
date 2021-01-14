package com.amadornes.rscircuits.util;

import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;

import net.minecraftforge.common.property.IUnlistedProperty;
import scala.actors.threadpool.Arrays;

public class UnlistedPropertyIOModes implements IUnlistedProperty<EnumCircuitIOMode[]> {

    @Override
    public String getName() {

        return "mode";
    }

    @Override
    public boolean isValid(EnumCircuitIOMode[] value) {

        return true;
    }

    @Override
    public Class<EnumCircuitIOMode[]> getType() {

        return EnumCircuitIOMode[].class;
    }

    @Override
    public String valueToString(EnumCircuitIOMode[] value) {

        return Arrays.toString(value);
    }

}
