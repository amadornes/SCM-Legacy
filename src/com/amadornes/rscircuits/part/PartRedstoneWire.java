package com.amadornes.rscircuits.part;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.util.ProjectionHelper;
import com.amadornes.rscircuits.util.UnlistedPropertyDyeColor;

import mcmultipart.MCMultiPartMod;
import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.IRedstonePart;
import mcmultipart.multipart.IRedstonePart.ISlottedRedstonePart;
import mcmultipart.multipart.ISlottedPart;
import mcmultipart.multipart.Multipart;
import mcmultipart.multipart.MultipartHelper;
import mcmultipart.multipart.MultipartRedstoneHelper;
import mcmultipart.multipart.PartSlot;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;

public class PartRedstoneWire extends Multipart implements ISlottedPart, ISlottedRedstonePart {

    public static final IProperty<Boolean> FRONT = PropertyBool.create("front");
    public static final IProperty<Boolean> BACK = PropertyBool.create("back");
    public static final IProperty<Boolean> LEFT = PropertyBool.create("left");
    public static final IProperty<Boolean> RIGHT = PropertyBool.create("right");
    public static final IUnlistedProperty<EnumFacing> FACE = Properties.toUnlisted(PropertyEnum.create("face", EnumFacing.class));
    public static final IUnlistedProperty<EnumDyeColor> COLOR = new UnlistedPropertyDyeColor();
    public static final IUnlistedProperty<Integer> POWER = Properties.toUnlisted(PropertyInteger.create("power", 0, 255));

    public static final AxisAlignedBB[][][] BOXES = new AxisAlignedBB[2][6][5];

    static {
        AxisAlignedBB[] uninsulatedBoxes = genBoxes(1 / 16D, 2 / 16D);
        AxisAlignedBB[] insulatedBoxes = genBoxes(2 / 16D, 3 / 16D);
        for (EnumFacing f : EnumFacing.VALUES) {
            BOXES[0][f.ordinal()] = ProjectionHelper.rotateFaces(uninsulatedBoxes, f);
            BOXES[1][f.ordinal()] = ProjectionHelper.rotateFaces(insulatedBoxes, f);
        }
    }

    private static AxisAlignedBB[] genBoxes(double r, double h) {

        AxisAlignedBB[] boxes = new AxisAlignedBB[5];
        boxes[0] = new AxisAlignedBB(0, 0, 0.5 - r, 0.5 - r, h, 0.5 + r);
        boxes[1] = new AxisAlignedBB(0.5 - r, 0, 0, 0.5 + r, h, 0.5 - r);
        boxes[2] = new AxisAlignedBB(0.5 + r, 0, 0.5 - r, 1, h, 0.5 + r);
        boxes[3] = new AxisAlignedBB(0.5 - r, 0, 0.5 + r, 0.5 + r, h, 1);
        boxes[4] = new AxisAlignedBB(0.5 - r, 0, 0.5 - r, 0.5 + r, h, 0.5 + r);
        return boxes;
    }

    private EnumFacing face;
    private EnumDyeColor color;
    private byte power = 0, prevInput = 0;
    private int[] connections = new int[4];

    public PartRedstoneWire(EnumFacing face, EnumDyeColor color) {

        this.face = face;
        this.color = color;
    }

    public PartRedstoneWire() {

    }

    // ISlottedPart

    @Override
    public EnumSet<PartSlot> getSlotMask() {

        return EnumSet.of(PartSlot.getFaceSlot(face));
    }

    // TODO: IRedstoneWire

