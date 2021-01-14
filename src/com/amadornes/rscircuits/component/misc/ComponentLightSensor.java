package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseButton;
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
import net.minecraft.util.ResourceLocation;

public class ComponentLightSensor extends ComponentBaseButton {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "light_sensor");
    private byte power = 0;

    public ComponentLightSensor(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_RANDOM;
    }

    @Override
    public boolean isDynamic() {

        return false;
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

        return power;
    }

    @Override
    public void tick() {

        if (!getCircuit().getWorld().isRemote && getCircuit().getWorld().getTotalWorldTime() % 20 == 0) {
            int newPow = (int) ((Math.abs(((getCircuit().getWorld().getWorldTime() + 18000) % 24000) - 12000) / 12000D) * 255);
            if (newPow != (power & 0xFF)) {
                power = (byte) newPow;
                getCircuit().notifyUpdateAll(getPos(), slot);
                getCircuit().markDirty();
            }
        }
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.LIGHT_SENSOR.ordinal());
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

    public static class Factory extends SimpleFactory<ComponentLightSensor> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/light_sensor");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.LIGHT_SENSOR.ordinal();
        }

        @Override
        public ComponentLightSensor instantiate(ICircuit circuit) {

            return new ComponentLightSensor(circuit);
        }

    }

}
