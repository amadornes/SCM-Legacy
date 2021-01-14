package com.amadornes.rscircuits.component.lamp;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.component.ComponentBaseInt;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class ComponentLampDriver extends ComponentBaseInt {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "lamp_driver");

    private int color = 0x000000;

    public ComponentLampDriver(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_INVSCANNER;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.allOf(EnumComponentSlot.class);
    }

    @Override
    public boolean isSideSolid(EnumCircuitSide side) {

        return side == EnumCircuitSide.TOP;
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return false;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side == EnumCircuitSide.TOP;
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return EnumConnectionType.CATHODE;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return 0;
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM) {
            Pair<IComponent, EnumComponentSlot> c = getNeighborPair(EnumComponentSlot.BOTTOM, EnumCircuitSide.BOTTOM);
            int color;
            if (c != null && c.getKey().getBundledConnection(c.getValue(), EnumCircuitSide.TOP).isOutput) {
                int r = c.getKey().getOutputSignal(c.getValue(), EnumCircuitSide.TOP, EnumDyeColor.RED, true) & 0xFF;
                int g = c.getKey().getOutputSignal(c.getValue(), EnumCircuitSide.TOP, EnumDyeColor.GREEN, true) & 0xFF;
                int b = c.getKey().getOutputSignal(c.getValue(), EnumCircuitSide.TOP, EnumDyeColor.BLUE, true) & 0xFF;
                color = (r << 16) + (g << 8) + (b << 0);
            } else {
                color = 0x000000;
            }
            if (color != this.color) {
                this.color = color;
                getCircuit().markDirty();
                getCircuit().sendUpdate(getPos(), EnumComponentSlot.BOTTOM, false);
            }
        }
    }

    public int getColor() {

        return color;
    }

    public boolean isOn() {

        return true;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.LAMP_DRIVER.ordinal());
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return box;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = super.writeToNBT(tag);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeInt(color);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        color = buf.readInt();
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

    }

    public static class Factory extends SimpleFactory<ComponentLampDriver> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/lamp_driver");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.LAMP_DRIVER.ordinal();
        }

        @Override
        public ComponentLampDriver instantiate(ICircuit circuit) {

            return new ComponentLampDriver(circuit);
        }

    }

}
