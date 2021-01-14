package com.amadornes.rscircuits.network;

import com.amadornes.rscircuits.init.SCMItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketColorPick extends Packet<PacketColorPick> {

    private int color;

    public PacketColorPick(int color) {
        this.color = color;
    }

    public PacketColorPick() {
    }

    @Override
    public void handleClientSide(EntityPlayer player) {

    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
            ItemStack stack = player.getHeldItemMainhand();
            if (stack == null || stack.getItem() != SCMItems.palette_and_brush) {
                stack = player.getHeldItemOffhand();
            }
            if (stack != null && stack.getItem() == SCMItems.palette_and_brush) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    stack.setTagCompound(tag = new NBTTagCompound());
                }
                tag.setInteger("color", color);
            }
        });
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeInt(color);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        color = buf.readInt();
    }

}
