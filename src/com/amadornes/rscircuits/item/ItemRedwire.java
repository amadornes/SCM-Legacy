package com.amadornes.rscircuits.item;

import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.part.PartRedstoneWire;

import mcmultipart.item.ItemMultiPart;
import mcmultipart.multipart.IMultipart;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ItemRedwire extends ItemMultiPart {

    public ItemRedwire() {

        setUnlocalizedName(SCM.MODID + ":redwire");
    }

    @Override
    public IMultipart createPart(World world, BlockPos pos, EnumFacing side, Vec3d hit, ItemStack stack, EntityPlayer player) {

        side = EnumFacing.DOWN;
        if (world.getBlockState(pos.offset(side)).isSideSolid(world, pos.offset(side), side.getOpposite()))
            return new PartRedstoneWire(side, null);
        return null;
    }

    @Override
    public boolean getHasSubtypes() {

        return true;
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {

        for (int i = 0; i < 18; i++) {
            subItems.add(new ItemStack(itemIn, 1, i));
        }
    }

}
