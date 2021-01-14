package com.amadornes.rscircuits.component.wire;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.signal.EnumConnectionState;
import com.amadornes.rscircuits.api.signal.IConnectable;
import com.amadornes.rscircuits.api.signal.IConnection;
import com.amadornes.rscircuits.api.signal.IConnection.EnumRedstoneConnection;
import com.amadornes.rscircuits.api.signal.IConnectionType;
import com.amadornes.rscircuits.api.signal.ISignal;
import com.amadornes.rscircuits.api.signal.ISignal.IRedstoneSignal;
import com.amadornes.rscircuits.api.signal.ISignalReceiver;
import com.amadornes.rscircuits.api.signal.ISignalSupplier;
import com.amadornes.rscircuits.api.signal.IWire;
import com.amadornes.rscircuits.component.ComponentBaseFace;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;
import com.amadornes.rscircuits.util.MiscUtils;
import com.amadornes.rscircuits.util.WireHelper;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;

public class ComponentRedstoneWire extends ComponentBaseFace
        implements IWire<EnumCircuitSide>, ISignalSupplier<EnumCircuitSide>, ISignalReceiver<EnumCircuitSide> {

    public static final PropertyEnum<EnumRedstoneConnection> FRONT = PropertyEnum.create("front", EnumRedstoneConnection.class);
    public static final PropertyEnum<EnumRedstoneConnection> BACK = PropertyEnum.create("back", EnumRedstoneConnection.class);
    public static final PropertyEnum<EnumRedstoneConnection> LEFT = PropertyEnum.create("left", EnumRedstoneConnection.class);
    public static final PropertyEnum<EnumRedstoneConnection> RIGHT = PropertyEnum.create("right", EnumRedstoneConnection.class);
    public static final IUnlistedProperty<Integer> POWER_LEVEL = Properties.toUnlisted(PropertyInteger.create("power_level", 0, 255));

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "wire");

    private EnumRedstoneConnection[] connections = new EnumRedstoneConnection[6];

    private byte power = 0, input = 0;

    public ComponentRedstoneWire(ICircuit circuit) {
        super(circuit, EnumComponentSlot.BOTTOM);
        Arrays.fill(connections, EnumRedstoneConnection.NONE);
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

        return super.getActualState()//
                .withProperty(FRONT, connections[EnumCircuitSide.FRONT.ordinal()].getState())//
                .withProperty(BACK, connections[EnumCircuitSide.BACK.ordinal()].getState())//
                .withProperty(LEFT, connections[EnumCircuitSide.LEFT.ordinal()].getState())//
                .withProperty(RIGHT, connections[EnumCircuitSide.RIGHT.ordinal()].getState());
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 2 / 16D, 1));// FIXME: Cache all selection boxes
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.PILE_OF_REDSTONE.ordinal());
    }

    @Override
    public List<ItemStack> getDrops() {

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends IConnection> Optional<C> getConnection(IConnectionType<C, ?> type, EnumCircuitSide on) {

        return type == IConnectionType.REDSTONE && on != EnumCircuitSide.UNKNOWN ? Optional.of((C) connections[on.ordinal()])
                : Optional.empty();
    }

    @Override
    public EnumConnectionState getConnectionState(IConnectionType<?, ?> type, EnumCircuitSide on) {

        return type == IConnectionType.REDSTONE && connections[on.ordinal()] != EnumRedstoneConnection.NONE ? EnumConnectionState.CONNECTED
                : EnumConnectionState.DISCONNECTED;
    }

    @Override
    public IConnectable<EnumCircuitSide> visit(IConnectionType<?, ?> type, EnumCircuitSide from,
            BiConsumer<IConnectable<EnumCircuitSide>, EnumCircuitSide> consumer) {

        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            if (side != from) {
                IComponent neighbor = getNeighbor(slot, side);
                if (neighbor != null && neighbor instanceof IWire
                        && neighbor.getConnection(IConnectionType.REDSTONE, side.getOpposite()).isPresent()) {
                    consumer.accept(neighbor, side);
                }
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends ISignal<?>> S getSignal(IConnectionType<?, S> type, EnumCircuitSide from) {

        if (type == IConnectionType.REDSTONE) {
            return (S) IRedstoneSignal.of(WireHelper.wiresOutputPower ? (connections[from.ordinal()].isOutput() ? power : 0) : input, true);
        }
        return null;
    }

    @Override
    public <S extends ISignal<?>> void onSignalReceived(IConnectionType<?, S> type, EnumSet<EnumCircuitSide> from, S signal) {

        if (type == IConnectionType.REDSTONE) {
            this.power = ((IRedstoneSignal) signal).get();
        }
    }

    @Override
    public <S extends ISignal<?>> void onPropagationFinished(IConnectionType<?, S> type, EnumSet<EnumCircuitSide> from, S signal) {

        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            if (connections[side.ordinal()] == EnumRedstoneConnection.OUTPUT) {
                getCircuit().notifyUpdate(getPos(), slot, side);
            }
        }
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (!getCircuit().getWorld().isRemote && hand == EnumHand.MAIN_HAND) {
            System.out.println((input & 0xFF) + " " + (power & 0xFF));
            System.out.println(Arrays.toString(connections));
        }
        return super.onActivated(player, hand, heldItem, hit);
    }

    @Override
    public void onAdded() {

        propagate(EnumCircuitSide.UNKNOWN, true);
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        IComponent neighbor = getNeighbor(slot, side);
        if (neighbor != null) {
            Optional<EnumRedstoneConnection> connection = neighbor.getConnection(IConnectionType.REDSTONE, side.getOpposite());
            EnumRedstoneConnection prev = connections[side.ordinal()];
            if (connection.isPresent()) {
                if (neighbor instanceof IWire) {
                    connections[side.ordinal()] = EnumRedstoneConnection.BIDIRECTIONAL;
                } else if (!(neighbor instanceof IWire)) {
                    connections[side.ordinal()] = connection.get().getOpposite();
                    if (connections[side.ordinal()] == EnumRedstoneConnection.BIDIRECTIONAL) {
                        connections[side.ordinal()] = EnumRedstoneConnection.OUTPUT;
                    }
                }
            } else {
                connections[side.ordinal()] = EnumRedstoneConnection.NONE;
            }
            if (connections[side.ordinal()] != prev) {
                getCircuit().sendUpdate(getPos(), slot, true);
            }
        } else {
            connections[side.ordinal()] = EnumRedstoneConnection.NONE;
            getCircuit().sendUpdate(getPos(), slot, true);
        }

        propagate(side, updateType == EnumCircuitUpdate.COMPONENT_REMOVE);
    }

    @SuppressWarnings("unchecked")
    private void propagate(EnumCircuitSide startSide, boolean force) {

        int newInput = 0;
        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            if (connections[side.ordinal()] == EnumRedstoneConnection.INPUT) {
                IComponent c = getNeighbor(slot, side);
                if (c != null && c.getConnection(IConnectionType.REDSTONE, side).isPresent() && c instanceof ISignalSupplier) {
                    newInput = Math.max(newInput,
                            ((ISignalSupplier<EnumCircuitSide>) c).getSignal(IConnectionType.REDSTONE, side).get() & 0xFF);
                }
            }
        }
        if (!force && newInput == (input & 0xFF)) {
            return;
        }
        input = (byte) newInput;

        Multimap<IConnectable<EnumCircuitSide>, EnumCircuitSide> visited = MultimapBuilder.hashKeys().enumSetValues(EnumCircuitSide.class)
                .build();
        Multimap<IConnectable<EnumCircuitSide>, EnumCircuitSide> map = MultimapBuilder.hashKeys().enumSetValues(EnumCircuitSide.class)
                .build();
        Queue<Pair<IWire<EnumCircuitSide>, EnumCircuitSide>> queue = new ArrayDeque<>();
        queue.add(Pair.of(this, startSide));
        BiConsumer<IConnectable<EnumCircuitSide>, EnumCircuitSide> adder = (c, s) -> {
            if (visited.put(c, s)) {
                if (c instanceof IWire) {
                    queue.add(Pair.of((IWire<EnumCircuitSide>) c, s));
                } else {
                    map.put(c, s);
                }
            }
        };

        while (!queue.isEmpty()) {
            Pair<IWire<EnumCircuitSide>, EnumCircuitSide> pair = queue.poll();
            IConnectable<EnumCircuitSide> con = pair.getKey().visit(IConnectionType.REDSTONE, pair.getValue(), adder);
            if (con != null) {
                map.put(con, pair.getValue());
            }
        }

        try {
            WireHelper.wiresOutputPower = false;
            AtomicInteger power = new AtomicInteger(0);
            map.asMap().forEach((c, sides) -> {
                if (c instanceof ISignalSupplier) {
                    sides.forEach(side -> {
                        c.getConnection(IConnectionType.REDSTONE, side).ifPresent(con -> {
                            int pow = ((ISignalSupplier<EnumCircuitSide>) c).getSignal(IConnectionType.REDSTONE, side).get() & 0xFF;
                            if (pow > power.get()) {
                                power.set(pow);
                            }
                        });
                    });
                }
            });
            WireHelper.wiresOutputPower = true;

            System.out.println(power.get());

            IRedstoneSignal signal = IRedstoneSignal.of(power.byteValue(), true);
            map.asMap().forEach((c, sides) -> {
                if (c instanceof ISignalReceiver) {
                    ((ISignalReceiver<EnumCircuitSide>) c).onSignalReceived(IConnectionType.REDSTONE,
                            MiscUtils.asEnumSet(sides, EnumCircuitSide.class), signal);
                }
            });
            map.asMap().forEach((c, sides) -> {
                if (c instanceof ISignalReceiver) {
                    ((ISignalReceiver<EnumCircuitSide>) c).onPropagationFinished(IConnectionType.REDSTONE,
                            MiscUtils.asEnumSet(sides, EnumCircuitSide.class), signal);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            WireHelper.wiresOutputPower = true;
        }
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        for (int i = 2; i < 6; i++) {
            buf.writeByte(connections[i].ordinal());
        }
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        for (int i = 2; i < 6; i++) {
            connections[i] = EnumRedstoneConnection.VALUES[buf.readByte() & 0xFF];
        }
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        super.serializePlacement(buf);
        for (int i = 2; i < 6; i++) {
            buf.writeByte(connections[i].ordinal());
        }
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        super.deserializePlacement(buf);
        for (int i = 2; i < 6; i++) {
            connections[i] = EnumRedstoneConnection.VALUES[buf.readByte() & 0xFF];
        }
    }

    public static class Factory extends SimpleFactory<ComponentRedstoneWire> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer.Builder(MCMultiPartMod.multipart).add(FRONT, BACK, LEFT, RIGHT).add(POWER_LEVEL).build();
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/wire");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && (stack.getItemDamage() == EnumResourceType.PILE_OF_REDSTONE.ordinal()
                    || stack.getItemDamage() == EnumResourceType.PILE_OF_GLOWING_REDSTONE.ordinal());
        }

        private ComponentRedstoneWire mkWire(ICircuit circuit, BlockPos pos, ItemStack stack) {

            ComponentRedstoneWire wire = new ComponentRedstoneWire(circuit);
            if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
                    IComponent c = circuit.getComponent(pos.offset(side.face), EnumComponentSlot.BOTTOM);
                    if (c != null) {
                        Optional<EnumRedstoneConnection> connection = c.getConnection(IConnectionType.REDSTONE, side.getOpposite());
                        if (connection.isPresent()) {
                            if (connection.get() == EnumRedstoneConnection.NONE && c instanceof IWire) {
                                wire.connections[side.ordinal()] = EnumRedstoneConnection.BIDIRECTIONAL;
                            } else {
                                wire.connections[side.ordinal()] = connection.get().getOpposite();
                                if (wire.connections[side.ordinal()] == EnumRedstoneConnection.BIDIRECTIONAL) {
                                    wire.connections[side.ordinal()] = EnumRedstoneConnection.OUTPUT;
                                }
                            }
                        }
                    }
                }
            }
            return wire;
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.DRAW;
        }

        @Override
        public ComponentRedstoneWire getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec,
                ItemStack stack, EntityPlayer player, EnumPlacementType type, ComponentRedstoneWire previousData,
                Map<BlockPos, ComponentRedstoneWire> otherData, EnumInstantanceUse use) {

            return mkWire(circuit, pos, stack);// c instanceof ComponentWire ? (ComponentWire) c : mkWire(circuit, stack);
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentRedstoneWire data, EnumPlacementType type,
                Map<BlockPos, ComponentRedstoneWire> otherData, boolean simulate) {

            return super.placeComponent(circuit, pos, data, type, otherData, simulate);
        }

        @Override
        public ComponentRedstoneWire instantiate(ICircuit circuit) {

            return new ComponentRedstoneWire(circuit);
        }

    }

}
