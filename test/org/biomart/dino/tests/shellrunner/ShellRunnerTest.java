package org.biomart.dino.tests.shellrunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.biomart.dino.command.ShellCommand;
import org.biomart.dino.command.ShellException;
import org.biomart.dino.command.ShellRunner;
import org.biomart.dino.tests.fixtures.RunnerTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ShellRunnerTest {

    ShellCommand cmd;
    ShellRunner rn;
    final String fName = "shell_runner_test_file";
    final String dir = System.getProperty("user.dir");
    File workDir;
    
    @Before
    public void setUp() {
        cmd = mock(ShellCommand.class);
        when(cmd.build()).thenReturn("touch "+ fName);
        rn = new RunnerTest();
        workDir  = new File(dir);
        rn.setCmd(cmd);
        rn.setWorkingDir(workDir);
    }
    
    @Test
    public void buildTest() throws ShellException {
        
        rn.run();
        
        verify(cmd).build();
    }
    
    @Test
    public void commandTest() throws ShellException {
        rn.run();
        
        assertTrue(new File(dir, fName).exists());
    }
    
    @Test(expected = ShellException.class)
    public void commandFailTest() throws ShellException {
        when(cmd.build()).thenReturn("sleep 400; cmdthatdoesnotexist");
        
        rn.run();
        
    }
    
    @After
    public void tearDown() {
        File f = new File(dir, fName);
        if (f.exists()) {
            f.delete();
        }
    }
}
