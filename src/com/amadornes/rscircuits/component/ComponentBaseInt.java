package com.amadornes.rscircuits.component;

import java.util.List;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.ComponentBase;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

public abstract class ComponentBaseInt extends ComponentBase {

    public static IProperty<Boolean> ON = PropertyBool.create("on");
    public static final PropertyInteger ROTATION = PropertyInteger.create("rotation", 0, 3);

    public ComponentBaseInt(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public List<ItemStack> getPlacementItems() {

        return getDrops();
    }

    public abstract void serializePlacement(PacketBuffer buf);

    public abstract void deserializePlacement(PacketBuffer buf);

}
