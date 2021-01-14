package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ItemScrewdriver extends Item {

    public ItemScrewdriver() {

        setUnlocalizedName(SCM.MODID + ":screwdriver");
        setMaxStackSize(1);
        setMaxDamage(1024);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {

        return true;
    }

    @Override
    public float getStrVsBlock(ItemStack stack, IBlockState state) {

        return 0;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {

        return true;
    }

    @Override
    public boolean canHarvestBlock(IBlockState blockIn) {

        return false;
    }

}
