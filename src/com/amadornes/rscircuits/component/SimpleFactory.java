package com.amadornes.rscircuits.component;

import java.awt.Color;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.vecmath.Point3f;

import com.amadornes.rscircuits.api.circuit.ICircuit;
import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.client.TmpModel;
import com.amadornes.rscircuits.util.RedstoneUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class SimpleFactory<T extends ComponentBaseInt> implements IComponentFactory<T> {

    @Override
    public abstract T instantiate(ICircuit circuit);

    public T instantiate(ICircuit circuit, ItemStack stack) {

        return instantiate(circuit);
    }

    @Override
    public void serialize(PacketBuffer buf, Map<BlockPos, T> data, EntityPlayer player) {

        serialize(buf, data, Function.identity());
    }

    @Override
    public void deserialize(PacketBuffer buf, Map<BlockPos, T> data, com.google.common.base.Function<BlockPos, ICircuit> circuitProvider,
            EntityPlayer player) {

        deserialize(buf, data, circuitProvider, Function.identity(), this::instantiate);
    }

    @Override
    public EnumPlacementType getPlacementType(ItemStack stack, EntityPlayer player) {

        return EnumPlacementType.SINGLE;
    }

    @Override
    public T getPlacementData(ICircuit circuit, BlockPos pos, EnumCircuitSide faceClicked, Vec3d hitVec, ItemStack stack,
            EntityPlayer player, EnumPlacementType type, T previousData, Map<BlockPos, T> otherData, EnumInstantanceUse use) {

        if (previousData != null) {
            return previousData;
        }
        return instantiate(circuit, stack);
    }

    @Override
    public boolean placeComponent(ICircuit circuit, BlockPos pos, T data, EnumPlacementType type, Map<BlockPos, T> otherData,
            boolean simulate) {

        return circuit.addComponent(pos, data, simulate);
    }

    @Override
    public void drawPlacement(ICircuit circuit, BlockPos pos, T data, EnumPlacementType type, Map<BlockPos, T> otherData) {

        draw(data, Function.identity());
    }

    public static <T extends ComponentBaseInt, V> void serialize(PacketBuffer buf, Map<BlockPos, V> data, Function<V, T> adapter) {

        buf.writeInt(data.size());
        data.forEach((pos, comp) -> {
            buf.writeBlockPos(pos);
            adapter.apply(comp).serializePlacement(buf);
        });
    }

    public static <T extends ComponentBaseInt, V> void deserialize(PacketBuffer buf, Map<BlockPos, V> data,
            com.google.common.base.Function<BlockPos, ICircuit> circuitProvider, Function<T, V> adapter, Function<ICircuit, T> supplier) {

        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            T comp = supplier.apply(circuitProvider.apply(pos));
            comp.setPos(RedstoneUtils.limitPositionToBounds(pos));
            comp.deserializePlacement(buf);
            data.put(pos, adapter.apply(comp));
        }
    }

    public static <T extends ComponentBaseInt, V> void draw(V data, Function<V, T> adapter) {

        T comp = adapter.apply(data);
        IBlockState state = comp.getActualState();
        if (state != null) {
            draw(comp, state, comp.getExtendedState(state));
        }
    }

    @SideOnly(Side.CLIENT)
    public static <T extends ComponentBaseInt> void draw(T comp, IBlockState modelState, IBlockState state) {

        Minecraft mc = Minecraft.getMinecraft();
        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(modelState);

        float size = comp.getSize();
        Vec3d offset = comp.getOffset();
        Stream<BakedQuad> stream = model.getQuads(state, null, 0).stream();
        for (EnumFacing f : EnumFacing.VALUES) {
            stream = Stream.concat(stream, model.getQuads(state, f, 0).stream());
        }
        TmpModel.INSTANCE.getQuads().addAll(stream.map(q -> {
            int multiplier = q.hasTintIndex() ? comp.getColorMultiplier(q.getTintIndex()) : -1;
            Color colorMultiplier = multiplier != -1 ? new Color(multiplier, false) : null;
            int[] data = q.getVertexData().clone();
            for (int i = 0; i < 4; i++) {
                Point3f point = new Point3f(
                        ((Float.intBitsToFloat(data[i * 7 + 0]) - 0.5F + (offset != null ? (float) offset.xCoord : 0)) * size)
                                / IComponent.COMPONENT_SIZE + 0.5F,
                        ((Float.intBitsToFloat(data[i * 7 + 1]) + (offset != null ? (float) offset.yCoord : 0)) * size)
                                / IComponent.COMPONENT_SIZE,
                        ((Float.intBitsToFloat(data[i * 7 + 2]) - 0.5F + (offset != null ? (float) offset.zCoord : 0)) * size)
                                / IComponent.COMPONENT_SIZE + 0.5F);
                data[i * 7 + 0] = Float.floatToRawIntBits(point.x);
                data[i * 7 + 1] = Float.floatToRawIntBits(point.y);
                data[i * 7 + 2] = Float.floatToRawIntBits(point.z);
                if (colorMultiplier != null) {
                    Color color = new Color(data[i * 7 + 3]);
                    data[i * 7 + 3] = (color.getAlpha() << 24) | (((color.getRed() * colorMultiplier.getBlue()) / 255) << 16)
                            | (((color.getGreen() * colorMultiplier.getGreen()) / 255) << 8)
                            | (((color.getBlue() * colorMultiplier.getRed()) / 255) << 0);
                }
            }
            return new BakedQuad(data, q.getTintIndex(), q.getFace(), q.getSprite(), q.shouldApplyDiffuseLighting(), q.getFormat());
        }).collect(Collectors.toList()));

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_DST_COLOR);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();

        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(7, DefaultVertexFormats.BLOCK);
        vb.setTranslation(-mc.objectMouseOver.getBlockPos().getX(), -mc.objectMouseOver.getBlockPos().getY(),
                -mc.objectMouseOver.getBlockPos().getZ());
        mc.getBlockRendererDispatcher().getBlockModelRenderer().renderModel(mc.theWorld, TmpModel.INSTANCE, state,
                mc.objectMouseOver.getBlockPos(), vb, false);
        vb.setTranslation(0, 0, 0);
        Tessellator.getInstance().draw();
        TmpModel.INSTANCE.getQuads().clear();

        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

}
