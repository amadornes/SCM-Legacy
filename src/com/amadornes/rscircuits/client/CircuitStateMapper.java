package com.amadornes.rscircuits.client;

import java.util.IdentityHashMap;
import java.util.Map;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.component.ComponentRegistry;

import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.DefaultStateMapper;
import net.minecraft.util.ResourceLocation;

public class CircuitStateMapper extends DefaultStateMapper {

    @Override
    public Map<IBlockState, ModelResourceLocation> putStateModelLocations(Block block) {

        Map<IBlockState, ModelResourceLocation> map = new IdentityHashMap<>();

        ResourceLocation circuit = new ResourceLocation(SCM.MODID, "circuit");
        for (IBlockState istate : MultipartRegistry.getDefaultState(circuit).getValidStates())
            map.put(istate, new ModelResourceLocation(circuit, this.getPropertyString(istate.getProperties())));

        ComponentRegistry.INSTANCE.getRegisteredFactories().forEach((name, factory) -> {
            ResourceLocation path = factory.getModelPath();
            if (path != null) {
                for (IBlockState state : ComponentRegistry.INSTANCE.getState(name).getValidStates()) {
                    ModelResourceLocation mrl = getModelResourceLocation(state);
                    map.put(state, new ModelResourceLocation(path, mrl.getVariant()));
                }
            }
        });
        return map;
    }

}
