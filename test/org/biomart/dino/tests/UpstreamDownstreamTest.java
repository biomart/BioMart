package org.biomart.dino.tests;

import static org.junit.Assert.*;

import org.biomart.dino.dinos.UpstreamDownstreamDino;
import org.junit.Before;
import org.junit.Test;

public class UpstreamDownstreamTest {

    
    final String in = "1:0:1,2:0:1:-1,3:0:1:+1", up = "100", down = "200";
    UpstreamDownstreamDino dino;
    
    @Before
    public void setUp() throws Exception {
        dino = new UpstreamDownstreamDino();
    }

    @Test
    public void downstreamUpstreamTest() {
        assertEquals("1:-200:101,2:-100:201:-1,3:-200:101:+1", dino.upstreamDownstream(in, up, down));
    }
    
    @Test
    public void onlyUpstreamTest() {
        assertEquals("1:0:101,2:-100:1:-1,3:0:101:+1", dino.upstreamDownstream(in, up, ""));
        assertEquals("1:0:101,2:-100:1:-1,3:0:101:+1", dino.upstreamDownstream(in, up, null));
    }
    
    @Test
    public void onlyDownstreamTest() {
        assertEquals("1:-200:1,2:0:201:-1,3:-200:1:+1", dino.upstreamDownstream(in, "", down));
        assertEquals("1:-200:1,2:0:201:-1,3:-200:1:+1", dino.upstreamDownstream(in, null, down));
    }

}
