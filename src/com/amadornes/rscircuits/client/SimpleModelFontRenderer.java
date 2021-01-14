package com.amadornes.rscircuits.client;

import java.util.concurrent.ExecutionException;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public abstract class SimpleModelFontRenderer extends FontRenderer {

    private static final ResourceLocation font = new ResourceLocation("minecraft", "textures/font/ascii.png");
    private static final ResourceLocation font2 = new ResourceLocation("minecraft", "font/ascii");
    private static final Cache<VertexFormat, SimpleModelFontRenderer> fontCache = CacheBuilder.newBuilder().maximumSize(3).build();
    private static TextureAtlasSprite fontTexture;

    public static SimpleModelFontRenderer get(VertexFormat format) {

        try {
            SimpleModelFontRenderer fontRenderer = fontCache.get(format, () -> {
                Matrix4f m = new Matrix4f();
                m.m20 = 1f / 128f;
                m.m01 = m.m12 = -m.m20;
                m.m33 = 1;
                m.setTranslation(new Vector3f(1, 1, 0));
                return new SimpleModelFontRenderer(Minecraft.getMinecraft().gameSettings, font,
                        Minecraft.getMinecraft().getTextureManager(), false, m, format) {

                    @Override
                    protected float renderUnicodeChar(char c, boolean italic) {

                        return super.renderDefaultChar(126, italic);
                    }
                };
            });
            fontRenderer.setSprite(fontTexture);
            return fontRenderer;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {

        fontTexture = event.getMap().registerSprite(font2);
    }

    private float r, g, b, a;
    private final Matrix4f matrix;
    private ImmutableList.Builder<BakedQuad> builder = ImmutableList.builder();
    private final VertexFormat format;
    private final Vector3f normal = new Vector3f(0, 0, 1);
    private final EnumFacing orientation;
    private boolean fillBlanks = false;

    private TextureAtlasSprite sprite;

    private SimpleModelFontRenderer(GameSettings settings, ResourceLocation font, TextureManager manager, boolean isUnicode,
            Matrix4f matrix, VertexFormat format) {
        super(settings, font, manager, isUnicode);
        manager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.matrix = new Matrix4f(matrix);
        Matrix3f nm = new Matrix3f();
        this.matrix.getRotationScale(nm);
        nm.invert();
        nm.transpose();
        this.format = format;
        nm.transform(normal);
        normal.normalize();
        orientation = EnumFacing.getFacingFromVector(normal.x, normal.y, normal.z);
    }

    public void setSprite(TextureAtlasSprite sprite) {

        this.sprite = sprite;
        super.onResourceManagerReload(null);
    }

    public void setFillBlanks(boolean fillBlanks) {

        this.fillBlanks = fillBlanks;
    }

    @Override
    protected float renderDefaultChar(int pos, boolean italic) {

        float x = (pos % 16) / 16f;
        float y = (pos / 16) / 16f;
        float sh = italic ? 1f : 0f;
        float w = charWidth[pos] - 1.01f;
        float h = FONT_HEIGHT - 1.01f;
        float wt = w / 128f;
        float ht = h / 128f;

        UnpackedBakedQuad.Builder quadBuilder = new UnpackedBakedQuad.Builder(format);
        quadBuilder.setQuadOrientation(orientation);

        addVertex(quadBuilder, posX + sh, posY, x, y);
        addVertex(quadBuilder, posX - sh, posY + h, x, y + ht);
        addVertex(quadBuilder, posX + w + sh, posY + h, x + wt, y + ht);
        addVertex(quadBuilder, posX + w - sh, posY, x + wt, y);
        builder.add(quadBuilder.build());

        if (fillBlanks) {
            float cuv = 15f / 16f;

            quadBuilder = new UnpackedBakedQuad.Builder(format);
            quadBuilder.setQuadOrientation(orientation);

            addVertex(quadBuilder, posX + w + sh, posY, cuv, cuv);
            addVertex(quadBuilder, posX + w - sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] + sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] - sh, posY, cuv, cuv);
            builder.add(quadBuilder.build());

            quadBuilder = new UnpackedBakedQuad.Builder(format);
            quadBuilder.setQuadOrientation(orientation);

            addVertex(quadBuilder, posX + sh, posY + h, cuv, cuv);
            addVertex(quadBuilder, posX - sh, posY + FONT_HEIGHT, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] + sh, posY + FONT_HEIGHT, cuv, cuv);
            addVertex(quadBuilder, posX + charWidth[pos] - sh, posY + h, cuv, cuv);
            builder.add(quadBuilder.build());
        }
        return charWidth[pos];
    }

    private final Vector4f vec = new Vector4f();

    private void addVertex(UnpackedBakedQuad.Builder quadBuilder, float x, float y, float u, float v) {

        vec.x = x;
        vec.y = y;
        vec.z = 0;
        vec.w = 1;
        matrix.transform(vec);
        for (int e = 0; e < format.getElementCount(); e++) {
            switch (format.getElement(e).getUsage()) {
            case POSITION:
                quadBuilder.put(e, vec.x, vec.y, vec.z, vec.w);
                break;
            case UV:
                quadBuilder.put(e, sprite.getInterpolatedU(u * 16), sprite.getInterpolatedV(v * 16), 0, 1);
                break;
            case COLOR:
                quadBuilder.put(e, r, g, b, a);
                break;
            case NORMAL:
                // quadBuilder.put(e, normal.x, normal.y, normal.z, 1);
                quadBuilder.put(e, 0, 0, 1, 1);
                break;
            default:
                quadBuilder.put(e);
                break;
            }
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

        super.onResourceManagerReload(resourceManager);
        String p = locationFontTexture.getResourcePath();
        if (p.startsWith("textures/")) {
            p = p.substring("textures/".length(), p.length());
        }
        if (p.endsWith(".png")) {
            p = p.substring(0, p.length() - ".png".length());
        }
        String f = locationFontTexture.getResourceDomain() + ":" + p;
        sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(f);
    }

    @Override
    protected abstract float renderUnicodeChar(char c, boolean italic);

    @Override
    protected void doDraw(float shift) {

        posX += (int) shift;
    }

    @Override
    protected void setColor(float r, float g, float b, float a) {

        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public void enableAlpha() {

    }

    public ImmutableList<BakedQuad> build() {

        ImmutableList<BakedQuad> ret = builder.build();
        builder = ImmutableList.builder();
        return ret;
    }
}