package com.amadornes.rscircuits.circuit;

import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ICircuitContainer {

    public boolean isInWorld();

    public World getWorld();

    public BlockPos getPos();

    public EnumFacing getFace();

    public Circuit getCircuitAt(BlockPos pos, EnumFacing face);

    public void markDirty();

    public void markRenderUpdate();

    public void notifyNeighbors();

    public void notifyNeighbor(EnumCircuitSide side, boolean strong);

    public void sendUpdatePacket();

    public boolean isEncapsulated();

    public void sendCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buf);

    public byte getInput(EnumCircuitSide side, EnumDyeColor color, boolean bundled);

    public void spawnMagicSmoke(BlockPos pos);

    public void spawnStack(ItemStack stack);

    public void onCleared();

}
