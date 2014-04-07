package org.biomart.queryEngine.v2;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * Executes the TestIdentity job the set number of times.
 */
public class TestTimesTask extends Task<Integer> {
    private int count = 0;

    public TestTimesTask(int i) {
        count = i;
    }

    @Override
    public List<Job<Integer,Integer>> pullJobs() {
        List<Job<Integer,Integer>> list = new ArrayList<Job<Integer,Integer>>();
        if (count-- > 0) {
            Job<Integer,Integer> job = new TestIdentity<Integer>();
            job.setInput(1);
            list.add(job);
        }
        return list;
    }
}
