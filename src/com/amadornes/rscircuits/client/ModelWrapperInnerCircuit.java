package com.amadornes.rscircuits.client;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

import com.amadornes.rscircuits.api.circuit.EnumCircuitIOMode;
import com.amadornes.rscircuits.client.ModelTransformer.IVertexTransformer;
import com.amadornes.rscircuits.part.PartCircuit;

import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraftforge.common.property.IExtendedBlockState;

public class ModelWrapperInnerCircuit implements IBakedModel {

    private final IBakedModel parent;

    public ModelWrapperInnerCircuit(IBakedModel parent) {

        this.parent = parent;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

        return processQuads(state, side, parent.getQuads(state, side, rand));
    }

    private List<BakedQuad> processQuads(IBlockState state, EnumFacing side, List<BakedQuad> quads) {

        if (side == null && state instanceof IExtendedBlockState
                && ((IExtendedBlockState) state).getValue(PartCircuit.PROPERTY_IO_MODE) != null
                && ((IExtendedBlockState) state).getValue(PartCircuit.PROPERTY_NAME) != null) {
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

            if (name.length() > 0) {
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
