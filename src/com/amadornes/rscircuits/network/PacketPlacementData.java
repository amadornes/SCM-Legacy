package com.amadornes.rscircuits.network;

import java.util.HashMap;
import java.util.Map;

import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.api.component.IComponentFactory.EnumPlacementType;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.part.PartCircuit;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class PacketPlacementData extends Packet<PacketPlacementData> {

    private ResourceLocation factoryType;
    private BlockPos offset;
    private EnumFacing face;
    private byte[] data;
    private EnumPlacementType type;

    public PacketPlacementData(ResourceLocation factoryType, BlockPos offset, EnumFacing face, byte[] data, EnumPlacementType type) {

        this.factoryType = factoryType;
        this.offset = offset;
        this.face = face;
        this.data = data;
        this.type = type;
    }

    public PacketPlacementData() {

    }

    @Override
    public void handleClientSide(EntityPlayer player) {

    }

    @Override
    public void handleServerSide(EntityPlayer player) {

        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(() -> handle(player));
    }

    private <T> void handle(EntityPlayer player) {

        IComponentFactory<T> factory = ComponentRegistry.INSTANCE.getFactory(factoryType);
        Map<BlockPos, T> map = new HashMap<>();
        PartCircuit circuit = PartCircuit.getCircuitAt(player.worldObj, offset, face);
        if (circuit == null) {
            return;
        }
        factory.deserialize(new PacketBuffer(Unpooled.copiedBuffer(data)), map, p -> circuit.circuit.getCircuit(p), player);
        ItemStack stack = player.getHeldItemMainhand();
        if (stack != null) {
            map.forEach((pos, d) -> {
                if (stack.stackSize > 0 && factory.placeComponent(circuit.circuit, pos, d, type, map, false)
                        && !player.capabilities.isCreativeMode) {
                    stack.stackSize--;
                    if (stack.stackSize == 0) {
                        player.setHeldItem(EnumHand.MAIN_HAND, null);
                    }
                }
            });
        }
    }

    @Override
    public void toBytes(PacketBuffer buf) {

        buf.writeString(factoryType.toString());
        buf.writeBlockPos(offset);
        buf.writeEnumValue(face);
        buf.writeByteArray(data);
        buf.writeEnumValue(type);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {

        factoryType = new ResourceLocation(buf.readStringFromBuffer(128));
        offset = buf.readBlockPos();
        face = buf.readEnumValue(EnumFacing.class);
        data = buf.readByteArray();
        type = buf.readEnumValue(EnumPlacementType.class);
    }

}
