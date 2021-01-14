package com.amadornes.rscircuits.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.vecmath.Vector2d;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class ProjectionHelper {

    public static Vec3d project(EnumFacing faceHit, double hitX, double hitY, double hitZ) {

        double x = 0, y = 0, z = 0;

        switch (faceHit) {
        case DOWN:
            x = -hitX;
            y = -hitY;
            z = hitZ;
            break;
        case UP:
            x = hitX;
            y = hitY;
            z = hitZ;
            break;
        case NORTH:
            x = hitX;
            y = -hitZ;
            z = hitY;
            break;
        case SOUTH:
            x = hitX;
            y = hitZ;
            z = 1 - hitY;
            break;
        case WEST:
            x = hitY;
            y = -hitX;
            z = hitZ;
            break;
        case EAST:
            x = 1 - hitY;
            y = hitX;
            z = hitZ;
            break;
        default:
            break;
        }

        if (x < 0)
            x = 1 + x;
        if (y < 0)
            y = 1 + y;
        if (z < 0)
            z = 1 + z;

        return new Vec3d(x, y, z);
    }

    public static int getPlacementRotation(EnumFacing faceHit, double hitX, double hitY, double hitZ) {

        return getPlacementRotation(project(faceHit, hitX, hitY, hitZ));
    }

    public static int getPlacementRotation(Vec3d proj) {

        Vector2d projected = new Vector2d(proj.xCoord, proj.zCoord);
        projected.x -= 0.5;
        projected.y -= 0.5;

        if (projected.y > 0 && projected.y > Math.abs(projected.x)) {
            return 0;
        } else if (projected.x > 0 && projected.x > Math.abs(projected.y)) {
            return 3;
        } else if (projected.y < 0 && Math.abs(projected.y) > Math.abs(projected.x)) {
            return 2;
        } else {
            return 1;
        }
    }

    public static AxisAlignedBB rotateFace(AxisAlignedBB box, EnumFacing facing) {

        if (facing == EnumFacing.DOWN) {
            return box;
        }
        Vec3d min = RedstoneUtils.unproject(new Vec3d(box.minX, box.minY, box.minZ), facing);
        Vec3d max = RedstoneUtils.unproject(new Vec3d(box.maxX, box.maxY, box.maxZ), facing);
        return new AxisAlignedBB(min.xCoord, min.yCoord, min.zCoord, max.xCoord, max.yCoord, max.zCoord);
    }

    public static AxisAlignedBB[] rotateFaces(AxisAlignedBB[] boxes, EnumFacing facing) {

        return rotateFaces(Arrays.asList(boxes), facing).toArray(new AxisAlignedBB[boxes.length]);
    }

    public static List<AxisAlignedBB> rotateFaces(List<AxisAlignedBB> boxes, EnumFacing facing) {

        if (facing == EnumFacing.DOWN) {
            return boxes;
        }
        return boxes.stream().map(b -> rotateFace(b, facing)).collect(Collectors.toList());
    }

}
