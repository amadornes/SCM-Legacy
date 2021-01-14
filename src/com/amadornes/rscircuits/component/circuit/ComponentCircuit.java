package com.amadornes.rscircuits.component.circuit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.circuit.ICircuitContainer;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.util.ComponentReference;
import com.google.common.base.Function;

import io.netty.buffer.ByteBuf;
import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class ComponentCircuit extends ComponentBaseFace implements ICircuitContainer {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "circuit");

    final Circuit circuit = new Circuit(this);
    int size;
    boolean removing = false;

    public ComponentCircuit(ICircuit circuit) {

        super(circuit, EnumComponentSlot.BOTTOM);
    }

    public ComponentCircuit(ICircuit circuit, NBTTagCompound tag) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.circuit.readFromNBT(tag, true);
        size = Circuit.getSize(this.circuit.computeComplexity() + ComponentReference.COMPLEXITY_CIRCUIT);
        if (tag.hasKey("complexity")) {
            size = Circuit.getSize(tag.getFloat("complexity"));
        }
        if (tag.hasKey("size")) {
            size = Math.max(size, Math.min(tag.getInteger("size"), 3));
        }
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_CIRCUIT + circuit.computeComplexity();
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public float getSize() {

        return super.getSize() * size;
    }

    @Override
    public Vec3d getOffset() {

        return new Vec3d(1, 0, 1).scale((size - 1) * 0.5 / size);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state) {

        return ((IExtendedBlockState) state).withProperty(PartCircuit.PROPERTY_IO_MODE, circuit.getIOModes())
                .withProperty(PartCircuit.PROPERTY_NAME, circuit.getName());
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() != Axis.Y;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() != Axis.Y;
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() != Axis.Y ? EnumConnectionType.BIDIRECTIONAL : EnumConnectionType.NONE;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return circuit.getOutput(side, color, bundled);
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        if (slot == EnumComponentSlot.BOTTOM && side.face.getAxis() != Axis.Y
                && !(component instanceof ComponentCircuit || component instanceof ComponentCircuitSlave)) {
            circuit.forEachEdge(IComponent::onWorldChange, side, 0, 0, EnumComponentSlot.VALUES);
        }
    }

    @Override
    public void onAddedPost() {

        circuit.forEach(IComponent::onLoaded);
        circuit.forEach(IComponent::onCircuitAdded);

        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            circuit.forEachEdge(IComponent::onWorldChange, side, 0, 0, EnumComponentSlot.VALUES);
        }
    }

    @Override
    public void onCircuitAdded() {

        circuit.forEach(IComponent::onLoaded);
        circuit.forEach(IComponent::onCircuitAdded);

        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            circuit.forEachEdge(IComponent::onWorldChange, side, 0, 0, EnumComponentSlot.VALUES);
        }
    }

    @Override
    public void onRemoved() {

        circuit.forEach(IComponent::onCircuitRemoved);
        if (!removing) {
            removing = true;
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (x != 0 || z != 0) {
                        getCircuit().removeComponent(getCircuit().getComponent(getPos().add(x, 0, z), slot));
                    }
                }
            }
            removing = false;
        }
    }

    @Override
    public void onCircuitRemoved() {

        circuit.forEach(IComponent::onCircuitRemoved);
    }

    @Override
    public void tick() {

        circuit.tickScheduled();
        circuit.tick();
        circuit.tickEnd();
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, size, (2 * size) / 16D, size));
    }

    @Override
    public ItemStack getPickedItem() {

        ItemStack stack = new ItemStack(SCMItems.circuit);
        if (!circuit.isEmpty()) {
            NBTTagCompound tag = new NBTTagCompound();
            circuit.writeToNBT(tag);
            stack.setTagCompound(tag);
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public List<ItemStack> getPlacementItems() {

        List<ItemStack> list = new ArrayList<ItemStack>();
        list.add(new ItemStack(SCMItems.circuit));
        circuit.forEach(c -> list.addAll(c.getPlacementItems()));
        return list;
    }

    @Override
    public void rotatePre(Rotation rotation) {

        circuit.rotate(rotation);

        // Function<BlockPos, BlockPos> rotatePos;
        // switch (rotation) {
        // case CLOCKWISE_90:
        // rotatePos = p -> new BlockPos(6 - p.getZ(), p.getY(), p.getX());
        // break;
        // case CLOCKWISE_180:
        // rotatePos = p -> new BlockPos(6 - p.getX(), p.getY(), 6 - p.getZ());
        // break;
        // case COUNTERCLOCKWISE_90:
        // rotatePos = p -> new BlockPos(p.getZ(), p.getY(), 6 - p.getX());
        // break;
        // case NONE:
        // default:
        // rotatePos = Function.identity();
        // break;
        // }
        // BlockPos nPos1 = rotatePos.apply(getPos()), nPos2 = rotatePos.apply(getPos().add(size, 0, size));
        // BlockPos newPos = new BlockPos(Math.min(nPos1.getX(), nPos2.getX()), nPos1.getY(), Math.min(nPos1.getZ(), nPos2.getZ()));
        //
        // removing = true;
        // for (int x = 0; x < size; x++) {
        // for (int z = 0; z < size; z++) {
        // BlockPos pos = newPos.add(x, 0, z);
        // getCircuit().replaceComponent(getCircuit().getComponent(pos, slot),
        // x == 0 && z == 0 ? this : new ComponentCircuitSlave(getCircuit().getCircuit(pos), this, new BlockPos(x, 0, z)));
        // }
        // }
        // removing = false;
    }

    @Override
    public void rotatePost() {

    }

    @Override
    public boolean isInWorld() {

        return false;
    }

    @Override
    public World getWorld() {

        return getCircuit().getWorld();
    }

    @Override
    public EnumFacing getFace() {

        return EnumFacing.DOWN;
    }

    @Override
    public Circuit getCircuitAt(BlockPos pos, EnumFacing face) {

        if (face == EnumFacing.DOWN) {
            IComponent c = getCircuit().getComponent(pos, slot);
            return c instanceof ComponentCircuit ? ((ComponentCircuit) c).circuit : null;
        }
        return null;
    }

    @Override
    public void markDirty() {

        getCircuit().markDirty();
    }

    @Override
    public void markRenderUpdate() {

        // NO-OP since it's encapsulated
    }

    @Override
    public void notifyNeighbors() {

        getCircuit().notifyUpdateAll(getPos(), slot);
    }

    @Override
    public void notifyNeighbor(EnumCircuitSide side, boolean strong) {

        for (int i = 0; i < size; i++) {
            BlockPos p = getPos().add(side == EnumCircuitSide.RIGHT ? 0 : side == EnumCircuitSide.LEFT ? size - 1 : i, 0,
                    side == EnumCircuitSide.BACK ? 0 : side == EnumCircuitSide.FRONT ? size - 1 : i);
            getCircuit().notifyUpdate(p, slot, side);
        }
    }

    @Override
    public void sendUpdatePacket() {

        // getCircuit().sendUpdate(getPos(), slot, false);
    }

    @Override
    public boolean isEncapsulated() {

        return true;
    }

    @Override
    public void sendCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buf) {

        // ByteBuf finalBuf = Unpooled.buffer();
        // finalBuf.writeLong(pos.toLong());
        // finalBuf.writeInt(slot.ordinal());
        // finalBuf.writeBytes(buf);
        // getCircuit().sendCustomPayload(getPos(), this.slot, finalBuf);
    }

    @Override
    public byte getInput(EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (side.face.getAxis() != Axis.Y) {
            int input = 0;
            for (int i = 0; i < size; i++) {
                BlockPos p = getPos().add(side == EnumCircuitSide.RIGHT ? 0 : side == EnumCircuitSide.LEFT ? size - 1 : i, 0,
                        side == EnumCircuitSide.BACK ? 0 : side == EnumCircuitSide.FRONT ? size - 1 : i);
                IComponent c = getNeighbor(getCircuit(), p, slot, side);
                if (c != null) {
                    if (c.isOutput(slot, side.getOpposite()) && c.isStrongOutput(slot, side.getOpposite())) {
                        if (!bundled && c.getBundledConnection(EnumComponentSlot.BOTTOM, side.getOpposite()) == EnumConnectionType.NONE) {
                            for (EnumDyeColor col : EnumDyeColor.values()) {
                                input = Math.max(input, c.getOutputSignal(slot, side.getOpposite(), col, false) & 0xFF);
                            }
                        } else {
                            input = Math.max(input, c.getOutputSignal(slot, side.getOpposite(), color, bundled) & 0xFF);
                        }
                    }
                }
            }
            return (byte) input;
        }
        return 0;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = circuit.writeToNBT(super.writeToNBT(tag));
        tag.setInteger("size", size);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        circuit.readFromNBT(tag);
        size = tag.getInteger("size");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeNBTTagCompoundToBuffer(circuit.writeToNBT(new NBTTagCompound()));
        buf.writeInt(size);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        try {
            circuit.readFromNBT(buf.readNBTTagCompoundFromBuffer());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        size = buf.readInt();
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        super.serializePlacement(buf);
        buf.writeNBTTagCompoundToBuffer(circuit.writeToNBT(new NBTTagCompound()));
        buf.writeInt(size);
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        super.deserializePlacement(buf);
        try {
            circuit.readFromNBT(buf.readNBTTagCompoundFromBuffer(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        size = buf.readInt();
    }

    @Override
    public void spawnMagicSmoke(BlockPos pos) {

        circuit.spawnMagicSmoke(getPos());
    }

    @Override
    public void spawnStack(ItemStack stack) {

        circuit.spawnStack(stack);
    }

    @Override
    public void onCleared() {

    }

    @Override
    public void debug(EntityPlayer player) {

        super.debug(player);
    }

    public static class Factory extends SimpleFactory<ComponentCircuit> {

        @Override
        public BlockStateContainer createBlockState() {

            return new ExtendedBlockState(MCMultiPartMod.multipart, new IProperty[0],
                    new IUnlistedProperty[] { PartCircuit.PROPERTY_NAME, PartCircuit.PROPERTY_IO_MODE });
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/circuit");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.circuit && stack.hasTagCompound() && stack.getTagCompound().hasKey("complexity");
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentCircuit data, EnumPlacementType type,
                Map<BlockPos, ComponentCircuit> otherData, boolean simulate) {

            if (data.size <= 0 || data.size > 3) {
                return false;
            }
            int size = Math.max(1, data.size);
            IComponent[][] components = new IComponent[size][size];
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    IComponent c = components[x][z] = (x == 0 && z == 0) ? data
                            : new ComponentCircuitSlave(circuit, data, new BlockPos(x, 0, z));
                    if (!circuit.addComponent(pos.add(x, 0, z), c, true)) {
                        return false;
                    }
                }
            }
            if (!simulate) {
                for (int x = 0; x < size; x++) {
                    for (int z = 0; z < size; z++) {
                        circuit.addComponent(pos.add(x, 0, z), components[x][z], false);
                    }
                }
            }
            return true;
        }

        @Override
        public ComponentCircuit instantiate(ICircuit circuit) {

            return new ComponentCircuit(circuit);
        }

        @Override
        public ComponentCircuit instantiate(ICircuit circuit, ItemStack stack) {

            return new ComponentCircuit(circuit, stack.getTagCompound());
        }

        @Override
        public void serialize(PacketBuffer buf, Map<BlockPos, ComponentCircuit> data, EntityPlayer player) {

            super.serialize(buf, data, player);
            buf.writeInt(player.inventory.currentItem);
        }

        @Override
        public void deserialize(PacketBuffer buf, Map<BlockPos, ComponentCircuit> data, Function<BlockPos, ICircuit> circuitProvider,
                EntityPlayer player) {

            super.deserialize(buf, data, circuitProvider, player);
            ItemStack curItem = player.inventory.getStackInSlot(buf.readInt());
            if (curItem != null && curItem.getItem() == SCMItems.circuit && curItem.hasTagCompound()) {
                data.values().iterator().next().circuit.readFromNBT(curItem.getTagCompound());
            }
        }

    }

}
