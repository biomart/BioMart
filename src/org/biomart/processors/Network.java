package org.biomart.processors;

import org.biomart.common.exceptions.BioMartQueryException;
import org.biomart.common.resources.Log;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.biomart.processors.annotations.FieldInfo;
import org.biomart.processors.fields.IntegerField;
import org.biomart.queryEngine.Query;


/**
 * Author: lpand AKA Luca Pandini
 *
 * ProcessorImpl already defined the proper ContentType (text/plain)
 */
public class Network extends ProcessorImpl {
	static final HashMap<EntryKey, Double> h = new HashMap<EntryKey, Double>();
	
    private SumFn cb;
    @FieldInfo(clientDefined=true, required=true)
    private IntegerField nqueries = new IntegerField();
    private static int queryCount = 0;
    private boolean goodToGo = false, header = false;

    public Network() {
        Log.debug("Network::Network invoked");
        cb = new SumFn();
    }

    @Override
    public void setQuery(Query query) {
        super.setQuery(query);
    }

    @Override
    public Function getCallback() {
        return cb;
    }
    
    @Override
    public void done() {
        goodToGo = ++queryCount == nqueries.getValue().intValue();
        if (goodToGo)
        		cb.send();
        Log.debug("Network#done invoked, queryCount = "+ queryCount + ". nqueries = "+ nqueries.getValue());
        Log.debug("Network#done goodToGo = "+ goodToGo);
    }
    

    protected final class EntryKey {
    		
    		public String g0, g1;
    		
    		public EntryKey(String g0, String g1) {
    			this.g0 = g0;
    			this.g1 = g1;
    		}
    		
    		public EntryKey(String[] gs) {
    			// assume gs != null
    			this(gs[0], gs[1]);
    		}
    		
    		public boolean equals(Object e) {
    			if (e instanceof EntryKey) {
    				EntryKey k = (EntryKey)e;
    				return g0 == k.g0 && g1 == k.g1;
    			}
    			
    			return false;
    		}
    		
    		public int hashCode() {
    			return g0.hashCode() + g1.hashCode();
    		}
    		
    		public String toString(char delim) {
    			return Joiner.on(delim).join(g0, g1);
    		}
    }

    protected final class SumFn implements Function<String[],Boolean> {
    		static final int FLUSH_INTERVAL = 50;
    		static final char DELIMITER = '\t';
        int count = 0, linesReceived = 0, linesSent = 0;
        
        public SumFn() {
        		super();
        }

                
        @Override
        public Boolean apply(String[] row) {
        		linesReceived++;
        		if (!header) {
        			printHeader(row);
        			header = true;
        		} else if (goodToGo) {
        			send();
        		} else {
        			store(row);
        		}
        		
            return false; // Let QueryRunner decide when to stop
        }
        
        private void store(String[] row) {
        		EntryKey k = new EntryKey(row);
        		Double v = new Double(row[2]);
        		
        		if (h.containsKey(k)) {
        			h.put(k, h.get(k) + v);
        		} else {
        			h.put(k, v);
        		}
        }
        
        public void send() {
        		Log.debug("Network#send sending data...");
        		EntryKey k = null;
        		String vstr = null;
        		
        		ArrayList<Map.Entry<EntryKey, Double>> sortedMap = 
        				sortByValue(h);
        		try {
        			for (Entry<EntryKey, Double> entry : sortedMap) {
        				k = entry.getKey();
        				vstr = Double.toString(entry.getValue());
        				
        				linesSent++;

        				out.write(Joiner.on(DELIMITER).join(k.g0, k.g1, vstr).getBytes());
        				out.write(NEWLINE_BYTES);

        				if (++count % FLUSH_INTERVAL == 0)
        					out.flush();
        			}
        		} catch (IOException e1) {
        			throw new BioMartQueryException("Problem writing to OutputStream", e1);
        		}
			
        		
        		clear();
        }
        		
        	private void clear() {
        		Log.debug("Network#clear lines: received: "+linesReceived+" sent "+linesSent);
        		// Reset static fields
        		h.clear();
        		queryCount = 0;
        		header = false;
        	}
        	
        	private void printHeader(String[] row) {
        		// print only on the first query
        		if (queryCount > 0) return;
        		
        		try {
        			Log.debug("Network#printHeader: "+ Joiner.on(DELIMITER).join(row));
        			out.write(Joiner.on(DELIMITER).join(row).getBytes());
        			out.write(NEWLINE_BYTES);
        		} catch (IOException e) {
    				throw new BioMartQueryException("Problem writing to OutputStream", e);
    			}
        	}
        	
        	private ArrayList<Entry<EntryKey, Double>> sortByValue(Map<EntryKey, Double> m) {
        		Log.debug(this.getClass().getName() + "#sortByValue invoked");
        		ArrayList<Map.Entry<EntryKey, Double>> a = 
        				new ArrayList<Entry<EntryKey, Double>>(m.entrySet());
        		
        		Collections.sort(a, new Comparator<Entry<EntryKey, Double>>() {
        			@Override
        			public int compare(Entry<EntryKey, Double> a, Entry<EntryKey, Double> b) {
        				double av = a.getValue(), bv = b.getValue();
        				return av < bv ? 1 : av == bv ? 0 : -1;
        			}
        		});
        		
        		Log.debug(this.getClass().getName() + "#sortByValue "+ a);
        		
        		return a;
        	}
    }


}
