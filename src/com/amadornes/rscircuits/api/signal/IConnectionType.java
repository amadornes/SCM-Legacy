package com.amadornes.rscircuits.api.signal;

import com.amadornes.rscircuits.api.signal.IConnection.EnumRedstoneConnection;
import com.amadornes.rscircuits.api.signal.IConnection.IBundledConnection;
import com.amadornes.rscircuits.api.signal.ISignal.IBundledSignal;
import com.amadornes.rscircuits.api.signal.ISignal.IRedstoneSignal;

public interface IConnectionType<C extends IConnection, S extends ISignal<?>> {

    public static final IConnectionType<EnumRedstoneConnection, IRedstoneSignal> REDSTONE = new IConnectionType<EnumRedstoneConnection, IRedstoneSignal>() {
    };
    public static final IConnectionType<IBundledConnection, IBundledSignal> BUNDLED = new IConnectionType<IBundledConnection, IBundledSignal>() {
    };

}
