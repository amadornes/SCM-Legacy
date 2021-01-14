package com.amadornes.rscircuits.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

public class PacketSplit extends Packet<PacketSplit> {

    private static final ThreadLocal<Cache<UUID, Triple<Integer, Integer, Map<Integer, byte[]>>>> packetCache = //
            ThreadLocal.withInitial(() -> CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build());

    private UUID id;
    private int discriminator;
    private int subID;
    private int totalPackets;
    private byte[] data;

    public PacketSplit(UUID id, int discriminator, int subID, int totalPackets, byte[] data) {

        this.id = id;
        this.discriminator = discriminator;
        this.subID = subID;
        this.totalPackets = totalPackets;
        this.data = data;
    }

    public PacketSplit() {
    }

    @Override
    public void handleClientSide(EntityPlayer player) {

        handleSingle(player, Side.CLIENT);
    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        handleSingle(player, Side.SERVER);
    }

    private void handleSingle(EntityPlayer player, Side side) {

        try {
            Triple<Integer, Integer, Map<Integer, byte[]>> pair = packetCache.get().get(id,
                    () -> Triple.of(discriminator, totalPackets, new HashMap<>()));
            pair.getRight().put(subID, data);
            if (pair.getMiddle() == pair.getRight().size()) {
                joinAndHandle(pair.getRight(), pair.getLeft(), player, side);
            }
        } catch (ExecutionException e) {
            throw Throwables.propagate(e);
        }
    }

    private void joinAndHandle(Map<Integer, byte[]> value, int discriminator, EntityPlayer player, Side side) {

        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < value.size(); i++) {
            buf.writeBytes(value.get(i));
        }
        try {
            Packet<?> packet = NetworkHandler.instance.discriminatorMap.inverse().get(discriminator).getKey().newInstance();
            packet.fromBytes(buf);
            if (side == Side.CLIENT) {
                packet.handleClientSide(player);
            } else {
                packet.handleServerSide(player);
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeUuid(id);
        buf.writeInt(discriminator);
        buf.writeInt(subID);
        buf.writeInt(totalPackets);
        buf.writeByteArray(data);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        id = buf.readUuid();
        discriminator = buf.readInt();
        subID = buf.readInt();
        totalPackets = buf.readInt();
        data = buf.readByteArray();
    }

    public static List<PacketSplit> split(Packet<?> packet, Side side) {

        UUID id = UUID.randomUUID();
        int discriminator = NetworkHandler.instance.discriminatorMap.get(Pair.of(packet.getClass(), side));
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        packet.toBytes(buf);
        byte[] allData = buf.array();
        int lengthLeft = allData.length;
        if (lengthLeft < 30000) {
            return null;
        }
        List<byte[]> dataArrays = new LinkedList<>();
        while (lengthLeft > 0) {
            byte[] data = new byte[Math.min(lengthLeft, 30000)];
            System.arraycopy(allData, allData.length - lengthLeft, data, 0, data.length);
            dataArrays.add(data);
            lengthLeft -= data.length;
        }
        List<PacketSplit> packets = new ArrayList<>();
        int i = 0;
        for (byte[] data : dataArrays) {
            packets.add(new PacketSplit(id, discriminator, i++, dataArrays.size(), data));
        }
        return packets;
    }

}
