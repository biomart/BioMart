package org.biomart.queryEngine.v2;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * The return values from each job will be passed to the transform function.
 * We expect only unique values to be returned in the ResultSet.
 */
public class TestUniqueTask extends Task<int[]> {
    private final Set<Integer> set = new HashSet<Integer>();

    private Function<int[],int[]> transform = new Function<int[],int[]>() {
            @Override
            public int[] apply(int[] ints) {
                List<Integer> reduced = new ArrayList<Integer>();
                synchronized (set) {
                    for (int i : ints) {
                        if (!set.contains(i)) {
                            set.add(i);
                            reduced.add(i);
                        }
                    }
                    int[] rval = new int[reduced.size()];
                    for (int i=0; i<rval.length; i++) {
                        rval[i] = reduced.get(i);
                    }
                    return rval;
                }
            }
    };

    private List<Job<int[],int[]>> list = new ArrayList<Job<int[],int[]>>();

    public TestUniqueTask() {

        list.add(new TestIdentity<int[]>()
            .setInput(new int[] { 1, 8, 3, 4 })
            .setTransform(transform));

        list.add(new TestIdentity<int[]>()
            .setInput(new int[] { 1, 2, 1, 4 })
            .setTransform(transform));

        list.add(new TestIdentity<int[]>()
            .setInput(new int[] { 1, 6, 3, 5 })
            .setTransform(transform));

        list.add(new TestIdentity<int[]>()
            .setInput(new int[] { 8, 6, 7, 5 })
            .setTransform(transform));

        list.add(new TestIdentity<int[]>()
            .setInput(new int[] { 1, 4, 6, 1 })
            .setTransform(transform));

    }

    @Override
    public List<Job<int[],int[]>> pullJobs() {
        List<Job<int[],int[]>> nextJobs = new ArrayList<Job<int[],int[]>>();
        for (Job job : Iterables.consumingIterable(list)) {
            nextJobs.add(job);
        }
        return nextJobs;
    }
}

