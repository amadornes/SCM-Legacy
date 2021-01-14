package com.amadornes.rscircuits.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IScrollableItem {

    public boolean scroll(EntityPlayer player, ItemStack stack, int dwheel);

}
