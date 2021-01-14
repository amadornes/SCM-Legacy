package com.amadornes.rscircuits.api.component;

import java.util.Map;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.google.common.base.Function;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public interface IComponentFactory<T> {

    public BlockStateContainer createBlockState();

    public ResourceLocation getModelPath();

    public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player);

    public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player);

    public T getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
            EntityPlayer player, EnumPlacementType type, T previousData, Map<BlockPos, T> otherData, EnumInstantanceUse use);

    public boolean placeComponent(ICircuit circuit, BlockPos pos, T data, EnumPlacementType type, Map<BlockPos, T> otherData,
            boolean simulate);

    public void drawPlacement(ICircuit circuit, BlockPos pos, T data, EnumPlacementType type, Map<BlockPos, T> otherData);

    public void serialize(PacketBuffer buf, Map<BlockPos, T> data, EntityPlayer player);

    public void deserialize(PacketBuffer buf, Map<BlockPos, T> data, Function<BlockPos, ICircuit> circuitProvider, EntityPlayer player);

    public IComponent instantiate(ICircuit circuit);

    public static interface IDrawListener<T> extends IComponentFactory<T> {

        public void onStartDrawing(EntityPlayer player);

        public void onFinishDrawing(EntityPlayer player);

    }

    public static interface IDrawHandler<T> extends IComponentFactory<T> {

        public boolean finishDrawing(ICircuit circuit, Map<BlockPos, T> dataMap, ItemStack stack, EntityPlayer player);

    }

    public enum EnumPlacementType {
        SINGLE,
        DRAW,
        LINE;
    }

    public enum EnumInstantanceUse {
        PLACEMENT,
        RENDER;
    }

}
