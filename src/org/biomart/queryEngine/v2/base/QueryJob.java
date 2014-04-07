package org.biomart.queryEngine.v2.base;

import java.util.List;
import org.biomart.queryEngine.v2.Job;

/**
 *
 * @author jhsu
 */
public abstract class QueryJob extends Job<Dataset,List<String[]>> {
    protected int offset = 0;
    protected int fetchSize = Integer.MAX_VALUE;

    public QueryJob setFetchSize(int size) {
        this.fetchSize = size;
        return this;
    }

    public QueryJob setOffset(int offset) {
        this.offset = offset;
        return this;
    }
}
