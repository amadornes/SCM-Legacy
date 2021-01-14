package com.amadornes.rscircuits.api.component;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.util.ResourceLocation;

public interface IComponentRegistry {

    public void registerFactory(ResourceLocation name, IComponentFactory<?> factory);

    public BlockStateContainer getState(ResourceLocation name);

}
