package com.amadornes.rscircuits.component.analogue;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class ComponentConst extends ComponentBaseFace {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "const");

    private byte output = 0;

    public ComponentConst(ICircuit circuit) {

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
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return false;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side != EnumCircuitSide.TOP;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return side.face.getAxis() != Axis.Y ? output : 0;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (heldItem != null && (heldItem.getItem() == SCMItems.screwdriver || heldItem.getItem() == SCMItems.multimeter)) {
            SCM.proxy.displayTimerGui((o, s) -> "Constant Output: " + o, s -> "", () -> output & 0xFF, o -> {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(o);
                getCircuit().sendCustomPayload(getPos(), slot, buf);
            }, 0, 255, 1);
            return true;
        }

        return false;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 4 / 16D, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.CONSTANT.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setByte("output", output);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        output = tag.getByte("output");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeByte(output);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        output = buf.readByte();
    }

    @Override
    public void handleCustomPayload(ByteBuf buf) {

        output = buf.readByte();
        getCircuit().markDirty();
        getCircuit().notifyUpdateAll(getPos(), slot);
        getCircuit().sendUpdate(getPos(), slot, false);
    }

    public static class Factory extends SimpleFactory<ComponentConst> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/const");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.CONSTANT.ordinal();
        }

        @Override
        public ComponentConst instantiate(ICircuit circuit) {

            return new ComponentConst(circuit);
        }

    }

}
