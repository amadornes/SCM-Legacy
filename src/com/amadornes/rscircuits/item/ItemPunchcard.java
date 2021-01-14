package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.client.gui.GuiPunchcard;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemPunchcard extends Item {

    public ItemPunchcard() {

        setUnlocalizedName(SCM.MODID + ":punchcard");
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {

        if (world.isRemote) {
            openGUI(stack);
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    private void openGUI(ItemStack stack) {

        boolean[][] holes = new boolean[8][12];
        if (stack.hasTagCompound()) {
            int[] data = stack.getTagCompound().getIntArray("punchcard");
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 12; j++) {
                    holes[i][j] = (data[j] & (1 << i)) != 0;
                }
            }
        }
        Minecraft.getMinecraft().displayGuiScreen(new GuiPunchcard(holes));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {

        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

}
