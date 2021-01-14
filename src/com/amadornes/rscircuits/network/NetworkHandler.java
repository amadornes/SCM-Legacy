package com.amadornes.rscircuits.network;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.SCM;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class NetworkHandler {

    public static final NetworkHandler instance = new NetworkHandler(SCM.MODID);

    public final SimpleNetworkWrapper wrapper;
    private int lastDiscriminator = 0;
    BiMap<Pair<Class<Packet<?>>, Side>, Integer> discriminatorMap = HashBiMap.create();

    public NetworkHandler(String modid) {

        wrapper = NetworkRegistry.INSTANCE.newSimpleChannel(modid);
    }

    public static void init() {

        instance.registerPacket(PacketPlacementData.class, Side.SERVER);
        instance.registerPacket(PacketCircuitUpdate.class, Side.CLIENT);
        instance.registerPacket(PacketCustomPayload.class, Side.SERVER);
        instance.registerPacket(PacketTrayScroll.class, Side.SERVER);
        instance.registerPacket(PacketColorPick.class, Side.SERVER);
        instance.registerPacket(PacketSpawnMagicSmoke.class, Side.CLIENT);
        instance.registerPacket(PacketSplit.class, Side.CLIENT);
        instance.registerPacket(PacketSplit.class, Side.SERVER);
        instance.registerPacket(PacketPunchcard.class, Side.SERVER);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void registerPacket(Class packetHandler, Class packetType, Side side) {

        wrapper.registerMessage(packetHandler, packetType, lastDiscriminator, side);
        discriminatorMap.put(Pair.of(packetType, side), lastDiscriminator++);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void registerPacket(Class packetType, Side side) {

        wrapper.registerMessage(packetType, packetType, lastDiscriminator, side);
        discriminatorMap.put(Pair.of(packetType, side), lastDiscriminator++);
    }

    public void sendToAll(Packet<?> packet) {

        wrapper.sendToAll(packet);

        List<PacketSplit> split = PacketSplit.split(packet, Side.CLIENT);
        if (split != null) {
            split.forEach(wrapper::sendToAll);
        } else {
            wrapper.sendToAll(packet);
        }
    }

    public void sendTo(Packet<?> packet, EntityPlayerMP player) {

        List<PacketSplit> split = PacketSplit.split(packet, Side.CLIENT);
        if (split != null) {
            split.forEach(p -> wrapper.sendTo(p, player));
        } else {
            wrapper.sendTo(packet, player);
        }
    }

    public <T extends LocatedPacket<T>> void sendToAllAround(T packet, World world, double range) {

        sendToAllAround(packet, packet.getTargetPoint(world, range));
    }

    public <T extends LocatedPacket<T>> void sendToAllAround(T packet, World world) {

        sendToAllAround(packet, packet.getTargetPoint(world, 64));
    }

    public void sendToAllAround(Packet<?> packet, NetworkRegistry.TargetPoint point) {

        List<PacketSplit> split = PacketSplit.split(packet, Side.CLIENT);
        if (split != null) {
            split.forEach(p -> wrapper.sendToAllAround(p, point));
        } else {
            wrapper.sendToAllAround(packet, point);
        }
    }

    public void sendToDimension(Packet<?> packet, int dimensionId) {

        List<PacketSplit> split = PacketSplit.split(packet, Side.CLIENT);
        if (split != null) {
            split.forEach(p -> wrapper.sendToDimension(p, dimensionId));
        } else {
            wrapper.sendToDimension(packet, dimensionId);
        }
    }

    public void sendToServer(Packet<?> packet) {

        List<PacketSplit> split = PacketSplit.split(packet, Side.SERVER);
        if (split != null) {
            split.forEach(wrapper::sendToServer);
        } else {
            wrapper.sendToServer(packet);
        }
    }

}
