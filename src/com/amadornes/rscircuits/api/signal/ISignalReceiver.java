package com.amadornes.rscircuits.api.signal;

import java.util.EnumSet;

public interface ISignalReceiver<P extends Enum<P>> extends IConnectable<P> {

    public <S extends ISignal<?>> void onSignalReceived(IConnectionType<?, S> type, EnumSet<P> from, S signal);

    public default <S extends ISignal<?>> void onPropagationFinished(IConnectionType<?, S> type, EnumSet<P> from, S signal) {

    }

}
