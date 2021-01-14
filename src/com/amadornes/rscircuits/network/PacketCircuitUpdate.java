package com.amadornes.rscircuits.network;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.part.PartCircuit;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class PacketCircuitUpdate extends LocatedPacket<PacketCircuitUpdate> {

    private EnumFacing face;
    private byte[] data;

    public PacketCircuitUpdate(BlockPos pos, EnumFacing face, byte[] data) {
        super(pos);
        this.face = face;
        this.data = data;
    }

    public PacketCircuitUpdate() {

    }

    @Override
    public void handleClientSide(EntityPlayer player) {

        Minecraft.getMinecraft().addScheduledTask(() -> {
            PartCircuit c = PartCircuit.getCircuitAt(player.worldObj, pos, face);
            if (c != null) {
                SCM.proxy.handleUpdate(c.circuit, new PacketBuffer(Unpooled.copiedBuffer(data)));
            }
        });
    }

    @Override
    public void handleServerSide(EntityPlayer player) {

    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeEnumValue(face);
        buf.writeByteArray(data);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        face = buf.readEnumValue(EnumFacing.class);
        data = buf.readByteArray();
    }

}
