package com.amadornes.rscircuits.init;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.item.ItemBlueprint;
import com.amadornes.rscircuits.item.ItemCircuit;
import com.amadornes.rscircuits.item.ItemComponentTray;
import com.amadornes.rscircuits.item.ItemMonocle;
import com.amadornes.rscircuits.item.ItemMultimeter;
import com.amadornes.rscircuits.item.ItemPaletteAndBrush;
import com.amadornes.rscircuits.item.ItemPunchcard;
import com.amadornes.rscircuits.item.ItemRedwire;
import com.amadornes.rscircuits.item.ItemResource;
import com.amadornes.rscircuits.item.ItemScrewdriver;
import com.amadornes.rscircuits.item.ItemSqueegee;
import com.amadornes.rscircuits.part.PartCircuit;
import com.amadornes.rscircuits.part.PartRedstoneWire;

import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class SCMItems {

    public static Item resource;
    public static Item circuit;
    public static Item redwire;
    public static Item component_tray;
    public static Item palette_and_brush;
    public static Item screwdriver;
    public static Item monocle;
    public static Item blueprint;
    public static Item punchcard;
    public static Item multimeter;
    public static Item squeegee;

    public static void init() {

        resource = new ItemResource();
        circuit = new ItemCircuit();
        redwire = new ItemRedwire();
        component_tray = new ItemComponentTray();
        palette_and_brush = new ItemPaletteAndBrush();
        screwdriver = new ItemScrewdriver();
        monocle = new ItemMonocle();
        blueprint = new ItemBlueprint();
        punchcard = new ItemPunchcard();
        multimeter = new ItemMultimeter();
        squeegee = new ItemSqueegee();
    }

    public static void register() {

        GameRegistry.register(resource.setRegistryName("resource").setCreativeTab(SCM.tab));
        GameRegistry.register(circuit.setRegistryName("circuit").setCreativeTab(SCM.tab));
        MultipartRegistry.registerPart(PartCircuit.class, "circuit");
        GameRegistry.register(redwire.setRegistryName("redwire").setCreativeTab(SCM.tab));
        MultipartRegistry.registerPart(PartRedstoneWire.class, "redwire");
        GameRegistry.register(component_tray.setRegistryName("component_tray").setCreativeTab(SCM.tab));
        GameRegistry.register(palette_and_brush.setRegistryName("palette_and_brush").setCreativeTab(SCM.tab));
        GameRegistry.register(screwdriver.setRegistryName("screwdriver").setCreativeTab(SCM.tab));
        GameRegistry.register(monocle.setRegistryName("monocle").setCreativeTab(SCM.tab));
        GameRegistry.register(blueprint.setRegistryName("blueprint").setCreativeTab(SCM.tab));
        GameRegistry.register(punchcard.setRegistryName("punchcard").setCreativeTab(SCM.tab));
        GameRegistry.register(multimeter.setRegistryName("multimeter").setCreativeTab(SCM.tab));
        GameRegistry.register(squeegee.setRegistryName("squeegee").setCreativeTab(SCM.tab));
    }

}
