package com.amadornes.rscircuits.component.digital;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.component.ComponentBaseFaceRotatable;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentLever extends ComponentBaseFaceRotatable {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "lever");

    private boolean input = false, prevInput = false;
    private boolean state = false;

    public ComponentLever(ICircuit circuit, int rotation) {

        super(circuit, EnumComponentSlot.BOTTOM, rotation);
    }

    public ComponentLever(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_LEVER;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, state).withProperty(ROTATION, rotation);
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (side != EnumCircuitSide.TOP && side != EnumCircuitSide.BOTTOM) {
            return ((side.ordinal() - 2) & 0x2) == ((rotation == 0 || rotation == 2) ? 2 : 0);
        }
        return side == EnumCircuitSide.BOTTOM;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (side != EnumCircuitSide.TOP && side != EnumCircuitSide.BOTTOM) {
            return ((side.ordinal() - 2) & 0x2) != ((rotation == 0 || rotation == 2) ? 2 : 0);
        }
        return side == EnumCircuitSide.TOP;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (side == EnumCircuitSide.TOP) {
            return state ? (byte) 255 : 0;
        }
        return ((side.ordinal() & 0x1) == ((state ? 1 : 0) ^ (rotation == 0 || rotation == 3 ? 1 : 0))) ? (byte) 255 : 0;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (super.onActivated(player, hand, heldItem, hit)) {
            return true;
        }

        if (!getCircuit().getWorld().isRemote) {
            toggle();
        }
        return true;
    }

    @Override
    public void onAddedPost() {

        updateInput();
        prevInput = input;
    }

    @Override
    public void onCircuitAdded() {

        updateInput();
        prevInput = input;
    }

    @Override
    public void rotatePost() {

        updateInput();
        if (input && !prevInput) {
            toggle();
        }
        prevInput = input;

        super.rotatePost();
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        if (isInput(this.slot, side)) {
            updateInput();
            getCircuit().scheduleTick(this, 0, 0, input);
        }
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        boolean input = (boolean) data;
        if (input && !prevInput) {
            toggle();
        }
        prevInput = input;
    }

    private void updateInput() {

        input = false;
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            if (isInput(this.slot, s) && getInputAny(EnumComponentSlot.BOTTOM, s) != 0) {
                input = true;
                break;
            }
        }
        if (!input) {
            IComponent c = getCircuit().getComponent(getPos().down(), EnumComponentSlot.TOP);
            if (c != null) {
                if (c.getOutputSignalAny(EnumComponentSlot.TOP, EnumCircuitSide.TOP) != 0) {
                    input = true;
                }
            }
        }
    }

    private void toggle() {

        state = !state;
        getCircuit().sendUpdate(getPos(), slot, false);
        getCircuit().markDirty();

        EnumCircuitSide s = rotation == 0 || rotation == 2 ? EnumCircuitSide.FRONT : EnumCircuitSide.RIGHT;
        getCircuit().notifyUpdate(getPos(), slot, EnumCircuitSide.TOP, s, s.getOpposite());
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        if (rotation % 2 == 0) {
            boxes.add(new AxisAlignedBB(2 / 16F, 0, 0, 14 / 16F, 6 / 16D, 1));
        } else {
            boxes.add(new AxisAlignedBB(0, 0, 2 / 16F, 1, 6 / 16D, 14 / 16F));
        }
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(Blocks.LEVER);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setBoolean("input", input);
        tag.setBoolean("prevInput", prevInput);
        tag.setBoolean("state", state);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        input = tag.getBoolean("input");
        prevInput = tag.getBoolean("prevInput");
        state = tag.getBoolean("state");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeBoolean(state);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        state = buf.readBoolean();
    }

    public static class Factory extends RotatableFactory<ComponentLever> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON, ROTATION);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/lever");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == Item.getItemFromBlock(Blocks.LEVER);
        }

        @Override
        public ComponentLever getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, ComponentLever previousData, Map<BlockPos, ComponentLever> otherData,
                EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            return new ComponentLever(circuit, getRotation(player));
        }

        @Override
        public ComponentLever instantiate(ICircuit circuit) {

            return new ComponentLever(circuit);
        }

    }

}
