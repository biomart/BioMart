package org.biomart.dino.processor;

public class ProcessorInstantiationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProcessorInstantiationException(Throwable t) {
        super("The required processor could not be initialized", t);
    }

}
