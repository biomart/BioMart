package org.biomart.queryEngine.v2;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * Returns the sum of the integers passed through the constructor.
 */
public class TestSumTask extends Task<Integer> {
    private List<Job<Integer,Integer>> jobs = new ArrayList<Job<Integer,Integer>>();

    /*
     * Add jobs to list
     */
    public TestSumTask(Integer... numbers) {
        Job<Integer,Integer> prev = new TestIdentity<Integer>();
        prev.setInput(0);

        for (int i=0; i< numbers.length; i++) {
            Integer number = numbers[i];

            Job<Integer,Integer> job = new TestIncrement(number);
            job.setInput(prev);

            prev = job;
        }

        jobs.add(prev);
    }

    /*
     * Remove each job from the list and return them
     */
    @Override
    public List<Job<Integer,Integer>> pullJobs() {
        List<Job<Integer,Integer>> nextJobs = new ArrayList<Job<Integer,Integer>>();
        for (Job job : Iterables.consumingIterable(jobs)) {
            nextJobs.add(job);
        }
        return nextJobs;
    }
}
