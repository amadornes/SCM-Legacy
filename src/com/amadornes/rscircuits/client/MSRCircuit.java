package com.amadornes.rscircuits.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import com.amadornes.rscircuits.api.component.IComponent;
import com.amadornes.rscircuits.part.PartCircuit;

import mcmultipart.client.multipart.MultipartSpecialRenderer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3d;

public class MSRCircuit extends MultipartSpecialRenderer<PartCircuit> {

    public static final Matrix4f[] matrices = new Matrix4f[6];
    static {// Yup, this looks horrible... TODO: Cleanup!
        Matrix4f tr1 = new Matrix4f();
        tr1.setIdentity();
        tr1.setTranslation(new Vector3f(0.5F, 0.5F, 0.5F));
        Matrix4f tr2 = new Matrix4f();
        tr2.setIdentity();
        tr2.setTranslation(new Vector3f(-0.5F, -0.5F, -0.5F));
        Matrix4f rot1 = new Matrix4f();
        Matrix4f rot2 = new Matrix4f();
        for (EnumFacing f : EnumFacing.VALUES) {
            rot1.setIdentity();
            rot2.setIdentity();
            switch (f) {
            case DOWN:
                break;
            case UP:
                rot1.rotY((float) Math.toRadians(180));
                rot2.rotZ((float) Math.toRadians(180));
                break;
            case NORTH:
                rot1.rotY((float) Math.toRadians(-90));
                rot2.rotX((float) Math.toRadians(90));
                break;
            case SOUTH:
                rot1.rotY((float) Math.toRadians(90));
                rot2.rotX((float) Math.toRadians(-90));
                break;
            case WEST:
                rot2.rotZ((float) Math.toRadians(-90));
                break;
            case EAST:
                rot1.rotY((float) Math.toRadians(180));
                rot2.rotZ((float) Math.toRadians(90));
                break;
            default:
                break;
            }
            Matrix4f mat = new Matrix4f();
            mat.setIdentity();
            mat.mul(tr1);
            mat.mul(rot2);
            mat.mul(rot1);
            mat.mul(tr2);

            matrices[f.ordinal()] = mat;
        }
    }

    @Override
    public void renderMultipartAt(PartCircuit part, double x_, double y_, double z_, float partialTicks, int destroyStage) {

        if (part.isEncapsulated() || part.isSad()) {
            return;
        }

        // TODO: Per-layer rendering

        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vb = tessellator.getBuffer();
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableBlend();
        GlStateManager.enableCull();

        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        } else {
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }

        GlStateManager.pushMatrix();

        GlStateManager.translate(x_, y_, z_);

        BlockRendererDispatcher brd = Minecraft.getMinecraft().getBlockRendererDispatcher();
        MutableBlockPos pos = new MutableBlockPos();

        List<BakedQuad> quads = TmpModel.INSTANCE.getQuads();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    pos.setPos(x, y, z);
                    for (int s = 0; s < 7; s++) {
                        IComponent c = part.circuit.components[x][y][z][s];
                        if (c != null && c.isDynamic()) {
                            IBlockState actualState = c.getActualState();
                            if (actualState != null) {
                                IBakedModel model = brd.getModelForState(actualState);
                                IBlockState extendedState = c.getExtendedState(actualState);
                                float size = c.getSize();
                                Vec3d offset = c.getOffset();
                                List<BakedQuad> mQuads = new ArrayList<BakedQuad>(model.getQuads(extendedState, null, 0));
                                for (EnumFacing f : EnumFacing.VALUES) {
                                    mQuads.addAll(model.getQuads(extendedState, f, 0));
                                }
                                mQuads.forEach(q -> {
                                    int multiplier = q.hasTintIndex() ? c.getColorMultiplier(q.getTintIndex()) : -1;
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
                                        matrices[part.getFace().ordinal()].transform(point);
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
                        }
                    }
                }
            }
        }
        vb.begin(7, DefaultVertexFormats.BLOCK);
        vb.setTranslation(-part.getPos().getX(), -part.getPos().getY(), -part.getPos().getZ());
        brd.getBlockModelRenderer().renderModel(getWorld(), TmpModel.INSTANCE, Blocks.STONE.getDefaultState(), part.getPos(), vb, false);
        vb.setTranslation(0, 0, 0);
        Tessellator.getInstance().draw();
        quads.clear();

        GlStateManager.popMatrix();

        RenderHelper.enableStandardItemLighting();
    }
}
