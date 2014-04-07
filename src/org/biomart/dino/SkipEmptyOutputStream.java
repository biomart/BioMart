package org.biomart.dino;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SkipEmptyOutputStream extends FilterOutputStream {

    public SkipEmptyOutputStream(OutputStream out) {
        super(out);
    }
    
    public void write(byte[] b) throws IOException {
        String s = new String(b);
        String[] tk = s.split("\\t");
        if (tk.length > 1) {
            out.write(b);
        }
    }

}
