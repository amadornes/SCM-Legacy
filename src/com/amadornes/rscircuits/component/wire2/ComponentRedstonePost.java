package com.amadornes.rscircuits.component.wire2;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;
import com.google.common.collect.Multimap;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;

public class ComponentRedstonePost extends ComponentWirePostBase {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "post_rs");

    private byte power = (byte) 0x00;
    private boolean shouldOutputPower = true, shouldPropagate = true;

    public ComponentRedstonePost(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_POST + ComponentReference.COMPLEXITY_WIRE;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public boolean isBundled() {

        return false;
    }

    @Override
    public EnumDyeColor getColor() {

        return EnumDyeColor.SILVER;
    }

    @Override
    public boolean isColorBiased() {

        return false;
    }

    @Override
    public byte getInput(EnumDyeColor color) {

        if (!shouldPropagate) {
            return 0;
        }
        int input = 0;
        Pair<IComponent, EnumComponentSlot> neighbor;

        neighbor = getNeighbors()[EnumCircuitSide.TOP.ordinal()].getNeighbor() == null
                ? getNeighborPair(EnumComponentSlot.CENTER, EnumCircuitSide.TOP) : null;
        if (neighbor != null && !(neighbor.getKey() instanceof IRedstoneConductor)) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                input = Math.max(input, neighbor.getKey().getOutputSignal(neighbor.getValue(), EnumCircuitSide.BOTTOM, c, false) & 0xFF);
            }
        }

        neighbor = getNeighbors()[EnumCircuitSide.BOTTOM.ordinal()].getNeighbor() == null
                ? getNeighborPair(EnumComponentSlot.CENTER, EnumCircuitSide.BOTTOM) : null;
        if (neighbor != null && !(neighbor.getKey() instanceof IRedstoneConductor)) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                input = Math.max(input, neighbor.getKey().getOutputSignal(neighbor.getValue(), EnumCircuitSide.TOP, c, false) & 0xFF);
            }
        }

        return (byte) input;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return !bundled && shouldOutputPower && isOutput(slot, side) ? power : 0;
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() == Axis.Y;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() == Axis.Y;
    }

    @Override
    public boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side == EnumCircuitSide.TOP;
    }

    @Override
    public void propagate(IRedstoneConductor from, EnumDyeColor color, Multimap<IRedstoneConductor, EnumDyeColor> wires,
            Set<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled) {

        if (shouldPropagate) {
            super.propagate(from, color, wires, scheduled);
        }
    }

    @Override
    public void onPropagated(Set<EnumDyeColor> colors, byte power) {

        if (this.power != power) {
            this.power = power;
            super.onPropagated(colors, power);
            getCircuit().markDirty();
            getCircuit().notifyUpdate(getPos(), getSlot(),
                    cond.getNeighbors()[EnumCircuitSide.BOTTOM.ordinal()].getNeighbor() == null ? EnumCircuitSide.BOTTOM : null,
                    cond.getNeighbors()[EnumCircuitSide.TOP.ordinal()].getNeighbor() == null ? EnumCircuitSide.TOP : null);
        }
    }

    @Override
    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated) {

        return side.face.getAxis() == Axis.Y && !conductor.isBundled() && super.connect(side, conductor, type, simulated);
    }

    @Override
    public void onConnect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type) {

        getCircuit().markDirty();
    }

    @Override
    public void onDisconnect(EnumCircuitSide side) {

        getCircuit().markDirty();
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.REDSTONE_STICK.ordinal());
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        super.addSelectionBoxes(boxes);
        double d = 7 / 16D;
        boxes.add(new AxisAlignedBB(d, 0, d, 1 - d, 1, 1 - d));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setByte("power", power);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);

        power = tag.getByte("power");
    }

    @Override
    public void debug(EntityPlayer player) {

        player.addChatMessage(new TextComponentString("Power Level: " + (power & 0xFF)));
    }

    public static class Factory extends SimpleFactory<ComponentRedstonePost> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/post_rs");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.REDSTONE_STICK.ordinal();
        }

        @Override
        public ComponentRedstonePost instantiate(ICircuit circuit) {

            return new ComponentRedstonePost(circuit);
        }

    }

}
