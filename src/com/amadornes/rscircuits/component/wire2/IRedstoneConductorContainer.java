package com.amadornes.rscircuits.component.wire2;

import com.amadornes.rscircuits.api.component.EnumCircuitSide;
import com.amadornes.rscircuits.api.component.EnumComponentSlot;
import com.amadornes.rscircuits.api.component.IRedstoneConductor;
import com.amadornes.rscircuits.api.component.IRedstoneConductor.EnumConnectionType;

public interface IRedstoneConductorContainer {

    public EnumComponentSlot getSlot();

    public void onConnect(EnumCircuitSide side, IRedstoneConductor conductor, EnumConnectionType type);

    public void onDisconnect(EnumCircuitSide side);

}
