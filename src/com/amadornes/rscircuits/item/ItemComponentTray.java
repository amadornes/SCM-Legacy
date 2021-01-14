package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.IItemWrapper;
import com.amadornes.rscircuits.network.NetworkHandler;
import com.amadornes.rscircuits.network.PacketTrayScroll;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class ItemComponentTray extends Item implements IItemWrapper, IScrollableItem {

    public ItemComponentTray() {

        setUnlocalizedName(SCM.MODID + ":component_tray");
        setMaxStackSize(1);
    }

    @Override
    public ItemStack getWrappedStack(ItemStack stack) {

        return new ItemStack(Blocks.REDSTONE_TORCH);
    }

    @Override
    public String getHighlightTip(ItemStack item, String displayName) {

        return "";
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return slotChanged;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {

        if (player.isSneaking()) {
            if (!world.isRemote) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    stack.setTagCompound(tag = new NBTTagCompound());
                }
                int mode = tag.getInteger("mode");
                tag.setInteger("mode", (mode + 1) % 2);
            }
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult<ItemStack>(EnumActionResult.PASS, stack);
    }

    @Override
    public boolean scroll(EntityPlayer player, ItemStack stack, int dwheel) {

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            stack.setTagCompound(tag = new NBTTagCompound());
        }
        int mode = tag.getInteger("mode");

        if (mode == 0) {
            if (player.isSneaking()) {
                NetworkHandler.instance.sendToServer(new PacketTrayScroll(dwheel < 0));
                return true;
            }
        } else if (mode == 1) {
            int slot = tag.getInteger("slot");
            if (!player.isSneaking() && ((dwheel < 0 && slot < 8) || (dwheel > 0 && slot > 0))) {
                NetworkHandler.instance.sendToServer(new PacketTrayScroll(dwheel < 0));
                return true;
            }
        }
        return false;
    }

}
