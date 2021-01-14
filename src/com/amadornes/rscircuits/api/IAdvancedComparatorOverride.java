package com.amadornes.rscircuits.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IAdvancedComparatorOverride {

    public byte getAdvancedComparatorInputOverride(IBlockState state, World world, BlockPos pos, EnumFacing face);

}
