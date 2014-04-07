package org.biomart.queryEngine.v2;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * Returns the set number of TestWait jobs.
 */
public class TestWaitTask extends Task<Integer> {
    private int count = 0;
    private int time = 0;

    public TestWaitTask(int i, int time) {
        count = i;
        this.time = time;
    }

    @Override
    public List<Job<Integer,Integer>> pullJobs() {
        List<Job<Integer,Integer>> list = new ArrayList<Job<Integer,Integer>>();
        if (count-- > 0) {
            Job<Integer,Integer> job = new TestWait<Integer>(time);
            job.setInput(1);
            list.add(job);
        }
        return list;
    }
}
