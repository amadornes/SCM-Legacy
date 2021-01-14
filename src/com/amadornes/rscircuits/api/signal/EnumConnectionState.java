package com.amadornes.rscircuits.api.signal;

public enum EnumConnectionState {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN;

    public boolean isConnected() {

        return this == EnumConnectionState.CONNECTED;
    }

    public boolean isDisconnected() {

        return this == EnumConnectionState.DISCONNECTED;
    }

}
