package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory.IDrawListener;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.api.component.IWirePassthroughComponent;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentPlate extends ComponentBaseFace implements IWirePassthroughComponent {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "plate");

    public ComponentPlate(ICircuit circuit) {

        super(circuit, EnumComponentSlot.TOP);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_PLATE;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (side == slot.side) {
            IComponent component = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            return component != null ? component.isOutput(EnumComponentSlot.CENTER, side) : false;
        }
        return false;
    }

    @Override
    public boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        if (side == slot.side) {
            IComponent component = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            return component != null ? component.isStrongOutput(EnumComponentSlot.CENTER, side) : false;
        }
        return false;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (side == slot.side) {
            IComponent component = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            return component != null ? component.getOutputSignal(EnumComponentSlot.CENTER, side, color, bundled) : 0;
        }
        return 0;
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        if (side == slot.side) {
            IComponent component = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            return component != null ? component.getBundledConnection(EnumComponentSlot.CENTER, side) : EnumConnectionType.NONE;
        }
        return EnumConnectionType.NONE;
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM) {
            IComponent c = getCircuit().getComponent(getPos().up(), EnumComponentSlot.BOTTOM);
            if (c != null) {
                c.onNeighborChange(side, slot, component, updateType);
            }
        } else if (side == EnumCircuitSide.TOP) {
            IComponent c = getCircuit().getComponent(getPos(), EnumComponentSlot.CENTER);
            if (c != null) {
                c.onNeighborChange(side, slot, component, updateType);
            }
        }
    }

    @Override
    public boolean isSideSolid(EnumCircuitSide side) {

        return true;
    }

    @Override
    public boolean blocksWire() {

        return false;
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

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_PLATE.ordinal());
    }

    public static class Factory extends SimpleFactory<ComponentPlate> implements IDrawListener<ComponentPlate> {

        private int height = -1;

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/plate");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.TINY_PLATE.ordinal();
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.DRAW;
        }

        @Override
        public ComponentPlate getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, ComponentPlate previousData, Map<BlockPos, ComponentPlate> otherData,
                EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            if (height == -1) {
                if (use == EnumInstantanceUse.PLACEMENT) {
                    height = pos.getY();
                    PartCircuit.selectionBoxOffset = (2 * (height)) / 16F;
                }
                return new ComponentPlate(circuit);
            } else if (pos.getY() == height) {
                return new ComponentPlate(circuit);
            }
            return null;
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentPlate data, EnumPlacementType type,
                Map<BlockPos, ComponentPlate> otherData, boolean simulate) {

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
        public ComponentPlate instantiate(ICircuit circuit) {

            return new ComponentPlate(circuit);
        }

    }

}
