package com.amadornes.rscircuits.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumType;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@SuppressWarnings({ "deprecation", "unchecked" })
public class ModelTransformer {

    public static IBakedModel transformLive(IBakedModel model, IVertexTransformer transformer) {

        return transformLive(model, transformer, f -> f);
    }

    public static IBakedModel transformLive(IBakedModel model, IVertexTransformer transformer,
            Function<VertexFormat, VertexFormat> formatRemapper) {

        return model instanceof IPerspectiveAwareModel ? new LiveTransformedPerspectiveModel(model, transformer, formatRemapper)
                : new LiveTransformedModel(model, transformer, formatRemapper);
    }

    public static IBakedModel transform(IBakedModel model, IVertexTransformer transformer, IBlockState state, long rand) {

        return transform(model, transformer, state, rand, f -> f);
    }

    public static IBakedModel transform(IBakedModel model, IVertexTransformer transformer, IBlockState state, long rand,
            Function<VertexFormat, VertexFormat> formatRemapper) {

        List<BakedQuad>[] quads = new List[7];
        for (int i = 0; i < quads.length; i++) {
            quads[i] = new ArrayList<BakedQuad>();
            for (BakedQuad quad : model.getQuads(state, (i == 6 ? null : EnumFacing.getFront(i)), rand))
                quads[i].add(transform(quad, transformer, formatRemapper));
        }
        return new TransformedModel(model, quads);
    }

    public static BakedQuad transform(BakedQuad quad, IVertexTransformer transformer) {

        return transform(quad, transformer, f -> f);
    }

    public static BakedQuad transform(BakedQuad quad, IVertexTransformer transformer, Function<VertexFormat, VertexFormat> formatRemapper) {

        VertexFormat format = quad.getFormat();
        VertexFormat newFormat = formatRemapper.apply(format);
        Field f = ReflectionHelper.findField(UnpackedBakedQuad.class, "unpackedData");
        f.setAccessible(true);
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(newFormat);
        if (quad.hasTintIndex())
            builder.setQuadTint(quad.getTintIndex());
        builder.setQuadOrientation(quad.getFace());
        LightUtil.putBakedQuad(builder, quad);
        UnpackedBakedQuad unpackedQuad = builder.build();
        try {
            float[][][] unpackedData = (float[][][]) f.get(unpackedQuad);
            int count = newFormat.getElementCount();
            for (int v = 0; v < 4; v++) {
                for (int e = 0; e < count; e++) {
                    VertexFormatElement element = newFormat.getElement(e);
                    unpackedData[v][e] = transformer.transform(quad, element.getType(), element.getUsage(), unpackedData[v][e]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unpackedQuad;
    }

    private static class LiveTransformedModel implements IBakedModel {

        private final IBakedModel parent;
        protected IVertexTransformer transformer;
        protected Function<VertexFormat, VertexFormat> formatRemapper;

        public LiveTransformedModel(IBakedModel parent, IVertexTransformer transformer,
                Function<VertexFormat, VertexFormat> formatRemapper) {

            this.parent = parent;
            this.transformer = transformer;
            this.formatRemapper = formatRemapper;
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

            List<BakedQuad> quads = new ArrayList<BakedQuad>();
            for (BakedQuad quad : parent.getQuads(state, side, rand))
                quads.add(transform(quad, transformer, formatRemapper));
            return quads;
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

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {

            return parent.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {

            return parent.getOverrides();
        }
    }

    private static class LiveTransformedPerspectiveModel extends LiveTransformedModel implements IPerspectiveAwareModel {

        private final Cache<IBakedModel, IBakedModel> modelCache = CacheBuilder.newBuilder().maximumSize(128)
                .expireAfterAccess(5, TimeUnit.MINUTES).build();
        private IPerspectiveAwareModel parent;

        public LiveTransformedPerspectiveModel(IBakedModel parent, IVertexTransformer transformer,
                Function<VertexFormat, VertexFormat> formatRemapper) {
            super(parent, transformer, formatRemapper);
            this.parent = (IPerspectiveAwareModel) parent;
        }

        @Override
        public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {

            Pair<IBakedModel, Matrix4f> pair = (Pair<IBakedModel, Matrix4f>) parent.handlePerspective(cameraTransformType);
            try {
                return pair.getKey() == parent || pair.getKey() == this ? Pair.of(this, pair.getValue())
                        : Pair.of(modelCache.get(pair.getKey(), () -> new LiveTransformedModel(pair.getKey(), transformer, formatRemapper)),
                                pair.getValue());
            } catch (ExecutionException e) {
                return pair;
            }
        }

    }

    private static final class TransformedModel implements IBakedModel {

        private final IBakedModel parent;
        private final List<BakedQuad>[] quads;

        public TransformedModel(IBakedModel parent, List<BakedQuad>[] quads) {

            this.parent = parent;
            this.quads = quads;
        }

        @Override
        public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

            return quads[side == null ? 6 : side.ordinal()];
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

        @Override
        public ItemCameraTransforms getItemCameraTransforms() {

            return parent.getItemCameraTransforms();
        }

        @Override
        public ItemOverrideList getOverrides() {

            return parent.getOverrides();
        }

    }

    public static interface IVertexTransformer {

        public float[] transform(BakedQuad quad, EnumType type, EnumUsage usage, float... data);

    }

}