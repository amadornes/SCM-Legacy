package com.amadornes.rscircuits.component.digital;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.api.signal.IConnection;
import com.amadornes.rscircuits.api.signal.IConnection.EnumRedstoneConnection;
import com.amadornes.rscircuits.api.signal.IConnectionType;
import com.amadornes.rscircuits.api.signal.ISignal;
import com.amadornes.rscircuits.api.signal.ISignal.IRedstoneSignal;
import com.amadornes.rscircuits.api.signal.ISignalSupplier;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.util.ComponentReference;
import com.google.common.base.Function;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentTorch extends ComponentBaseFace implements ISignalSupplier<EnumCircuitSide> {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "torch");

    private boolean master;
    private boolean state = true, nextState = true;

    public ComponentTorch(ICircuit circuit, boolean master) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.master = master;
    }

    public ComponentTorch(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_TORCH;
    }

    @Override
    public boolean isDynamic() {

        return true;
    }

    @Override
    public IBlockState getActualState() {

        return master ? super.getActualState().withProperty(ON, state) : null;
    }

    @Override
    public float getSize() {

        return 10 / 16F;
    }

    @Override
    public Vec3d getOffset() {

        return new Vec3d(0, -5 / 16F, 0);
    }

    private boolean getState() {

        if (master) {
            return state;
        }
        ComponentTorch t = ((ComponentTorch) getCircuit().getComponent(getPos().down(), EnumComponentSlot.CENTER));
        return t != null ? t.state : false;
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return master ? EnumSet.of(EnumComponentSlot.BOTTOM, EnumComponentSlot.CENTER, EnumComponentSlot.TOP)
                : EnumSet.of(EnumComponentSlot.BOTTOM, EnumComponentSlot.CENTER);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends IConnection> Optional<C> getConnection(IConnectionType<C, ?> type, EnumCircuitSide on) {

        return type == IConnectionType.REDSTONE ? Optional.of((C) EnumRedstoneConnection.of(master && on != EnumCircuitSide.TOP,
                on != EnumCircuitSide.BOTTOM && (!master || on != EnumCircuitSide.TOP))) : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ISignal<?>> S getSignal(IConnectionType<?, S> type, EnumCircuitSide from) {

        return type == IConnectionType.REDSTONE
                ? (S) IRedstoneSignal.of((byte) (getState() ? 255 : 0), !master && from == EnumCircuitSide.TOP) : null;
    }

    @Override
    public void onAdded() {

        onNeighborChange(EnumCircuitSide.BOTTOM, slot, this, EnumCircuitUpdate.COMPONENT_UPDATE);
        state = nextState;
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        boolean prev = state;
        state = (boolean) data;
        if (prev != state) {
            getCircuit().notifyUpdateAll(getPos(), EnumComponentSlot.BOTTOM);
            getCircuit().notifyUpdateAll(getPos(), EnumComponentSlot.CENTER);
            getCircuit().notifyUpdateAll(getPos(), EnumComponentSlot.TOP);
            getCircuit().notifyUpdateAll(getPos().up(), EnumComponentSlot.BOTTOM);
            getCircuit().notifyUpdateAll(getPos().up(), EnumComponentSlot.CENTER);
            getCircuit().notifyUpdateAll(getPos().up(), EnumComponentSlot.TOP);
            getCircuit().sendUpdate(getPos(), slot, false);
            getCircuit().markDirty();
        }
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (!master || component == this || (master && component.getPos().equals(getPos().up()))) {
            return;
        }

        if (side == EnumCircuitSide.BOTTOM) {
            if (!getCircuit().isSideSolid(getPos().down(), EnumCircuitSide.TOP)) {
                getCircuit().removeComponent(this);
                getCircuit().removeComponent(getCircuit().getComponent(getPos().up(), EnumComponentSlot.CENTER));
                getDrops().forEach(getCircuit()::spawnStack);
                return;
            }
        }
        nextState = true;
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            if (getInputAny(EnumComponentSlot.BOTTOM, s) != 0) {
                nextState = false;
                break;
            }
        }
        if (nextState) {
            IComponent c = getCircuit().getComponent(getPos().down(), EnumComponentSlot.TOP);
            if (c != null && c.getOutputSignalAny(EnumComponentSlot.TOP, EnumCircuitSide.TOP) != 0) {
                nextState = false;
            }
        }
        getCircuit().scheduleTick(this, 1, 0, nextState);
        getCircuit().markDirty();
    }

    @Override
    public boolean harvest(EntityPlayer player, Vec3d hitPos) {

        super.harvest(player, hitPos);
        getCircuit().removeComponent(getCircuit().getComponent(getPos().up(), EnumComponentSlot.CENTER));
        return true;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        double r = 0.5 * 10 / 16D;
        if (master) {
            boxes.add(new AxisAlignedBB(0.5 - r, 0, 0.5 - r, 0.5 + r, 2.5 * 10 / 16D, 0.5 + r));
        }
    }

    @Override
    public List<ItemStack> getDrops() {

        return master ? Arrays.asList(getPickedItem()) : Arrays.asList();
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(Blocks.REDSTONE_TORCH);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setBoolean("master", master);
        tag.setBoolean("prevState", state);
        tag.setBoolean("state", nextState);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        master = tag.getBoolean("master");
        state = tag.getBoolean("prevState");
        nextState = tag.getBoolean("state");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeBoolean(master);
        buf.writeBoolean(state);
        buf.writeBoolean(nextState);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        master = buf.readBoolean();
        state = buf.readBoolean();
        nextState = buf.readBoolean();
    }

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("state", (boolean) data);
        return tag;
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return tag.getBoolean("state");
    }

    public static class Factory implements IComponentFactory<Object> {

        private final Object object = new Object();

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ON);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/torch");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == Item.getItemFromBlock(Blocks.REDSTONE_TORCH);
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.SINGLE;
        }

        @Override
        public Object getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, Object previousData, Map<BlockPos, Object> otherData, EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            if (otherData.containsKey(pos.down()) || otherData.containsKey(pos.up())) {
                return null;
            }
            if (!circuit.isSideSolid(pos.down(), EnumCircuitSide.TOP)) {
                return null;
            }
            return object;
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, Object data, EnumPlacementType type, Map<BlockPos, Object> otherData,
                boolean simulate) {

            ComponentTorch master = new ComponentTorch(circuit, true);
            ComponentTorch slave = new ComponentTorch(circuit, false);

            if (circuit.addComponent(pos, master, true) && circuit.addComponent(pos.up(), slave, true)) {
                if (!simulate) {
                    circuit.addComponent(pos, master, false);
                    circuit.addComponent(pos.up(), slave, false);
                }
                return true;
            }
            return false;
        }

        @Override
        public void drawPlacement(ICircuit circuit, BlockPos pos, Object data, EnumPlacementType type, Map<BlockPos, Object> otherData) {

            SimpleFactory.draw(data, o -> new ComponentTorch(circuit, true));
        }

        @Override
        public void serialize(PacketBuffer buf, Map<BlockPos, Object> data, EntityPlayer player) {

            buf.writeInt(data.size());
            data.forEach((pos, comp) -> {
                buf.writeBlockPos(pos);
            });
        }

        @Override
        public void deserialize(PacketBuffer buf, Map<BlockPos, Object> data, Function<BlockPos, ICircuit> circuitProvider,
                EntityPlayer player) {

            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                BlockPos pos = buf.readBlockPos();
                data.put(pos, object);
            }
        }

        @Override
        public IComponent instantiate(ICircuit circuit) {

            return new ComponentTorch(circuit);
        }

    }

}
