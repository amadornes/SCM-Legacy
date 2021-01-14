package com.amadornes.rscircuits.util;

import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import org.apache.commons.lang3.tuple.Triple;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.property.IUnlistedProperty;

public class UnlistedPropertyComponentStates
        implements IUnlistedProperty<Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>>> {

    @Override
    public String getName() {

        return "components";
    }

    @Override
    public boolean isValid(Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>> value) {

        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class<Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>>> getType() {

        return (Class) Map.class;
    }

    @Override
    public String valueToString(Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>> value) {

        return value.toString();
    }

}
