package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.part.PartCircuit;

import mcmultipart.item.ItemMultiPart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.MultipartHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ItemCircuit extends ItemMultiPart implements IScrollableItem, ICircuitStorage {

    public ItemCircuit() {

        setUnlocalizedName(SCM.MODID + ":circuit");
    }

    @Override
    public PartCircuit createPart(World world, BlockPos pos, EnumFacing side, Vec3d hit, ItemStack stack, EntityPlayer player) {

        if (world.getBlockState(pos.offset(side)).isSideSolid(world, pos.offset(side), side.getOpposite()))
            return new PartCircuit(side, stack.getTagCompound());
        return null;
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {

        if (!world.isRemote && stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("circPos") && !tag.hasKey("components")) {
                if (world.provider.getDimension() == tag.getInteger("circDim")) {
                    IMultipartContainer container = MultipartHelper.getPartContainer(world, BlockPos.fromLong(tag.getLong("circPos")));
                    if (container != null) {
                        container.getParts().stream().filter(p -> p instanceof PartCircuit).findFirst()
                                .ifPresent(p -> tag.merge(((PartCircuit) p).getStack().getTagCompound()));
                    }
                }
            }
            if (tag.hasKey("components") && !tag.hasKey("complexity")) {
                Circuit circuit = new Circuit(null);
                circuit.readFromNBT(tag);
                tag.setFloat("complexity", circuit.computeComplexity());
            }
        }
    }

    @Override
    public boolean scroll(EntityPlayer player, ItemStack stack, int dwheel) {

        if (stack.hasTagCompound() && player.isSneaking()) {
            NBTTagCompound tag = stack.getTagCompound();
            Circuit circuit = new Circuit(null);
            circuit.readFromNBT(tag, false);
            float complexity = circuit.computeComplexity();
            int minSize = Circuit.getSize(complexity);
            int size = minSize;
            if (tag.hasKey("size")) {
                size = tag.getInteger("size");
            }
            size = Math.max(minSize, Math.min(size + (dwheel > 0 ? 1 : -1), 3));
            tag.setInteger("size", size);
            return true;
        }
        return false;
    }

    @Override
    public NBTTagCompound getCircuitData(EntityPlayer player, ItemStack stack) {

        if (!stack.hasTagCompound()) {
            return null;
        }
        NBTTagCompound stackTag = stack.getTagCompound();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("components", stackTag.getTag("components").copy());
        tag.setTag("componentsVersion", stackTag.getTag("componentsVersion").copy());
        tag.setTag("updates", stackTag.getTag("updates").copy());
        tag.setTag("name", stackTag.getTag("name").copy());
        tag.setTag("iomodes", stackTag.getTag("iomodes").copy());
        // Circuit.tags.forEach(t -> tag.setTag(t, stackTag.getTag(t).copy()));
        return tag;
    }

    @Override
    public boolean canOverrideCircuitData(EntityPlayer player, ItemStack stack) {

        return player.capabilities.isCreativeMode;
    }

    @Override
    public ActionResult<ItemStack> overrideCircuitData(EntityPlayer player, ItemStack stack, NBTTagCompound tag) {

        stack = stack.copy();
        if (stack.hasTagCompound()) {
            stack.getTagCompound().merge(tag);
        } else {
            stack.setTagCompound(tag.copy());
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public NBTTagCompound getNBTShareTag(ItemStack stack) {

        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound().copy();
            tag.removeTag("components");
            tag.removeTag("updates");
            tag.removeTag("componentsVersion");
            tag.removeTag("iomodes");
            return tag;
        } else {
            return super.getNBTShareTag(stack);
        }
    }

}
