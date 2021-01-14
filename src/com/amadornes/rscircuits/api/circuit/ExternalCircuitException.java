package com.amadornes.rscircuits.api.circuit;

public class ExternalCircuitException extends RuntimeException {

    private static final long serialVersionUID = -6838481765983077058L;

    private final Throwable t;

    public ExternalCircuitException(Throwable t) {

        super(t);
        this.t = t;
    }

    public Throwable getOriginalThrowable() {

        return t;
    }

}
