package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
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

public class ComponentDelay extends ComponentBaseButton {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "delay");

    private int delay = 20, ticks = 0;
    private byte input = 0, state = 0;

    public ComponentDelay(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_DELAY;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, state != 0);
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

        return side.face.getAxis() != Axis.Y ? state : 0;
    }

    @Override
    public void tick() {

        ticks++;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (heldItem != null && (heldItem.getItem() == SCMItems.screwdriver || heldItem.getItem() == SCMItems.multimeter)) {
            // TODO: Add textbox to the gui
            SCM.proxy.displayTimerGui((d, s) -> "Quartz Resonator: " + d + (s ? " seconds" : " ticks"), s -> s ? "s" : "t", () -> delay,
                    d -> {
                        ByteBuf buf = Unpooled.buffer();
                        buf.writeInt(d);
                        getCircuit().sendCustomPayload(getPos(), slot, buf);
                    }, 1, 20 * 60 * 60, 20);
            return true;
        }

        return false;
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        byte prevState = state;
        state = (Byte) data;
        if (state != prevState) {
            getCircuit().notifyUpdate(getPos(), slot, EnumCircuitSide.HORIZONTALS);
            getCircuit().sendUpdate(getPos(), slot, false);
            getCircuit().markDirty();
        }
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        byte power = getInput();
        if (input != power) {
            input = power;
            getCircuit().scheduleTick(this, delay, ticks, input);
            getCircuit().markDirty();
        }
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.QUARTZ_RESONATOR.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setInteger("delay", delay);
        tag.setInteger("ticks", ticks);
        tag.setByte("input", input);
        tag.setByte("state", state);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        delay = tag.getInteger("delay");
        ticks = tag.getInteger("ticks");
        input = tag.getByte("input");
        state = tag.getByte("state");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeInt(delay);
        buf.writeBoolean(state != 0);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        delay = buf.readInt();
        state = (byte) (buf.readBoolean() ? 255 : 0);
    }

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("state", (byte) data);
        return tag;
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return tag.getByte("state");
    }

    @Override
    public void handleCustomPayload(ByteBuf buf) {

        delay = buf.readInt();
        getCircuit().sendUpdate(getPos(), slot, false);
    }

    public static class Factory extends SimpleFactory<ComponentDelay> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/delay");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.QUARTZ_RESONATOR.ordinal();
        }

        @Override
        public ComponentDelay instantiate(ICircuit circuit) {

            return new ComponentDelay(circuit);
        }

    }

}
