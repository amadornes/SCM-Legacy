package com.amadornes.rscircuits.api.component;

import net.minecraft.item.EnumDyeColor;

public interface IPaintableComponent extends IComponent {

    public boolean paint(EnumDyeColor color);

}
