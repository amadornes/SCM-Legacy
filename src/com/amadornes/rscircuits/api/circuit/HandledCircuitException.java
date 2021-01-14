package com.amadornes.rscircuits.api.circuit;

public class HandledCircuitException extends RuntimeException {

    private static final long serialVersionUID = -6838481765983077058L;

    public HandledCircuitException(Throwable t) {

        super(t);
    }

}
