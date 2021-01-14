package com.amadornes.rscircuits.component.lamp;

import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IPaintableComponent;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class ComponentLampSegmented extends ComponentLamp implements IPaintableComponent {

    public static final PropertyBool FRONT = PropertyBool.create("front");
    public static final PropertyBool BACK = PropertyBool.create("back");
    public static final PropertyBool LEFT = PropertyBool.create("left");
    public static final PropertyBool RIGHT = PropertyBool.create("right");

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "lamp_segm");

    public ComponentLampSegmented(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_LAMP_SEG;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, isOn()).withProperty(FRONT, isLampOn(EnumCircuitSide.FRONT))
                .withProperty(BACK, isLampOn(EnumCircuitSide.BACK)).withProperty(LEFT, isLampOn(EnumCircuitSide.LEFT))
                .withProperty(RIGHT, isLampOn(EnumCircuitSide.RIGHT));
    }

    private boolean isLampOn(EnumCircuitSide side) {

        if (getPos() == null || !isOn()) {
            return false;
        }
        IComponent c = getCircuit().getComponent(getPos().offset(side.face), slot);
        return c != null && c instanceof ComponentLampSegmented && ((ComponentLampSegmented) c).isOn();
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_LAMP_SEGMENTED.ordinal());
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 0.5, 1));
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return new AxisAlignedBB(0, 0, 0, 1, 0.5, 1);
    }

    public static class Factory extends SimpleFactory<ComponentLampSegmented> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON, FRONT, BACK, LEFT, RIGHT);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/lamp_segm");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.TINY_LAMP_SEGMENTED.ordinal();
        }

        @Override
        public ComponentLampSegmented instantiate(ICircuit circuit) {

            return new ComponentLampSegmented(circuit);
        }

    }

}
