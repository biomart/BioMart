package org.biomart.dino.dinos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.biomart.common.resources.Log;
import org.biomart.dino.annotations.Func;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;
import org.biomart.queryEngine.QueryElement;


public class BedDino extends RegionsDino {

    static public final String BEDFILE = "bed_regions",
                               FILTER_NAME_ENV = "BED_DINO_OUTPUT_FILTER_NAME";
    
    @Func(id = BEDFILE, optional = true)
    String bedFile;
    
    
    private void doFormatAndDelegate() throws Exception {
        if (this.bedFile != null && !this.bedFile.isEmpty()) {
            Log.debug(this.getClass().getName() +"#run(): formatting");
            long startTime = System.nanoTime();
            String value = this.getEnsemblFormat(bedFile);
            long endTime = System.nanoTime();
            Log.info(this.getClass().getName() + " TIMES: BED to Ensembl translation took "+ (endTime - startTime) / 10e6 + "ms" );
            QueryElement qe = this.metaData.getQueryBindings().get(BEDFILE);
            Element thisFilter = this.metaData.getBindings().get(BEDFILE);
            if (!thisFilter.getName().equals(outFilterName)) {
                Filter f = getFilter(qe.getConfig(), outFilterName);
                
                qe = createQueryElement(qe.getDataset(), f, value);
                q.addFilter(qe);
            } else {
                qe.setFilterValues(value);
            }
        }
    }
    
    
    /*
     * Expected format of the input:
     * 
     * chr<string>\t<integer>\t<integer>
     * 
     * or
     * 
     * chr<string>\t<integer>\t<integer>\t<string>\t<integer>\t<string>
     * 
     * where, in the latter case the last column is the strand and the 4th and
     * 5th can be empty.
     * 
     * The output will be
     * 
     * <string>:<integer>:<integer>
     * 
     * or
     * 
     * <string>:<integet>:<integer>:<string>
     * 
     * where the last column is the strand
     */
    public String getEnsemblFormat(String bed) {
        // Groups:
        // 0: entire expression
        // 1: (chr) thing
        // 2: (\\S+) the name of the chromosome 
        // 3: (\\d+) start
        // 4: (\\d+) end
        // 5: all the optional columns
        // 6: the strand
        //  "(chr)?(\\S+)\t(\\d+)\t(\\d+)(\t\\S*\t\\d*(["+Pattern.quote("+")+"-]))?"
        String regex = "(chr)?(\\S+)\t(\\d+)\t(\\d+)(\t\\S*\t\\S*\t([-\\+]).*)?",
               lines[] = null, name, start, end, strand, od = ":", rd = ",";
        StringBuilder out = new StringBuilder();
        
        Pattern p = Pattern.compile(regex);
        Matcher m = null;
        lines = bed.split("\n");
        m = p.matcher("");
        
        for (int i = 0, len = lines.length; i < len; ++i) {
            m.reset(lines[i]);
            if (m.matches()) {
                name = m.group(2);
                start = m.group(3);
                end = m.group(4);
                out.append(name)
                    .append(od)
                    .append(start)
                    .append(od)
                    .append(end);
                if (m.group(6) != null) {
                    strand = m.group(6);
                    out.append(od).append(strand+"1");
                }
                if (i < len - 1) {
                    out.append(rd);
                }
            }
        }
        
        
        return out.toString();
    }


    @Override
    void doRun() throws Exception {
        this.doFormatAndDelegate();
    }

}

































































































