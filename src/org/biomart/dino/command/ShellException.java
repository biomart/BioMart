package org.biomart.dino.command;

public class ShellException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public ShellException(String message) {
        super(message);
    }
    
    public ShellException(String mex, int errorValue) {
        this(mex + " value of error : "+ errorValue);
    }
    
    public ShellException(int errorValue) {
        super("Process didn't terminate or retured a value of error: "
                + errorValue);
    }

}
