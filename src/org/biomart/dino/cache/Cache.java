package org.biomart.dino.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.biomart.dino.querybuilder.QueryBuilder;

public class Cache {

    private ConcurrentHashMap<String, String> c;
    private QueryBuilder qb;
    private String header, colDelim = "\t", lineDelim = "\n";
    private CacheCallback fn;
    
    /**
     * In the data returned by the query builder call the first column
     * is assumed to be the key column and the others data.
     * 
     * @param qb
     * @throws IOException
     */
    public Cache(QueryBuilder qb, CacheCallback fn) throws IOException {
        c = new ConcurrentHashMap<String, String>();
        this.qb = qb;
        this.fn = fn;
    }
    
    
    public Cache getResults() throws IOException {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            qb.getResults(out);
            
            String lines[] = out.toString().split(lineDelim);
            out.reset();
            
            if (lines.length > 0) {
                fn.setLine(lines[0]);
                header = fn.getValue();
                
                for (int i = 1, len = lines.length; i < len; ++i) {
                    String line = lines[i], k;
                    
                    fn.setLine(line);
                    k = fn.getKey();
                    if (!k.isEmpty())
                        c.put(k, fn.getValue());
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }

        return this;
    }
    
    
    public String getColDelim() {
        return colDelim;
    }


    public Cache setColDelim(String colDelim) {
        this.colDelim = colDelim;
        return this;
    }


    public String getLineDelim() {
        return lineDelim;
    }


    public Cache setLineDelim(String lineDelim) {
        this.lineDelim = lineDelim;
        return this;
    }


    public String get(String key) {
        return c.get(key);
    }
    
    
    public String getHeader() {
        return header;
    }
}



































































