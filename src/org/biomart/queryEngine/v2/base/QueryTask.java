package org.biomart.queryEngine.v2.base;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import java.util.List;
import org.biomart.queryEngine.v2.Job;
import org.biomart.queryEngine.v2.Task;
import org.biomart.util.String2LongSet;

/**
 *
 * @author jhsu
 *
 * QueryTask is responsible for taking in a query XML (String or JDOM Document)
 * and creating the required number of QueryJob objects to fetch those results.
 *
 * Because QueryJobs are fired in parallel, the order of the returned results may change
 * between multiple executions of QueryTask.
 *
 */
public class QueryTask extends Task<List<String[]>> {
    private static final int BATCH_SIZE = 5000;

    private final String queryXml;

    public QueryTask(String queryXml) {
        this.queryXml = queryXml;
    }

    // Display only unique results
    private final String2LongSet uniqueResults = new String2LongSet();

    @VisibleForTesting 
    protected Function<List<String[]>,List<String[]>> transform = new Function<List<String[]>,List<String[]>>() {
        @Override
        public List<String[]> apply(List<String[]> rows) {
            synchronized (uniqueResults) {
                for (int i=0; i<rows.size(); i++) {
                    String[] row = rows.get(i);
                    String str = Joiner.on('\t').join(row);
                    if (!uniqueResults.contains(str)) {
                        uniqueResults.add(str);
                    } else {
                        // Remove current row, and decrement i to account for removal
                        rows.remove(i--);
                    }
                }
                return rows;
            }
        }
    };

    @Override
    public List<Job<?,List<String[]>>> pullJobs() {
        return null;
    }
}
