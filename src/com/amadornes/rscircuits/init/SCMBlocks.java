package com.amadornes.rscircuits.init;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.block.BlockBUD;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class SCMBlocks {

    public static Block update_detector;
    public static Item update_detector_item;

    public static void init() {

        update_detector = new BlockBUD();
        update_detector_item = new ItemBlock(update_detector);
    }

    public static void register() {

        GameRegistry.register(update_detector.setRegistryName("update_detector").setCreativeTab(SCM.tab));
        GameRegistry.register(update_detector_item.setRegistryName("update_detector").setCreativeTab(SCM.tab));
    }

}
