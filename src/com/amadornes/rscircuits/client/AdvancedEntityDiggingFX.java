package com.amadornes.rscircuits.client;

import net.minecraft.client.particle.ParticleDigging;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class AdvancedEntityDiggingFX extends ParticleDigging {

    public AdvancedEntityDiggingFX(World worldIn, double xCoordIn, double yCoordIn, double zCoordIn, double xSpeedIn, double ySpeedIn,
            double zSpeedIn, TextureAtlasSprite icon) {

        super(worldIn, xCoordIn, yCoordIn, zCoordIn, xSpeedIn, ySpeedIn, zSpeedIn, Blocks.STONE.getDefaultState());
        setParticleTexture(icon);
    }

}