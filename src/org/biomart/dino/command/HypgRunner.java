package org.biomart.dino.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.biomart.common.resources.Log;

public class HypgRunner extends ShellRunner {

    /**
     * It parses the output file, hypg.pv, living in the working directory,
     * and returns a __sorted__ List of string lists.
     * 
     * Results are sorted in increasing order based on p-value and filtered: 
     * only the first 50 results are returned.
     * 
     * The format of a line is: ["annotation", "p-value", "bp-value", "g0,g1,..."]
     * where the last item can be missing.
     * 
     * @throws IOException 
     * 
     */
    @Override
    public Object getResults() throws IOException {
        // With the current hypg.exe program, the output will be in the
        // hypg.pv file in the folder the bin has been run.

        // Input format
        // string float float string*
        
        File fin = new File(this.dir, "hypg.pv");
        List<List<String>> results = new ArrayList<List<String>>();
        final String colDelim = "\t";
        String genes;
        List<String> tks;
        String[] lineTks;
        BufferedReader in = null;
        
        try {
            in = new BufferedReader(new FileReader(fin));
            String line = null;
        
            while((line = in.readLine()) != null) {
                lineTks = line.split(colDelim);
                tks = take(lineTks, 3);
                
                if (lineTks.length < 3) {
                    Log.error(this.getClass().getName()
                        + "#getResults() bad input: "+ line);
                    continue;
                }
                
                genes = takeGenes(lineTks);
                if (! genes.isEmpty()) tks.add(genes);
                results.add(tks);
            }
            
        } catch (FileNotFoundException e) {
            Log.error(this.getClass().getName()
                    + "#getResults() cannot find inputfile hypg.pv");
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        
        Collections.sort(results, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> a, List<String> b) {
                return a.get(1).compareTo(b.get(1));
            }
        });
        
        return results;
    }
    
    private String takeGenes(String[] tks) {
        if (tks.length > 3) {
            return StringUtils.join(Arrays.copyOfRange(tks, 3, tks.length), ",");
        } else {
            return "";
        }
    }
    
    private List<String> take(String[] tks, int nth) {
        int n;
        n = tks.length < nth ? tks.length : nth;
        return new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(tks, 0, n)));
    }

}





//public Object getResults() {
//    // With the current hypg.exe program, the output will be in the
//    // hypg.pv file in the folder the bin has been run.
//
//    // Input format
//    // string float float string*
//    
//    int resultsLimit = 50, count = 0;
//    File fin = new File(this.dir, "hypg.pv");
//    SortedSet<String[]> results = new TreeSet<String[]>(
//        new Comparator<String[]>() {
//            @Override
//            public int compare(String[] a, String[] b) {
//                return a[1].compareTo(b[1]);
//            }
//        }
//    );
//    
//    try (BufferedReader in = new BufferedReader(new FileReader(fin))) {
//        String[] tokens = null;
//        String line = null;
//        while(count < resultsLimit && (line = in.readLine()) != null) {
//            tokens = line.trim().split("\t");
//            
//            if (tokens.length < 3) {
//                Log.error(this.getClass().getName()
//                    + "#getResults() bad input: "+ line);
//                continue;
//            }
//            
//            for (int i = 0; i < tokens.length; ++i) {
//                // Ignoring possibility of empty items
//                tokens[i] = tokens[i].trim();
//            }
//            results.add(tokens);
//        }
//        
//    } catch (FileNotFoundException e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//    } catch (IOException e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//    }
//    
//    return results;
//}

