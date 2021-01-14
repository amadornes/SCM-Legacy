package com.amadornes.rscircuits.network;

import com.amadornes.rscircuits.init.SCMItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketTrayScroll extends Packet<PacketTrayScroll> {

    private boolean direction;

    public PacketTrayScroll(boolean direction) {
        this.direction = direction;
    }

    public PacketTrayScroll() {
    }

    @Override
    public void handleClientSide(EntityPlayer player) {

    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
            ItemStack stack = player.getHeldItemMainhand();
            if (stack != null && stack.getItem() == SCMItems.component_tray) {
                NBTTagCompound tag = stack.getTagCompound();
                if (tag == null) {
                    stack.setTagCompound(tag = new NBTTagCompound());
                }
                int slot = tag.getInteger("slot");
                if (direction) {
                    slot = (slot + 1) % 9;
                } else {
                    slot = (slot + 8) % 9;
                }
                tag.setInteger("slot", slot);
            }
        });
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeBoolean(direction);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        direction = buf.readBoolean();
    }

}
