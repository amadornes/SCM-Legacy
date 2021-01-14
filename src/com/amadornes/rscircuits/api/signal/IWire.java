package com.amadornes.rscircuits.api.signal;

import java.util.function.BiConsumer;

public interface IWire<P extends Enum<P>> extends IConnectable<P> {

    public IConnectable<P> visit(IConnectionType<?, ?> type, P from, BiConsumer<IConnectable<P>, P> consumer);

}
