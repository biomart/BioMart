package org.biomart.dino.dinos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biomart.common.resources.Log;
import org.biomart.dino.annotations.Func;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;
import org.biomart.queryEngine.QueryElement;


public class UpstreamDownstreamDino extends RegionsDino {
    
    public static String REGIONS = "regions";
    
    @Func(id="regions", optional=true)
    String regions;
    @Func(id="upstream", optional=true)
    String upstream;
    @Func(id="downstream", optional=true)
    String downstream;
    
    
     
    @Override
    void doRun() throws Exception {
        this.doFormatAndDelegate();
    }
    
    private boolean check(String req) {
        return req != null && !req.isEmpty();
    }
    
    private void doFormatAndDelegate() throws Exception {
        
        if (check(regions)) {
            String value = null;
            QueryElement qe = this.metaData.getQueryBindings().get(REGIONS);
            Element thisFilter = this.metaData.getBindings().get(REGIONS);
        
            if (check(upstream) || check(downstream)) {
                Log.debug(this.getClass().getName() +"#run(): formatting");
                long startTime = System.nanoTime();
                value = this.upstreamDownstream(regions, upstream, downstream);
                long endTime = System.nanoTime();
                Log.info(this.getClass().getName() + " TIMES: upstream, downstream took "+ (endTime - startTime) / 10e6 + "ms" );
            }
            
            send(thisFilter, qe, value == null ? regions : value);
        }
    }
    
    
    private void send(Element thisFilter, QueryElement qe, String value) {
        if (!thisFilter.getName().equals(outFilterName)) {
            Filter f = getFilter(qe.getConfig(), outFilterName);
            
            qe = createQueryElement(qe.getDataset(), f, value);
            q.addFilter(qe);
        } else {
            qe.setFilterValues(value);
        }
    }
    
    
    public String upstreamDownstream(String content, String up, String down) {
        Boolean doUp = false, doDown = false;
        int upInt = 0, downInt = 0;
        
        try {
            if (check(up)) {
                upInt = Integer.parseInt(up);
                doUp = true;
            }
            
            if (check(down)) {
                downInt = Integer.parseInt(down);
                doDown = true;
            }
        } catch (NumberFormatException e) {
            Log.error("Upstream or Downstream are not numbers", e);
            return null;
        }
        
        String regex = "(\\S+):(\\d+):(\\d+)(:([-\\+])1)?",
               lines[] = null, name, start, end, rest, strand, od = ":", rd = ",";
        StringBuilder out = new StringBuilder();

        Pattern p = Pattern.compile(regex);
        Matcher m = null;
        
        lines = content.split(rd);
        for (int i = 0, len = lines.length; i < len; ++i) {
            m = p.matcher(lines[i]);
            if (m.matches()) {
                try {
                    name = m.group(1);
                    start = m.group(2);
                    end = m.group(3);
                    rest = m.group(4);
                    strand = m.group(5);
                    int istart = Integer.parseInt(start);
                    int iend = Integer.parseInt(end);
                    if (rest == null) {
                        // If there is not strand assume plus
                        strand = "+";
                    }
                    if (strand.equals("-")) {
                        if (doUp) {
                            iend += upInt;
                        }
                        if (doDown) {
                            istart -= downInt;
                        }
                    } else {
                        if (doUp) {
                            istart -= upInt;
                        }
                        if (doDown) {
                            iend += downInt;
                        }
                    }
                    out.append(name).append(od)
                        .append(istart).append(od)
                        .append(iend);
                    if (rest != null) {
                        out.append(rest);
                    }
                    if (i < len - 1) {
                        out.append(rd);
                    }
                } catch (NumberFormatException e) {
                    Log.error("UpstreamDownstreamDino: bad format for region start, end ", e);
                }
            }
        }
        

        return out.toString();
    }


}



















































