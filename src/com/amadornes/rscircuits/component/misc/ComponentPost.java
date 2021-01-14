package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseInt;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class ComponentPost extends ComponentBaseInt {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "post");

    public ComponentPost(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_POST;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.of(EnumComponentSlot.CENTER);
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        double d = 1 / 16D;
        double t = 2 / 16D;
        boxes.add(new AxisAlignedBB(d, 0, d, d + t, 1, d + t));
        boxes.add(new AxisAlignedBB(1 - d - t, 0, d, 1 - d, 1, d + t));
        boxes.add(new AxisAlignedBB(1 - d - t, 0, 1 - d - t, 1 - d, 1, 1 - d));
        boxes.add(new AxisAlignedBB(d, 0, 1 - d - t, d + t, 1, 1 - d));

        // double d = 2 / 16D;
        // boxes.add(new AxisAlignedBB(0.5 - d, 0, 0.5 - d, 0.5 + d, 1, 0.5 + d));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(Items.STICK);
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        double d = 1 / 16D;
        return new AxisAlignedBB(d, 0, d, 1 - d, 1, 1 - d);
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

    }

    public static class Factory extends SimpleFactory<ComponentPost> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/post");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == Items.STICK;
        }

        @Override
        public ComponentPost instantiate(ICircuit circuit) {

            return new ComponentPost(circuit);
        }

    }

}
