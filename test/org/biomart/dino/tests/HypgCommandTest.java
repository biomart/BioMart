package org.biomart.dino.tests;

import static org.junit.Assert.*;

import java.io.File;

import org.biomart.dino.command.HypgCommand;
import org.junit.Before;
import org.junit.Test;

public class HypgCommandTest {

    HypgCommand cmd;
    final File[] fs = new File[] {
            new File("ann/path"),
            new File("background/path"),
            new File("sets/path"),
            new File("bin/path") };

    String cutoff = "0.4";

    @Before
    public void setUp() {
        cmd = new HypgCommand();
        cmd.setAnnotations(fs[0]).setBackground(fs[1]).setSets(fs[2])
                .setCutoff(cutoff).setCmdBinPath(fs[3]);
    }

    @Test
    public void test() {
        String r = cmd.build();
        assertTrue(r.contains("-g " + fs[1].getPath()));
        assertTrue(r.contains("-s " + fs[2].getPath()));
        assertTrue(r.contains("-a " + fs[0].getPath()));
        assertTrue(r.contains("-c " + cutoff));
    }
}
