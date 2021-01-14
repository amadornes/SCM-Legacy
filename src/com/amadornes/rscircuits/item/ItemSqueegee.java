package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ItemSqueegee extends Item {

    public ItemSqueegee() {

        setUnlocalizedName(SCM.MODID + ":squeegee");
        setMaxStackSize(1);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {

        return true;
    }

}
