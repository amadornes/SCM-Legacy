package com.amadornes.rscircuits.api.circuit;

import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface ICircuit {

    public World getWorld();

    public BlockPos getPos();

    public EnumFacing getFace();

    public boolean isEncapsulated();

    public EnumCircuitIOMode getIOMode(EnumCircuitSide side);

    public IComponent getComponent(BlockPos pos, EnumComponentSlot slot);

    public boolean addComponent(BlockPos pos, IComponent component, boolean simulate);

    public void removeComponent(IComponent component);

    public void replaceComponent(IComponent component, IComponent newComponent);

    public boolean isSideSolid(BlockPos pos, EnumCircuitSide side);

    public void scheduleTick(IComponent component, int delay, int type, Object data);

    public void notifyUpdate(BlockPos pos, EnumComponentSlot slot, EnumCircuitSide... directions);

    public void notifyUpdateAll(BlockPos pos, EnumComponentSlot slot);

    public void sendUpdate(BlockPos pos, EnumComponentSlot slot, boolean reRender);

    public void markDirty();

    public ICircuit getCircuit(BlockPos pos);

    public byte getInput(EnumCircuitSide side, EnumDyeColor color, boolean bundled);

    void sendCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buffer);

    public void spawnMagicSmoke(BlockPos pos);

    public void spawnStack(ItemStack stack);

    public void rotate(Rotation rotation);

    public float computeComplexity();

}
