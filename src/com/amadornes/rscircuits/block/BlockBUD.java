package com.amadornes.rscircuits.block;

import java.util.Random;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockBUD extends BlockDirectional {

    public static final IProperty<Boolean> ON = PropertyBool.create("on");

    public BlockBUD() {

        super(Material.ROCK);
        setHardness(0.5F);
        setUnlocalizedName("rscircuits:update_detector");
        setDefaultState(getDefaultState().withProperty(ON, false));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected BlockStateContainer createBlockState() {

        return new BlockStateContainer(this, FACING, ON);
    }

    @Override
    public int getMetaFromState(IBlockState state) {

        return state.getValue(FACING).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {

        return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta));
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer) {

        return this.getDefaultState().withProperty(FACING, BlockPistonBase.getFacingFromEntity(pos, placer).getOpposite());
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {

        return side == state.getValue(FACING);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {

        return side == state.getValue(FACING) && state.getValue(ON) ? 15 : 0;
    }

    @Override
    public int getStrongPower(IBlockState state, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {

        return side == state.getValue(FACING) && state.getValue(ON) ? 15 : 0;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {

        return true;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

        world.setBlockState(pos, state.withProperty(ON, false));
    }

    @Override
    public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {

        return true;
    }

    @SubscribeEvent
    public void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {

        if (event.getState().getBlock() == this) {
            return;
        }
        for (EnumFacing f : event.getNotifiedSides()) {
            BlockPos pos = event.getPos().offset(f);
            IBlockState state = event.getWorld().getBlockState(pos);
            if (state.getBlock() == this && state.getValue(FACING) == f.getOpposite()) {
                event.getWorld().setBlockState(pos, state.withProperty(ON, true));
                event.getWorld().scheduleBlockUpdate(pos, this, 2, 100);
            }
        }
    }

}
