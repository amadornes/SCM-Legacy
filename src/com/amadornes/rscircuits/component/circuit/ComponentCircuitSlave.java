package com.amadornes.rscircuits.component.circuit;

import java.util.Arrays;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentCircuitSlave extends ComponentBaseFace {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "circuit_slave");

    private ComponentCircuit master;
    private BlockPos selfPos = null;

    public ComponentCircuitSlave(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    public ComponentCircuitSlave(ICircuit circuit, ComponentCircuit master, BlockPos selfPos) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.master = master;
        this.selfPos = selfPos;
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return 0;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public IBlockState getActualState() {

        return null;
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return master != null && master.isInput(slot, side);
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return master != null && master.isOutput(slot, side);
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return master != null ? master.getBundledConnection(slot, side) : EnumConnectionType.NONE;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (master == null) {
            return 0;
        }
        return master.getOutputSignal(slot, side, color, bundled);
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (master != null && slot == EnumComponentSlot.BOTTOM && side.face.getAxis() != Axis.Y
                && !(component instanceof ComponentCircuit || component instanceof ComponentCircuitSlave)) {
            master.circuit.forEachEdge(IComponent::onWorldChange, side, 0, 0, EnumComponentSlot.VALUES);
        }
    }

    @Override
    public void onLoaded() {

        master = (ComponentCircuit) getCircuit().getComponent(getPos().subtract(selfPos), slot);
    }

    @Override
    public void onAdded() {

        master = (ComponentCircuit) getCircuit().getComponent(getPos().subtract(selfPos), slot);
    }

    @Override
    public void onCircuitAdded() {

        master = (ComponentCircuit) getCircuit().getComponent(getPos().subtract(selfPos), slot);
    }

    @Override
    public void onRemoved() {

        if (master != null && !master.removing) {
            master.removing = true;
            for (int x = 0; x < master.size; x++) {
                for (int z = 0; z < master.size; z++) {
                    if (x != selfPos.getX() || z != selfPos.getZ()) {
                        getCircuit().removeComponent(getCircuit().getComponent(getPos().subtract(selfPos).add(x, 0, z), slot));
                    }
                }
            }
            master.removing = false;
        }
    }

    @Override
    public boolean harvest(EntityPlayer player, Vec3d hitPos) {

        if (player != null && !player.capabilities.isCreativeMode) {
            if (!getCircuit().getWorld().isRemote) {
                master.getDrops().forEach(getCircuit()::spawnStack);
            }
        }
        return super.harvest(player, hitPos);
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        if (master != null) {
            boxes.add(new AxisAlignedBB(0, 0, 0, master.size, (2 * master.size) / 16D, master.size)
                    .offset(BlockPos.ORIGIN.subtract(selfPos)));
        }
    }

    @Override
    public ItemStack getPickedItem() {

        return master != null ? master.getPickedItem() : new ItemStack(SCMItems.circuit);
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag.setLong("selfPos", selfPos.toLong());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        selfPos = BlockPos.fromLong(tag.getLong("selfPos"));
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeLong(selfPos.toLong());
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        selfPos = BlockPos.fromLong(buf.readLong());
    }

    @Override
    public void debug(EntityPlayer player) {

        if (master != null) {
            master.debug(player);
        }
    }

    public static class Factory extends SimpleFactory<ComponentCircuitSlave> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return null;
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return false;
        }

        @Override
        public ComponentCircuitSlave instantiate(ICircuit circuit) {

            return new ComponentCircuitSlave(circuit);
        }

    }

}
