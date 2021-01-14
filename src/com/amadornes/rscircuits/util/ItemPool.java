package com.amadornes.rscircuits.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;

public class ItemPool {

    private final Map<StackData, Integer> items;

    public ItemPool(ItemPool pool) {

        this.items = new HashMap<StackData, Integer>(pool.items);
    }

    public ItemPool() {

        this.items = new HashMap<StackData, Integer>();
    }

    public void add(ItemStack stack) {

        StackData data = new StackData(stack);
        items.merge(data, stack.stackSize, (a, b) -> (a == null ? 0 : a) + b);
    }

    public EnumActionResult remove(ItemStack stack) {

        StackData data = new StackData(stack);
        if (items.containsKey(stack)) {
            int size = items.merge(data, stack.stackSize, (a, b) -> (a == null ? 0 : a) - b);
            if (size <= 0) {
                items.remove(data);
            }
            return size >= 0 ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
        }
        return EnumActionResult.PASS;
    }

    public Pair<ItemStack, Integer> getFirstMissing(ItemPool pool) {

        for (Entry<StackData, Integer> e : pool.items.entrySet()) {
            Integer amt = items.get(e.getKey());
            if (amt == null || amt < e.getValue()) {
                return Pair.of(e.getKey().stack, e.getValue() - (amt == null ? 0 : amt));
            }
        }
        return null;
    }

    public boolean containsAll(ItemPool pool) {

        return getFirstMissing(pool) == null;
    }

    public List<ItemStack> getItems() {

        List<ItemStack> list = new ArrayList<ItemStack>();
        items.forEach((d, s) -> {
            ItemStack stack = d.stack;
            do {
                stack = stack.copy();
                stack.stackSize = Math.min(s, 64);
                list.add(stack);
                s -= 64;
            } while (s > 0);
        });
        return list;
    }

    public Map<StackData, Integer> getRawItems() {

        return items;
    }

    public class StackData {

        private final ItemStack stack;

        public StackData(ItemStack stack) {

            this.stack = stack;
        }

        public ItemStack getStack() {

            return stack;
        }

        @Override
        public int hashCode() {

            final int prime = 31;
            int result = 1;
            result = prime * result + stack.getItem().hashCode();
            result = prime * result + stack.getItemDamage();
            result = prime * result + ((stack.getTagCompound() == null) ? 0 : stack.getTagCompound().hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            StackData other = (StackData) obj;
            return other.stack.getItem() == stack.getItem() && other.stack.getItemDamage() == stack.getItemDamage()
                    && (stack.hasTagCompound()
                            ? (other.stack.hasTagCompound() ? stack.getTagCompound().equals(other.stack.getTagCompound()) : false)
                            : !other.stack.hasTagCompound());
        }

    }

}
