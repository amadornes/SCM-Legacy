package com.amadornes.rscircuits.component.wire2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.input.Keyboard;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IPaintableComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.component.wire2.SimpleRedstoneConductor.ConnectionInfo;
import com.amadornes.rscircuits.init.SCMCaps;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;
import com.amadornes.rscircuits.util.ProjectionHelper;
import com.google.common.collect.Multimap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import mcmultipart.MCMultiPartMod;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
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
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;

public class ComponentWire extends ComponentWireFlatBase implements IPaintableComponent {

    public static final PropertyEnum<EnumConnectionType> FRONT = PropertyEnum.create("front", EnumConnectionType.class);
    public static final PropertyEnum<EnumConnectionType> BACK = PropertyEnum.create("back", EnumConnectionType.class);
    public static final PropertyEnum<EnumConnectionType> LEFT = PropertyEnum.create("left", EnumConnectionType.class);
    public static final PropertyEnum<EnumConnectionType> RIGHT = PropertyEnum.create("right", EnumConnectionType.class);
    public static final IUnlistedProperty<Integer> POWER_LEVEL = Properties.toUnlisted(PropertyInteger.create("power_level", 0, 255));

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "wire");

    private byte power = (byte) 0x00;
    private boolean shouldOutputPower = true, shouldPropagate = true;
    private EnumDyeColor color = EnumDyeColor.SILVER;
    private boolean glowing = false;
    private boolean connectOnAdd = true;

    public ComponentWire(ICircuit circuit, boolean glowing, boolean connectOnAdd) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.glowing = glowing;
        this.connectOnAdd = connectOnAdd;
        if (!connectOnAdd) {
            for (ConnectionInfo info : cond.getNeighborsRAW()) {
                info.type = EnumConnectionType.DISABLED;
            }
        }
    }

    public ComponentWire(ICircuit circuit) {

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

        return glowing;
    }

    @Override
    public IBlockState getActualState() {

        IConnectionInfo[] info = getNeighbors();
        return super.getActualState().withProperty(FRONT, info[EnumCircuitSide.FRONT.ordinal()].getType())
                .withProperty(BACK, info[EnumCircuitSide.BACK.ordinal()].getType())
                .withProperty(LEFT, info[EnumCircuitSide.LEFT.ordinal()].getType())
                .withProperty(RIGHT, info[EnumCircuitSide.RIGHT.ordinal()].getType());
    }

    @Override
    public boolean isBundled() {

        return false;
    }

    @Override
    public EnumDyeColor getColor() {

        return color;
    }

    @Override
    public boolean isColorBiased() {

        return true;
    }

    @Override
    public byte getInput(EnumDyeColor color) {

        if (!shouldPropagate) {
            return 0;
        }

        int input = 0;
        for (IConnectionInfo info : getNeighbors()) {
            if (info.getType() == EnumConnectionType.CATHODE) {
                Pair<IComponent, EnumComponentSlot> neighbor = getNeighborPair(slot, info.getSide());
                if (neighbor != null) {
                    if (neighbor.getKey() instanceof IRedstoneConductor && !((IRedstoneConductor) neighbor.getKey()).isBundled()) {
                        for (EnumDyeColor c : EnumDyeColor.values()) {
                            input = Math.max(input,
                                    neighbor.getKey().getOutputSignal(neighbor.getValue(), info.getSide().getOpposite(), c, false) & 0xFF);
                        }
                    } else {
                        if (neighbor.getKey().isOutput(neighbor.getValue(), info.getSide().getOpposite())) {
                            input = Math.max(input,
                                    neighbor.getKey().getOutputSignal(neighbor.getValue(), info.getSide().getOpposite(), this.color, false)
                                            & 0xFF);
                        }
                        if (neighbor.getKey().getBundledConnection(neighbor.getValue(), info.getSide().getOpposite()).isOutput) {
                            input = Math.max(input,
                                    neighbor.getKey().getOutputSignal(neighbor.getValue(), info.getSide().getOpposite(), this.color, true)
                                            & 0xFF);
                        }
                    }
                }
            }
        }
        if (getPos().getY() == 0) {
            for (EnumCircuitSide edge : getEdgesOn()) {
                if (getNeighbors()[edge.ordinal()].getType() == EnumConnectionType.CATHODE) {
                    input = Math.max(input,
                            getCircuit().getInput(edge, color, getCircuit().getIOMode(edge) == EnumCircuitIOMode.BUNDLED) & 0xFF);
                }
            }
        }
        return (byte) input;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return (!getEdgesOn().contains(side) || (getCircuit().getIOMode(side) == EnumCircuitIOMode.BUNDLED) == bundled) && shouldOutputPower
                && isOutput(slot, side) && color == this.color ? power : 0;
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
    public boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return true;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (heldItem != null) {
            if (heldItem.getItem() == SCMItems.resource) {
                ItemStack stack;
                if (glowing && heldItem.getItemDamage() == EnumResourceType.PILE_OF_REDSTONE.ordinal()) {
                    stack = new ItemStack(SCMItems.resource, 1, EnumResourceType.PILE_OF_GLOWING_REDSTONE.ordinal());
                } else if (!glowing && heldItem.getItemDamage() == EnumResourceType.PILE_OF_GLOWING_REDSTONE.ordinal()) {
                    stack = new ItemStack(SCMItems.resource, 1, EnumResourceType.PILE_OF_REDSTONE.ordinal());
                } else {
                    return false;
                }
                glowing = !glowing;
                if (!player.capabilities.isCreativeMode) {
                    heldItem.stackSize--;
                    if (!player.inventory.addItemStackToInventory(stack)) {
                        getCircuit().spawnStack(stack);
                    }
                }
                getCircuit().sendUpdate(getPos(), slot, true);
                getCircuit().markDirty();
            } else if (heldItem.getItem() == SCMItems.screwdriver) {
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
                    if (player.isSneaking()) {
                        if (prevType == EnumConnectionType.CATHODE) {
                            if (neighbor.getKey().isInput(neighbor.getValue(), side.getOpposite())) {
                                type = EnumConnectionType.ANODE;
                            } else {
                                type = EnumConnectionType.DISABLED;
                            }
                        } else {
                            if (neighbor.getKey() instanceof IRedstoneConductor
                                    || neighbor.getKey().isOutput(neighbor.getValue(), side.getOpposite())) {
                                type = EnumConnectionType.CATHODE;
                            } else {
                                type = EnumConnectionType.DISABLED;
                            }
                        }
                    } else {
                        if (prevType.isOutput) {
                            type = EnumConnectionType.DISABLED;
                        } else {
                            if (neighbor.getKey() instanceof IRedstoneConductor
                                    || neighbor.getKey().isInput(neighbor.getValue(), side.getOpposite())) {
                                type = EnumConnectionType.ANODE;
                            } else if (!(neighbor.getKey() instanceof IRedstoneConductor) && type != EnumConnectionType.CATHODE
                                    && neighbor.getKey().isOutput(neighbor.getValue(), side.getOpposite())) {
                                type = EnumConnectionType.CATHODE;
                            } else {
                                type = EnumConnectionType.DISABLED;
                            }
                        }
                    }
                } else if (!isOnSeam() && getPos().getY() == 0) {
                    List<EnumCircuitSide> edges = getEdgesOn();
                    if (edges.contains(side) && isOnEdge(side)) {
                        if (player.isSneaking()) {
                            if (type == EnumConnectionType.CATHODE) {
                                type = EnumConnectionType.ANODE;
                            } else {
                                type = EnumConnectionType.CATHODE;
                            }
                        } else {
                            if (type == EnumConnectionType.ANODE) {
                                type = EnumConnectionType.DISABLED;
                            } else {
                                type = EnumConnectionType.ANODE;
                            }
                        }
                    }
                }
                if (type != prevType) {
                    if (!getCircuit().getWorld().isRemote) {
                        info.type = type;
                        if (neighbor != null && neighbor.getKey() instanceof IRedstoneConductor) {
                            IRedstoneConductor w = (IRedstoneConductor) neighbor.getKey();
                            if (type == EnumConnectionType.DISABLED) {
                                w.disconnect(side.getOpposite());
                                w.propagate(w.isBundled() ? getColor() : w.getColor());
                                info.neighbor = null;
                            } else {
                                info.neighbor = w;
                                w.connect(side.getOpposite(), this, EnumConnectionType.ANODE, false);
                                if (type == EnumConnectionType.CATHODE) {
                                    w.propagate(w.isBundled() ? getColor() : w.getColor());
                                }
                            }
                        } else {
                            getCircuit().notifyUpdate(getPos(), slot, side);
                        }
                        if (!player.capabilities.isCreativeMode) {
                            heldItem.damageItem(1, player);
                        }
                        propagate(getColor());
                        getCircuit().sendUpdate(getPos(), slot, !glowing);
                        getCircuit().markDirty();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onAdded() {

        if (connectOnAdd) {
            super.onAdded();
        }
    }

    @Override
    public void propagate(IRedstoneConductor from, EnumDyeColor color, Multimap<IRedstoneConductor, EnumDyeColor> wires,
            Set<Triple<IRedstoneConductor, IRedstoneConductor, EnumDyeColor>> scheduled) {

        if (shouldPropagate && (!from.isBundled() || color == this.color)) {
            super.propagate(from, color, wires, scheduled);
        }
    }

    @Override
    public void onPropagated(Set<EnumDyeColor> colors, byte power) {

        if (this.power != power) {
            this.power = power;
            if (getPos() != null) {
                super.onPropagated(colors, power);
                getCircuit().markDirty();
                if (glowing) {
                    getCircuit().sendUpdate(getPos(), slot, false);
                }
            }
        }
    }

    @Override
    public boolean connect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type, boolean simulated) {

        return (!simulated || connectOnAdd || cond.getNeighbors()[side.ordinal()].getType() != EnumConnectionType.DISABLED)
                && super.connect(side, conductor, type, simulated);
    }

    @Override
    public void onConnect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type) {

        getCircuit().markDirty();
        getCircuit().sendUpdate(getPos(), slot, !glowing);
    }

    @Override
    public void onDisconnect(EnumCircuitSide side) {

        getCircuit().markDirty();
        getCircuit().sendUpdate(getPos(), slot, !glowing);
    }

    @Override
    public void onWorldChange() {

        if (getPos().getY() == 0) {
            cond.propagate(getColor());
        }
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 2 / 16D, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1,
                (glowing ? EnumResourceType.PILE_OF_GLOWING_REDSTONE : EnumResourceType.PILE_OF_REDSTONE).ordinal());
    }

    @Override
    public int getColorMultiplier(int index) {

        if (index == 0) {
            return color.getMapColor().colorValue;
        } else if (index == 1) {
            int minBrightness = 128;
            int pow = glowing ? (int) (((power & 0xFF) / 255F) * (255 - minBrightness) + minBrightness) : (255 + 128) / 2;
            return (pow << 16) | (pow << 8) | (pow << 0);
        }
        return super.getColorMultiplier(index);
    }

    @Override
    public boolean paint(EnumDyeColor color) {

        if (getCircuit().getWorld().isRemote && this.color != color) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(color.getMetadata());
            getCircuit().sendCustomPayload(getPos(), slot, buf);
            return this.color != color;
        }
        return false;
    }

    @Override
    public void handleCustomPayload(ByteBuf buf) {

        shouldPropagate = false;
        shouldOutputPower = false;
        getCircuit().notifyUpdateAll(getPos(), slot);
        shouldOutputPower = true;
        shouldPropagate = true;

        EnumDyeColor prevColor = this.color;
        this.color = EnumDyeColor.byMetadata(buf.readByte() & 0xFF);
        if (prevColor != this.color) {
            getCircuit().notifyUpdateAll(getPos(), slot);
            cond.propagate(getColor());
            getCircuit().sendUpdate(getPos(), slot, !glowing);
            getCircuit().markDirty();
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);

        tag.setBoolean("glowing", glowing);
        tag.setInteger("color", color.getMetadata());
        tag.setByte("power", power);

        int connections = 0;
        for (IConnectionInfo info : getNeighbors()) {
            // 2 data bits per connection, so 4 possible states (EWC has 5 but only 4 are used by regular wires)
            connections |= info.getType().ordinal() << (info.getSide().ordinal() * 2);
        }
        tag.setInteger("connections", connections);

        tag.setByte("version", (byte) 3);

        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);

        glowing = tag.getBoolean("glowing");
        color = tag.hasKey("color") ? EnumDyeColor.byMetadata(tag.getInteger("color")) : EnumDyeColor.SILVER;
        power = tag.getByte("power");

        int version = tag.hasKey("version") ? tag.getByte("version") & 0xFF : 1;
        if (version == 1) {
            int connections = tag.getByteArray("connections")[0] & 0xFF;
            for (int i = 0; i < 4; i++) {
                cond.getNeighborsRAW()[i + 2].type = EnumConnectionType.VALUES[(connections >> (i * 2)) & 0x3];
            }
        } else if (version == 2) {
            int connections = tag.getByte("connections") & 0xFF;
            for (int i = 0; i < 4; i++) {
                cond.getNeighborsRAW()[i + 2].type = EnumConnectionType.VALUES[(connections >> (i * 2)) & 0x3];
            }
        } else if (version == 3) {
            int connections = tag.getInteger("connections");
            for (ConnectionInfo info : cond.getNeighborsRAW()) {
                info.type = EnumConnectionType.VALUES[(connections >> (info.side.ordinal() * 2)) & 0x3];
            }
        }
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeBoolean(glowing);
        buf.writeEnumValue(color);
        if (glowing) {
            buf.writeByte(power);
        }
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
        glowing = buf.readBoolean();
        color = buf.readEnumValue(EnumDyeColor.class);
        if (glowing) {
            power = buf.readByte();
        } else {
            power = (byte) 0;
        }
        int connections = buf.readInt();
        for (ConnectionInfo info : cond.getNeighborsRAW()) {
            info.type = EnumConnectionType.VALUES[(connections >> (info.side.ordinal() * 2)) & 0x3];
        }
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

        super.serializePlacement(buf);
        buf.writeBoolean(glowing);
        buf.writeBoolean(connectOnAdd);
        buf.writeEnumValue(color);
    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

        super.deserializePlacement(buf);
        glowing = buf.readBoolean();
        connectOnAdd = buf.readBoolean();
        color = buf.readEnumValue(EnumDyeColor.class);
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

        player.addChatMessage(new TextComponentString("Power Level: " + (power & 0xFF)));
    }

    public static class Factory extends SimpleFactory<ComponentWire> {

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

        private ComponentWire mkWire(ICircuit circuit, ItemStack stack) {

            ComponentWire wire = new ComponentWire(circuit, stack.getItemDamage() == EnumResourceType.PILE_OF_GLOWING_REDSTONE.ordinal(),
                    !Keyboard.isKeyDown(Keyboard.KEY_LCONTROL));
            EntityPlayer player = SCM.proxy.getPlayer();
            if (player != null && player.getHeldItemOffhand() != null) {
                ItemStack offStack = player.getHeldItemOffhand();
                if (offStack.hasCapability(SCMCaps.PALETTE, null)) {
                    wire.color = offStack.getCapability(SCMCaps.PALETTE, null).getColor();
                }
            }
            return wire;
        }

        @Override
        public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

            return EnumPlacementType.DRAW;
        }

        @Override
        public ComponentWire getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, ComponentWire previousData, Map<BlockPos, ComponentWire> otherData,
                EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            if (!circuit.isSideSolid(pos.down(), EnumCircuitSide.TOP)) {
                return null;
            }
            IComponent c = circuit.getComponent(pos, EnumComponentSlot.BOTTOM);
            return c instanceof ComponentWire ? (ComponentWire) c : mkWire(circuit, stack);
        }

        @Override
        public boolean placeComponent(ICircuit circuit, BlockPos pos, ComponentWire data, EnumPlacementType type,
                Map<BlockPos, ComponentWire> otherData, boolean simulate) {

            for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
                ComponentWire neighbor = otherData.get(pos.offset(side.face));
                if (neighbor != null) {
                    ConnectionInfo info = data.cond.getNeighborsRAW()[side.ordinal()];
                    info.type = EnumConnectionType.ANODE;
                    info.neighbor = neighbor;
                }
            }
            return circuit.addComponent(pos, data, simulate);
        }

        @Override
        public void drawPlacement(ICircuit circuit, BlockPos pos, ComponentWire data, EnumPlacementType type,
                Map<BlockPos, ComponentWire> otherData) {

            Pair<EnumConnectionType, Boolean> front = getState(circuit, pos, EnumCircuitSide.FRONT, otherData, data.connectOnAdd,
                    data.color);
            Pair<EnumConnectionType, Boolean> back = getState(circuit, pos, EnumCircuitSide.BACK, otherData, data.connectOnAdd, data.color);
            Pair<EnumConnectionType, Boolean> left = getState(circuit, pos, EnumCircuitSide.LEFT, otherData, data.connectOnAdd, data.color);
            Pair<EnumConnectionType, Boolean> right = getState(circuit, pos, EnumCircuitSide.RIGHT, otherData, data.connectOnAdd,
                    data.color);

            IBlockState state = data.getActualState().withProperty(FRONT, front.getKey()).withProperty(BACK, back.getKey())
                    .withProperty(LEFT, left.getKey()).withProperty(RIGHT, right.getKey());
            SimpleFactory.draw(data, state, state);
            if (data.connectOnAdd) {
                if (front.getValue()) {
                    state = data.getActualState().withProperty(FRONT, EnumConnectionType.NONE).withProperty(BACK, front.getKey())
                            .withProperty(LEFT, EnumConnectionType.NONE).withProperty(RIGHT, EnumConnectionType.NONE);
                    GlStateManager.translate(0, 0, 1);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(0, 0, -1);
                }
                if (back.getValue()) {
                    state = data.getActualState().withProperty(FRONT, back.getKey()).withProperty(BACK, EnumConnectionType.NONE)
                            .withProperty(LEFT, EnumConnectionType.NONE).withProperty(RIGHT, EnumConnectionType.NONE);
                    GlStateManager.translate(0, 0, -1);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(0, 0, 1);
                }
                if (left.getValue()) {
                    state = data.getActualState().withProperty(FRONT, EnumConnectionType.NONE).withProperty(BACK, EnumConnectionType.NONE)
                            .withProperty(LEFT, EnumConnectionType.NONE).withProperty(RIGHT, left.getKey());
                    GlStateManager.translate(1, 0, 0);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(-1, 0, 0);
                }
                if (right.getValue()) {
                    state = data.getActualState().withProperty(FRONT, EnumConnectionType.NONE).withProperty(BACK, EnumConnectionType.NONE)
                            .withProperty(LEFT, right.getKey()).withProperty(RIGHT, EnumConnectionType.NONE);
                    GlStateManager.translate(-1, 0, 0);
                    SimpleFactory.draw(data, state, state);
                    GlStateManager.translate(1, 0, 0);
                }
            }
        }

        private Pair<EnumConnectionType, Boolean> getState(ICircuit circuit, BlockPos pos, EnumCircuitSide side,
                Map<BlockPos, ComponentWire> otherData, boolean connectOnAdd, EnumDyeColor color) {

            if (otherData.containsKey(pos.offset(EnumCircuitSide.FRONT.face))) {
                return Pair.of(EnumConnectionType.ANODE, true);
            }
            if (connectOnAdd) {
                IComponent c = circuit.getComponent(pos.offset(side.face), EnumComponentSlot.BOTTOM);
                if (c != null) {
                    if (c instanceof IRedstoneConductor) {
                        EnumDyeColor col = ((IRedstoneConductor) c).getColor();
                        return col == null || col == color ? Pair.of(EnumConnectionType.ANODE, true)
                                : Pair.of(EnumConnectionType.NONE, false);
                    } else if (c.isInput(EnumComponentSlot.BOTTOM, side.getOpposite())) {
                        return Pair.of(EnumConnectionType.ANODE, false);
                    } else if (c.isOutput(EnumComponentSlot.BOTTOM, side.getOpposite())) {
                        return Pair.of(EnumConnectionType.CATHODE, false);
                    } else {
                        return Pair.of(EnumConnectionType.NONE, false);
                    }
                }
            }
            return Pair.of(EnumConnectionType.NONE, false);
        }

        @Override
        public ComponentWire instantiate(ICircuit circuit) {

            return new ComponentWire(circuit);
        }

    }

}