    private byte calculateInput() {

        int power = 0;
        int i = 0;
        for (EnumFacing f : EnumFacing.VALUES) {
            if (f == face.getOpposite()) {
                continue;
            } else if (f == face) {
                // power = Math.max(power, getWorld().getRedstonePower(getPos().offset(f), f) * 17);
            } else {
                int con = connections[i++];
                if (con == 1) {
                    IBlockState state = getWorld().getBlockState(getPos().offset(f));
                    if (state.getBlock() == Blocks.REDSTONE_WIRE) {
                        power = Math.max(power, state.getValue(BlockRedstoneWire.POWER));
                    } else {
                        power = Math.max(power, getWorld().getRedstonePower(getPos().offset(f), f) * 17);
                    }
                } else if (con == 2) {
                    IMultipart part = getContainer().getPartInSlot(PartSlot.getFaceSlot(face));
                    if (!(part instanceof PartRedstoneWire) && part instanceof IRedstonePart) {
                        power = Math.max(power, ((IRedstonePart) part).getWeakSignal(face) * 17);
                    }
                } else if (con == 3) {
                    IMultipartContainer imc = MultipartHelper.getPartContainer(getWorld(), getPos().offset(f));
                    if (imc != null) {
                        power = Math.max(power, MultipartRedstoneHelper.getWeakSignal(imc, f.getOpposite(), face) * 17);
                    }
                }
            }
        }
        return (byte) power;
    }

    // ISlottedRedstonePart

    @Override
    public boolean canConnectRedstone(EnumFacing side) {

        if (face == EnumFacing.DOWN)
            return side != face && side != face.getOpposite();
        return false;
    }

    @Override
    public int getWeakSignal(EnumFacing side) {

        return outputPower && canConnectRedstone(side) ? (power & 0xFF) / 17 : 0;
    }

    @Override
    public int getStrongSignal(EnumFacing side) {

        return outputPower && side == face ? (power & 0xFF) / 17 : 0;
    }

