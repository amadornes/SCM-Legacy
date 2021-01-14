package com.amadornes.rscircuits.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3d;

public class PacketSpawnMagicSmoke extends Packet<PacketSpawnMagicSmoke> {

    private Vec3d pos;

    public PacketSpawnMagicSmoke(Vec3d pos) {
        this.pos = pos;
    }

    public PacketSpawnMagicSmoke() {
    }

    @Override
    public void handleClientSide(EntityPlayer player) {

        player.worldObj.spawnParticle(EnumParticleTypes.REDSTONE, pos.xCoord, pos.yCoord, pos.zCoord, -0.2, 0.8, 0.8);
    }

    @Override
    public void handleServerSide(EntityPlayer player) {

    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeDouble(pos.xCoord).writeDouble(pos.yCoord).writeDouble(pos.zCoord);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

}
