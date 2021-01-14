package com.amadornes.rscircuits.component;

import java.util.HashMap;
import java.util.Map;

import com.amadornes.rscircuits.api.component.IComponentFactory;
import com.amadornes.rscircuits.api.component.IComponentRegistry;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public enum ComponentRegistry implements IComponentRegistry {

    INSTANCE;

    private final BiMap<ResourceLocation, IComponentFactory<?>> factories = HashBiMap.create();
    private final Map<ResourceLocation, BlockStateContainer> states = new HashMap<ResourceLocation, BlockStateContainer>();

    @Override
    public void registerFactory(ResourceLocation name, IComponentFactory<?> factory) {

        factories.put(name, factory);
        states.put(name, factory.createBlockState());
    }

    public IComponentFactory<?> getFactory(ItemStack stack, EntityPlayer player) {

        for (IComponentFactory<?> factory : factories.values()) {
            if (factory.isValidPlacementStack(stack, player)) {
                return factory;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> IComponentFactory<T> getFactory(ResourceLocation name) {

        return (IComponentFactory<T>) factories.get(name);
    }

    public ResourceLocation getName(IComponentFactory<?> factory) {

        return factories.inverse().get(factory);
    }

    public BlockStateContainer getState(ResourceLocation name) {

        return states.get(name);
    }

    public Map<ResourceLocation, IComponentFactory<?>> getRegisteredFactories() {

        return factories;
    }

}
