package com.amadornes.rscircuits.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import org.apache.commons.lang3.tuple.Triple;

import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;
import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.client.ModelTransformer.IVertexTransformer;
import com.amadornes.rscircuits.part.PartCircuit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.property.IExtendedBlockState;

public class ModelWrapperCircuit implements IBakedModel {

    private final IBakedModel parent;

    private final Cache<Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>>, List<BakedQuad>> modelCache = CacheBuilder
            .newBuilder().maximumSize(128).expireAfterAccess(5, TimeUnit.MINUTES).build();

    public ModelWrapperCircuit(IBakedModel parent) {

        this.parent = parent;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

        if (side == null) {
            Map<BlockPos, List<Triple<IBlockState, IBlockState, Triple<Float, Vec3d, IntUnaryOperator>>>> states = ((IExtendedBlockState) state)
                    .getValue(PartCircuit.PROPERTY_COMPONENTS);
            if (states != null) {
                try {
                    BlockRendererDispatcher brd = Minecraft.getMinecraft().getBlockRendererDispatcher();
                    return processQuads(state, side, modelCache.get(states, () -> {
                        List<BakedQuad> quads = new ArrayList<BakedQuad>(parent.getQuads(state, side, rand));
                        states.forEach((pos, l) -> {
                            l.forEach((st) -> {
                                IBakedModel model = brd.getModelForState(st.getLeft());
                                if (model != null) {
                                    float size = st.getRight().getLeft();
                                    Vec3d offset = st.getRight().getMiddle();
                                    IntUnaryOperator colorProvider = st.getRight().getRight();
                                    Stream<BakedQuad> stream = model.getQuads(st.getMiddle(), null, rand).stream();
                                    for (EnumFacing f : EnumFacing.VALUES) {
                                        stream = Stream.concat(stream, model.getQuads(st.getMiddle(), f, rand).stream());
                                    }
                                    stream.forEach(q -> {
                                        int multiplier = q.hasTintIndex() ? colorProvider.applyAsInt(q.getTintIndex()) : -1;
                                        Color colorMultiplier = multiplier != -1 ? new Color(multiplier, false) : null;
                                        int[] data = q.getVertexData().clone();
                                        for (int i = 0; i < 4; i++) {
                                            Point3f point = new Point3f(
                                                    (Float.intBitsToFloat(data[i * 7 + 0]) - 0.5F
                                                            + (offset != null ? (float) offset.xCoord : 0)) * size
                                                            + (pos.getX() + 0.5F) * IComponent.COMPONENT_SIZE + 1 / 16F,
                                                    (Float.intBitsToFloat(data[i * 7 + 1]) + (offset != null ? (float) offset.yCoord : 0))
                                                            * size + pos.getY() * IComponent.COMPONENT_SIZE + 2 / 16F,
                                                    (Float.intBitsToFloat(data[i * 7 + 2]) - 0.5F
                                                            + (offset != null ? (float) offset.zCoord : 0)) * size
                                                            + (pos.getZ() + 0.5F) * IComponent.COMPONENT_SIZE + 1 / 16F);
                                            MSRCircuit.matrices[state.getValue(BlockDirectional.FACING).ordinal()].transform(point);
                                            data[i * 7 + 0] = Float.floatToRawIntBits(point.x);
                                            data[i * 7 + 1] = Float.floatToRawIntBits(point.y);
                                            data[i * 7 + 2] = Float.floatToRawIntBits(point.z);
                                            if (colorMultiplier != null) {
                                                Color color = new Color(data[i * 7 + 3]);
                                                data[i * 7 + 3] = (color.getAlpha() << 24)
                                                        | (((color.getRed() * colorMultiplier.getBlue()) / 255) << 16)
                                                        | (((color.getGreen() * colorMultiplier.getGreen()) / 255) << 8)
                                                        | (((color.getBlue() * colorMultiplier.getRed()) / 255) << 0);
                                            }
                                        }
                                        quads.add(new BakedQuad(data, q.getTintIndex(), q.getFace(), q.getSprite(),
                                                q.shouldApplyDiffuseLighting(), q.getFormat()));
                                    });
                                }
                            });
                        });
                        return quads;
                    }));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return processQuads(state, side, parent.getQuads(state, side, rand));
    }

    private List<BakedQuad> processQuads(IBlockState state, EnumFacing side, List<BakedQuad> quads) {

        if (side == null && state.getValue(PartCircuit.PROPERTY_CAPSULE) && !state.getValue(PartCircuit.PROPERTY_SAD)) {
            quads = new ArrayList<>(quads);
            EnumCircuitIOMode[] modes = ((IExtendedBlockState) state).getValue(PartCircuit.PROPERTY_IO_MODE);
            List<BakedQuad> added = new ArrayList<>();
            ListIterator<BakedQuad> li = quads.listIterator();

            while (li.hasNext()) {
                BakedQuad q = li.next();
                try {
                    if (q.getSprite().getIconName().equals("rscircuits:blocks/overlay_normal")) {
                        doTheThing(state, modes, EnumCircuitIOMode.REGULAR, li, added, q);
                    } else if (q.getSprite().getIconName().equals("rscircuits:blocks/overlay_combo")) {
                        doTheThing(state, modes, EnumCircuitIOMode.BUNDLED, li, added, q);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            quads.addAll(added);
            added.clear();

            // TEXT

            String name = ((IExtendedBlockState) state).getValue(PartCircuit.PROPERTY_NAME);

            if (name != null && name.length() > 0) {
                SimpleModelFontRenderer fontRenderer = SimpleModelFontRenderer.get(quads.iterator().next().getFormat());
                fontRenderer.setFillBlanks(true);
                int width = fontRenderer.getStringWidth(name) - 1;
                int size = Math.max(width, fontRenderer.FONT_HEIGHT + 2);
                fontRenderer.drawString(name, 0, 0, 0xFFFFFFFF);
                IVertexTransformer transformer = (q, type, usage, data) -> {
                    if (usage == EnumUsage.POSITION) {
                        Point3f p = new Point3f(data[0], data[1], data[2]);

                        p.x = (p.x - (1 - (fontRenderer.FONT_HEIGHT - 2) / 16F / 16F)) * ((64F * 9 / 10F) / size) + 0.5F;
                        p.y = p.y - 1 + 2.75F / 16F + 0.002F;
                        p.z = (p.z - (width / 16F / 16F)) * ((64F * 9 / 10F) / size) + 0.5F;

                        Matrix4f mat;
                        mat = new Matrix4f(MSRCircuit.matrices[state.getValue(BlockDirectional.FACING).ordinal()]);
                        if (state.getValue(BlockDirectional.FACING).getAxis() != Axis.Y) {
                            Matrix4f mat2 = new Matrix4f();
                            mat2.setIdentity();
                            mat2.rotY((float) Math.PI);
                            p.x -= 0.5;
                            p.z -= 0.5;
                            mat2.transform(p);
                            p.x += 0.5;
                            p.z += 0.5;
                        }
                        mat.transform(p);

                        return new float[] { p.x, p.y, p.z, data[3] };
                    }
                    return data;
                };
                fontRenderer.build().stream().map(q -> ModelTransformer.transform(q, transformer)).forEach(quads::add);
            }
        }
        return quads;
    }

    public void doTheThing(IBlockState state, EnumCircuitIOMode[] modes, EnumCircuitIOMode mode, ListIterator<BakedQuad> li,
            List<BakedQuad> quads, BakedQuad quad) {

        int k = state.getValue(BlockDirectional.FACING).getAxis() == Axis.Y ? 0 : 1;
        for (int i = 0; i < 4; i++) {
            int j = i == 2 ? 0 : i == 0 ? 1 : i == 1 ? 2 : 3;
            if (modes[(j + k) % 4] == mode) {
                Matrix4f matrix = ModelRotation.getModelRotation(0, i * 90).getMatrix();
                if (matrix != null) {
                    quads.add(ModelTransformer.transform(quad, (q, type, usage, data) -> {
                        if (usage == EnumUsage.POSITION) {
                            Point3f point = new Point3f(data[0], data[1], data[2]);
                            Matrix4f mat = new Matrix4f(MSRCircuit.matrices[state.getValue(BlockDirectional.FACING).ordinal()]);
                            mat.invert();
                            mat.transform(point);
                            point.y += 0.001F;
                            matrix.transform(point);
                            MSRCircuit.matrices[state.getValue(BlockDirectional.FACING).ordinal()].transform(point);
                            return new float[] { point.x, point.y, point.z, data[3] };
                        }
                        return data;
                    }));
                }
            }
        }
        li.remove();
    }

    @Override
    public boolean isAmbientOcclusion() {

        return parent.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {

        return parent.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {

        return parent.isBuiltInRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {

        return parent.getParticleTexture();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ItemCameraTransforms getItemCameraTransforms() {

        return parent.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {

        return parent.getOverrides();
    }

}
