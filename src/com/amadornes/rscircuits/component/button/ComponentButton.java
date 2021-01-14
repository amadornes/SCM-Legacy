package com.amadornes.rscircuits.component.button;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseButton;
import com.amadornes.rscircuits.component.SimpleFactory;
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
import net.minecraft.util.math.Vec3d;

public class ComponentButton extends ComponentBaseButton {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "button");

    private boolean state = false;

    public ComponentButton(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_BUTTON;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ON, state);
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

        return state && side != EnumCircuitSide.TOP ? (byte) 255 : 0;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (!state) {
            if (!getCircuit().getWorld().isRemote) {
                state = true;
                getCircuit().scheduleTick(this, 5, 0, null);
                getCircuit().markDirty();
                getCircuit().notifyUpdateAll(getPos(), slot);
                getCircuit().sendUpdate(getPos(), slot, false);
            }
            return true;
        }
        return false;
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        state = false;
        getCircuit().markDirty();
        getCircuit().notifyUpdateAll(getPos(), slot);
        getCircuit().sendUpdate(getPos(), slot, false);
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(Item.getItemFromBlock(Blocks.STONE_BUTTON));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);

        tag.setBoolean("state", state);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
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

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        return new NBTTagCompound();
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return null;
    }

    public static class Factory extends SimpleFactory<ComponentButton> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/button");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == Item.getItemFromBlock(Blocks.STONE_BUTTON);
        }

        @Override
        public ComponentButton instantiate(ICircuit circuit) {

            return new ComponentButton(circuit);
        }

    }

}
