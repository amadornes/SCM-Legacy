package com.amadornes.rscircuits.api.component;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.api.signal.IConnectable;
import com.amadornes.rscircuits.api.signal.IConnection;
import com.amadornes.rscircuits.api.signal.IConnection.EnumRedstoneConnection;
import com.amadornes.rscircuits.api.signal.IConnectionType;
import com.amadornes.rscircuits.api.signal.ISignalSupplier;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;

@SuppressWarnings("deprecation")
public interface IComponent extends IConnectable<EnumCircuitSide> {

    public static final float COMPONENT_SIZE = 2 / 16F;

    public ResourceLocation getName();

    public float getComplexity();

    public boolean isDynamic();

    public IBlockState getActualState();

    public default IBlockState getExtendedState(IBlockState state) {

        return state;
    }

    public default float getSize() {

        return COMPONENT_SIZE;
    }

    public default Vec3d getOffset() {

        return null;
    }

    public ICircuit getCircuit();

    public BlockPos getPos();

    public void setPos(BlockPos pos);

    public EnumSet<EnumComponentSlot> getSlots();

    @Override
    public default <C extends IConnection> Optional<C> getConnection(IConnectionType<C, ?> type, EnumCircuitSide on) {

        return null;
    }

    @Deprecated
    public default boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return getConnection(IConnectionType.REDSTONE, side).map(EnumRedstoneConnection::isInput).orElse(false);
    }

    @Deprecated
    public default boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return getConnection(IConnectionType.REDSTONE, side).map(EnumRedstoneConnection::isOutput).orElse(false);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public default boolean isStrongOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return this instanceof ISignalSupplier
                && ((ISignalSupplier<EnumCircuitSide>) this).getSignal(IConnectionType.REDSTONE, side).isStrong();
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public default byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return this instanceof ISignalSupplier ? ((ISignalSupplier<EnumCircuitSide>) this).getSignal(IConnectionType.REDSTONE, side).get()
                : 0;
    }

    @Deprecated
    public default byte getOutputSignalAny(EnumComponentSlot slot, EnumCircuitSide side) {

        int power = 0;
        for (EnumDyeColor color : EnumDyeColor.values()) {
            power = Math.max(power, getOutputSignal(slot, side, color, false) & 0xFF);
        }
        return (byte) power;
    }

    @Deprecated
    public default EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return EnumConnectionType.NONE;
    }

    public default void tick() {

    }

    public default void onCircuitAdded() {

    }

    public default void onCircuitRemoved() {

    }

    public default void onAdded() {

    }

    public default void onAddedPost() {

    }

    public default void onRemoved() {

    }

    public default void onRemovedPost() {

    }

    public default void onScheduledTick(int type, Object data) {

    }

    public default void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

    }

    public default void onWorldChange() {

    }

    public default void onWorldTileChange() {

    }

    public default boolean canRotate(Rotation rotation) {

        return true;
    }

    public default void rotatePre(Rotation rotation) {

    }

    public default void rotatePost() {

    }

    public default void onLoaded() {

    }

    public default boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        return false;
    }

    public default boolean harvest(EntityPlayer player, Vec3d hitPos) {

        getCircuit().removeComponent(this);
        return true;
    }

    public default boolean isSideSolid(EnumCircuitSide side) {

        return false;
    }

    public void addSelectionBoxes(List<AxisAlignedBB> boxes);

    public default AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return box;
    }

    public ItemStack getPickedItem();

    public List<ItemStack> getDrops();

    public List<ItemStack> getPlacementItems();

    public default int getColorMultiplier(int index) {

        return -1;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag);

    public void readFromNBT(NBTTagCompound tag);

    public void writeDescription(PacketBuffer buf);

    public void readDescription(PacketBuffer buf);

    public default NBTTagCompound serializeTickData(int type, Object data) {

        return null;
    }

    public default Object deserializeTickData(int type, NBTTagCompound tag) {

        return null;
    }

    public default void handleCustomPayload(ByteBuf buf) {

    }

    public default void debug(EntityPlayer player) {

        player.addChatMessage(new TextComponentString("No debug data available"));
    }

}
