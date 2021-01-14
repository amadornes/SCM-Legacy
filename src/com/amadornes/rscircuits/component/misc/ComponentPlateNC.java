package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory.IDrawListener;
import com.amadornes.rscircuits.api.component.IWirePassthroughComponent;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentPlateNC extends ComponentBaseFace implements IWirePassthroughComponent {

    public static final PropertyInteger MODE = PropertyInteger.create("mode", 0, 2);

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "plate_noncond");

    private int mode = 0;

    public ComponentPlateNC(ICircuit circuit) {

        super(circuit, EnumComponentSlot.TOP);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_PLATE_NONCOND;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(MODE, mode);
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (mode == 0) {
            return false;
        }
        if (side == (mode == 2 ? EnumCircuitSide.TOP : EnumCircuitSide.BOTTOM)) {
            Pair<IComponent, EnumComponentSlot> component = getNeighborPair(this.slot, side.getOpposite());
            return component != null ? component.getKey().isOutput(component.getValue(), side) : false;
        }
        return false;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (mode == 0) {
            return false;
        }
        if (side == (mode == 1 ? EnumCircuitSide.TOP : EnumCircuitSide.BOTTOM)) {
            Pair<IComponent, EnumComponentSlot> component = getNeighborPair(this.slot, side.getOpposite());
            return component != null ? component.getKey().isOutput(component.getValue(), side) : false;
        }
        return false;
    }

    @Override
    public boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (mode == 0) {
            return false;
        }
        if (side == (mode == 1 ? EnumCircuitSide.TOP : EnumCircuitSide.BOTTOM)) {
            Pair<IComponent, EnumComponentSlot> component = getNeighborPair(this.slot, side.getOpposite());
            return component != null ? component.getKey().isStrongOutput(component.getValue(), side) : false;
        }
        return false;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (mode == 0) {
            return 0;
        }
        if (side == (mode == 1 ? EnumCircuitSide.TOP : EnumCircuitSide.BOTTOM)) {
            Pair<IComponent, EnumComponentSlot> component = getNeighborPair(this.slot, side.getOpposite());
            return component != null ? component.getKey().getOutputSignal(component.getValue(), side, color, bundled) : 0;
        }
        return 0;
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        update(side);
    }

    private void update(EnumCircuitSide side) {

        if (mode == 1 && (side == null || side == EnumCircuitSide.BOTTOM)) {
            IComponent c = getCircuit().getComponent(getPos().up(), EnumComponentSlot.BOTTOM);
            if (c != null) {
                c.onNeighborChange(EnumCircuitSide.BOTTOM, EnumComponentSlot.TOP, this, EnumCircuitUpdate.COMPONENT_UPDATE);
            }
        } else if (mode == 2 && (side == null || side == EnumCircuitSide.TOP)) {
            IComponent c = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            if (c != null) {
                c.onNeighborChange(EnumCircuitSide.TOP, EnumComponentSlot.TOP, this, EnumCircuitUpdate.COMPONENT_UPDATE);
            }
        }
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (hand == EnumHand.MAIN_HAND && heldItem != null && heldItem.getItem() == SCMItems.screwdriver && !player.isSneaking()) {
            if (!getCircuit().getWorld().isRemote) {
                mode = (mode + 1) % 3;
                getCircuit().notifyUpdate(getPos(), slot, EnumCircuitSide.TOP, EnumCircuitSide.BOTTOM);
                getCircuit().markDirty();
                getCircuit().sendUpdate(getPos(), slot, true);
            }
            return true;
        }
        return super.onActivated(player, hand, heldItem, hit);
    }

    @Override
    public boolean isSideSolid(EnumCircuitSide side) {

        return true;
    }

    @Override
    public boolean blocksWire() {

        return true;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 14 / 16D, 0, 1, 1, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_PLATE_NONCOND.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = super.writeToNBT(tag);
        tag.setInteger("mode", mode);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        mode = tag.getInteger("mode");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeInt(mode);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        mode = buf.readInt();
    }

    public static class Factory extends SimpleFactory<ComponentPlateNC> implements IDrawListener<ComponentPlateNC> {

        private int height = -1;

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, MODE);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/plate_noncond");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.TINY_PLATE_NONCOND.ordinal();
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.DRAW;
        }

        @Override
        public ComponentPlateNC getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, ComponentPlateNC previousData, Map<BlockPos, ComponentPlateNC> otherData,
                EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            if (height == -1) {
                if (use == EnumInstantanceUse.PLACEMENT) {
                    height = pos.getY();
                    PartCircuit.selectionBoxOffset = (2 * (height)) / 16F;
                }
                return new ComponentPlateNC(circuit);
            } else if (pos.getY() == height) {
                return new ComponentPlateNC(circuit);
            }
            return null;
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentPlateNC data, EnumPlacementType type,
                Map<BlockPos, ComponentPlateNC> otherData, boolean simulate) {

            return circuit.addComponent(pos, data, simulate);
        }

        @Override
        public void onStartDrawing(EntityPlayer player) {

        }

        @Override
        public void onFinishDrawing(EntityPlayer player) {

            height = -1;
            PartCircuit.selectionBoxOffset = 0;
        }

        @Override
        public ComponentPlateNC instantiate(ICircuit circuit) {

            return new ComponentPlateNC(circuit);
        }

    }

}
