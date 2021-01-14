package com.amadornes.rscircuits.api;

import net.minecraft.item.ItemStack;

public interface IItemWrapper {

    public ItemStack getWrappedStack(ItemStack stack);

}
