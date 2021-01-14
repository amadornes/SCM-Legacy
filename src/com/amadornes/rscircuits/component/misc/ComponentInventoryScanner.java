package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.IAdvancedComparatorOverride;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.component.ComponentBaseInt;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;
import com.amadornes.rscircuits.util.RedstoneUtils;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ComponentInventoryScanner extends ComponentBaseInt {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "inv_scanner");

    private byte input = 0;

    public ComponentInventoryScanner(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_INVSCANNER;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.allOf(EnumComponentSlot.class);
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side != EnumCircuitSide.TOP;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return side.face.getAxis() != Axis.Y ? input : 0;
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        byte prevInput = input;
        input = (Byte) data;
        if (input != prevInput) {
            getCircuit().markDirty();
            getCircuit().notifyUpdate(getPos(), EnumComponentSlot.BOTTOM, EnumCircuitSide.HORIZONTALS);
            getCircuit().sendUpdate(getPos(), EnumComponentSlot.BOTTOM, false);
        }
    }

    @Override
    public void onAdded() {

        onWorldTileChange();
    }

    @Override
    public void onCircuitAdded() {

        onWorldTileChange();
    }

    @Override
    public void onWorldChange() {

        onWorldTileChange();
    }

    @Override
    public void onWorldTileChange() {

        BlockPos pos = getPos();
        if (pos.getY() == 0) {
            EnumCircuitSide side = null;
            if (pos.getX() == 0) {
                if (pos.getZ() == 0) {
                    // Corner!
                } else if (pos.getZ() < 6) {
                    side = EnumCircuitSide.RIGHT;
                } else if (pos.getZ() == 6) {
                    // Corner!
                }
            } else if (pos.getX() < 6) {
                if (pos.getZ() == 0) {
                    side = EnumCircuitSide.BACK;
                } else if (pos.getZ() < 6) {
                    // NO-OP, we're in the middle of the plate. This shouldn't even happen
                } else if (pos.getZ() == 6) {
                    side = EnumCircuitSide.FRONT;
                }
            } else if (pos.getX() == 6) {
                if (pos.getZ() == 0) {
                    // Corner!
                } else if (pos.getZ() < 6) {
                    side = EnumCircuitSide.LEFT;
                } else if (pos.getZ() == 6) {
                    // Corner!
                }
            }
            if (side != null) {
                byte input = calculateInputStrength(getCircuit().getWorld(), getCircuit().getPos(),
                        RedstoneUtils.convert(getCircuit().getFace(), side));
                getCircuit().scheduleTick(this, 1, 0, input);
                getCircuit().markDirty();
            }
        }
    }

    protected byte calculateInputStrength(World world, BlockPos pos, EnumFacing side) {

        int in = 0;
        pos = pos.offset(side);
        IBlockState state = world.getBlockState(pos);
        if (state.hasComparatorInputOverride()) {
            if (state.getBlock() instanceof IAdvancedComparatorOverride) {
                in = ((IAdvancedComparatorOverride) state.getBlock()).getAdvancedComparatorInputOverride(state, world, pos,
                        side.getOpposite()) & 0xFF;
            } else {
                in = state.getComparatorInputOverride(world, pos) * 17;
            }
        } else if (state.isNormalCube()) {
            pos = pos.offset(side);
            state = world.getBlockState(pos);
            if (state.hasComparatorInputOverride()) {
                if (state.getBlock() instanceof IAdvancedComparatorOverride) {
                    in = ((IAdvancedComparatorOverride) state.getBlock()).getAdvancedComparatorInputOverride(state, world, pos,
                            side.getOpposite()) & 0xFF;
                } else {
                    in = state.getComparatorInputOverride(world, pos) * 17;
                }
            } else if (state.getMaterial() == Material.AIR) {
                EntityItemFrame itemframe = this.findItemFrame(world, side, pos);
                if (itemframe != null) {
                    in = itemframe.getAnalogOutput() * 17;
                }
            }
        }
        return (byte) in;
    }

    private EntityItemFrame findItemFrame(World worldIn, final EnumFacing facing, BlockPos pos) {

        List<EntityItemFrame> list = worldIn.<EntityItemFrame> getEntitiesWithinAABB(EntityItemFrame.class,
                new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                p_apply_1_ -> p_apply_1_ != null && p_apply_1_.getHorizontalFacing() == facing);
        return list.size() == 1 ? (EntityItemFrame) list.get(0) : null;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.INV_SCANNER.ordinal());
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return box;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = super.writeToNBT(tag);
        tag.setByte("input", input);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        input = tag.getByte("input");
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

    }

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        NBTTagCompound tag = new NBTTagCompound();
        tag.setByte("input", (byte) data);
        return tag;
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return tag.getByte("input");
    }

    public static class Factory extends SimpleFactory<ComponentInventoryScanner> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/inv_scanner");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.INV_SCANNER.ordinal();
        }

        @Override
        public ComponentInventoryScanner getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec,
                ItemStack stack, EntityPlayer player, EnumPlacementType type, ComponentInventoryScanner previousData,
                Map<BlockPos, ComponentInventoryScanner> otherData, EnumInstantanceUse use) {

            if (pos.getY() == 0) {
                ComponentInventoryScanner data = super.getPlacementData(circuit, pos, faceClicked, hitVec, stack, player, type,
                        previousData, otherData, use);
                data.setPos(pos);
                return data.isOnEdge(data.getEdgeOn()) ? data : null;
            }
            return null;
        }

        @Override
        public ComponentInventoryScanner instantiate(ICircuit circuit) {

            return new ComponentInventoryScanner(circuit);
        }

    }

}
