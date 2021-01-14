package com.amadornes.rscircuits.component.wire2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.component.wire2.SimpleRedstoneConductor.ConnectionInfo;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;
import com.amadornes.rscircuits.util.ProjectionHelper;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

public class ComponentWireBundled extends ComponentWireFlatBase {

    public static final PropertyBool FRONT = PropertyBool.create("front");
    public static final PropertyBool BACK = PropertyBool.create("back");
    public static final PropertyBool LEFT = PropertyBool.create("left");
    public static final PropertyBool RIGHT = PropertyBool.create("right");

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "bundled_wire");

    private byte[] power = new byte[16];
    private boolean shouldOutputPower = true;
    private boolean connectOnAdd = true;

    public ComponentWireBundled(ICircuit circuit, boolean connectOnAdd) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.connectOnAdd = connectOnAdd;
    }

    public ComponentWireBundled(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_WIRE;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public IBlockState getActualState() {

        IConnectionInfo[] info = getNeighbors();
        return super.getActualState().withProperty(FRONT, info[EnumCircuitSide.FRONT.ordinal()].getType().canConnect)
                .withProperty(BACK, info[EnumCircuitSide.BACK.ordinal()].getType().canConnect)
                .withProperty(LEFT, info[EnumCircuitSide.LEFT.ordinal()].getType().canConnect)
                .withProperty(RIGHT, info[EnumCircuitSide.RIGHT.ordinal()].getType().canConnect);
    }

    @Override
    public boolean isBundled() {

        return true;
    }

    @Override
    public EnumDyeColor getColor() {

        return null;
    }

    @Override
    public boolean isColorBiased() {

        return true;
    }

    @Override
    public byte getInput(EnumDyeColor color) {

        int input = 0;
        for (IConnectionInfo info : getNeighbors()) {
            if (info.getType().isInput && info.getNeighbor() == null) {
                Pair<IComponent, EnumComponentSlot> neighbor = getNeighborPair(slot, info.getSide());
                if (neighbor != null
                        && neighbor.getKey().getBundledConnection(neighbor.getValue(), info.getSide().getOpposite()).isOutput) {
                    input = Math.max(input,
                            neighbor.getKey().getOutputSignal(neighbor.getValue(), info.getSide().getOpposite(), color, true) & 0xFF);
                }
            }
        }

        if (getPos().getY() == 0) {
            for (EnumCircuitSide edge : getEdgesOn()) {
                if (getNeighbors()[edge.ordinal()].getType().isInput) {
                    input = Math.max(input, getCircuit().getInput(edge, color, true) & 0xFF);
                }
            }
        }

        return (byte) input;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return bundled & shouldOutputPower && isOutput(slot, side) && color != null ? power[color.ordinal()] : 0;
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return cond.getNeighbors()[side.ordinal()].getType().isInput;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return cond.getNeighbors()[side.ordinal()].getType().isOutput;
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return cond.getNeighbors()[side.ordinal()].getType();
    }

    @Override
    public boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return true;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (heldItem != null && heldItem.getItem() == SCMItems.screwdriver) {
            int rot = ProjectionHelper.getPlacementRotation(new Vec3d(hit.xCoord, 0, 1 - hit.zCoord));
            if (rot == 1 || rot == 2) {
                rot = ((rot - 1) ^ 1) + 1;
            }

            ConnectionInfo info = cond.getNeighborsRAW()[rot + 2];
            EnumConnectionType prevType = info.type;
            EnumConnectionType type = prevType;

            EnumCircuitSide side = info.side;
            Pair<IComponent, EnumComponentSlot> neighbor = getNeighborPair(slot, side);
            if (neighbor != null) {
                if (type.canConnect) {
                    type = EnumConnectionType.DISABLED;
                } else {
                    if (neighbor.getKey().getBundledConnection(neighbor.getValue(), side).canConnect) {
                        type = EnumConnectionType.BIDIRECTIONAL;
                    }
                }
            } else if (!isOnSeam() && getPos().getY() == 0) {
                List<EnumCircuitSide> edges = getEdgesOn();
                if (edges.contains(side) && isOnEdge(side)) {
                    type = type.canConnect ? EnumConnectionType.DISABLED : EnumConnectionType.BIDIRECTIONAL;
                }
            }
            if (type != prevType) {
                if (!getCircuit().getWorld().isRemote) {
                    info.type = type;
                    if (neighbor != null && neighbor.getKey() instanceof IRedstoneConductor) {
                        IRedstoneConductor w = (IRedstoneConductor) neighbor.getKey();
                        if (type == EnumConnectionType.DISABLED) {
                            w.disconnect(side.getOpposite());
                            if (w.isBundled()) {
                                for (EnumDyeColor c : EnumDyeColor.values()) {
                                    w.propagate(c);
                                }
                            } else {
                                w.propagate(w.getColor());
                            }
                            info.neighbor = null;
                        } else {
                            info.neighbor = w;
                            w.connect(side.getOpposite(), this, EnumConnectionType.ANODE, false);
                        }
                    } else {
                        getCircuit().notifyUpdate(getPos(), slot, side);
                    }
                    if (!player.capabilities.isCreativeMode) {
                        heldItem.damageItem(1, player);
                    }
                    for (EnumDyeColor c : EnumDyeColor.values()) {
                        propagate(c);
                    }
                    getCircuit().sendUpdate(getPos(), slot, true);
                    getCircuit().markDirty();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated) {

        return (!simulated || connectOnAdd || cond.getNeighbors()[side.ordinal()].getType() != EnumConnectionType.DISABLED)
                && super.connect(side, conductor, type.canConnect ? EnumConnectionType.BIDIRECTIONAL : type, simulated);
    }

    @Override
    public void onAdded() {

        if (connectOnAdd) {
            super.onAdded();
        }
    }

    @Override
    public void onPropagated(Set<EnumDyeColor> colors, byte power) {

        boolean update = false;
        for (EnumDyeColor c : colors) {
            if (this.power[c.ordinal()] != power) {
                this.power[c.ordinal()] = power;
                update = true;
            }
        }
        if (update) {
            super.onPropagated(colors, power);
            getCircuit().markDirty();
        }
    }

    @Override
    public void onConnect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type) {

        getCircuit().markDirty();
        getCircuit().sendUpdate(getPos(), slot, true);
    }

    @Override
    public void onDisconnect(EnumCircuitSide side) {

        getCircuit().markDirty();
        getCircuit().sendUpdate(getPos(), slot, true);
    }

    @Override
    public void onWorldChange() {

        if (getPos().getY() == 0) {
            for (EnumDyeColor c : EnumDyeColor.values()) {
                cond.propagate(c);
            }
        }
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

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_BUNDLED_WIRE.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);

        tag.setByteArray("power", power);

        int connections = 0;
        for (IConnectionInfo info : getNeighbors()) {
            connections |= (info.getType().canConnect ? 1 : 0) << info.getSide().ordinal();
        }
        tag.setInteger("connections", connections);

        tag.setByte("version", (byte) 3);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);

        power = tag.getByteArray("power");

        int version = tag.hasKey("version") ? tag.getByte("version") & 0xFF : 1;
        if (version == 1) {
            int connections = tag.getByteArray("connections")[0] & 0xFF;
            for (int i = 0; i < 4; i++) {
                cond.getNeighborsRAW()[i + 2].type = EnumConnectionType.VALUES[(connections >> (i * 2)) & 0x3];
            }
        } else if (version == 2) {
            int connections = tag.getInteger("connections");
            for (ConnectionInfo info : cond.getNeighborsRAW()) {
                info.type = EnumConnectionType.VALUES[(connections >> (info.side.ordinal() * 2)) & 0x3];
            }
        } else if (version == 3) {
            int connections = tag.getInteger("connections");
            for (ConnectionInfo info : cond.getNeighborsRAW()) {
                info.type = ((connections >> info.side.ordinal()) & 0x1) != 0 ? EnumConnectionType.BIDIRECTIONAL : EnumConnectionType.NONE;
            }
        }
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        int connections = 0;
        for (IConnectionInfo info : getNeighbors()) {
            // 2 data bits per connection, so 4 possible states (EWC has 5 but only 4 are used by regular wires)
            connections |= info.getType().ordinal() << (info.getSide().ordinal() * 2);
        }
        buf.writeInt(connections);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        int connections = buf.readInt();
        for (ConnectionInfo info : cond.getNeighborsRAW()) {
            info.type = EnumConnectionType.VALUES[(connections >> (info.side.ordinal() * 2)) & 0x3];
        }
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        super.serializePlacement(buf);
        buf.writeBoolean(connectOnAdd);
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        super.deserializePlacement(buf);
        connectOnAdd = buf.readBoolean();
        if (!connectOnAdd) {
            for (ConnectionInfo info : cond.getNeighborsRAW()) {
                if (info.side.face.getAxis() != Axis.Y) {
                    info.type = EnumConnectionType.DISABLED;
                }
            }
        }
    }

    @Override
    public void debug(EntityPlayer player) {

        StringBuilder power = new StringBuilder("Power: [");
        for (byte b : this.power) {
            power.append((b & 0xFF) + ", ");
        }
        power.reverse().delete(0, 2).reverse().append("]");
        player.addChatMessage(new TextComponentString(power.toString()));
    }

    public static class Factory extends SimpleFactory<ComponentWireBundled> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer.Builder(MCMultiPartMod.multipart).add(FRONT, BACK, LEFT, RIGHT).build();
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/bundled_wire");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.TINY_BUNDLED_WIRE.ordinal();
        }

        private ComponentWireBundled mkWire(ICircuit circuit, ItemStack stack) {

            return new ComponentWireBundled(circuit, !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL));
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.DRAW;
        }

        @Override
        public ComponentWireBundled getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec,
                ItemStack stack, EntityPlayer player, EnumPlacementType type, ComponentWireBundled previousData,
                Map<BlockPos, ComponentWireBundled> otherData, EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            if (!circuit.isSideSolid(pos.down(), EnumCircuitSide.TOP)) {
                return null;
            }
            IComponent c = circuit.getComponent(pos, EnumComponentSlot.BOTTOM);
            return c instanceof ComponentWireBundled ? (ComponentWireBundled) c : mkWire(circuit, stack);
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentWireBundled data, EnumPlacementType type,
                Map<BlockPos, ComponentWireBundled> otherData, boolean simulate) {

            for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
                ComponentWireBundled neighbor = otherData.get(pos.offset(side.face));
                if (neighbor != null) {
                    ConnectionInfo info = data.cond.getNeighborsRAW()[side.ordinal()];
                    info.type = EnumConnectionType.ANODE;
                    info.neighbor = neighbor;
                }
            }
            return circuit.addComponent(pos, data, simulate);
        }

        @Override
        public void drawPlacement(ICircuit circuit, BlockPos pos, ComponentWireBundled data, EnumPlacementType type,
                Map<BlockPos, ComponentWireBundled> otherData) {

            super.drawPlacement(circuit, pos, data, type, otherData);

            boolean front = getState(circuit, pos, EnumCircuitSide.FRONT, otherData, data.connectOnAdd);
            boolean back = getState(circuit, pos, EnumCircuitSide.BACK, otherData, data.connectOnAdd);
            boolean left = getState(circuit, pos, EnumCircuitSide.LEFT, otherData, data.connectOnAdd);
            boolean right = getState(circuit, pos, EnumCircuitSide.RIGHT, otherData, data.connectOnAdd);

            IBlockState state = data.getActualState().withProperty(FRONT, front).withProperty(BACK, back).withProperty(LEFT, left)
                    .withProperty(RIGHT, right);
            SimpleFactory.draw(data, state, state);
            if (data.connectOnAdd) {
                if (front) {
                    state = data.getActualState().withProperty(FRONT, false).withProperty(BACK, front).withProperty(LEFT, false)
                            .withProperty(RIGHT, false);
                    GlStateManager.translate(0, 0, 1);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(0, 0, -1);
                }
                if (back) {
                    state = data.getActualState().withProperty(FRONT, back).withProperty(BACK, false).withProperty(LEFT, false)
                            .withProperty(RIGHT, false);
                    GlStateManager.translate(0, 0, -1);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(0, 0, 1);
                }
                if (left) {
                    state = data.getActualState().withProperty(FRONT, false).withProperty(BACK, false).withProperty(LEFT, false)
                            .withProperty(RIGHT, left);
                    GlStateManager.translate(1, 0, 0);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(-1, 0, 0);
                }
                if (right) {
                    state = data.getActualState().withProperty(FRONT, false).withProperty(BACK, false).withProperty(LEFT, right)
                            .withProperty(RIGHT, false);
                    GlStateManager.translate(-1, 0, 0);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(1, 0, 0);
                }
            }
        }

        private boolean getState(ICircuit circuit, BlockPos pos, EnumCircuitSide side, Map<BlockPos, ComponentWireBundled> otherData,
                boolean connectOnAdd) {

            if (otherData.containsKey(pos.offset(EnumCircuitSide.FRONT.face))) {
                return true;
            }
            if (connectOnAdd) {
                // TODO: Implement this!
                IComponent c = circuit.getComponent(pos.offset(side.face), EnumComponentSlot.BOTTOM);
                if (c != null) {
                }
            }
            return false;
        }

        @Override
        public ComponentWireBundled instantiate(ICircuit circuit) {

            return new ComponentWireBundled(circuit);
        }

    }

}
