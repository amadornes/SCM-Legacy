package com.amadornes.rscircuits.init;

import java.util.Arrays;

import com.amadornes.rscircuits.item.EnumResourceType;
import com.amadornes.rscircuits.item.ItemBlueprint;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

public class SCMRecipes {

    public static void registerRecipes() {

        ItemStack redstone_pile = new ItemStack(SCMItems.resource, 1, EnumResourceType.PILE_OF_REDSTONE.ordinal());
        ItemStack glowstone_pile = new ItemStack(SCMItems.resource, 1, EnumResourceType.PILE_OF_GLOWSTONE.ordinal());
        ItemStack plate = new ItemStack(SCMItems.resource, 8, EnumResourceType.TINY_PLATE.ordinal());
        String redstone = "dustRedstone";
        String glowstone_dust = "dustGlowstone";
        String obsidian = "obsidian";
        String glowstone = "glowstone";
        ItemStack lamp = new ItemStack(Blocks.REDSTONE_LAMP);
        ItemStack eye = new ItemStack(Items.ENDER_EYE);
        String quartz = "blockQuartz";
        ItemStack slab = new ItemStack(Blocks.STONE_SLAB);
        String iron = "ingotIron";
        String gold = "ingotGold";
        String glass_pane = "paneGlass";
        ItemStack wool = new ItemStack(Blocks.WOOL, 1, OreDictionary.WILDCARD_VALUE);
        String string = "string";
        String stick = "stickWood";

        registerShapeless(new ItemStack(SCMItems.resource, 9, EnumResourceType.PILE_OF_REDSTONE.ordinal()), redstone);
        registerShapeless(new ItemStack(Items.REDSTONE), redstone_pile, redstone_pile, redstone_pile, redstone_pile, redstone_pile,
                redstone_pile, redstone_pile, redstone_pile, redstone_pile);

        registerShapeless(plate, new ItemStack(SCMItems.circuit));
        registerShapeless(new ItemStack(SCMItems.circuit), plate, plate, plate, plate, plate, plate, plate, plate);

        registerShaped(new ItemStack(SCMItems.resource, 8, EnumResourceType.TINY_PLATE_NONCOND.ordinal()), "ppp", "pop", "ppp", 'p', plate,
                'o', obsidian);

        registerShaped(new ItemStack(SCMItems.resource, 6, EnumResourceType.ENERGIZED_GLOWSTONE.ordinal()), "ggg", "ooo", 'g', glowstone,
                'o', obsidian);
        registerShaped(new ItemStack(SCMItems.resource, 6, EnumResourceType.ENDER_PULSAR.ordinal()), "eee", "ooo", 'e', eye, 'o', obsidian);
        registerShaped(new ItemStack(SCMItems.resource, 6, EnumResourceType.QUARTZ_RESONATOR.ordinal()), "qqq", "ooo", 'q', quartz, 'o',
                obsidian);
        registerShaped(new ItemStack(SCMItems.resource, 6, EnumResourceType.TINY_LAMP.ordinal()), "lll", "ooo", 'l', lamp, 'o', obsidian);

        registerShapeless(new ItemStack(Items.GLOWSTONE_DUST), glowstone_pile, glowstone_pile, glowstone_pile, glowstone_pile);
        registerShapeless(new ItemStack(SCMItems.resource, 4, EnumResourceType.PILE_OF_GLOWSTONE.ordinal()), glowstone_dust);

        registerShapeless(new ItemStack(SCMItems.resource, 2, EnumResourceType.PILE_OF_GLOWING_REDSTONE.ordinal()), redstone_pile,
                glowstone_pile);

        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.REDSTONE_STICK.ordinal()), redstone_pile, stick);

        registerShaped(new ItemStack(SCMItems.screwdriver), "  i", " i ", "l  ", 'i', new ItemStack(Items.IRON_INGOT), 'l',
                new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()));

        registerShaped(new ItemStack(SCMItems.redwire, 12), "rir", "rir", "rir", 'i', new ItemStack(Items.IRON_INGOT), 'r', redstone_pile);

        registerShaped(new ItemStack(SCMItems.circuit, 12), "sss", "sss", 's', slab);

        registerShaped(new ItemStack(SCMItems.monocle, 1, 0), " i ", "ipi", " i ", 'i', iron, 'p', glass_pane);
        registerShaped(new ItemStack(SCMItems.monocle, 1, 1), " g ", "gpg", " g ", 'g', gold, 'p', glass_pane);

        registerShaped(new ItemStack(SCMItems.resource, 3, EnumResourceType.TINY_BUNDLED_WIRE.ordinal()), "wrw", "srs", "wrw", 'w', wool,
                'r', redstone_pile, 's', string);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.BUNDLED_STICK.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_BUNDLED_WIRE.ordinal()), stick);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_LAMP_SEGMENTED.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_LAMP.ordinal()), obsidian, glowstone_pile);

        registerShaped(new ItemStack(SCMItems.resource, 1, EnumResourceType.PAINT_BRUSH.ordinal()), "  w", " p ", "s  ", 'w', wool, 'p',
                "plankWood", 's', stick);
        registerShaped(new ItemStack(SCMItems.resource, 1, EnumResourceType.PALETTE.ordinal()), " p ", "psp", " p ", 'p',
                new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_PLATE.ordinal()), 's', "stone");
        registerShapeless(new ItemStack(SCMItems.palette_and_brush),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.PALETTE.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.PAINT_BRUSH.ordinal()));

        registerShaped(new ItemStack(SCMItems.resource, 3, EnumResourceType.ADDER.ordinal()), "ppp", "rrr", "ppp", 'p',
                new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_PLATE.ordinal()), 'r', redstone_pile);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.SUBTRACTOR.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.ADDER.ordinal()));
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.MULTIPLIER.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.SUBTRACTOR.ordinal()));
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.DIVIDER.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.MULTIPLIER.ordinal()));
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.ADDER.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.DIVIDER.ordinal()));

        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.CONSTANT.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.ADDER.ordinal()), Blocks.REDSTONE_TORCH);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.CONSTANT.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.SUBTRACTOR.ordinal()), Blocks.REDSTONE_TORCH);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.CONSTANT.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.MULTIPLIER.ordinal()), Blocks.REDSTONE_TORCH);
        registerShapeless(new ItemStack(SCMItems.resource, 1, EnumResourceType.CONSTANT.ordinal()),
                new ItemStack(SCMItems.resource, 1, EnumResourceType.DIVIDER.ordinal()), Blocks.REDSTONE_TORCH);

        registerShaped(new ItemStack(SCMItems.resource, 3, EnumResourceType.INV_SCANNER.ordinal()), "rcr", "ppp", 'p',
                new ItemStack(SCMItems.resource, 1, EnumResourceType.TINY_PLATE.ordinal()), 'r', redstone_pile, 'c', "chest");

        registerShaped(new ItemStack(SCMItems.blueprint, 1, 0), "lll", "lml", "lll", 'l',
                new ItemStack(Items.DYE, 1, EnumDyeColor.BLUE.getDyeDamage()), 'm', Items.MAP);
        registerShaped(new ItemStack(SCMItems.blueprint, 1, 2), "drd", "rmr", "drd", 'd',
                new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage()), 'm', Items.MAP, 'r', Items.REDSTONE);

        ItemStack bp1 = new ItemStack(SCMItems.blueprint, 1, 4);
        bp1.setTagCompound(ItemBlueprint.BLUEPRINTS.get(0).copy());
        String name = bp1.getTagCompound().getString("name");
        bp1.setStackDisplayName(TextFormatting.RESET + bp1.getDisplayName() + (name.length() == 0 ? "" : ": " + name));
        registerShaped(bp1, "b", 'b', new ItemStack(SCMItems.blueprint, 1, 0));
        GameRegistry.addRecipe(new RecipeBlueprintCycle());

        registerShaped(new ItemStack(SCMItems.multimeter), "pgp", "pbp", "rpr", 'p', plate, 'g', "blockGlass", 'b', Blocks.STONE_BUTTON,
                'r', redstone_pile);
        registerShaped(new ItemStack(SCMItems.squeegee), "p  ", " p ", "p p", 'p', plate);
        registerShaped(new ItemStack(SCMBlocks.update_detector), "r ", " p", 'p', Blocks.PISTON, 'r', "blockRedstone");
        registerShaped(new ItemStack(SCMBlocks.update_detector), " r", "p ", 'p', Blocks.PISTON, 'r', "blockRedstone");
        registerShaped(new ItemStack(SCMBlocks.update_detector), "r", " ", "p", 'p', Blocks.PISTON, 'r', "blockRedstone");

        GameRegistry.addRecipe(new RecipeRedprintCircuit());
    }

    private static void registerShaped(ItemStack out, Object... in) {

        GameRegistry.addRecipe(new ShapedOreRecipe(out, in));
    }

    private static void registerShapeless(ItemStack out, Object... in) {

        GameRegistry.addRecipe(new ShapelessOreRecipe(out, in));
    }

    private static class RecipeRedprintCircuit implements IRecipe {

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {

            return getCraftingResult(inv) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {

            ItemStack blueprint = null, circuit = null;

            int size = inv.getSizeInventory();
            for (int i = 0; i < size; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack != null) {
                    if (blueprint == null && stack.getItem() == SCMItems.blueprint && stack.getItemDamage() == 3) {
                        blueprint = stack;
                    } else if (circuit == null && stack.getItem() == SCMItems.circuit && (!stack.hasTagCompound()
                            || stack.getTagCompound().getKeySet().containsAll(Arrays.asList("components", "updates")))) {
                        circuit = stack;
                    } else {
                        return null;
                    }
                }
            }

            if (blueprint != null && circuit != null) {
                circuit = circuit.copy();
                circuit.stackSize = 1;
                circuit.setTagCompound(blueprint.getTagCompound());
                return circuit;
            }

            return null;
        }

        @Override
        public int getRecipeSize() {

            return 2;
        }

        @Override
        public ItemStack getRecipeOutput() {

            return new ItemStack(SCMItems.circuit);
        }

        @Override
        public ItemStack[] getRemainingItems(InventoryCrafting inv) {

            int size = inv.getSizeInventory();
            for (int i = 0; i < size; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack != null && stack.getItem() == SCMItems.blueprint && stack.getItemDamage() == 3) {
                    ItemStack[] stacks = new ItemStack[size];
                    stacks[i] = new ItemStack(SCMItems.blueprint, stack.stackSize, 2);
                    return stacks;
                }
            }
            return new ItemStack[inv.getSizeInventory()];
        }

    }

    private static class RecipeBlueprintCycle implements IRecipe {

        @Override
        public boolean matches(InventoryCrafting inv, World worldIn) {

            return getCraftingResult(inv) != null;
        }

        @Override
        public ItemStack getCraftingResult(InventoryCrafting inv) {

            ItemStack blueprint = null;

            int size = inv.getSizeInventory();
            for (int i = 0; i < size; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack != null) {
                    if (blueprint == null && stack.getItem() == SCMItems.blueprint && stack.getItemDamage() == 4) {
                        blueprint = stack;
                    } else {
                        return null;
                    }
                }
            }

            if (blueprint != null) {
                int i = blueprint.getTagCompound().getInteger("blueprintID") + 1;
                if (i >= ItemBlueprint.BLUEPRINTS.size()) {
                    i = 0;
                }
                ItemStack is = new ItemStack(SCMItems.blueprint, 1, 4);
                is.setTagCompound(ItemBlueprint.BLUEPRINTS.get(i).copy());
                String name = is.getTagCompound().getString("name");
                is.setStackDisplayName(TextFormatting.RESET + is.getDisplayName() + (name.length() == 0 ? "" : ": " + name));
                return is;
            }

            return null;
        }

        @Override
        public int getRecipeSize() {

            return 1;
        }

        @Override
        public ItemStack getRecipeOutput() {

            return new ItemStack(SCMItems.blueprint, 1, 4);
        }

        @Override
        public ItemStack[] getRemainingItems(InventoryCrafting inv) {

            return new ItemStack[inv.getSizeInventory()];
        }

    }

}
