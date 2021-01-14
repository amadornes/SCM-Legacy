package com.amadornes.rscircuits.item;

import com.amadornes.rscircuits.SCM;
import com.amadornes.rscircuits.api.IColorPalette;
import com.amadornes.rscircuits.init.SCMCaps;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ItemPaletteAndBrush extends Item {

    public ItemPaletteAndBrush() {

        setUnlocalizedName(SCM.MODID + ":palette_and_brush");
        setMaxStackSize(1);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {

        return true;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {

        return new ICapabilityProvider() {

            private final IColorPalette palette = new IColorPalette() {

                @Override
                public EnumDyeColor getColor() {

                    NBTTagCompound tag = stack.getTagCompound();
                    if (tag == null) {
                        stack.setTagCompound(tag = new NBTTagCompound());
                    }
                    return tag.hasKey("color") ? EnumDyeColor.byMetadata(tag.getInteger("color")) : EnumDyeColor.WHITE;
                }
            };

            @Override
            public boolean hasCapability(Capability<?> capability, EnumFacing facing) {

                return capability == SCMCaps.PALETTE && facing == null;
            }

            @SuppressWarnings("unchecked")

            @Override
            public <T> T getCapability(Capability<T> capability, EnumFacing facing) {

                return capability == SCMCaps.PALETTE && facing == null ? (T) palette : null;
            }
        };
    }

}
