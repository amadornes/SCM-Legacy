package com.amadornes.rscircuits.component.analogue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumCircuitUpdate;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;
import com.amadornes.rscircuits.component.ComponentBaseFaceRotatable;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.util.ComponentReference;

import mcmultipart.MCMultiPartMod;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class ComponentMath extends ComponentBaseFaceRotatable {

    private final Factory factory;
    private byte[] input = new byte[5];
    private byte output;

    public ComponentMath(ICircuit circuit, int rotation, Factory factory) {

        super(circuit, EnumComponentSlot.BOTTOM, rotation);
        this.factory = factory;
    }

    public ComponentMath(ICircuit circuit, Factory factory) {

        super(circuit, EnumComponentSlot.BOTTOM);
        this.factory = factory;
    }

    @Override
    public ResourceLocation getName() {

        return factory.name;
    }

    @Override
    public float getComplexity() {

        return ComponentReference.COMPLEXITY_LEVER;
    }

    @Override
    public boolean isDynamic() {

        return false;
    }

    @Override
    public IBlockState getActualState() {

        return super.getActualState().withProperty(ROTATION, rotation);
    }

    @Override
    public boolean isInput(EnumComponentSlot slot, EnumCircuitSide side) {

        return slot == this.slot && factory.type.isInput.test(side, rotation);
    }

    @Override
    public boolean isOutput(EnumComponentSlot slot, EnumCircuitSide side) {

        return slot == this.slot && factory.type.isOutput.test(side, rotation);
    }

    @Override
    public byte getOutputSignal(EnumComponentSlot slot, EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        return output;
    }

    @Override
    public void onAddedPost() {

        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            onChangeInput(s);
        }
        onChange();
    }

    @Override
    public void rotatePost() {

        super.rotatePost();
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            onChangeInput(s);
        }
        onChange();
    }

    @Override
    public void onScheduledTick(int type, Object data) {

        byte prev = output;
        output = (byte) data;
        if (prev != output) {
            getCircuit().notifyUpdateAll(getPos(), EnumComponentSlot.BOTTOM);
            getCircuit().sendUpdate(getPos(), slot, false);
            getCircuit().markDirty();
        }
    }

    @Override
    public void onNeighborChange(EnumCircuitSide side, EnumComponentSlot slot, IComponent component, EnumCircuitUpdate updateType) {

        if (side == EnumCircuitSide.BOTTOM && dropIfNeeded()) {
            return;
        }

        if (side != EnumCircuitSide.TOP) {
            onChangeInput(side);
            onChange();
        }
    }

    private boolean onChangeInput(EnumCircuitSide side) {

        byte in = getInputAny(this.slot, side);
        int i = side == EnumCircuitSide.BOTTOM ? 4 : side.face.getHorizontalIndex();
        if (in == input[i]) {
            return false;
        }
        input[i] = in;
        return true;
    }

    private void onChange() {

        Map<EnumCircuitSide, Pair<EnumConnectionType, Byte>> connections = new EnumMap<>(EnumCircuitSide.class);
        for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
            IComponent neighbor = getNeighbor(this.slot, s);
            if (neighbor != null && neighbor instanceof IRedstoneConductor) {
                connections.put(s,
                        Pair.of(getConnectionState((IRedstoneConductor) neighbor, s.getOpposite()), input[s.face.getHorizontalIndex()]));
            }
        }

        byte out = factory.type.operation.operate(input[EnumCircuitSide.LEFT.face.getHorizontalIndex()],
                input[EnumCircuitSide.RIGHT.face.getHorizontalIndex()], input[EnumCircuitSide.FRONT.face.getHorizontalIndex()],
                input[EnumCircuitSide.BACK.face.getHorizontalIndex()], input[4], connections, rotation);

        getCircuit().scheduleTick(this, 1, 0, out);
        getCircuit().markDirty();
    }

    private EnumConnectionType getConnectionState(IRedstoneConductor neighbor, EnumCircuitSide side) {

        return neighbor.getNeighbors()[side.ordinal()].getType();
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> boxes) {

        boxes.add(new AxisAlignedBB(0, 0, 0, 1, 4 / 16D, 1));
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getPickedItem());
    }

    @Override
    public ItemStack getPickedItem() {

        return new ItemStack(SCMItems.resource, 1, factory.type.res.ordinal());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        super.writeToNBT(tag);
        tag.setByteArray("in", input);
        tag.setByte("out", output);
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        super.readFromNBT(tag);
        if (tag.hasKey("io")) {
            byte[] io = tag.getByteArray("io");
            input[EnumCircuitSide.LEFT.face.getHorizontalIndex()] = io[0];
            input[EnumCircuitSide.RIGHT.face.getHorizontalIndex()] = io[1];
            input[EnumCircuitSide.FRONT.face.getHorizontalIndex()] = io[2];
            input[EnumCircuitSide.BACK.face.getHorizontalIndex()] = io[3];
            output = io[4];
        } else {
            byte[] in = tag.getByteArray("in");
            System.arraycopy(in, 0, input, 0, in.length);
            output = tag.getByte("out");
        }
    }

    public static class Factory extends RotatableFactory<ComponentMath> {

        private final ResourceLocation name;
        private final EnumMathComponent type;

        public Factory(EnumMathComponent type) {
            this.name = new ResourceLocation(SCM.MODID, type.name().toLowerCase());
            this.type = type;
        }

        @Override
        public BlockStateContainer createBlockState() {

            return new BlockStateContainer(MCMultiPartMod.multipart, ROTATION);
        }

        @Override
        public ResourceLocation getModelPath() {

            return new ResourceLocation(SCM.MODID, "component/" + name.getResourcePath());
        }

        @Override
        public boolean isValidPlacementStack(ItemStack stack, EntityPlayer player) {

            return stack.getItem() == SCMItems.resource && stack.getItemDamage() == type.res.ordinal();
        }

        @Override
        public ComponentMath getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
                EntityPlayer player, EnumPlacementType type, ComponentMath previousData, Map<BlockPos, ComponentMath> otherData,
                EnumInstantanceUse use) {

            if (previousData != null) {
                return previousData;
            }
            return new ComponentMath(circuit, (getRotation(player) + 3) % 4, this);
        }

        @Override
        public ComponentMath instantiate(ICircuit circuit) {

            return new ComponentMath(circuit, this);
        }

        public void register() {

            ComponentRegistry.INSTANCE.registerFactory(name, this);
        }

    }

    public static interface IMathOperation {

        public byte operate(byte left, byte right, byte front, byte back, byte bottom,
                Map<EnumCircuitSide, Pair<EnumConnectionType, Byte>> sides, int rotation);
    }

    public static enum EnumMathComponent implements IStringSerializable {
        ADDER(EnumResourceType.ADDER, (l, r, f, b, b_, sides, rot) -> {
            return (byte) Math.min(255, (l & 0xFF) + (r & 0xFF) + (f & 0xFF) + (b & 0xFF) + (b_ & 0xFF));
        }, (s, r) -> true, (s, r) -> true),
        SUBTRACTOR(EnumResourceType.SUBTRACTOR, (l, r, f, b, b_, sides, rot) -> {
            Pair<Integer, Integer> in = getInputs(l, r, f, b, b_, rot);
            return (byte) Math.max(0, in.getLeft() - in.getRight());
        }, (s, r) -> true, ComponentMath::isNotLeft),
        MULTIPLIER(EnumResourceType.MULTIPLIER, (l, r, f, b, b_, sides, rot) -> {
            int total = 1, amt = 0;
            for (EnumCircuitSide s : EnumCircuitSide.HORIZONTALS) {
                Pair<EnumConnectionType, Byte> pair = sides.get(s);
                if (pair != null && pair.getKey().isOutput) {
                    total *= pair.getValue() & 0xFF;
                    amt++;
                }
            }
            if (amt == 0) {
                return 0;
            }
            return (byte) Math.min(total, 255);
        }, (s, r) -> true, (s, r) -> true),
        DIVIDER(EnumResourceType.DIVIDER, (l, r, f, b, b_, sides, rot) -> {
            Pair<Integer, Integer> in = getInputs(l, r, f, b, b_, rot);
            if (in.getRight() == 0) {
                return 0;
            }
            return (byte) Math.max(0, (int) Math.floor(in.getLeft() / (float) in.getRight()));
        }, (s, r) -> true, ComponentMath::isNotLeft);

        private final EnumResourceType res;
        private final IMathOperation operation;
        private final BiPredicate<EnumCircuitSide, Integer> isInput;
        private final BiPredicate<EnumCircuitSide, Integer> isOutput;

        private EnumMathComponent(EnumResourceType res, IMathOperation operation, BiPredicate<EnumCircuitSide, Integer> isInput,
                BiPredicate<EnumCircuitSide, Integer> isOutput) {
            this.res = res;
            this.operation = operation;
            this.isInput = isInput;
            this.isOutput = isOutput;
        }

        @Override
        public String getName() {

            return name().toLowerCase();
        }

    }

    private static boolean isLeft(EnumCircuitSide s, Integer r) {

        return getLeft(r) == s;
    }

    private static boolean isNotLeft(EnumCircuitSide s, Integer r) {

        return !isLeft(s, r);
    }

    private static EnumCircuitSide getLeft(Integer r) {

        return r == 0 ? EnumCircuitSide.RIGHT : r == 1 ? EnumCircuitSide.BACK : r == 2 ? EnumCircuitSide.LEFT : EnumCircuitSide.FRONT;
    }

    private static Pair<Integer, Integer> getInputs(byte l, byte r, byte f, byte b, byte b_, int rot) {

        EnumCircuitSide left = getLeft(rot);
        int in1 = 0, in2 = 0;
        if (left == EnumCircuitSide.LEFT) {
            in1 = l & 0xFF;
        } else {
            in2 = Math.max(in2, l & 0xFF);
        }
        if (left == EnumCircuitSide.RIGHT) {
            in1 = r & 0xFF;
        } else {
            in2 = Math.max(in2, r & 0xFF);
        }
        if (left == EnumCircuitSide.FRONT) {
            in1 = f & 0xFF;
        } else {
            in2 = Math.max(in2, f & 0xFF);
        }
        if (left == EnumCircuitSide.BACK) {
            in1 = b & 0xFF;
        } else {
            in2 = Math.max(in2, b & 0xFF);
        }
        in2 = Math.max(in2, b_ & 0xFF);
        return Pair.of(in1, in2);
    }

}
