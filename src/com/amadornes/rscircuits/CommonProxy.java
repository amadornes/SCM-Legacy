package com.amadornes.rscircuits;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.network.NetworkHandler;
import com.amadornes.rscircuits.network.PacketCircuitUpdate;
import com.amadornes.rscircuits.util.BoolFunction;
import com.amadornes.rscircuits.util.IntBoolFunction;
import com.google.common.base.Supplier;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

public class CommonProxy {

    public static final Set<Circuit> updatedCircuits = new HashSet<Circuit>();

    public void preInit() {

    }

    public EntityPlayer getPlayer() {

        return null;
    }

    public RayTraceResult getHit() {

        return null;
    }

    public void playPlaceSound(BlockPos pos) {

    }

    @SubscribeEvent
    public <T> void serverBreakPrevention(PlayerInteractEvent.LeftClickBlock event) {

        ItemStack stack = event.getItemStack();
        if (event.getWorld() != null && !event.getWorld().isRemote && stack != null && stack.getItem() == SCMItems.screwdriver) {
            event.setCanceled(true);
            event.setUseItem(Result.DENY);
            event.setUseBlock(Result.DENY);
            event.getWorld().getBlockState(event.getPos()).getBlock().onBlockClicked(event.getWorld(), event.getPos(),
                    event.getEntityPlayer());
        }
    }

    @SubscribeEvent
    public <T> void serverTick(ServerTickEvent event) {

        if (event.phase == Phase.END) {
            synchronized (updatedCircuits) {
                updatedCircuits.forEach(circuit -> {
                    if (circuit.getWorld() == null || circuit.getPos() == null) {
                        return;
                    }
                    PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
                    Map<Pair<BlockPos, EnumComponentSlot>, Boolean> updates = circuit.getUpdates();
                    buf.writeInt(updates.size());
                    updates.forEach((k, v) -> {
                        buf.writeBlockPos(k.getKey());
                        buf.writeEnumValue(k.getValue());
                        buf.writeBoolean(v);
                        IComponent comp = circuit.getComponent(k.getKey(), k.getValue());
                        if (comp == null) {
                            buf.writeBoolean(false);
                        } else {
                            buf.writeBoolean(true);
                            buf.writeString(comp.getName().toString());
                            comp.writeDescription(buf);
                        }
                    });
                    updates.clear();
                    NetworkHandler.instance.sendToAllAround(new PacketCircuitUpdate(circuit.getPos(), circuit.getFace(), buf.array()),
                            circuit.getWorld());
                });
                updatedCircuits.clear();
            }
        }

    }

    public void handleUpdate(Circuit circuit, PacketBuffer buf) {

        boolean reRender = false;
        int count = buf.readInt();
        Set<IComponent> newComponents = new HashSet<IComponent>();
        for (int i = 0; i < count; i++) {
            BlockPos pos = buf.readBlockPos();
            EnumComponentSlot slot = buf.readEnumValue(EnumComponentSlot.class);
            reRender |= buf.readBoolean();
            if (buf.readBoolean()) {
                IComponent comp = circuit.components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()];
                ResourceLocation name = new ResourceLocation(buf.readStringFromBuffer(128));
                if (comp == null || !comp.getName().equals(name)) {
                    circuit.components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()] = comp = ComponentRegistry.INSTANCE
                            .getFactory(name).instantiate(circuit);
                    comp.setPos(pos);
                    comp.readDescription(buf);
                    IComponent comp_ = comp;
                    comp.getSlots().forEach(s -> circuit.components[pos.getX()][pos.getY()][pos.getZ()][s.ordinal()] = comp_);
                    newComponents.add(comp);
                } else {
                    comp.readDescription(buf);
                }
            } else {
                circuit.components[pos.getX()][pos.getY()][pos.getZ()][slot.ordinal()] = null;
            }
        }
        newComponents.forEach(IComponent::onLoaded);
        if (reRender && !circuit.isEncapsulated()) {
            circuit.getWorld().markBlockRangeForRenderUpdate(circuit.getPos(), circuit.getPos());
        }
    }

    public void displayTimerGui(IntBoolFunction<String> titleSupplier, BoolFunction<String> shortSupplier, Supplier<Integer> getter,
            Consumer<Integer> setter, int min, int max, double shiftMul) {

    }

}
