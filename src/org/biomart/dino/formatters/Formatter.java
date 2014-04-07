package org.biomart.dino.formatters;

import java.io.OutputStream;

public interface Formatter<T> {

    public Formatter<T> setInput(T input);
    public Formatter<T> setSink(OutputStream out);
    public Formatter<T> format();
}
