package com.amadornes.rscircuits.component.lamp;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IPaintableComponent;
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
import net.minecraft.util.ResourceLocation;

public class ComponentLamp extends ComponentBaseButton implements IPaintableComponent {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "lamp");

    protected boolean state = false;
    protected EnumDyeColor color = EnumDyeColor.YELLOW;

    public ComponentLamp(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_LAMP;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, isOn());
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side != EnumCircuitSide.TOP;
    }

    @Override
    public void onAdded() {

        onNeighborChange(null, null, null, null);
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        boolean prevState = state;
        state = getInput() != 0;
        if (state != prevState) {
            getCircuit().sendUpdate(getPos(), this.slot, false);
            getCircuit().markDirty();
        }
    }

    protected int getColor() {

        IComponent component = getCircuit() != null && getPos() != null ? getNeighbor(EnumComponentSlot.BOTTOM, EnumCircuitSide.BOTTOM)
                : null;
        if (component != null && component instanceof ComponentLampDriver) {
            return ((ComponentLampDriver) component).getColor();
        }
        return color.getMapColor().colorValue;
    }

    public boolean isOn() {

        IComponent component = getCircuit() != null && getPos() != null ? getNeighbor(EnumComponentSlot.BOTTOM, EnumCircuitSide.BOTTOM)
                : null;
        if (component != null && component instanceof ComponentLampDriver) {
            return ((ComponentLampDriver) component).isOn();
        }
        return state;
    }

    @Override
    public int getColorMultiplier(int index) {

        return index == 0 ? getColor() : super.getColorMultiplier(index);
    }

    @Override
    public boolean paint(EnumDyeColor color) {

        if (getCircuit().getWorld().isRemote) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(color.getMetadata());
            getCircuit().sendCustomPayload(getPos(), slot, buf);
            return this.color != color;
        } else {
            this.color = color;
            return true;
        }
    }

    @Override
    public void handleCustomPayload(ByteBuf buf) {

        this.color = EnumDyeColor.byMetadata(buf.readByte() & 0xFF);
        getCircuit().sendUpdate(getPos(), slot, false);
        getCircuit().markDirty();
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_LAMP.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setBoolean("state", state);
        tag.setInteger("color", color.getMetadata());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        state = tag.getBoolean("state");
        if (tag.hasKey("color")) {
            color = EnumDyeColor.values()[tag.getInteger("color")];
        }
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeBoolean(state);
        buf.writeEnumValue(color);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        state = buf.readBoolean();
        color = buf.readEnumValue(EnumDyeColor.class);
    }

    public static class Factory extends SimpleFactory<ComponentLamp> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/lamp");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.TINY_LAMP.ordinal();
        }

        @Override
        public ComponentLamp instantiate(ICircuit circuit) {

            return new ComponentLamp(circuit);
        }

    }

}
