package com.amadornes.rscircuits.component;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.init.SCMItems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class ComponentBaseFaceRotatable extends ComponentBaseFace {

    protected int rotation;

    public ComponentBaseFaceRotatable(ICircuit circuit, EnumComponentSlot slot, int rotation) {

        super(circuit, slot);
        this.rotation = rotation;
    }

    public ComponentBaseFaceRotatable(ICircuit circuit, EnumComponentSlot slot) {

        super(circuit, slot);
    }

    @Override
    public void rotatePre(Rotation rotation) {

        this.rotation = (this.rotation + rotation.ordinal()) % 4;
    }

    @Override
    public void rotatePost() {

        getCircuit().notifyUpdateAll(getPos(), slot);
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (hand == EnumHand.MAIN_HAND && heldItem != null && heldItem.getItem() == SCMItems.screwdriver && !player.isSneaking()) {
            if (!getCircuit().getWorld().isRemote) {
                rotatePre(Rotation.CLOCKWISE_90);
                rotatePost();
                getCircuit().markDirty();
                getCircuit().notifyUpdateAll(getPos(), slot);
                getCircuit().sendUpdate(getPos(), slot, !isDynamic());
            }
            return true;
        }
        return super.onActivated(player, hand, heldItem, hit);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setInteger("rotation", rotation);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        rotation = tag.getInteger("rotation");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeInt(rotation);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        rotation = buf.readInt();
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        super.serializePlacement(buf);
        buf.writeInt(rotation);
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        super.deserializePlacement(buf);
        rotation = buf.readInt();
    }

    public static abstract class RotatableFactory<C extends ComponentBaseFaceRotatable> extends SimpleFactory<C> {

        public int getRotation(EntityPlayer player) {

            return MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 3.5D) & 3;
        }

    }

}
