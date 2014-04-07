package org.biomart.dino.tests.shellrunner;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.biomart.dino.command.HypgRunner;
import org.biomart.dino.command.ShellRunner;
import org.biomart.dino.tests.TestSupport;
import org.junit.Before;
import org.junit.Test;

public class HypgRunnerTest {

    ShellRunner rn;
    
    @Before
    public void setUp() {
        rn = new HypgRunner();
        rn.setWorkingDir(new File(TestSupport.fixtureDir()));
    }
    
    @Test
    public void test() throws IOException {
        @SuppressWarnings("unchecked")
        List<List<String>> res = (List<List<String>>) rn.getResults();
        List<String> r;
        
        assertEquals(3, res.size());
        
        r = res.get(0);
        assertEquals(r.size(), 4);
        assertEquals("c", r.get(0)); assertEquals("0.2", r.get(1)); assertEquals("0.1", r.get(2));
        assertEquals("g0,g1", r.get(3));
        
        r = res.get(1);
        assertEquals(4, r.size());
        assertEquals("b", r.get(0)); assertEquals("0.5", r.get(1)); assertEquals("0.4", r.get(2));
        assertEquals("g0", r.get(3));
        
        r = res.get(2);
        assertEquals(3, r.size());
        assertEquals("a", r.get(0)); assertEquals("0.6", r.get(1)); assertEquals("0.5", r.get(2));
    }
    
}
