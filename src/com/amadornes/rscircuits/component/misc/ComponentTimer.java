package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseButton;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class ComponentTimer extends ComponentBaseButton {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "timer");

    private int delay = 20, ticks = 0, length = 2;
    private boolean locked = false, shouldOutput = true;

    public ComponentTimer(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_TIMER;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, shouldOutput && ticks < length);
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side != EnumCircuitSide.TOP;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side != EnumCircuitSide.TOP;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return shouldOutput && ticks < length && side.face.getAxis() != Axis.Y ? (byte) 255 : 0;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (heldItem != null && (heldItem.getItem() == SCMItems.screwdriver || heldItem.getItem() == SCMItems.multimeter)) {
            SCM.proxy.displayTimerGui((d, s) -> "Ender Pulsar: " + d + (s ? " seconds" : " ticks"), s -> s ? "s" : "t", () -> delay, d -> {
                ByteBuf buf = Unpooled.buffer();
                buf.writeInt(d);
                getCircuit().sendCustomPayload(getPos(), slot, buf);
            }, 3, 20 * 60 * 60, 20);
            return true;
        }

        return false;
    }

    @Override
    public void onAdded() {

        getCircuit().scheduleTick(this, 1, 0, null);
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        if (getInput() != 0) {
            ticks = -1;
            boolean wasLocked = locked;
            locked = true;
            shouldOutput = false;
            if (!wasLocked) {
                getCircuit().notifyUpdateAll(getPos(), slot);
                getCircuit().sendUpdate(getPos(), slot, false);
                getCircuit().markDirty();
            }
        } else {
            boolean wasLocked = locked;
            locked = false;
            if (wasLocked) {
                getCircuit().notifyUpdateAll(getPos(), slot);
                getCircuit().sendUpdate(getPos(), slot, false);
                getCircuit().markDirty();
            }
        }
        ticks++;
        if (ticks == length) {
            getCircuit().notifyUpdateAll(getPos(), slot);
            getCircuit().sendUpdate(getPos(), slot, false);
            getCircuit().markDirty();
        }
        if (ticks >= delay) {
            ticks = 0;
            shouldOutput = true;
            getCircuit().notifyUpdateAll(getPos(), slot);
            getCircuit().sendUpdate(getPos(), slot, false);
            getCircuit().markDirty();
        }
        getCircuit().scheduleTick(this, 1, 0, null);
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.ENDER_PULSAR.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setInteger("delay", delay);
        tag.setInteger("ticks", ticks);
        tag.setInteger("length", length);
        tag.setBoolean("locked", locked);
        tag.setBoolean("shouldOutput", shouldOutput);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        delay = tag.getInteger("delay");
        ticks = tag.getInteger("ticks");
        length = tag.getInteger("length");
        locked = tag.getBoolean("locked");
        shouldOutput = tag.getBoolean("shouldOutput");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeInt(delay);
        buf.writeInt(ticks);
        buf.writeInt(length);
        buf.writeBoolean(shouldOutput);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        delay = buf.readInt();
        ticks = buf.readInt();
        length = buf.readInt();
        shouldOutput = buf.readBoolean();
    }

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        return new NBTTagCompound();
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return null;
    }

    @Override
    public void handleCustomPayload(ByteBuf buf) {

        delay = buf.readInt();
        getCircuit().sendUpdate(getPos(), slot, false);
    }

    public static class Factory extends SimpleFactory<ComponentTimer> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/timer");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.ENDER_PULSAR.ordinal();
        }

        @Override
        public ComponentTimer instantiate(ICircuit circuit) {

            return new ComponentTimer(circuit);
        }

    }

}
