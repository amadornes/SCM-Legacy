package com.amadornes.rscircuits.part;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.BufferUtils;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;
import com.amadornes.rscircuits.api.circuit.ExternalCircuitException;
import com.amadornes.rscircuits.api.circuit.HandledCircuitException;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.api.component.IComponentFactory.EnumInstantanceUse;
import com.amadornes.rscircuits.api.component.IComponentFactory.EnumPlacementType;
import com.amadornes.rscircuits.circuit.Circuit;
import com.amadornes.rscircuits.circuit.EnumCircuitCrashStatus;
import com.amadornes.rscircuits.circuit.ICircuitContainer;
import com.amadornes.rscircuits.client.AdvancedEntityDiggingFX;
import com.amadornes.rscircuits.client.MSRCircuit;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.network.NetworkHandler;
import com.amadornes.rscircuits.network.PacketCustomPayload;
import com.amadornes.rscircuits.network.PacketSpawnMagicSmoke;
import com.amadornes.rscircuits.util.ComponentReference;
import com.amadornes.rscircuits.util.ItemPool;
import com.amadornes.rscircuits.util.ProjectionHelper;
import com.amadornes.rscircuits.util.RedstoneUtils;
import com.amadornes.rscircuits.util.UnlistedPropertyComponentStates;
import com.amadornes.rscircuits.util.UnlistedPropertyIOModes;
import com.amadornes.rscircuits.util.UnlistedPropertyName;
import com.google.common.base.Throwables;

