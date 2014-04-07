package org.biomart.dino.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.dino.command.HypgCommand;
import org.biomart.dino.command.ShellRunner;
import org.biomart.dino.dinos.Dino;
import org.biomart.dino.dinos.enrichment.EnrichmentDino;
import org.biomart.dino.dinos.enrichment.Graph;
import org.biomart.dino.dinos.enrichment.GuiResponseCompiler;
import org.biomart.dino.querybuilder.QueryBuilder;
import org.junit.Before;
import org.junit.Test;


public class EnrichmentDinoTest {

    EnrichmentDino dino;
    List<Map<String, String>> nodeList;
    Map<String, List<Map<String, Object>>> links;
    final String[] nks = new String[] { "pro1", "pro2" },
             nvs = new String[] { "val1", "val2" },
             anns = new String[] { "ann1", "ann2" };
    final int[][] edges = new int[][] { {0,1}, {0,2} };
    Map<String, Integer> l = new HashMap<String, Integer>();
    final String s = "source", t = "target";
    
    Map<String, Object> scope;
    
    @Before
    public void setUp() throws Exception {
        dino = new EnrichmentDino(
                mock(HypgCommand.class),
                mock(ShellRunner.class),
                mock(QueryBuilder.class),
                TestSupport.fixtureDir() + TestSupport.sep + "EnrichmentDino.json",
                mock(GuiResponseCompiler.class),
                new Graph()
        );
        
        nodeList = new ArrayList<Map<String, String>>();
        scope = new HashMap<String, Object>();
        Map<String, Map<String, Object>> tabs = new HashMap<String, Map<String, Object>>();
        links = new HashMap<String, List<Map<String, Object>>>();
        links.put(anns[0], new ArrayList<Map<String, Object>>());
        links.put(anns[1], new ArrayList<Map<String, Object>>());
        tabs.put(anns[0], new HashMap<String, Object>());
        tabs.put(anns[1], new HashMap<String, Object>());
        tabs.get(anns[0]).put("links", links.get(anns[0]));
        tabs.get(anns[1]).put("links", links.get(anns[1]));
        scope.put("tabs", tabs);
        scope.put("nodes", nodeList);
        
        
//        nodeList = new ArrayList<Map<String, String>>();
//        for (int j = 0; j < 2; ++j) {
//            for (int i = 0; i < nks.length; ++i) {
//                String k = nks[i], v = nvs[i];
//                Map<String, String> m = new HashMap<String, String>();
//                m.put(k, v);
//                nodeList.add(m);
//            }
//        }
//        
//        
//        links = new HashMap<String, List<Map<String, Object>>>();
//        links.put(anns[0], new ArrayList<Map<String, Object>>());
//        links.put(anns[1], new ArrayList<Map<String, Object>>());
//        for (int i = 0; i < edges.length; ++i) {
//            
//            for (int j = 0; j < anns.length; ++j) {
//                int[] l = edges[i];
//                Map<String, Object> al = new HashMap<String, Object>();
//                al.put(s, l[0]); al.put(t, l[1]);
//                links.get(anns[i]).add(al);
//            }
//        }
//        
//        scope = new HashMap<String, Object>();
//        Map<String, Map<String, Object>> tabs = new HashMap<String, Map<String, Object>>();
//        
//        for (int i = 0; i < anns.length; ++i) {
//            List<Object> lst = new ArrayList<Object>();
//            tabs.put(anns[i], new HashMap<String, Object>());
//            tabs.get(anns[i]).put("links", lst);
//            for (int j = 0; j < this.edges.length; ++j) {
//                HashMap<String, Integer> l = new HashMap<String, Integer>();
//                lst.add(l);
//                l.put(s, edges[j][0]);
//                l.put(t, edges[j][1]);
//            }
//        }
//        
//        scope.put("nodes", nodeList);
//        
//        scope.put("tabs", tabs);
        
    }

    @Test
    public void test() {
        assertTrue("EnrichmentDino implements Dino", dino instanceof Dino);
    }
    
    @Test
    public void jsonScopeTest() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method method = dino.getClass().getDeclaredMethod("getScope", List.class, Map.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) method.invoke(dino, nodeList, links);
        
        assertEquals(scope, r);
    }
    
    @Test
    public void mkJsonTest() throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String s = "{\"nodes\":[],\"tabs\":{\"ann2\":{\"links\":[]},\"ann1\":{\"links\":[]}}}";
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
//            Field nodes = dino.getClass().getDeclaredField("nodes");
//            nodes.setAccessible(true);
//            nodes.set(dino, nodeList);
//            Field edges = dino.getClass().getDeclaredField("links");
//            edges.setAccessible(true);
//            edges.set(dino, links);
            Method method = dino.getClass().getDeclaredMethod("mkJson", List.class, Map.class, OutputStream.class);
            method.setAccessible(true);

            method.invoke(dino, nodeList, links, o);
            
            assertEquals(s, o.toString());
        } finally {
            o.close();
        }
    }

    
    @Test
    public void sendGuiResponseTest() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchMethodException, IOException, InvocationTargetException {

        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            Field nodes = dino.getClass().getDeclaredField("nodes");
            nodes.setAccessible(true);
            nodes.set(dino, nodeList);
            Field edges = dino.getClass().getDeclaredField("links");
            edges.setAccessible(true);
            edges.set(dino, links);
            Method method = dino.getClass().getDeclaredMethod("sendGuiResponse", OutputStream.class);
            method.setAccessible(true);

            try {
                method.invoke(dino, o);
            } catch(InvocationTargetException e) {
                e.getTargetException().printStackTrace();
                throw e;
            }
            
            String s = o.toString();
            assertTrue(s.contains("html"));
            assertTrue(s.contains("body"));
            assertTrue(s.contains("nodes"));
            assertTrue(s.contains("links"));
            assertTrue(s.contains("tabs"));
            assertTrue(s.contains("ann1"));
            assertTrue(s.contains("ann2"));
            assertTrue(s.contains("links"));
        } finally {
            o.close();
        }
    }

}






























































