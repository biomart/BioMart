package org.biomart.processors.formatters;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.biomart.common.constants.OutputConstants;

/**
 *
 * @author jbaran
 */
public abstract class TSVFormatter extends FilterOutputStream implements OutputConstants {
    // Processing fields:
    //   Invariant: column will appear in ascending order
    public abstract void writeField(int column, String value) throws IOException;
    
    // Handling line boundaries:
    public abstract void writeStartOfLine() throws IOException;
    public abstract void writeEndOfLine() throws IOException;
    
    // Overwrite me, if you like to transpose single characters:
    public String transposeCharacter(int b) {
        return new String(new byte[] { (byte)b });
    }

    private int column = 0;
    private boolean startOfLine = true;
    private StringBuilder fieldContents = new StringBuilder();
    
    public TSVFormatter(OutputStream out) {
        super(out);
    }
    
    @Override
    public void write(int b) throws IOException {
        if (startOfLine) {
            writeStartOfLine();
            startOfLine = false;
        }
        
        switch(b) {
        case NEWLINE:
            writeField(column, fieldContents.toString());
            column = 0;
            fieldContents = new StringBuilder();
            writeEndOfLine();
            startOfLine = true;
            break;
        case TAB:
            writeField(column, fieldContents.toString());
            column++;
            fieldContents = new StringBuilder();
            break;
        default:
            fieldContents.append(transposeCharacter(b));
            break;
        }
    }
    
    @Override
    public void close() throws IOException {
        super.close();
    }
}
