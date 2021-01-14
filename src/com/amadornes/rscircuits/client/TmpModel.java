package com.amadornes.rscircuits.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum TmpModel implements IBakedModel {

    INSTANCE;

    private List<BakedQuad> quads = new ArrayList<BakedQuad>();

    public TmpModel setQuads(List<BakedQuad> quads) {

        this.quads = quads;
        return this;
    }

    public List<BakedQuad> getQuads() {

        return quads;
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {

        return side == null ? quads : Collections.emptyList();
    }

    @Override
    public boolean isAmbientOcclusion() {

        return true;
    }

    @Override
    public boolean isGui3d() {

        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {

        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {

        return null;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {

        return null;
    }

    @Override
    public ItemOverrideList getOverrides() {

        return null;
    }

}
