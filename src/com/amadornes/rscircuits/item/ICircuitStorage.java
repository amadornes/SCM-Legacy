package com.amadornes.rscircuits.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;

public interface ICircuitStorage {

    public NBTTagCompound getCircuitData(EntityPlayer player, ItemStack stack);

    public boolean canOverrideCircuitData(EntityPlayer player, ItemStack stack);

    public ActionResult<ItemStack> overrideCircuitData(EntityPlayer player, ItemStack stack, NBTTagCompound tag);

}
