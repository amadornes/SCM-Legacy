package com.amadornes.rscircuits.circuit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.CommonProxy;
import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;
import com.amadornes.rscircuits.api.circuit.HandledCircuitException;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.ComponentBase;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.util.GistPublisher;
import com.amadornes.rscircuits.util.RedstoneUtils;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.AtomicDouble;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Circuit implements ICircuit {

    public static final List<String> tags = Arrays.asList("components", "updates", "name");

    private final ICircuitContainer container;

    public IComponent[][][][] components = new IComponent[8][5][8][7];
    private Map<Long, Map<IComponent, Map<Integer, Object>>> scheduledTicks = new TreeMap<>(Long::compare),
            scheduledTicksBkp = new TreeMap<>(Long::compare);
    private Map<Pair<BlockPos, EnumComponentSlot>, Boolean> updates = new HashMap<>();
    private boolean iterating = false, scheduledOffset = false;
    private EnumCircuitIOMode[] ioModes = new EnumCircuitIOMode[4];
    private String name = "";
    private Set<IComponent> nonInteractable = new HashSet<>();

    private MutablePair<EnumCircuitCrashStatus, String> crash = new MutablePair<>(EnumCircuitCrashStatus.NO_CRASH, null);

    public Circuit(ICircuitContainer container) {

        this.container = container;
        Arrays.fill(ioModes, EnumCircuitIOMode.REGULAR);
    }

    @Override
    public World getWorld() {

        return container != null ? container.getWorld() : null;
    }

    @Override
    public BlockPos getPos() {

        return container != null ? container.getPos() : BlockPos.ORIGIN;
    }

    @Override
    public EnumFacing getFace() {

        return container.getFace();
    }

    @Override
    public EnumCircuitIOMode getIOMode(EnumCircuitSide side) {

        return ioModes[side.ordinal() - 2];
    }

    public EnumCircuitIOMode[] getIOModes() {

        return ioModes;
    }

    public void cycleModes(EnumCircuitSide side) {

        ioModes[side.ordinal() - 2] = ioModes[side.ordinal() - 2] == EnumCircuitIOMode.BUNDLED ? EnumCircuitIOMode.REGULAR
                : EnumCircuitIOMode.BUNDLED;
        container.sendUpdatePacket();
    }

    @Override
    public IComponent getComponent(BlockPos pos, EnumComponentSlot slot) {

        Circuit circuit = getCircuit(pos);
        pos = RedstoneUtils.limitPositionToBounds(pos);
        if (circuit == this) {
            return components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()];
        } else if (circuit != null) {
            return circuit.getComponent(pos, slot);
        }
        return null;
    }

    @Override
    public boolean addComponent(BlockPos pos, IComponent component, boolean simulate) {

        Circuit circuit = getCircuit(pos);
        pos = RedstoneUtils.limitPositionToBounds(pos);
        if (circuit == this) {
            if (pos.getY() < 4) {
                EnumSet<EnumComponentSlot> slots = component.getSlots();
                if (slots.isEmpty()) {
                    return false;
                }
                for (EnumComponentSlot slot : slots) {
                    if (components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()] != null) {
                        return false;
                    }
                }
                if (!simulate) {
                    component.setPos(pos);
                    for (EnumComponentSlot slot : slots) {
                        components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()] = component;
                    }
                    component.onAdded();
                    for (EnumComponentSlot s : component.getSlots()) {
                        notifyUpdate(component.getPos(), s, EnumCircuitUpdate.COMPONENT_ADD, EnumCircuitSide.VALUES);
                    }
                    component.onAddedPost();
                    sendUpdate(pos, component.getSlots().iterator().next(), !component.isDynamic());
                    markDirty();
                    nonInteractable.add(component);
                }
                return true;
            }
            return false;
        } else if (circuit != null) {
            return circuit.addComponent(pos, component, simulate);
        }
        return false;
    }

    @Override
    public void removeComponent(IComponent component) {

        if (component == null) {
            return;
        }
        BlockPos pos = component.getPos();
        for (EnumComponentSlot s : component.getSlots()) {
            components[pos.getX()][pos.getY()][pos.getZ()][s.ordinal()] = null;
        }
        component.onRemoved();
        for (EnumComponentSlot s : component.getSlots()) {
            notifyUpdate(component.getPos(), s, EnumCircuitUpdate.COMPONENT_REMOVE, component, EnumCircuitSide.VALUES);
        }
        component.onRemovedPost();
        if (!component.isDynamic() && component.getActualState() != null) {
            container.markRenderUpdate();
        }
        scheduledTicks.values().forEach(m -> m.remove(component));
        component.getSlots().forEach(s -> sendUpdate(pos, s, !component.isDynamic()));
        markDirty();
    }

    @Override
    public void replaceComponent(IComponent component, IComponent newComponent) {

        if (component == null || !component.getSlots().equals(newComponent.getSlots())) {
            return;
        }
        BlockPos pos = component.getPos();
        newComponent.setPos(pos);
        for (EnumComponentSlot s : component.getSlots()) {
            components[pos.getX()][pos.getY()][pos.getZ()][s.ordinal()] = newComponent;
        }
        component.onRemoved();
        newComponent.onAdded();
        for (EnumComponentSlot s : component.getSlots()) {
            notifyUpdate(component.getPos(), s, EnumCircuitUpdate.COMPONENT_ADD, component, EnumCircuitSide.VALUES);
        }
        component.onRemovedPost();
        if (!component.isDynamic() && component.getActualState() != null) {
            container.markRenderUpdate();
        }
        scheduledTicks.values().forEach(m -> m.remove(component));
        component.getSlots().forEach(s -> sendUpdate(pos, s, !component.isDynamic()));
        markDirty();
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumCircuitSide side) {

        if (pos.getY() == -1 && side == EnumCircuitSide.TOP) {
            return true;
        }
        Circuit circuit = getCircuit(pos);
        pos = RedstoneUtils.limitPositionToBounds(pos);
        if (circuit == this) {
            IComponent comp = components[pos.getX()][pos.getY()][pos.getZ()][side.ordinal()];
            if (comp != null) {
                return comp.isSideSolid(side);
            }
            comp = components[pos.getX()][pos.getY()][pos.getZ()][EnumComponentSlot.CENTER.ordinal()];
            if (comp != null) {
                return comp.isSideSolid(side);
            }
            return false;
        } else if (circuit != null) {
            return circuit.isSideSolid(pos, side);
        }
        return false;
    }

    @Override
    public void scheduleTick(IComponent component, int delay, int type, Object data) {

        if (getWorld() != null && getWorld().isRemote) {
            return;
        }

        Map<Long, Map<IComponent, Map<Integer, Object>>> scheduledTicks = !iterating ? this.scheduledTicks : this.scheduledTicksBkp;
        long finalTime = getWorld().getTotalWorldTime() + delay;
        Map<IComponent, Map<Integer, Object>> componentMap = scheduledTicks.get(finalTime);
        if (componentMap == null) {
            scheduledTicks.put(finalTime, componentMap = new HashMap<>());
        }
        Map<Integer, Object> tickMap = componentMap.get(component);
        if (tickMap == null) {
            componentMap.put(component, tickMap = new HashMap<>());
        }
        tickMap.put(type, data);
    }

    @Override
    public void notifyUpdate(BlockPos pos, EnumComponentSlot slot, EnumCircuitSide... directions) {

        notifyUpdate(pos, slot, EnumCircuitUpdate.COMPONENT_UPDATE, directions);
    }

    public void notifyUpdate(BlockPos pos, EnumComponentSlot slot, EnumCircuitUpdate updateType, EnumCircuitSide... directions) {

        if (getWorld() == null || getWorld().isRemote) {
            return;
        }

        notifyUpdate(pos, slot, updateType, getComponent(pos, slot), directions);
    }

    public void notifyUpdate(BlockPos pos, EnumComponentSlot slot, EnumCircuitUpdate updateType, IComponent comp,
            EnumCircuitSide... directions) {

        if (getWorld() == null || getWorld().isRemote) {
            return;
        }

        for (EnumCircuitSide side : directions) {
            if (side == null) {
                continue;
            }
            Pair<IComponent, EnumComponentSlot> neighbor = ComponentBase.getNeighborPairConditioned(this, pos, slot, side,
                    (c, s) -> c != comp);
            if (neighbor != null) {
                neighbor.getKey().onNeighborChange(side.getOpposite(), neighbor.getRight(), comp, updateType);
            }
        }
    }

    @Override
    public void notifyUpdateAll(BlockPos pos, EnumComponentSlot slot) {

        notifyUpdate(pos, slot, EnumCircuitSide.VALUES);
    }

    @Override
    public void sendUpdate(BlockPos pos, EnumComponentSlot slot, boolean reRender) {

        if (pos == null) {
            return;
        }
        updates.merge(Pair.of(pos, slot), reRender, (a, b) -> a || b);
        synchronized (CommonProxy.updatedCircuits) {
            CommonProxy.updatedCircuits.add(this);
        }
    }

    @Override
    public void markDirty() {

        container.markDirty();
    }

    public Map<Pair<BlockPos, EnumComponentSlot>, Boolean> getUpdates() {

        return updates;
    }

    // public Circuit getOwnCircuit(BlockPos pos) {
    //
    // if (pos.getX() >= 0 && pos.getX() < 7 && pos.getY() >= 0 && pos.getY() < 5 && pos.getZ() >= 0 && pos.getZ() < 7) {
    // return this;
    // }
    // if (getWorld() == null || getPos() == null) {
    // return null;
    // }
    // boolean gX = pos.getX() == 7, gZ = pos.getZ() == 7;
    // if ((gX && container.getCircuitAt(getWorld(), getPos().add(RedstoneUtils.correctOffset(new BlockPos(1, 0, 0), getFace())),
    // getFace()) == null)
    // || (gZ && container.getCircuitAt(getWorld(), getPos().add(RedstoneUtils.correctOffset(new BlockPos(0, 0, 1), getFace())),
    // getFace()) == null)
    // || (gX && gZ && container.getCircuitAt(getWorld(),
    // getPos().add(RedstoneUtils.correctOffset(new BlockPos(1, 0, 1), getFace())), getFace()) == null)) {
    // return null;
    // }
    // return this;
    // }

    @Override
    public Circuit getCircuit(BlockPos pos) {

        if (pos.getX() >= 0 && pos.getX() < 7 && pos.getY() >= 0 && pos.getY() < 5 && pos.getZ() >= 0 && pos.getZ() < 7) {
            return this;
        }
        if (!container.isInWorld() || getWorld() == null || getPos() == null) {
            return null;
        }
        Pair<BlockPos, BlockPos> coords = RedstoneUtils.correctCoords(BlockPos.ORIGIN, getFace(), pos);
        if (coords.getKey().distanceSq(BlockPos.ORIGIN) == 0) {
            boolean gX = coords.getValue().getX() == 7, gZ = coords.getValue().getZ() == 7;
            if ((gX && container.getCircuitAt(getPos().add(RedstoneUtils.correctOffset(new BlockPos(1, 0, 0), getFace())),
                    getFace()) == null)
                    || (gZ && container.getCircuitAt(getPos().add(RedstoneUtils.correctOffset(new BlockPos(0, 0, 1), getFace())),
                            getFace()) == null)
                    || (gX && gZ && container.getCircuitAt(getPos().add(RedstoneUtils.correctOffset(new BlockPos(1, 0, 1), getFace())),
                            getFace()) == null)) {
                return null;
            }
            return this;
        } else {
            return container.getCircuitAt(getPos().add(coords.getKey()), getFace());
        }
    }

    @Override
    public byte getInput(EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return container.getInput(side, color, bundled);
    }

    @Override
    public void rotate(Rotation rotation) {

        Function<BlockPos, BlockPos> rotatePos;
        switch (rotation) {
        case CLOCKWISE_90:
            rotatePos = p -> new BlockPos(6 - p.getZ(), p.getY(), p.getX());
            break;
        case CLOCKWISE_180:
            rotatePos = p -> new BlockPos(6 - p.getX(), p.getY(), 6 - p.getZ());
            break;
        case COUNTERCLOCKWISE_90:
            rotatePos = p -> new BlockPos(p.getZ(), p.getY(), 6 - p.getX());
            break;
        case NONE:
        default:
            rotatePos = Function.identity();
            break;
        }

        EnumCircuitIOMode[] ioModes2 = new EnumCircuitIOMode[ioModes.length];
        for (int i = 0; i < ioModes.length; i++) {
            ioModes2[EnumCircuitSide.HORIZONTALS[i].rotate(rotation).ordinal() - 2] = ioModes[i];
        }
        System.arraycopy(ioModes2, 0, ioModes, 0, ioModes.length);

        Set<IComponent> rotated = new HashSet<IComponent>();
        IComponent[][][][] components = new IComponent[8][5][8][7];// TODO: Check this. Y = 4, not 5...?
        forEach(rotated::add, true);
        rotated.forEach(c -> c.rotatePre(rotation));
        rotated.forEach(c -> {
            BlockPos pos = rotatePos.apply(c.getPos());
            for (EnumComponentSlot s : c.getSlots()) {
                components[pos.getX()][pos.getY()][pos.getZ()][s.ordinal()] = c;
            }
            c.setPos(pos);
        });
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                components[i][j][7] = this.components[i][j][7];
                components[7][j][i] = this.components[7][j][i];
            }
        }
        this.components = components;
        rotated.forEach(c -> c.rotatePost());
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 4; j++) {
                for (EnumComponentSlot s : EnumComponentSlot.VALUES) {
                    IComponent c;

                    c = getComponent(new BlockPos(i, j, -1), s);
                    if (c != null) {
                        c.onNeighborChange(EnumCircuitSide.FRONT, s, components[i][j][0][s.ordinal()], //
                                EnumCircuitUpdate.CIRCUIT_ROTATE);
                    }
                    c = getComponent(new BlockPos(i, j, 7), s);
                    if (c != null) {
                        c.onNeighborChange(EnumCircuitSide.BACK, s, components[i][j][6][s.ordinal()], //
                                EnumCircuitUpdate.CIRCUIT_ROTATE);
                    }
                    c = getComponent(new BlockPos(-1, j, i), s);
                    if (c != null) {
                        c.onNeighborChange(EnumCircuitSide.LEFT, s, components[0][j][i][s.ordinal()], //
                                EnumCircuitUpdate.CIRCUIT_ROTATE);
                    }
                    c = getComponent(new BlockPos(7, j, i), s);
                    if (c != null) {
                        c.onNeighborChange(EnumCircuitSide.RIGHT, s, components[6][j][i][s.ordinal()], //
                                EnumCircuitUpdate.CIRCUIT_ROTATE);
                    }
                }
            }
        }

        markDirty();
        container.sendUpdatePacket();
    }

    @Override
    public void sendCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buf) {

        container.sendCustomPayload(pos, slot, buf);
    }

    @Override
    public boolean isEncapsulated() {

        return container.isEncapsulated();
    }

    public void handleCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buf) {

        IComponent c = components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()];
        if (c != null) {
            c.handleCustomPayload(buf);
        }
    }

    public void forEach(Consumer<IComponent> consumer) {

        forEach(consumer, false);
    }

    public void forEach(Consumer<IComponent> consumer, BiFunction<IComponent, Throwable, RuntimeException> errorHandler) {

        forEach(consumer, false, errorHandler);
    }

    public void forEach(Consumer<IComponent> consumer, boolean limitTo7x7) {

        forEach(consumer, limitTo7x7, (a, b) -> Throwables.propagate(b));
    }

    public void forEach(Consumer<IComponent> consumer, boolean limitTo7x7,
            BiFunction<IComponent, Throwable, RuntimeException> errorHandler) {

        Set<IComponent> visited = new HashSet<IComponent>();
        int size = limitTo7x7 ? 7 : 8;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < size; z++) {
                    for (int s = 0; s < 7; s++) {
                        IComponent c = components[x][y][z][s];
                        if (c != null && visited.add(c)) {
                            try {
                                consumer.accept(c);
                            } catch (HandledCircuitException e) {
                                throw e;
                            } catch (Throwable e) {
                                RuntimeException ex = errorHandler.apply(c, e);
                                if (ex != null) {
                                    throw ex;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void forEachEdge(Consumer<IComponent> consumer, EnumCircuitSide edge, int startY, int endY, EnumComponentSlot... slots) {

        if (edge == EnumCircuitSide.RIGHT) {
            for (int z = 0; z < 7; z++) {
                for (int y = startY; y <= endY; y++) {
                    for (EnumComponentSlot s : slots) {
                        IComponent c = components[0][y][z][s.ordinal()];
                        if (c != null) {
                            consumer.accept(c);
                        }
                    }
                }
            }
        } else if (edge == EnumCircuitSide.LEFT) {
            for (int z = 0; z < 7; z++) {
                for (int y = startY; y <= endY; y++) {
                    for (EnumComponentSlot s : slots) {
                        IComponent c = components[6][y][z][s.ordinal()];
                        if (c != null) {
                            consumer.accept(c);
                        }
                    }
                }
            }
        } else if (edge == EnumCircuitSide.BACK) {
            for (int x = 0; x < 7; x++) {
                for (int y = startY; y <= endY; y++) {
                    for (EnumComponentSlot s : slots) {
                        IComponent c = components[x][y][0][s.ordinal()];
                        if (c != null) {
                            consumer.accept(c);
                        }
                    }
                }
            }
        } else if (edge == EnumCircuitSide.FRONT) {
            for (int x = 0; x < 7; x++) {
                for (int y = startY; y <= endY; y++) {
                    for (EnumComponentSlot s : slots) {
                        IComponent c = components[x][y][6][s.ordinal()];
                        if (c != null) {
                            consumer.accept(c);
                        }
                    }
                }
            }
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        NBTTagList components = new NBTTagList();
        Set<IComponent> visited = new HashSet<IComponent>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    for (int s = 0; s < 7; s++) {
                        IComponent c = this.components[x][y][z][s];
                        if (c != null && visited.add(c)) {
                            NBTTagCompound t = new NBTTagCompound();
                            c.writeToNBT(t);
                            t.setString("__type", c.getName().toString());
                            t.setLong("__pos", c.getPos().toLong());
                            components.appendTag(t);
                        }
                    }
                }
            }
        }
        visited.clear();
        tag.setTag("components", components);
        tag.setInteger("componentsVersion", 2);

        NBTTagCompound updates = new NBTTagCompound();
        scheduledTicks.forEach((k, v) -> {
            NBTTagCompound tickUpdates = new NBTTagCompound();
            v.forEach((c, u) -> {
                NBTTagCompound t = new NBTTagCompound();
                u.forEach((m, o) -> {
                    NBTTagCompound d = c.serializeTickData(m, o);
                    if (d != null) {
                        t.setTag(m + "", d);
                    }
                });
                tickUpdates.setTag(c.getPos().toLong() + "_" + c.getSlots().iterator().next().name().toLowerCase(), t);
            });
            updates.setTag((k - container.getWorld().getTotalWorldTime()) + "", tickUpdates);
        });
        tag.setTag("updates", updates);

        tag.setString("name", name);

        tag.setInteger("crash", crash.getLeft().ordinal());
        if (crash.getLeft() == EnumCircuitCrashStatus.UPLOADED) {
            tag.setString("crashInfo", crash.getRight());
        }

        tag.setIntArray("iomodes", new int[] { ioModes[0].ordinal(), ioModes[1].ordinal(), ioModes[2].ordinal(), ioModes[3].ordinal() });

        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {

        readFromNBT(tag, false);
    }

    public void readFromNBT(NBTTagCompound tag, boolean notify) {

        NBTTagList components = tag.getTagList("components", 10);
        int componentsVersion = tag.hasKey("componentsVersion") ? tag.getInteger("componentsVersion") : 1;
        if (componentsVersion == 1) {
            int i = 0;
            for (int x = 0; x < 8; x++) {
                int x_ = x;
                for (int y = 0; y < 5; y++) {
                    int y_ = y;
                    for (int z = 0; z < 8; z++) {
                        int z_ = z;
                        BlockPos pos = null;
                        for (int s = 0; s < 7; s++) {
                            NBTTagCompound t = components.getCompoundTagAt(i);
                            if (t.hasKey("__type")) {
                                IComponent c = ComponentRegistry.INSTANCE.getFactory(new ResourceLocation(t.getString("__type")))
                                        .instantiate(this);
                                if (c != null) {
                                    c.setPos(pos == null ? pos = new BlockPos(x, y, z) : pos);
                                    c.readFromNBT(t);
                                    c.getSlots().forEach(slot -> this.components[x_][y_][z_][slot.ordinal()] = c);
                                }
                            }
                            i++;
                        }
                    }
                }
            }
        } else if (componentsVersion == 2) {
            for (int i = 0; i < components.tagCount(); i++) {
                NBTTagCompound t = components.getCompoundTagAt(i);
                IComponent c = ComponentRegistry.INSTANCE.getFactory(new ResourceLocation(t.getString("__type"))).instantiate(this);
                if (c != null) {
                    BlockPos pos = BlockPos.fromLong(t.getLong("__pos"));
                    c.setPos(pos);
                    c.readFromNBT(t);
                    c.getSlots().forEach(slot -> this.components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()] = c);
                }
            }
        }

        this.scheduledTicks.clear();
        NBTTagCompound updates = tag.getCompoundTag("updates");
        updates.getKeySet().forEach(s1 -> {
            NBTTagCompound tickUpdates = updates.getCompoundTag(s1);
            long time = Long.parseLong(s1);
            Map<IComponent, Map<Integer, Object>> map = new HashMap<>();
            tickUpdates.getKeySet().forEach(s2 -> {
                String[] s3 = s2.split("_");
                BlockPos pos = BlockPos.fromLong(Long.parseLong(s3[0]));
                EnumComponentSlot slot = EnumComponentSlot.valueOf(s3[1].toUpperCase());
                IComponent c = this.components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()];
                if (c != null) {
                    Map<Integer, Object> m = new HashMap<>();
                    NBTTagCompound d = tickUpdates.getCompoundTag(s2);
                    d.getKeySet().forEach(s4 -> {
                        int type = Integer.parseInt(s4);
                        m.put(type, c.deserializeTickData(type, d.getCompoundTag(s4)));
                    });
                    map.put(c, m);
                }
            });
            scheduledTicks.put(time, map);
        });
        this.scheduledOffset = true;

        if (tag.hasKey("name")) {
            name = tag.getString("name");
        }

        if (tag.hasKey("crash")) {
            crash.setLeft(EnumCircuitCrashStatus.values()[tag.getInteger("crash")]);
            if (tag.hasKey("crashInfo")) {
                crash.setRight(tag.getString("crashInfo"));
            }
        }

        if (tag.hasKey("iomodes")) {
            int[] iomodes = tag.getIntArray("iomodes");
            for (int i = 0; i < 4; i++) {
                ioModes[i] = EnumCircuitIOMode.values()[iomodes[i]];
            }
        }

        if (notify) {
            forEach(IComponent::onLoaded);
        }
    }

    public void writeUpdatePacket(PacketBuffer buf) {

        Set<IComponent> visited = new HashSet<IComponent>();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    for (int s = 0; s < 7; s++) {
                        IComponent c = components[x][y][z][s];
                        if (c == null) {
                            buf.writeBoolean(false);
                        } else {
                            buf.writeBoolean(true);
                            if (visited.add(c)) {
                                buf.writeBoolean(true);
                                buf.writeString(c.getName().toString());
                                c.writeDescription(buf);
                            } else {
                                buf.writeBoolean(false);
                            }
                        }
                    }
                }
            }
        }
        buf.writeEnumValue(ioModes[0]).writeEnumValue(ioModes[1]).writeEnumValue(ioModes[2]).writeEnumValue(ioModes[3]);
        visited.clear();
        buf.writeString(name);
    }

    public void readUpdatePacket(PacketBuffer buf) {

        Set<IComponent> newComponents = new HashSet<IComponent>();
        for (int x = 0; x < 8; x++) {
            int x_ = x;
            for (int y = 0; y < 5; y++) {
                int y_ = y;
                for (int z = 0; z < 8; z++) {
                    int z_ = z;
                    BlockPos pos = null;
                    for (int s = 0; s < 7; s++) {
                        if (buf.readBoolean()) {
                            if (buf.readBoolean()) {
                                IComponent c = components[x][y][z][s];
                                ResourceLocation name = new ResourceLocation(buf.readStringFromBuffer(128));
                                boolean wasThere = c != null && c.getName().equals(name);
                                if (!wasThere) {
                                    c = ComponentRegistry.INSTANCE.getFactory(name).instantiate(this);
                                    c.setPos(pos == null ? pos = new BlockPos(x, y, z) : pos);
                                    c.readDescription(buf);
                                    IComponent c_ = c;
                                    c.getSlots().forEach(slot -> components[x_][y_][z_][slot.ordinal()] = c_);
                                    newComponents.add(c);
                                } else {
                                    c.readDescription(buf);
                                }
                            }
                        } else {
                            components[x][y][z][s] = null;
                        }
                    }
                }
            }
        }
        ioModes[0] = buf.readEnumValue(EnumCircuitIOMode.class);
        ioModes[1] = buf.readEnumValue(EnumCircuitIOMode.class);
        ioModes[2] = buf.readEnumValue(EnumCircuitIOMode.class);
        ioModes[3] = buf.readEnumValue(EnumCircuitIOMode.class);
        newComponents.forEach(IComponent::onLoaded);
        newComponents.clear();
        name = buf.readStringFromBuffer(16);
    }

    public void tickScheduled() {

        if (getWorld() != null && !getWorld().isRemote) {
            long now = getWorld().getTotalWorldTime();
            if (scheduledOffset) {
                scheduledOffset = false;
                scheduledTicks.forEach((t, m) -> scheduledTicksBkp.put(t + now, m));
                Map<Long, Map<IComponent, Map<Integer, Object>>> tmp = scheduledTicks;
                scheduledTicks = scheduledTicksBkp;
                scheduledTicksBkp = tmp;
            }
            Set<Long> removed = new HashSet<Long>();
            iterating = true;
            for (Entry<Long, Map<IComponent, Map<Integer, Object>>> e : scheduledTicks.entrySet()) {
                if (e.getKey() > now) {
                    break;
                }
                e.getValue().forEach((c, u) -> {
                    u.forEach((t, d) -> {
                        c.onScheduledTick(t, d);
                    });
                });
                removed.add(e.getKey());
            }
            removed.forEach(scheduledTicks::remove);
            removed.clear();
            iterating = false;
            scheduledTicksBkp.forEach((k, v) -> {
                scheduledTicks.merge(k, v, (m1, m2) -> {
                    m2.forEach((k1, v1) -> {
                        m1.merge(k1, v1, (m3, m4) -> {
                            m3.putAll(m4);
                            return m3;
                        });
                    });
                    return m1;
                });
            });
            scheduledTicksBkp.clear();
        }
    }

    public void tick() {

        forEach(IComponent::tick, (c, e) -> {
            container.spawnMagicSmoke(c.getPos());
            return new HandledCircuitException(e);
        });
    }

    public void tickEnd() {

        nonInteractable.clear();
    }

    public boolean isEmpty() {

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    for (int s = 0; s < 7; s++) {
                        IComponent c = components[x][y][z][s];
                        if (c != null) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public void clear() {

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    for (int s = 0; s < 7; s++) {
                        components[x][y][z][s] = null;
                    }
                }
            }
        }
        container.onCleared();
        container.notifyNeighbors();
        crash.setLeft(EnumCircuitCrashStatus.NO_CRASH);
        crash.setRight(null);
    }

    public byte getOutput(EnumCircuitSide cside, EnumDyeColor color, boolean bundled) {

        AtomicInteger power = new AtomicInteger(0);
        forEachEdge(c -> {
            if (c.isOutput(EnumComponentSlot.BOTTOM, cside) && c.isStrongOutput(EnumComponentSlot.BOTTOM, cside)) {
                if (!bundled && c.getBundledConnection(EnumComponentSlot.BOTTOM, cside) == EnumConnectionType.NONE) {
                    for (EnumDyeColor col : EnumDyeColor.values()) {
                        power.set(Math.max(power.get(), c.getOutputSignal(EnumComponentSlot.BOTTOM, cside, col, false) & 0xFF));
                    }
                } else {
                    power.set(Math.max(power.get(), c.getOutputSignal(EnumComponentSlot.BOTTOM, cside, color, bundled) & 0xFF));
                }
            }
        }, cside, 0, 0, EnumComponentSlot.BOTTOM);
        return (byte) power.get();
    }

    @Override
    public void spawnMagicSmoke(BlockPos pos) {

        container.spawnMagicSmoke(pos);
    }

    @Override
    public void spawnStack(ItemStack stack) {

        container.spawnStack(stack);
    }

    @Override
    public float computeComplexity() {

        AtomicDouble complexity = new AtomicDouble(0);
        forEach(c -> complexity.addAndGet(c.getComplexity()));
        return complexity.floatValue();
    }

    public static int getSize(float complexity) {

        if (complexity <= 1) {
            return 1;
        } else if (complexity <= 4) {
            return 2;
        } else if (complexity <= 9) {
            return 3;
        }
        return -1;
    }

    public void onCrash(Throwable throwable) {

        if (crash.getLeft() == EnumCircuitCrashStatus.NO_CRASH) {
            crash.setLeft(EnumCircuitCrashStatus.UPLOADING);
            new Thread(() -> {
                Map<String, String> data = new HashMap<>();

                String crashlog = ExceptionUtils.getStackTrace(throwable);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    NBTTagCompound tag = new NBTTagCompound();
                    writeToNBT(tag);
                    CompressedStreamTools.writeCompressed(tag, baos);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save circuit!", e);
                }
                String blueprint = new String(Base64.getEncoder().encode(baos.toByteArray()));

                data.put("crashlog", crashlog);
                data.put("blueprint", blueprint);

                try {
                    URL url = GistPublisher.publish(data);
                    crash.setLeft(EnumCircuitCrashStatus.UPLOADED);
                    crash.setRight(url.toString());
                } catch (Exception e) {
                    crash.setLeft(EnumCircuitCrashStatus.UPLOAD_ERROR);
                    File crashDir = new File("./crash-reports/scm");
                    crashDir.mkdirs();
                    crashDir.mkdirs();
                    String time = new Date().toString().replace(" ", "_").replace(":", ".");

                    try {
                        File logFile = new File(crashDir, time + ".log");
                        logFile.createNewFile();
                        PrintWriter logWriter = new PrintWriter(logFile);
                        throwable.printStackTrace(logWriter);
                        logWriter.close();

                        File schematicFile = new File(crashDir, time + ".scm");
                        schematicFile.createNewFile();
                        PrintWriter schematicWriter = new PrintWriter(schematicFile);
                        schematicWriter.write(blueprint);
                        schematicWriter.close();

                        crash.setRight(time);
                    } catch (Exception ex) {
                        SCM.log.catching(ex);
                    }
                }
            }).start();
        }
    }

    public Pair<EnumCircuitCrashStatus, String> getCrash() {

        return Pair.of(crash.getLeft(), crash.getRight());
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        if (name == null) {
            this.name = "";
        } else {
            this.name = name.trim();
        }
        container.markDirty();
        container.sendUpdatePacket();
    }

    public boolean canInteractWith(IComponent component) {

        return !nonInteractable.contains(component);
    }

}