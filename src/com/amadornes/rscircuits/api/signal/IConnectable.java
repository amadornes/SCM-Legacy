package com.amadornes.rscircuits.api.signal;

import java.util.Optional;

public interface IConnectable<P extends Enum<P>> {

    public <C extends IConnection> Optional<C> getConnection(IConnectionType<C, ?> type, P on);

    public default EnumConnectionState getConnectionState(IConnectionType<?, ?> type, P on) {

        return EnumConnectionState.UNKNOWN;
    }

}
