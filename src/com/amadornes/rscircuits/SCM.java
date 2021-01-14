package com.amadornes.rscircuits;

import org.apache.logging.log4j.Logger;

import com.amadornes.rscircuits.command.CommandExport;
import com.amadornes.rscircuits.command.CommandImport;
import com.amadornes.rscircuits.command.CommandSCM;
import com.amadornes.rscircuits.component.ComponentRegistry;
import com.amadornes.rscircuits.component.analogue.ComponentConst;
import com.amadornes.rscircuits.component.analogue.ComponentMath;
import com.amadornes.rscircuits.component.analogue.ComponentMath.EnumMathComponent;
import com.amadornes.rscircuits.component.button.ComponentButton;
import com.amadornes.rscircuits.component.circuit.ComponentCircuit;
import com.amadornes.rscircuits.component.circuit.ComponentCircuitSlave;
import com.amadornes.rscircuits.component.digital.ComponentLever;
import com.amadornes.rscircuits.component.digital.ComponentTorch;
import com.amadornes.rscircuits.component.lamp.ComponentLamp;
import com.amadornes.rscircuits.component.lamp.ComponentLampDriver;
import com.amadornes.rscircuits.component.lamp.ComponentLampSegmented;
import com.amadornes.rscircuits.component.misc.ComponentDelay;
import com.amadornes.rscircuits.component.misc.ComponentInventoryScanner;
import com.amadornes.rscircuits.component.misc.ComponentLightSensor;
import com.amadornes.rscircuits.component.misc.ComponentNote;
import com.amadornes.rscircuits.component.misc.ComponentPlate;
import com.amadornes.rscircuits.component.misc.ComponentPlateNC;
import com.amadornes.rscircuits.component.misc.ComponentPost;
import com.amadornes.rscircuits.component.misc.ComponentPunchcardReader;
import com.amadornes.rscircuits.component.misc.ComponentRandom;
import com.amadornes.rscircuits.component.misc.ComponentTimer;
import com.amadornes.rscircuits.component.wire.ComponentRedstoneWire;
import com.amadornes.rscircuits.init.SCMBlocks;
import com.amadornes.rscircuits.init.SCMItems;
import com.amadornes.rscircuits.init.SCMRecipes;
import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.network.NetworkHandler;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent;
import net.minecraftforge.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.server.command.CommandTreeBase;

@Mod(modid = SCM.MODID, name = SCM.NAME, version = "1.1.0", dependencies = "required-after:Forge@[12.18.2.2120,);") // required-after:mcmultipart@[1.3.0,)")
public class SCM {

    @SidedProxy(serverSide = "com.amadornes.rscircuits.CommonProxy", clientSide = "com.amadornes.rscircuits.client.ClientProxy")
    public static CommonProxy proxy;

    public static final String MODID = "rscircuits";
    public static final String NAME = "Super Circuit Maker";

    public static final CreativeTabs tab = new CreativeTabs(MODID) {

        @Override
        public Item getTabIconItem() {

            return SCMItems.resource;
        }

        @Override
        public int getIconItemDamage() {

            return EnumResourceType.PILE_OF_REDSTONE.ordinal();
        }
    };

    public static Logger log;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        SCMItems.init();
        SCMBlocks.init();
        SCMItems.register();
        SCMBlocks.register();

        SCMRecipes.registerRecipes();

        ComponentRegistry.INSTANCE.registerFactory(ComponentRedstoneWire.NAME, new ComponentRedstoneWire.Factory());
        // ComponentRegistry.INSTANCE.registerFactory(ComponentWireBundled.NAME, new ComponentWireBundled.Factory());
        // ComponentRegistry.INSTANCE.registerFactory(ComponentRedstonePost.NAME, new ComponentRedstonePost.Factory());
        // ComponentRegistry.INSTANCE.registerFactory(ComponentRedstonePostBundled.NAME, new ComponentRedstonePostBundled.Factory());

        ComponentRegistry.INSTANCE.registerFactory(ComponentTorch.NAME, new ComponentTorch.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentPlate.NAME, new ComponentPlate.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentPost.NAME, new ComponentPost.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentLamp.NAME, new ComponentLamp.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentDelay.NAME, new ComponentDelay.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentTimer.NAME, new ComponentTimer.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentRandom.NAME, new ComponentRandom.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentLever.NAME, new ComponentLever.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentPlateNC.NAME, new ComponentPlateNC.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentLampSegmented.NAME, new ComponentLampSegmented.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentConst.NAME, new ComponentConst.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentInventoryScanner.NAME, new ComponentInventoryScanner.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentCircuit.NAME, new ComponentCircuit.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentCircuitSlave.NAME, new ComponentCircuitSlave.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentButton.NAME, new ComponentButton.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentPunchcardReader.NAME, new ComponentPunchcardReader.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentLightSensor.NAME, new ComponentLightSensor.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentLampDriver.NAME, new ComponentLampDriver.Factory());
        ComponentRegistry.INSTANCE.registerFactory(ComponentNote.NAME, new ComponentNote.Factory());
        for (EnumMathComponent comp : EnumMathComponent.values()) {
            new ComponentMath.Factory(comp).register();
        }

        MinecraftForge.EVENT_BUS.register(proxy);
        proxy.preInit();

        log = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {

        NetworkHandler.init();
    }

    @EventHandler
    public void onServerStart(FMLServerStartingEvent event) {

        CommandTreeBase cmd = new CommandSCM();
        cmd.addSubcommand(new CommandExport());
        cmd.addSubcommand(new CommandImport());
        event.registerServerCommand(cmd);
    }

    @EventHandler
    public void onRemap(FMLMissingMappingsEvent event) {

        for (MissingMapping m : event.get()) {
            if (m.name.equals(MODID + ":pallette_and_brush") || m.name.equals(MODID + ":pallete_and_brush")) {
                m.remap(SCMItems.palette_and_brush);
            }
        }
    }

}
