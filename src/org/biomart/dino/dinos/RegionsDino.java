package org.biomart.dino.dinos;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.List;

import org.biomart.common.resources.Log;
import org.biomart.dino.Binding;
import org.biomart.dino.exceptions.ConfigException;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.queryEngine.Query;
import org.biomart.queryEngine.QueryElement;

import com.google.inject.Inject;

abstract public class RegionsDino implements Dino {

    
    Binding metaData;
    Query q;
    String[] mimes;
    
    
    String outFilterName;
    
    
    @Inject(optional=true)
    public RegionsDino setOutputFilterName(String outFilterName) {
        this.outFilterName = outFilterName;
        return this;
    }
    
    
    @Override
    public void run(OutputStream out) throws Exception {
        Log.debug(this.getClass().getName() +"#run(): output filter = "+ this.outFilterName);
        if (this.outFilterName == null) {
            throw new ConfigException("Output filter not set");
        }
        
        List<Field> myFields = Binding.getAnnotatedFields(this.getClass());
        List<QueryElement> filters = this.q.getFilters();

        List<QueryElement> boundFilts = Binding.setFieldValues(this, myFields, filters);
        // create the binding table
        this.metaData.setBindings(myFields, boundFilts);
        
        doRun();
    }
    
    
    abstract void doRun() throws Exception;
    
    
    Filter getFilter(Config cfg, String name) {
        return cfg.getFilterByName(name, null);
    }
    
    
    QueryElement createQueryElement(Dataset ds, Filter f, String value) {
        return new QueryElement(f, value, ds);
    }
    
    
    @Override
    public Dino setQuery(Query query) {
        this.q = query;
        return this;
    }

    @Override
    public Dino setMetaData(Binding metaData) {
        this.metaData = metaData;
        return this;
    }

    @Override
    public Dino setMimes(String[] mimes) {
        this.mimes = mimes;
        return this;
    }


    @Override
    public Query getQuery() {
        return this.q;
    }

}

































































































