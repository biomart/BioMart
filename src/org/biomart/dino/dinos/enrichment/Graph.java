package org.biomart.dino.dinos.enrichment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    
    List<Map<String, Object>> nodes;
    List<Map<String, Object>> edges;
    int nc, ec;
    
    Map<String, Object> n;
    
    public Graph(int nodesCapacity, int edgesCapacity) {
        nc = nodesCapacity;
        ec = edgesCapacity;
        clear();
    }
    
    public Graph(int nodesCapacity) {
        this(nodesCapacity, nodesCapacity);
    }
    
    public Graph() {
        this(200, 200);
    }
    
    
    public Graph clear() {
        nodes = new ArrayList<Map<String, Object>>(nc);
        edges = new ArrayList<Map<String, Object>>(ec);
        clearNode();
        return this;
    }
    
    
    private Graph clearNode() {
        n = null;
        return this;
    }
    
    
    public Graph addNode() {
        if (n != null) {
            nodes.add(n);
            clearNode();
        }
        
        return this;
    }
    
    
    public Graph addEdge(Map<String, Object> edge) {
        edges.add(edge);
        return this;
    }

    
    public List<Map<String, Object>> getNodes() {
        return nodes;
    }
    
    
    public List<Map<String, Object>> getEdges() {
        return edges;
    }
    
    
    public Graph initNode() {
        n = new HashMap<String, Object>();
        return this;
    }
    
    
    public Graph addNodeProp(String k, Object v) {
        n.put(k, v);
        return this;
    }
    
    
    public boolean containsNode(String key, Object value) {
        Object v = null;
        for (Map<String, Object> o : nodes) {
            if ((v = o.get(key)) != null && v.equals(value)) {
                return true;
            }
        }
        
        return false;
    }
}








































































































