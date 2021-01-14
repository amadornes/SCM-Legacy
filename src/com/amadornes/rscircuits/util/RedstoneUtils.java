package com.amadornes.rscircuits.util;

import org.apache.commons.lang3.tuple.Pair;

import com.amadornes.rscircuits.api.IItemWrapper;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RedstoneUtils {

    private static final EnumFacing[][] CONVERT_CIRCUIT_FACING = { //
            { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST }, //
            { EnumFacing.UP, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST, EnumFacing.EAST }, //
            { EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN }, //
            { EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.WEST, EnumFacing.EAST, EnumFacing.UP, EnumFacing.DOWN }, //
            { EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN }, //
            { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP, EnumFacing.DOWN } //
    };

    private static final EnumCircuitSide[][] CONVERT_FACING_CIRCUIT = { //
            { EnumCircuitSide.BOTTOM, EnumCircuitSide.TOP, EnumCircuitSide.BACK, EnumCircuitSide.FRONT, EnumCircuitSide.RIGHT,
                    EnumCircuitSide.LEFT }, //
            { EnumCircuitSide.TOP, EnumCircuitSide.BOTTOM, EnumCircuitSide.FRONT, EnumCircuitSide.BACK, EnumCircuitSide.RIGHT,
                    EnumCircuitSide.LEFT }, //
            { EnumCircuitSide.LEFT, EnumCircuitSide.RIGHT, EnumCircuitSide.BOTTOM, EnumCircuitSide.TOP, EnumCircuitSide.FRONT,
                    EnumCircuitSide.BACK }, //
            { EnumCircuitSide.LEFT, EnumCircuitSide.RIGHT, EnumCircuitSide.TOP, EnumCircuitSide.BOTTOM, EnumCircuitSide.BACK,
                    EnumCircuitSide.FRONT }, //
            { EnumCircuitSide.LEFT, EnumCircuitSide.RIGHT, EnumCircuitSide.BACK, EnumCircuitSide.FRONT, EnumCircuitSide.BOTTOM,
                    EnumCircuitSide.TOP }, //
            { EnumCircuitSide.LEFT, EnumCircuitSide.RIGHT, EnumCircuitSide.FRONT, EnumCircuitSide.BACK, EnumCircuitSide.TOP,
                    EnumCircuitSide.BOTTOM }//
    };

    public static BlockPos limitPositionToBounds(BlockPos pos) {

        return new BlockPos(((pos.getX() % 8) + 8) % 8, ((pos.getY() % 5) + 5) % 5, ((pos.getZ() % 8) + 8) % 8);
    }

    public static Vec3d limitPositionToBounds(Vec3d pos) {

        return new Vec3d(((pos.xCoord % 8) + 8) % 8, ((pos.yCoord % 5) + 5) % 5, ((pos.zCoord % 8) + 8) % 8);
    }

    public static ItemStack unwrap(ItemStack stack) {

        if (stack != null && stack.getItem() instanceof IItemWrapper) {
            return ((IItemWrapper) stack.getItem()).getWrappedStack(stack);
        } else {
            return stack;
        }
    }

    public static BlockPos packCoords(BlockPos pos, EnumFacing face, BlockPos component) {

        Pair<BlockPos, BlockPos> p = correctCoords(pos, face, component);
        return new BlockPos(p.getKey().getX() * 8 + p.getValue().getX(), p.getKey().getY() * 8 + p.getValue().getY(),
                p.getKey().getZ() * 8 + p.getValue().getZ());
    }

    public static Pair<BlockPos, BlockPos> unpackCoords(BlockPos packed) {

        BlockPos pos = limitPositionToBounds(packed);
        BlockPos circPos = new BlockPos((packed.getX() - pos.getX()) / 8, (packed.getY() - pos.getY()) / 8,
                (packed.getZ() - pos.getZ()) / 8);
        return Pair.of(circPos, pos);
    }

    public static BlockPos correctOffset(BlockPos off, EnumFacing face) {

        switch (face) {
        case DOWN:
            return off;
        case UP:
            return new BlockPos(off.getX(), off.getY(), -off.getZ());
        case NORTH:
            return new BlockPos(-off.getZ(), -off.getX(), off.getY());
        case SOUTH:
            return new BlockPos(off.getZ(), -off.getX(), off.getY());
        case WEST:
            return new BlockPos(-off.getY(), -off.getX(), off.getZ());
        case EAST:
            return new BlockPos(-off.getY(), -off.getX(), -off.getZ());
        }
        return off;
    }

    public static Vec3d projectComponent(Vec3d pos, EnumFacing face, BlockPos compPos) {

        return project(pos, face).subtract(1 / 16D, 2 / 16D, 1 / 16D).scale(8).subtract(new Vec3d(compPos));
    }

    public static Vec3d project(Vec3d pos, EnumFacing face) {

        switch (face) {
        case DOWN:
            return pos;
        case UP:
            return new Vec3d(pos.xCoord, 1 - pos.yCoord, 1 - pos.zCoord);
        case NORTH:
            return new Vec3d(1 - pos.yCoord, pos.zCoord, 1 - pos.xCoord);
        case SOUTH:
            return new Vec3d(1 - pos.yCoord, 1 - pos.zCoord, pos.xCoord);
        case WEST:
            return new Vec3d(1 - pos.yCoord, pos.xCoord, pos.zCoord);
        case EAST:
            return new Vec3d(1 - pos.yCoord, 1 - pos.xCoord, 1 - pos.zCoord);
        }
        return pos;
    }

    public static Vec3d unproject(Vec3d pos, EnumFacing face) {

        switch (face) {
        case DOWN:
            return pos;
        case UP:
            return new Vec3d(pos.xCoord, 1 - pos.yCoord, 1 - pos.zCoord);
        case NORTH:
            return new Vec3d(1 - pos.zCoord, 1 - pos.xCoord, pos.yCoord);
        case SOUTH:
            return new Vec3d(pos.zCoord, 1 - pos.xCoord, 1 - pos.yCoord);
        case WEST:
            return new Vec3d(pos.yCoord, 1 - pos.xCoord, pos.zCoord);
        case EAST:
            return new Vec3d(1 - pos.yCoord, 1 - pos.xCoord, 1 - pos.zCoord);
        }
        return pos;
    }

    public static Pair<BlockPos, BlockPos> correctCoords(BlockPos pos, EnumFacing face, BlockPos component) {

        Pair<BlockPos, BlockPos> up = unpackCoords(component);
        BlockPos off = correctOffset(up.getKey(), face);
        return Pair.of(pos.add(off), up.getValue());
    }

    public static EnumFacing convert(EnumFacing face, EnumCircuitSide side) {

        return CONVERT_CIRCUIT_FACING[face.ordinal()][side.ordinal()];
    }

    public static EnumCircuitSide convert(EnumFacing face, EnumFacing side) {

        return CONVERT_FACING_CIRCUIT[face.ordinal()][side.ordinal()];
    }

}
