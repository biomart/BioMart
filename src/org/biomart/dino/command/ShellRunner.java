package org.biomart.dino.command;

import java.io.File;
import java.io.IOException;

import org.biomart.common.resources.Log;

public abstract class ShellRunner {

    protected ShellCommand cmd;
    protected int nIter = 0, maxWait = 15000, errorResult = -42,
            waitTime = 200;
    protected File dir;
//    static File dir;
//    static {
//
//        dir = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + ".enrichment");
//        // If it went wrong it'll raise an error
//        dir.mkdir();
//        
//    }

    public ShellRunner setCmd(ShellCommand cmd) {
        this.cmd = cmd;
        return this;
    }

    public ShellRunner run() throws ShellException {

        Runtime rt = Runtime.getRuntime();
        Process pr = null;
        String command = cmd.build();

        Log.debug(this.getClass().getName() + "#run() running command "+ command);
        try {
            pr = rt.exec(command, null, dir);
    
            while (true) {
                Thread.sleep(waitTime);
                Log.debug(this.getClass().getName() + "#run() iteration");
                
                try {
                    // If it hasn't finished yet, it throws an exception.
                    errorResult = pr.exitValue();
                    // If it reaches this point, it has finished.
                    break;
                } catch (IllegalThreadStateException e) {
                    if (++nIter < maxWait)
                        continue;
                    break;
                }
            }
        
        } catch (InterruptedException e1) {
            throw new ShellException(e1.getMessage());
        } catch (IOException e1) {
            throw new ShellException(e1.getMessage());
        }

        if (errorResult != 0) {
            throw new ShellException(errorResult);
        }

        return this;
    }
    
    abstract public Object getResults() throws IOException;
    
    public ShellRunner setWorkingDir(File dir) {
        this.dir = dir;
        return this;
    }
}
