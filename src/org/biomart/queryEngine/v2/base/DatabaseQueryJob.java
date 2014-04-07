package org.biomart.queryEngine.v2.base;

import java.util.List;

/**
 *
 * @author jhsu
 *
 * DatabaseQueryJob is responsible for fetching results from a database or URL source.
 *
 * It only retrieves a fixed size of results starting from the offset.
 *
 */
public class DatabaseQueryJob extends QueryJob {
    @Override
    public List<String[]> apply(Dataset dataset) {
        return null;
    }
}