import io.netty.buffer.ByteBuf;
import mcmultipart.MCMultiPartMod;
import mcmultipart.client.multipart.AdvancedParticleManager;
import mcmultipart.client.multipart.ICustomHighlightPart;
import mcmultipart.multipart.IMultipart;
import mcmultipart.multipart.IMultipartContainer;
import mcmultipart.multipart.INormallyOccludingPart;
import mcmultipart.multipart.IRedstonePart.ISlottedRedstonePart;
import mcmultipart.multipart.ISlottedPart;
import mcmultipart.multipart.Multipart;
import mcmultipart.multipart.MultipartHelper;
import mcmultipart.multipart.PartSlot;
import mcmultipart.raytrace.PartMOP;
import mcmultipart.raytrace.RayTraceUtils;
import mcmultipart.raytrace.RayTraceUtils.AdvancedRayTraceResult;
import mcmultipart.raytrace.RayTraceUtils.AdvancedRayTraceResultPart;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PartCircuit extends Multipart
        implements ISlottedPart, INormallyOccludingPart, ICustomHighlightPart, ITickable, ISlottedRedstonePart, ICircuitContainer {

    public static final float OFFSET = 2 / 16F;
    public static float selectionBoxOffset = 0;

    public static final IProperty<Boolean> PROPERTY_CAPSULE = PropertyBool.create("capsule");
    public static final IProperty<Boolean> PROPERTY_SAD = PropertyBool.create("sad");
    public static final IUnlistedProperty<Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>>> PROPERTY_COMPONENTS = new UnlistedPropertyComponentStates();
    public static final IUnlistedProperty<String> PROPERTY_NAME = new UnlistedPropertyName();
    public static final IUnlistedProperty<EnumCircuitIOMode[]> PROPERTY_IO_MODE = new UnlistedPropertyIOModes();

    private static final AxisAlignedBB[] BOXES = new AxisAlignedBB[] {
            new AxisAlignedBB(0 / 16D, 0 / 16D, 0 / 16D, 16 / 16D, OFFSET, 16 / 16D),
            new AxisAlignedBB(0 / 16D, 1 - OFFSET, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D),
            new AxisAlignedBB(0 / 16D, 0 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, OFFSET),
            new AxisAlignedBB(0 / 16D, 0 / 16D, 1 - OFFSET, 16 / 16D, 16 / 16D, 16 / 16D),
            new AxisAlignedBB(0 / 16D, 0 / 16D, 0 / 16D, OFFSET, 16 / 16D, 16 / 16D),
            new AxisAlignedBB(1 - OFFSET, 0 / 16D, 0 / 16D, 16 / 16D, 16 / 16D, 16 / 16D) };

    private EnumFacing face;
    public Circuit circuit = new Circuit(this);
    private boolean isEncapsulated = false;
    private boolean isSad = false;

    private boolean notifyAdded, notifyRemoved;

    public PartCircuit(EnumFacing face, NBTTagCompound tag) {

        if (tag != null) {
            readFromNBT(tag);
        }
        this.face = face;
    }

    public PartCircuit() {
    }

    @Override
    public boolean isInWorld() {

        return true;
    }

    @Override
    public EnumFacing getFace() {

        return face;
    }

    @Override
    public Material getMaterial() {

        return Material.ROCK;
    }

    @Override
    public float getHardness(PartMOP hit) {

        return 1F;
    }

    @Override
    public String getHarvestTool() {

        return "pickaxe";
    }

    @Override
    public EnumSet<PartSlot> getSlotMask() {

        return EnumSet.of(PartSlot.getFaceSlot(face));
    }

    @Override
    public void addOcclusionBoxes(List<AxisAlignedBB> list) {

        list.add(BOXES[face.ordinal()]);
    }

    @Override
    public void addCollisionBoxes(AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {

        AxisAlignedBB box = BOXES[face.ordinal()];
        if (box.intersectsWith(mask)) {
            list.add(box);
        }
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> list) {

        list.add(BOXES[face.ordinal()]);
        if (selectionBoxOffset != 0) {
            list.add(new AxisAlignedBB(
                    face == EnumFacing.WEST ? 2 / 16D + selectionBoxOffset : face == EnumFacing.EAST ? 14 / 16D - selectionBoxOffset : 0,
                    face == EnumFacing.DOWN ? 2 / 16D + selectionBoxOffset : face == EnumFacing.UP ? 14 / 16D - selectionBoxOffset : 0,
                    face == EnumFacing.NORTH ? 2 / 16D + selectionBoxOffset : face == EnumFacing.SOUTH ? 14 / 16D - selectionBoxOffset : 0,
                    face == EnumFacing.WEST ? 2 / 16D + selectionBoxOffset : face == EnumFacing.EAST ? 14 / 16D - selectionBoxOffset : 1,
                    face == EnumFacing.DOWN ? 2 / 16D + selectionBoxOffset : face == EnumFacing.UP ? 14 / 16D - selectionBoxOffset : 1,
                    face == EnumFacing.NORTH ? 2 / 16D + selectionBoxOffset
                            : face == EnumFacing.SOUTH ? 14 / 16D - selectionBoxOffset : 1));
        }
    }

    @Override
    public boolean occlusionTest(IMultipart part) {

        return !(part instanceof PartCircuit) && super.occlusionTest(part);
    }

    @Override
    public AdvancedRayTraceResultPart collisionRayTrace(Vec3d start, Vec3d end) {

        // TODO: Per-layer raytracing

        AdvancedRayTraceResultPart defRT = super.collisionRayTrace(start, end);
        if (selectionBoxOffset != 0) {
            return defRT;
        }

        AdvancedRayTraceResult hit = null;
        double dist = Double.POSITIVE_INFINITY;

        if (defRT != null) {
            hit = new AdvancedRayTraceResult(defRT.hit, defRT.bounds);
            dist = defRT.hit.hitVec.squareDistanceTo(start);
        }

        if (!isEncapsulated && !isSad) {
            List<AxisAlignedBB> list = new ArrayList<>();
            MutableBlockPos pos = new MutableBlockPos();
            for (int x = -1; x < 8; x++) {
                int x_ = x;
                for (int y = 0; y < 5; y++) {
                    int y_ = y;
                    for (int z = -1; z < 8; z++) {
                        int z_ = z;
                        for (EnumComponentSlot slot : EnumComponentSlot.VALUES) {
                            pos.setPos(x, y, z);
                            IComponent c = circuit.getComponent(pos, slot);
                            if (c != null && !c.getCircuit().isEncapsulated()) {
                                c.addSelectionBoxes(list);
                                list.replaceAll(bb -> new AxisAlignedBB(bb.minX * 2 / 16D, bb.minY * 2 / 16D, bb.minZ * 2 / 16D,
                                        bb.maxX * 2 / 16D, bb.maxY * 2 / 16D, bb.maxZ * 2 / 16D).offset((2 * x_ + 1) / 16D,
                                                2 * (y_ + 1) / 16D, (2 * z_ + 1) / 16D));
                                AdvancedRayTraceResult result = RayTraceUtils.collisionRayTrace(getWorld(), getPos(), start, end,
                                        ProjectionHelper.rotateFaces(list, getFace()));
                                if (result != null) {
                                    double d = result.hit.hitVec.squareDistanceTo(start);
                                    if (d <= dist) {
                                        AxisAlignedBB newHitBox = c.getSelectionBox(result.bounds);
                                        if (newHitBox != result.bounds) {
                                            newHitBox = ProjectionHelper.rotateFace(new AxisAlignedBB(newHitBox.minX * 2 / 16D,
                                                    newHitBox.minY * 2 / 16D, newHitBox.minZ * 2 / 16D, newHitBox.maxX * 2 / 16D,
                                                    newHitBox.maxY * 2 / 16D, newHitBox.maxZ * 2 / 16D).offset(1 / 16D + (2 / 16D) * x_,
                                                            (2 / 16D) * (y_ + 1), 1 / 16D + (2 / 16D) * z_),
                                                    getFace());
                                            hit = new AdvancedRayTraceResult(result.hit, newHitBox);
                                        } else {
                                            hit = result;
                                        }
                                        hit.hit.hitInfo = c;
                                        dist = d;
                                    }
                                }
                                list.clear();
                            }
                        }
                    }
                }
            }
        }
        return hit != null ? new AdvancedRayTraceResultPart(hit, this) : null;
    }

    @Override
    public BlockStateContainer createBlockState() {

        return new BlockStateContainer.Builder(MCMultiPartMod.multipart).add(BlockDirectional.FACING, PROPERTY_CAPSULE, PROPERTY_SAD)
                .add(PROPERTY_COMPONENTS, PROPERTY_NAME).add(PROPERTY_IO_MODE).build();
    }

    @Override
    public IBlockState getActualState(IBlockState state) {

        return state.withProperty(BlockDirectional.FACING, face).withProperty(PROPERTY_CAPSULE, isEncapsulated && !isSad)
                .withProperty(PROPERTY_SAD, isSad);
    }

    @Override
    public IBlockState getExtendedState(IBlockState state) {

        Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>> states = new HashMap<>();
        if (!isEncapsulated && !isSad) {
            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 5; y++) {
                    for (int z = 0; z < 8; z++) {
                        List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>> l = null;
                        for (int s = 0; s < 7; s++) {
                            IComponent c = circuit.components[x][y][z][s];
                            if (c != null && !c.isDynamic()) {
                                IBlockState actualState = c.getActualState();
                                if (actualState != null) {
                                    IBlockState extendedState = c.getExtendedState(actualState);
                                    if (l == null) {
                                        states.put(new BlockPos(x, y, z), l = new ArrayList<>());
                                    }
                                    l.add(Triple.of(actualState, extendedState,
                                            Triple.of(c.getSize(), c.getOffset(), (IntUnaryOperator) c::getColorMultiplier)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return ((IExtendedBlockState) state).withProperty(PROPERTY_COMPONENTS, states).withProperty(PROPERTY_NAME, circuit.getName())
                .withProperty(PROPERTY_IO_MODE, circuit.getIOModes());
    }

    @Override
    public boolean canRenderInLayer(BlockRenderLayer layer) {

        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, PartMOP hit) {

        if (isSad) {
            if (heldItem != null && heldItem.getItem() == SCMItems.multimeter) {
                if (!getWorld().isRemote) {
                    Pair<EnumCircuitCrashStatus, String> crash = circuit.getCrash();
                    player.addChatMessage(new TextComponentString("Crash status: " + crash.getKey()));
                    if (crash.getKey() == EnumCircuitCrashStatus.UPLOADED) {
                        player.addChatMessage(ForgeHooks.newChatWithLinks(" Log URL: " + crash.getValue()));
                    } else if (crash.getKey() == EnumCircuitCrashStatus.UPLOAD_ERROR) {
                        player.addChatMessage(new TextComponentString(" File name: " + crash.getValue()));
                    }
                }
                return true;
            }
            return false;
        }
        if (isEncapsulated && !player.isSneaking() && heldItem != null && heldItem.getItem() == SCMItems.screwdriver) {
            Vec3d hitPos = hit.hitVec.subtract(new Vec3d(hit.getBlockPos()));
            Vec3d proj = ProjectionHelper.project(face, hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
            Vec3d nProj = new Vec3d(0.5 - Math.abs(proj.xCoord - 0.5), 0, 0.5 - Math.abs(proj.zCoord - 0.5));
            if ((nProj.xCoord > 4 / 16D && nProj.zCoord >= 1 / 16D && nProj.zCoord < 3 / 16D)
                    || (nProj.zCoord > 4 / 16D && nProj.xCoord >= 1 / 16D && nProj.xCoord < 3 / 16D)) {
                if (!getWorld().isRemote) {
                    int quadrant = 6 - ProjectionHelper.getPlacementRotation(proj);
                    switch (getFace()) {
                    case DOWN:
                    case WEST:
                        break;
                    case UP:
                    case EAST:
                        quadrant -= 2;
                        break;
                    case NORTH:
                        quadrant += 1;
                        break;
                    case SOUTH:
                        quadrant -= 1;
                        break;
                    }
                    quadrant %= 4;
                    if (getFace().getAxis() != Axis.Y) {
                        quadrant = quadrant == 3 ? 2 : quadrant == 2 ? 1 : quadrant == 1 ? 3 : 0;
                    }
                    circuit.cycleModes(EnumCircuitSide.HORIZONTALS_ROT[quadrant]);
                }
                return true;
            }
        } else {
            IComponent comp = (IComponent) hit.hitInfo;
            if (comp != null && ((Circuit) comp.getCircuit()).canInteractWith(comp)) {
                Vec3d hitPos = RedstoneUtils.projectComponent(new Vec3d(//
                        hit.hitVec.xCoord - comp.getCircuit().getPos().getX(), //
                        hit.hitVec.yCoord - comp.getCircuit().getPos().getY(), //
                        hit.hitVec.zCoord - comp.getCircuit().getPos().getZ()), getFace(), comp.getPos());
                try {
                    if (comp.onActivated(player, hand, heldItem, hitPos)) {
                        return true;
                    }
                } catch (ExternalCircuitException e) {
                    throw Throwables.propagate(e.getOriginalThrowable());
                } catch (HandledCircuitException e) {
                    if (!getWorld().isRemote) {
                        isSad = true;
                        sendUpdatePacket();
                    }
                    SCM.log.error("Error while interacting with circuit.", e);
                    circuit.onCrash(e);
                } catch (Throwable e) {
                    if (!getWorld().isRemote) {
                        isSad = true;
                        sendUpdatePacket();
                    }
                    spawnMagicSmoke(comp.getPos());
                    SCM.log.error("Error while interacting with circuit.", e);
                    circuit.onCrash(e);
                }
                if (heldItem != null && heldItem.getItem() == SCMItems.multimeter) {
                    if (!getWorld().isRemote) {
                        comp.debug(player);
                    }
                    return true;
                }
                return false;
            }
        }
        if (heldItem != null && heldItem.getItem() == SCMItems.multimeter) {
            if (!getWorld().isRemote) {
                player.addChatMessage(new TextComponentString(
                        "Circuit complexity: " + (circuit.computeComplexity() + ComponentReference.COMPLEXITY_CIRCUIT)));
            }
            return true;
        }
        if (heldItem != null && heldItem.getItem() == SCMItems.squeegee && (player instanceof FakePlayer || player.isSneaking())) {
            if (!getWorld().isRemote) {
                ItemPool pool = new ItemPool();
                circuit.forEach(c -> c.getDrops().forEach(pool::add));
                World world = getWorld();
                int x = getPos().getX();
                int y = getPos().getY();
                int z = getPos().getZ();
                if (player instanceof FakePlayer) {
                    pool.getItems().forEach(s -> InventoryHelper.spawnItemStack(world, x, y, z, s));
                } else {
                    pool.getItems().forEach(s -> {
                        if (!player.inventory.addItemStackToInventory(s)) {
                            InventoryHelper.spawnItemStack(world, x, y, z, s);
                        }
                    });
                }
                circuit.clear();
            }
            return true;
        }
        if (hand == EnumHand.MAIN_HAND && heldItem != null && heldItem.getItem() == SCMItems.screwdriver) {
            if (player.isSneaking()) {
                if (!getWorld().isRemote) {
                    isEncapsulated = !isEncapsulated;
                    sendUpdatePacket();
                }
            } else {
                if (isEncapsulated())
                    return false;
                if (!getWorld().isRemote) {
                    circuit.rotate(Rotation.CLOCKWISE_90);
                    markDirty();
                    notifyNeighbors();
                    notifyPartUpdate();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClicked(EntityPlayer player, PartMOP hit) {

        click(player, hit);
    }

    @Override
    public void harvest(EntityPlayer player, PartMOP hit) {

        if (player != null && hit != null && click(player, hit)) {
            return;
        }
        if (player == null || !player.capabilities.isCreativeMode || player.isSneaking() || circuit.isEmpty()) {
            super.harvest(player, hit);
        }
    }

    protected boolean click(EntityPlayer player, PartMOP hit) {

        if (isSad) {
            return false;
        }

        // TODO: Uncomment!
        if (player != null && hit != null && hit.hitInfo != null && hit.hitInfo instanceof IComponent) {
            IComponent comp = (IComponent) hit.hitInfo;
            Vec3d hitPos = RedstoneUtils.projectComponent(new Vec3d(hit.hitVec.xCoord - comp.getCircuit().getPos().getX(),
                    hit.hitVec.yCoord - comp.getCircuit().getPos().getY(), hit.hitVec.zCoord - comp.getCircuit().getPos().getZ()),
                    getFace(), comp.getPos());
            if (comp.harvest(player, hitPos) && !player.capabilities.isCreativeMode) {
                comp.getDrops().forEach(stack -> Block.spawnAsEntity(getWorld(), getPos(), stack));
            }
            return true;
        }
        return false;
    }

    @Override
    public void update() {

        if (isSad) {
            if (getWorld().isRemote && Math.random() < 0.5) {
                spawnMagicSmoke(new Vec3d(Math.random() * 7, 0, Math.random() * 7));
            }
            return;
        }

        try {
            if (notifyRemoved) {
                circuit.forEach(IComponent::onCircuitRemoved);
                notifyRemoved = false;
            } else if (notifyAdded) {
                circuit.forEach(IComponent::onLoaded);
                circuit.forEach(IComponent::onCircuitAdded);
                notifyAdded = false;
            }

            circuit.tickScheduled();
            circuit.tick();
            circuit.tickEnd();
        } catch (ExternalCircuitException e) {
            throw Throwables.propagate(e.getOriginalThrowable());
        } catch (HandledCircuitException e) {
            if (!getWorld().isRemote) {
                isSad = true;
                sendUpdatePacket();
            }
            SCM.log.error("Error while updating circuit.", e);
            circuit.onCrash(e);
        } catch (Throwable e) {
            if (!getWorld().isRemote) {
                isSad = true;
                sendUpdatePacket();
            }
            spawnMagicSmoke(getPos());
            SCM.log.error("Error while updating circuit.", e);
            circuit.onCrash(e);
        }
    }

    @Override
    public void onNeighborBlockChange(Block block) {

        if (isSad) {
            return;
        }

        if (!getWorld().isSideSolid(getPos().offset(getFace()), getFace().getOpposite())) {
            harvest(null, null);
            return;
        }

        for (EnumCircuitSide side : EnumCircuitSide.HORIZONTALS) {
            circuit.forEachEdge(IComponent::onWorldChange, side, 0, 0, EnumComponentSlot.VALUES);
        }
    }

    @Override
    public void onNeighborTileChange(EnumFacing facing) {

        if (isSad) {
            return;
        }

        circuit.forEachEdge(IComponent::onWorldTileChange, RedstoneUtils.convert(getFace(), facing), 0, 0, EnumComponentSlot.VALUES);
    }

    @SideOnly(Side.CLIENT)
    private FloatBuffer buf;

    @SideOnly(Side.CLIENT)
    @Override
    public boolean drawHighlight(PartMOP hit, EntityPlayer player, float partialTicks) {

        if (isEncapsulated) {
            ItemStack heldItem = RedstoneUtils.unwrap(player.getHeldItemMainhand());
            if (heldItem != null && heldItem.getItem() == SCMItems.screwdriver) {
                Vec3d hitPos = hit.hitVec.subtract(new Vec3d(hit.getBlockPos()));
                Vec3d proj = ProjectionHelper.project(face, hitPos.xCoord, hitPos.yCoord, hitPos.zCoord);
                Vec3d nProj = new Vec3d(0.5 - Math.abs(proj.xCoord - 0.5), 0, 0.5 - Math.abs(proj.zCoord - 0.5));
                if ((nProj.xCoord > 4 / 16D && nProj.zCoord >= 1 / 16D && nProj.zCoord < 3 / 16D)
                        || (nProj.zCoord > 4 / 16D && nProj.xCoord >= 1 / 16D && nProj.xCoord < 3 / 16D)) {
                    int quadrant = 6 - ProjectionHelper.getPlacementRotation(proj);
                    switch (getFace()) {
                    case DOWN:
                    case WEST:
                        break;
                    case UP:
                    case EAST:
                        quadrant -= 2;
                        break;
                    case NORTH:
                        quadrant += 1;
                        break;
                    case SOUTH:
                        quadrant -= 1;
                        break;
                    }
                    quadrant %= 4;
                    Matrix4f mat = ModelRotation.getModelRotation(0, quadrant * 90).getMatrix();
                    Point3f p1 = new Point3f(4 / 16F, 1 / 16F, 1 / 16F), p2 = new Point3f(12 / 16F, 2 / 16F + 0.002F, 3 / 16F + 1 / 64F);
                    mat.transform(p1);
                    mat.transform(p2);
                    AxisAlignedBB bb = new AxisAlignedBB(p1.x, p1.y, p1.z, p2.x, p2.y, p2.z);

                    GlStateManager.pushMatrix();
                    if (buf == null)
                        buf = BufferUtils.createFloatBuffer(16);
                    buf.rewind();
                    TRSRTransformation.toLwjgl(MSRCircuit.matrices[getFace().ordinal()]).store(buf);
                    buf.flip().rewind();

                    GlStateManager.multMatrix(buf);

                    drawAABB(bb);

                    GlStateManager.popMatrix();

                    return true;
                }
            }
            return false;
        } else if (isSad) {
            return false;
        }

        ItemStack heldItem = RedstoneUtils.unwrap(player.getHeldItemMainhand());
        if (heldItem != null) {
            IComponentFactory<?> factory = ComponentRegistry.INSTANCE.getFactory(heldItem, player);
            if (factory != null) {
                Vec3d extrudedProjection = RedstoneUtils.projectComponent(new Vec3d(//
                        hit.hitVec.xCoord - getPos().getX(), //
                        hit.hitVec.yCoord - getPos().getY(), //
                        hit.hitVec.zCoord - getPos().getZ()), getFace(), BlockPos.ORIGIN);
                BlockPos clickedPos = new BlockPos(extrudedProjection);

                if (hit.hitInfo != null && hit.hitInfo instanceof IComponent && ((IComponent) hit.hitInfo).getPos().equals(clickedPos)) {
                    Vec3d subPos = extrudedProjection.subtract(new Vec3d(clickedPos));
                    if (subPos.xCoord == 0) {
                        clickedPos = clickedPos.add(-1, 0, 0);
                    } else if (subPos.zCoord == 0) {
                        clickedPos = clickedPos.add(0, 0, -1);
                    }
                }

                Circuit circuit = this.circuit.getCircuit(clickedPos);
                if (circuit != null) {
                    clickedPos = RedstoneUtils.limitPositionToBounds(clickedPos);

                    double border = 1 / 16D;
                    double cellSize = 1 / 7D;
                    double height = 2 / 16D;
                    double d = 0.002 / (1 - 2 * border);

                    GlStateManager.pushMatrix();

                    GlStateManager.translate(circuit.getPos().getX() - getPos().getX(), circuit.getPos().getY() - getPos().getY(),
                            circuit.getPos().getZ() - getPos().getZ());

                    if (buf == null)
                        buf = BufferUtils.createFloatBuffer(16);
                    buf.rewind();
                    TRSRTransformation.toLwjgl(MSRCircuit.matrices[getFace().ordinal()]).store(buf);
                    buf.flip().rewind();
                    GlStateManager.multMatrix(buf);

                    GlStateManager.translate(border, height, border);
                    GlStateManager.scale((1 - 2 * border) * cellSize, (1 - 2 * border) * cellSize, (1 - 2 * border) * cellSize);
                    GlStateManager.translate(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());

                    drawComponentHighlight(player, heldItem, factory, extrudedProjection, clickedPos, circuit);

                    drawAABB(new AxisAlignedBB(0 - d, -clickedPos.getY(), 0 - d, 1 + d, d, 1 + d));

                    GlStateManager.popMatrix();
                }
            }

        }
        return false;
    }

    public void drawAABB(AxisAlignedBB bb) {

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);

        RenderGlobal.func_189697_a(bb, 0, 0, 0, 0.4F);

        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private <T> void drawComponentHighlight(EntityPlayer player, ItemStack heldItem, IComponentFactory<T> factory, Vec3d extrudedProjection,
            BlockPos clickedPos, Circuit circuit) {

        EnumPlacementType type = factory.getPlacementType(heldItem, player);
        T data = factory.getPlacementData(circuit, clickedPos, EnumCircuitSide.BOTTOM, extrudedProjection, heldItem, player, type, null,
                Collections.emptyMap(), EnumInstantanceUse.RENDER);
        if (data != null && factory.placeComponent(circuit, clickedPos, data, type, Collections.emptyMap(), true)) {
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(0.1F, 0.1F);
            factory.drawPlacement(circuit, clickedPos, data, type, Collections.emptyMap());
            GlStateManager.disablePolygonOffset();
        }
    }

    @Override
    public boolean addDestroyEffects(AdvancedParticleManager particleManager) {

        RayTraceResult mop = SCM.proxy.getHit();
        if (mop instanceof PartMOP) {
            PartMOP hit = (PartMOP) mop;
            if (hit.partHit == this) {
                IComponent component = (IComponent) hit.hitInfo;
                if (component == null) {
                    EntityPlayer player = SCM.proxy.getPlayer();
                    if (player.capabilities.isCreativeMode && !player.isSneaking() && !circuit.isEmpty()) {
                        return true;
                    }
                } else {
                    IBlockState state = component.getActualState();
                    if (state != null) {
                        IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()
                                .getModelForState(state);
                        if (model != null) {
                            TextureAtlasSprite icon = model.getParticleTexture();
                            if (icon != null) {
                                World world = getWorld();
                                BlockPos pos = getPos();
                                Vec3d cpos = RedstoneUtils.unproject(new Vec3d(component.getPos()).addVector(0.25, 0.25, 0.25).scale(1 / 8D)
                                        .addVector(1 / 16D, 2 / 16D, 1 / 16D), getFace());
                                int i = 2;
                                for (int j = 0; j < i; ++j) {
                                    for (int k = 0; k < i; ++k) {
                                        for (int l = 0; l < i; ++l) {
                                            double d0 = pos.getX() + cpos.xCoord + (j + 0.5D) / (i * 8);
                                            double d1 = pos.getY() + cpos.yCoord + (k + 0.5D) / (i * 8);
                                            double d2 = pos.getZ() + cpos.zCoord + (l + 0.5D) / (i * 8);
                                            particleManager.addEffect(new AdvancedEntityDiggingFX(world, d0, d1, d2, 0, 0, 0, icon)
                                                    .setBlockPos(pos).multipleParticleScaleBy(1 / 4F).multiplyVelocity(1 / 2F));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onAdded() {

        notifyAdded = true;
    }

    @Override
    public void onRemoved() {

        notifyRemoved = true;
    }

    @Override
    public void onLoaded() {

        notifyAdded = true;
    }

    @Override
    public void onUnloaded() {

        notifyRemoved = true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {

        tag.setInteger("face", face.ordinal());
        tag.setBoolean("isEncapsulated", isEncapsulated);
        tag.setBoolean("isSad", isSad);
        return circuit.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {

        face = EnumFacing.getFront(tag.getInteger("face"));
        isEncapsulated = tag.getBoolean("isEncapsulated");
        isSad = tag.getBoolean("isSad");
        circuit.readFromNBT(tag);
    }

    @Override
    public void writeUpdatePacket(PacketBuffer buf) {

        buf.writeEnumValue(face);
        buf.writeBoolean(isEncapsulated);
        buf.writeBoolean(isSad);
        circuit.writeUpdatePacket(buf);
    }

    @Override
    public void readUpdatePacket(PacketBuffer buf) {

        face = buf.readEnumValue(EnumFacing.class);
        isEncapsulated = buf.readBoolean();
        isSad = buf.readBoolean();
        circuit.readUpdatePacket(buf);
    }

    @Override
    public boolean isEncapsulated() {

        return isEncapsulated;
    }

    public boolean isSad() {

        return isSad;
    }

    @Override
    public void notifyNeighbors() {

        if (isSad) {
            return;
        }

        notifyBlockUpdate();
    }

    @Override
    public void notifyNeighbor(EnumCircuitSide side, boolean strong) {

        if (isSad) {
            return;
        }

        EnumFacing s = RedstoneUtils.convert(getFace(), side);
        BlockPos pos = getPos().offset(s);
        getWorld().notifyBlockOfStateChange(pos, MCMultiPartMod.multipart);
        if (strong) {
            IBlockState state = getWorld().getBlockState(pos);
            if (state.getBlock().shouldCheckWeakPower(state, getWorld(), pos, s)) {
                getWorld().notifyNeighborsOfStateExcept(pos, state.getBlock(), s.getOpposite());
            }
        }
    }

    @Override
    public byte getInput(EnumCircuitSide side, EnumDyeColor color, boolean bundled) {

        if (getWorld() == null || getPos() == null || isSad) {
            return 0;
        }

        if (bundled) {
            return 0;
        }

        EnumFacing f = RedstoneUtils.convert(getFace(), side);

        IBlockState state = getWorld().getBlockState(getPos().offset(f));
        if (state.getBlock() == Blocks.REDSTONE_WIRE) {
            return (byte) (state.getValue(BlockRedstoneWire.POWER) * 17);
        } else {
            IMultipartContainer container = MultipartHelper.getPartContainer(getWorld(), getPos().offset(f));
            if (container != null) {
                IMultipart part = container.getPartInSlot(PartSlot.getFaceSlot(getFace()));
                if (part != null && part instanceof PartCircuit) {
                    return 0;
                }
            }
        }
        return (byte) (getWorld().getRedstonePower(getPos().offset(f), f) * 17);
    }

    @Override
    public boolean canConnectRedstone(EnumFacing side) {

        return side != getFace() && side != getFace().getOpposite();
    }

    @Override
    public int getWeakSignal(EnumFacing side) {

        if (isSad || !canConnectRedstone(side)) {
            return 0;
        }
        EnumCircuitSide s = RedstoneUtils.convert(getFace(), side);
        return (circuit.getOutput(s, null, false) & 0xFF) / 17;
    }

    @Override
    public int getStrongSignal(EnumFacing side) {

        return getWeakSignal(side);
    }

    public ItemStack getStack() {

        ItemStack stack = new ItemStack(SCMItems.circuit);
        try {
            if (!circuit.isEmpty()) {
                NBTTagCompound tag = new NBTTagCompound();
                circuit.writeToNBT(tag);
                tag.setFloat("complexity", circuit.computeComplexity());
                stack.setTagCompound(tag);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return stack;
    }

    @Override
    public List<ItemStack> getDrops() {

        return Arrays.asList(getStack());
    }

    @Override
    public ItemStack getPickBlock(EntityPlayer player, PartMOP hit) {

        try {
            if (hit != null && hit.hitInfo != null && hit.hitInfo instanceof IComponent) {
                return ((IComponent) hit.hitInfo).getPickedItem();
            }
            ItemStack stack = getStack();
            if (stack.hasTagCompound()) {
                stack.getTagCompound().setLong("circPos", getPos().toLong());
                stack.getTagCompound().setInteger("circDim", getWorld().provider.getDimension());
            }
            return stack;
        } catch (Exception ex) {
            return new ItemStack(SCMItems.circuit);
        }
    }

    @Override
    public void markDirty() {

        super.markDirty();
    }

    @Override
    public void markRenderUpdate() {

        super.markRenderUpdate();
    }

    @Override
    public Circuit getCircuitAt(BlockPos pos, EnumFacing face) {

        PartCircuit c = getCircuitAt(getWorld(), pos, face);
        return c != null ? c.circuit : null;
    }

    @Override
    public void sendCustomPayload(BlockPos pos, EnumComponentSlot slot, ByteBuf buf) {

        if (isSad) {
            return;
        }

        NetworkHandler.instance.sendToServer(new PacketCustomPayload(this, pos, slot, buf.array()));
    }

    public static PartCircuit getCircuitAt(World world, BlockPos pos, EnumFacing face) {

        IMultipartContainer c = MultipartHelper.getPartContainer(world, pos);
        if (c != null) {
            IMultipart m = c.getPartInSlot(PartSlot.getFaceSlot(face));
            if (m != null & m instanceof PartCircuit) {
                return (PartCircuit) m;
            }
        }
        return null;
    }

    @Override
    public void spawnMagicSmoke(BlockPos compPos) {

        spawnMagicSmoke(new Vec3d(compPos));
    }

    public void spawnMagicSmoke(Vec3d compPos) {

        Vec3d pos = RedstoneUtils.unproject(compPos.addVector(0.5, 1, 0.5).scale(1 / 8D), face).add(new Vec3d(getPos()));
        if (getWorld().isRemote) {
            double rnd = Math.random();
            if (rnd <= 1 / 3D) {
                getWorld().spawnParticle(EnumParticleTypes.CLOUD, pos.xCoord, pos.yCoord, pos.zCoord, 0, 0, 0);
            } else if (rnd <= 2 / 3D) {
                getWorld().spawnParticle(EnumParticleTypes.SMOKE_NORMAL, pos.xCoord, pos.yCoord, pos.zCoord, 0, -5, 0);
            } else {
                getWorld().spawnParticle(EnumParticleTypes.SNOW_SHOVEL, pos.xCoord, pos.yCoord + 0.1, pos.zCoord, 0, 0.075, 0);
            }
        } else {
            NetworkHandler.instance.sendToAllAround(new PacketSpawnMagicSmoke(pos),
                    new TargetPoint(getWorld().provider.getDimension(), pos.xCoord, pos.yCoord, pos.zCoord, 64));
        }
    }

    @Override
    public void spawnStack(ItemStack stack) {

        BlockPos pos = getPos();
        InventoryHelper.spawnItemStack(getWorld(), pos.getX(), pos.getY(), pos.getZ(), stack);
    }

    @Override
    public void onCleared() {

        isSad = false;
        sendUpdatePacket();
    }

}