    // Multipart

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> list) {

        AxisAlignedBB[] boxes = BOXES[color == null ? 0 : 1][face.ordinal()];
        list.add(boxes[4]);
        switch (face) {
        case DOWN:
            if (connections[0] != 0)
                list.add(boxes[1]);
            if (connections[1] != 0)
                list.add(boxes[3]);
            if (connections[2] != 0)
                list.add(boxes[0]);
            if (connections[3] != 0)
                list.add(boxes[2]);
            break;
        case UP:
            break;
        case NORTH:
            break;
        case SOUTH:
            break;
        case WEST:
            break;
        case EAST:
            break;
        }
    }

    @Override
    public BlockStateContainer createBlockState() {

        return new BlockStateContainer.Builder(MCMultiPartMod.multipart).add(FRONT, BACK, LEFT, RIGHT).add(FACE, COLOR, POWER).build();
    }

    @Override
    public IBlockState getActualState(IBlockState state) {

        switch (face) {
        case DOWN:
            state = state.withProperty(FRONT, connections[2] != 0);// connections[2] != 0);
            state = state.withProperty(BACK, connections[3] != 0);// connections[1] != 0);
            state = state.withProperty(RIGHT, connections[0] != 0);
            state = state.withProperty(LEFT, connections[1] != 0);// connections[3] != 0);
            break;
        case UP:
            break;
        case NORTH:
            break;
        case SOUTH:
            break;
        case WEST:
            break;
        case EAST:
            break;
        }
        return state;
    }

    @Override
    public IBlockState getExtendedState(IBlockState state) {

        return ((IExtendedBlockState) state).withProperty(FACE, face).withProperty(COLOR, color).withProperty(POWER, power & 0xFF);
    }

    @Override
    public void onAdded() {

        onUpdate(true);
    }

    @Override
    public void onNeighborBlockChange(Block block) {

        if (!getWorld().isSideSolid(getPos().offset(face), face.getOpposite())) {
            harvest(null, null);
            return;
        }

        onUpdate(false);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag.setInteger("face", face.ordinal());
        tag.setInteger("color", color == null ? -1 : color.ordinal());
        tag.setByte("power", power);
        tag.setIntArray("connections", connections);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        face = EnumFacing.getFront(tag.getInteger("face"));
        color = tag.getInteger("color") == -1 ? null : EnumDyeColor.values()[tag.getInteger("color")];
        power = tag.getByte("power");
        if (tag.hasKey("connections")) {
            System.arraycopy(tag.getIntArray("connections"), 0, connections, 0, 4);
        }
    }

    @Override
    public void writeUpdatePacket(PacketBuffer buf) {

        buf.writeEnumValue(face);
        buf.writeInt(color == null ? -1 : color.ordinal());
        buf.writeByte(power);
        buf.writeVarIntArray(connections);
    }

    @Override
    public void readUpdatePacket(PacketBuffer buf) {

        face = buf.readEnumValue(EnumFacing.class);
        int c = buf.readInt();
        color = c == -1 ? null : EnumDyeColor.values()[c];
        power = buf.readByte();
        System.arraycopy(buf.readVarIntArray(), 0, connections, 0, 4);
    }

    // Internal

    @SuppressWarnings("unused")
    private boolean isConnected(EnumFacing face) {

        return connections[face.getHorizontalIndex()] != 0;
    }

    private boolean ignoreUpdates = false;
    private boolean outputPower = true;

    private void onUpdate(boolean joinWorld) {

        if (ignoreUpdates) {
            return;
        }

        boolean changeCon = false;
        int i = 0;
        for (EnumFacing f : EnumFacing.VALUES) {
            if (f == face || f == face.getOpposite()) {
                continue;
            }
            int con = connections[i];
            connections[i] = getConnectionType(f);
            if (con != connections[i++]) {
                changeCon = true;
            }
        }
        sendUpdatePacket();

        byte input = 0;
        if (!changeCon) {
            Set<PartRedstoneWire> wires = new HashSet<PartRedstoneWire>();
            propagate(wires, 1);
            wires.forEach(w -> w.outputPower = false);
            input = calculateInput();
            wires.forEach(w -> w.outputPower = true);
            wires.clear();
        }
        if (joinWorld || changeCon || prevInput != input) {
            prevInput = input;
            propagate();
        }
    }

    private void propagate() {

        Set<PartRedstoneWire> wires = new HashSet<PartRedstoneWire>();
        propagate(wires, 64);
        int power = 0x00;
        for (PartRedstoneWire wire : wires) {
            wire.outputPower = false;
        }
        for (PartRedstoneWire wire : wires) {
            byte in = wire.calculateInput();
            wire.prevInput = in;
            power = Math.max(power, in & 0xFF);
        }
        for (PartRedstoneWire wire : wires) {
            wire.outputPower = true;
            wire.ignoreUpdates = true;
        }
        for (PartRedstoneWire wire : wires) {
            wire.notifyNonWires((byte) power);
        }
        for (PartRedstoneWire wire : wires) {
            wire.ignoreUpdates = false;
        }
    }

    private void propagate(Set<PartRedstoneWire> wires, int maxLen) {

        if (wires.add(this) && maxLen >= 1) {
            int i = 0;
            for (EnumFacing f : EnumFacing.VALUES) {
                if (f == face || f == face.getOpposite()) {
                    continue;
                }
                int con = connections[i++];
                if (con == 3) {
                    IMultipartContainer c = MultipartHelper.getPartContainer(getWorld(), getPos().offset(f));
                    if (c != null) {
                        IMultipart p = c.getPartInSlot(PartSlot.getFaceSlot(face));
                        if (p != null && p instanceof PartRedstoneWire) {
                            ((PartRedstoneWire) p).propagate(wires, maxLen - 1);
                        }
                    }
                }
            }
        }
    }

    private void notifyNonWires(byte power) {

        this.power = power;
        getWorld().notifyNeighborsOfStateChange(getPos(), MCMultiPartMod.multipart);
    }

    private int getConnectionType(EnumFacing face) {

        IMultipartContainer c = MultipartHelper.getPartContainer(getWorld(), getPos().offset(face));
        IMultipart part = getContainer().getPartInSlot(PartSlot.getFaceSlot(face));
        if (part != null && part instanceof IRedstonePart) {
            return ((IRedstonePart) part).canConnectRedstone(this.face) ? 2 : 0;
        } else if (c != null) {
            return MultipartRedstoneHelper.canConnectRedstone(c, face.getOpposite(), this.face) ? 3 : 0;
        } else {
            IBlockState state = getWorld().getBlockState(getPos().offset(face));
            return state.getBlock().canConnectRedstone(state, getWorld(), getPos().offset(face), face) ? 1 : 0;
        }
    }

    @Override
    public float getHardness(PartMOP hit) {

        return 0.25F;
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(new ItemStack(SCMItems.redwire));
    }

    @Override
    public ItemStack getPickBlock(EntityPlayer player, PartMOP hit) {

        return new ItemStack(SCMItems.redwire);
    }

}
