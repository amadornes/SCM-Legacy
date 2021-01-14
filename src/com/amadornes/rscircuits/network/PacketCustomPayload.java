package com.amadornes.rscircuits.network;

import java.util.UUID;

import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.part.PartCircuit;

import io.netty.buffer.Unpooled;
import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.MultipartHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketCustomPayload extends LocatedPacket<PacketCustomPayload> {

    private UUID partID;
    private BlockPos pos;
    private EnumComponentSlot slot;
    private byte[] data;

    public PacketCustomPayload(PartCircuit circuit, BlockPos pos, EnumComponentSlot slot, byte[] data) {

        super(circuit.getPos());
        this.partID = circuit.getContainer().getPartID(circuit);
        this.pos = pos;
        this.slot = slot;
        this.data = data;
    }

    public PacketCustomPayload() {

    }

    @Override
    public void handleClientSide(EntityPlayer player) {

    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> {
            IMultipartContainer container = MultipartHelper.getPartContainer(player.worldObj, super.pos);
            if (container != null) {
                IMultipart part = container.getPartFromID(partID);
                if (part != null & part instanceof PartCircuit) {
                    ((PartCircuit) part).circuit.handleCustomPayload(pos, slot, Unpooled.copiedBuffer(data));
                }
            }
        });
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeUuid(partID);
        buf.writeBlockPos(pos);
        buf.writeEnumValue(slot);
        buf.writeByteArray(data);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        partID = buf.readUuid();
        pos = buf.readBlockPos();
        slot = buf.readEnumValue(EnumComponentSlot.class);
        data = buf.readByteArray();
    }

}
