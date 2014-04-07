package org.biomart.dino.dinos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.biomart.common.resources.Log;
import org.biomart.dino.Binding;
import org.biomart.dino.annotations.BaseConfigDir;
import org.biomart.dino.dinos.enrichment.EnrichmentDino;
import org.biomart.queryEngine.Query;

import com.google.inject.Inject;

public class EnrichmentDecorator implements Dino {

    OutputStream out;
    RegionsDino bed, udd;
    EnrichmentDino en;
    Query q;
    Binding b;
    String[] mimes;
    String bedOutFilter, upDownOutFilter;
  
    @Inject
    public EnrichmentDecorator(@BaseConfigDir String confDir, 
                               BedDino bed, 
                               UpstreamDownstreamDino udd, 
                               EnrichmentDino en) throws FileNotFoundException, IOException {
        String s = System.getProperty("file.separator"),
                f = StringUtils.join(new String[] {
                        confDir, "enrichment", "EnrichmentDecorator.properties"
                }, s);
        
        Properties pp = new Properties();
        pp.load(new FileInputStream(new File(f)));
        bedOutFilter = pp.getProperty("enrichment_decorator.bed_to_ensembl_output_filter");
        upDownOutFilter = pp.getProperty("enrichment_decorator.upstream_downstream_output_filter");
        
        
        this.bed = bed;
        this.en = en;
        this.udd = udd;
    }
    
    @Override
    public void run(OutputStream out) throws Exception {
        Log.debug(this.getClass().getName() +"#run(): bed output filter = "+ this.bedOutFilter);
        Log.debug(this.getClass().getName() +"#run(): up down output filter = "+ this.upDownOutFilter);
        this.out = out;
        bed.setOutputFilterName(bedOutFilter)
           .setMetaData(new Binding())
           .setMimes(mimes)
           .setQuery(q)
           .run(out);
        udd.setOutputFilterName(upDownOutFilter)
            .setMetaData(new Binding())
            .setMimes(mimes)
            .setQuery(q)
            .run(out);
        en.setMetaData(new Binding())
            .setMimes(mimes)
            .setQuery(q)
            .run(out);
    }

    @Override
    public Dino setQuery(Query query) {
        this.q = query;
        return this;
    }

    @Override
    public Dino setMetaData(Binding metaData) {
        this.b = metaData;
        return this;
    }

    @Override
    public Dino setMimes(String[] mimes) {
        this.mimes = mimes;
        return this;
    }

    @Override
    public Query getQuery() {
        return q;
    }

}
