package com.amadornes.rscircuits.api.signal;

public interface ISignalSupplier<P extends Enum<P>> extends IConnectable<P> {

    public <S extends ISignal<?>> S getSignal(IConnectionType<?, S> type, P from);

}
