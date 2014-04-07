package org.biomart.queryEngine.v2.base;

import java.util.ArrayList;
import java.util.List;
import org.biomart.queryEngine.v2.Job;
import org.biomart.queryEngine.v2.Message;
import org.biomart.queryEngine.v2.ProcessMaster;
import org.biomart.queryEngine.v2.ResultSet;
import org.biomart.queryEngine.v2.Task;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class QueryTaskTest {
    private static final String MART_SERVICE_URL = "http://central.biomart.org/martservice/results";

    @Test
    public void testWebQueryJob() {
        ProcessMaster master = new ProcessMaster();

        final Dataset dataset = new Dataset("hsapiens_gene_ensembl", "gene_ensembl_config");
        dataset.filters.add(new Filter("ensembl_gene_id", "ENSG00000251282"));
        dataset.attributes.add(new Attribute("ensembl_gene_id"));

        Task task = new Task<List<String[]>>() {
            private boolean hasExecuted = false;
            @Override
            public List<Job<Dataset,List<String[]>>> pullJobs() {
                List<Job<Dataset,List<String[]>>> list = new ArrayList<Job<Dataset,List<String[]>>>();
                if (!hasExecuted) {
                    WebQueryJob job = new WebQueryJob(MART_SERVICE_URL);
                    job.setInput(dataset);
                    list.add(job);
                    hasExecuted = true;
                }
                return list;
            }
        };

        ResultSet<List<String[]>> rs = master.submitTask(task);
        Message<List<String[]>> message = null;
        int count = 0;

        while ((message = rs.next()) != null) {
            for (String[] row : message.getData()) {
                assertEquals(1, row.length);
                count++;
            }
        }

        assertEquals(1, count);

        rs.close();
        master.shutdown();
    }

    /*
     * Test that the transform function of QueryTask modifies the same reference
     * of rows, and that the uniqueness property is maintained.
     */
    @Test
    public void testUniquenessFunction() {
        QueryTask instance = new QueryTask(null);
        List<String[]> rows = new ArrayList<String[]>() {{
            add(new String[] { "foo", "bar", "faz", "baz" });
            add(new String[] { "hello", "world", "!", "!" });
            add(new String[] { "foo", "bar", "faz", "baz" });
            add(new String[] { "a", "b", "c", "d" });
            add(new String[] { "foo", "baz", "faz", "bar" });
            add(new String[] { "a", "b", "c", "d" });
            add(new String[] { "e", "f", "g", "h" });
        }};

        List<String[]> result = instance.transform.apply(rows);

        assertEquals(rows, result);
        assertEquals(5, result.size());
    }
}
