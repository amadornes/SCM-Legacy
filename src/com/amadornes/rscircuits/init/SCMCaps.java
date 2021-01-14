package com.amadornes.rscircuits.init;

import java.util.concurrent.Callable;

import com.amadornes.rscircuits.api.IColorPalette;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class SCMCaps {

    @CapabilityInject(IColorPalette.class)
    public static final Capability<IColorPalette> PALETTE = null;

    public static void register() {

        CapabilityManager.INSTANCE.register(IColorPalette.class, new Capability.IStorage<IColorPalette>() {

            @Override
            public NBTBase writeNBT(Capability<IColorPalette> capability, IColorPalette instance, EnumFacing side) {

                return new NBTTagCompound();
            }

            @Override
            public void readNBT(Capability<IColorPalette> capability, IColorPalette instance, EnumFacing side, NBTBase nbt) {

            }
        }, new Callable<IColorPalette>() {

            @Override
            public IColorPalette call() throws Exception {

                return new IColorPalette() {

                    @Override
                    public EnumDyeColor getColor() {

                        return EnumDyeColor.SILVER;
                    }
                };
            }
        });
    }

}
