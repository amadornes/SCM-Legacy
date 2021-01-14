package com.amadornes.rscircuits.component.misc;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.component.ComponentBaseInt;
import com.amadornes.rscircuits.component.SimpleFactory;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class ComponentPunchcardReader extends ComponentBaseInt {

    public static final ResourceLocation NAME = new ResourceLocation(SCM.MODID, "punchcard_reader");

    private boolean input = false;
    private int currentRow;
    private boolean[][] punchcard = new boolean[8][12];
    private boolean hasPunchcard = false;

    public ComponentPunchcardReader(ICircuit circuit) {

        super(circuit);
    }

    @Override
    public ResourceLocation getName() {

        return NAME;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_PUNCHCARD_READER;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public EnumSet<EnumComponentSlot> getSlots() {

        return EnumSet.allOf(EnumComponentSlot.class);
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return side.face.getAxis() != Axis.Y;
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return false;
    }

    @Override
    public EnumConnectionType getBundledConnection(EnumComponentSlot slot, EnumCircuitSide side) {

        return EnumConnectionType.ANODE;
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return hasPunchcard && color.getMetadata() < 8 && punchcard[color.getMetadata()][currentRow] ? (byte) 255 : 0;
    }

    @Override
    public void onAddedPost() {

        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            if (getInputAny(EnumComponentSlot.BOTTOM, s) != 0) {
                input = true;
                break;
            }
        }
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        boolean prevInput = input;
        input = (boolean) data;
        if (input && !prevInput) {
            currentRow++;
            if (currentRow >= 12) {
                currentRow = 0;
            }

            getCircuit().markDirty();
            getCircuit().notifyUpdate(getPos(), EnumComponentSlot.BOTTOM, EnumCircuitSide.HORIZONTALS);
            getCircuit().sendUpdate(getPos(), EnumComponentSlot.BOTTOM, false);
        }
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (!hasPunchcard) {
            return;
        }

        boolean newInput = false;
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            if (getInputAny(EnumComponentSlot.BOTTOM, s) != 0) {
                newInput = true;
                break;
            }
        }
        getCircuit().scheduleTick(this, 0, 0, newInput);
        getCircuit().markDirty();
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, Vec3d hit) {

        if (hasPunchcard) {
            if (!getCircuit().getWorld().isRemote) {
                ItemStack stack = new ItemStack(SCMItems.punchcard);
                NBTTagCompound tag = new NBTTagCompound();

                int[] punchcardData = new int[12];
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 12; j++) {
                        punchcardData[j] |= punchcard[i][j] ? 1 << i : 0;
                    }
                }
                tag.setIntArray("punchcard", punchcardData);

                stack.setTagCompound(tag);
                getCircuit().spawnStack(stack);

                hasPunchcard = false;

                getCircuit().markDirty();
                getCircuit().notifyUpdate(getPos(), EnumComponentSlot.BOTTOM, EnumCircuitSide.HORIZONTALS);
                getCircuit().sendUpdate(getPos(), EnumComponentSlot.BOTTOM, true);
            }
            return true;
        } else if (heldItem != null && heldItem.getItem() == SCMItems.punchcard) {
            if (!getCircuit().getWorld().isRemote) {
                if (heldItem.hasTagCompound() && heldItem.getTagCompound().hasKey("punchcard")) {
                    int[] data = heldItem.getTagCompound().getIntArray("punchcard");
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 12; j++) {
                            punchcard[i][j] = (data[j] & (1 << i)) != 0;
                        }
                    }
                } else {
                    for (int i = 0; i < 8; i++) {
                        Arrays.fill(punchcard[i], false);
                    }
                }
                heldItem.stackSize--;
                currentRow = 0;
                hasPunchcard = true;

                getCircuit().markDirty();
                getCircuit().notifyUpdate(getPos(), EnumComponentSlot.BOTTOM, EnumCircuitSide.HORIZONTALS);
                getCircuit().sendUpdate(getPos(), EnumComponentSlot.BOTTOM, true);
            }
            return true;
        }

        return false;
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, EnumResourceType.PUNCHCARD_READER.ordinal());
    }

    @Override
    public AxisAlignedBB getSelectionBox(AxisAlignedBB box) {

        return box;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag = super.writeToNBT(tag);
        tag.setBoolean("input", input);
        tag.setInteger("currentRow", currentRow);
        int[] punchcardData = new int[12];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 12; j++) {
                punchcardData[j] |= punchcard[i][j] ? 1 << i : 0;
            }
        }
        tag.setIntArray("punchcard", punchcardData);
        tag.setBoolean("hasPunchcard", hasPunchcard);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        input = tag.getBoolean("input");
        currentRow = tag.getInteger("currentRow");
        int[] punchcardData = tag.getIntArray("punchcard");
        if (punchcardData.length >= 12) {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 12; j++) {
                    punchcard[i][j] = (punchcardData[j] & (1 << i)) != 0;
                }
            }
        }
        hasPunchcard = tag.getBoolean("hasPunchcard");
    }

    @Override
    public void writeDescription(PacketBuffer buf) {

        super.writeDescription(buf);
        buf.writeBoolean(hasPunchcard);
    }

    @Override
    public void readDescription(PacketBuffer buf) {

        super.readDescription(buf);
        hasPunchcard = buf.readBoolean();
    }

    @Override
    public void serializePlacement(PacketBuffer buf) {

    }

    @Override
    public void deserializePlacement(PacketBuffer buf) {

    }

    @Override
    public NBTTagCompound serializeTickData(int type, Object data) {

        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("input", (boolean) data);
        return tag;
    }

    @Override
    public Object deserializeTickData(int type, NBTTagCompound tag) {

        return tag.getBoolean("input");
    }

    public static class Factory extends SimpleFactory<ComponentPunchcardReader> {

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/inv_scanner");
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == EnumResourceType.PUNCHCARD_READER.ordinal();
        }

        @Override
        public ComponentPunchcardReader instantiate(ICircuit circuit) {

            return new ComponentPunchcardReader(circuit);
        }

    }

}
