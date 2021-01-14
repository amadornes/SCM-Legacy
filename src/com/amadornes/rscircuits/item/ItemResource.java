package com.amadornes.rscircuits.item;

import java.util.List;

import com.amadornes.rscircuits.SCM;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class ItemResource extends Item {

    // private final ItemBlock blockPlacer = new ItemBlock(SCM.lamp);

    public ItemResource() {

        setUnlocalizedName(SCM.MODID + ":resource");
    }

    private EnumResourceType getResource(ItemStack stack) {

        int meta = 0;
        for (EnumResourceType res : EnumResourceType.VALUES) {
            if (meta >= stack.getItemDamage()) {
                return res;
            }
            meta += res.metaValues;
        }
        return null;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {

        EnumResourceType res = getResource(stack);
        return super.getUnlocalizedName(stack) + (res != null && res.name() != null ? "." + res.name().toLowerCase() : "");
    }

    @Override
    public boolean getHasSubtypes() {

        return true;
    }

    @Override
    public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems) {

        int meta = 0;
        for (EnumResourceType res : EnumResourceType.VALUES) {
            subItems.add(new ItemStack(itemIn, 1, meta));
            meta += res.metaValues;
        }
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {

        EnumResourceType res = getResource(stack);
        return res != null ? res.maxStackSize : 0;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {

        return true;
    }

    // @Override
    // public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing
    // facing,
    // float hitX, float hitY, float hitZ) {
    //
    // if (ResourceType.VALUES[stack.getItemDamage()] == ResourceType.TINY_LAMP) {
    // return blockPlacer.onItemUse(stack, playerIn, worldIn, pos, hand, facing, hitX, hitY, hitZ);
    // }
    // return EnumActionResult.PASS;
    // }

}
