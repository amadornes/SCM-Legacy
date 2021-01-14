package com.amadornes.rscircuits.item;

import java.util.List;

import org.lwjgl.input.Keyboard;

import com.amadornes.rscircuits.SCM;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemMonocle extends ItemArmor {

    public static ArmorMaterial material_monocle = EnumHelper.addArmorMaterial("monocle", SCM.MODID + ":monocle_magnifying", 1024,
            new int[] { 0, 0, 0, 0 }, 0, ArmorMaterial.IRON.getSoundEvent(), 0);

    @SideOnly(Side.CLIENT)
    public static KeyBinding keybind;
    public static boolean active = false;
    public static float fovMultipier = 1;
    public static float rate = 0.05F;

    public ItemMonocle() {

        super(material_monocle, 0, EntityEquipmentSlot.HEAD);
        setMaxDamage(0);
        setMaxStackSize(1);
        setCreativeTab(SCM.tab);
        setUnlocalizedName(SCM.MODID + ":monocle");
        MinecraftForge.EVENT_BUS.register(this);

    }

    @SideOnly(Side.CLIENT)
    public static void initClient() {

        keybind = new KeyBinding("key." + SCM.MODID + ":monocle", Keyboard.KEY_Z, "key.categories.gameplay");
        ClientRegistry.registerKeyBinding(keybind);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {

        return "item." + SCM.MODID + ":monocle" + (stack.getItemDamage() == 1 ? "_magnifying" : "");
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {

        if (stack.getItem() != this)
            return null;
        return SCM.MODID + ":textures/armor/monocle" + (stack.getItemDamage() == 1 ? "_magnifying" : "") + ".png";
    }

    @Override
    public boolean isRepairable() {

        return false;
    }

    @Override
    public boolean getHasSubtypes() {

        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void getSubItems(Item item, CreativeTabs tab, List l) {

        l.add(new ItemStack(this, 1, 0));
        l.add(new ItemStack(this, 1, 1));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {

        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null)
            return;
        ItemStack helm = player.inventory.armorItemInSlot(3);

        float amt = 2;
        if (active)
            fovMultipier = Math.max(fovMultipier - rate, 1 / amt);
        else
            fovMultipier = Math.min(fovMultipier + rate, 1);

        if (helm == null || helm.getItem() != this || helm.getItemDamage() != 1) {
            if (active) {
                active = false;
                Minecraft.getMinecraft().gameSettings.mouseSensitivity *= 2;
            }
            return;
        }

        if (keybind.isPressed()) {
            active = !active;
            Minecraft.getMinecraft().gameSettings.mouseSensitivity *= active ? 1D / amt : amt;
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onFOVUpdate(FOVUpdateEvent event) {

        event.setNewfov(event.getNewfov() * fovMultipier);
    }
}