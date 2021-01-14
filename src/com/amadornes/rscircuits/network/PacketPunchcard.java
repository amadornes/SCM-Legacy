package com.amadornes.rscircuits.network;

import com.amadornes.rscircuits.init.SCMItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketPunchcard extends Packet<PacketPunchcard> {

    private int x, y;

    public PacketPunchcard(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public PacketPunchcard() {
    }

    @Override
    public void handleClientSide(EntityPlayer player) {

    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
            ItemStack stack = player.getHeldItemMainhand();
            if (stack == null || stack.getItem() != SCMItems.punchcard) {
                return;
            }
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                stack.setTagCompound(tag = new NBTTagCompound());
            }
            int[] data;
            if (tag.hasKey("punchcard")) {
                data = tag.getIntArray("punchcard");
            } else {
                tag.setIntArray("punchcard", data = new int[12]);
            }
            data[y] |= 1 << x;
        });
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeInt(x);
        buf.writeInt(y);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        x = buf.readInt();
        y = buf.readInt();
    }

}
