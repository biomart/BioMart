package org.biomart.queryEngine.v2;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class ProcessTest {
    /*
     * This will test that Jobs with an input Job will be correctly chained
     */
    @Test
    public void testTaskWithChainedJobs() {
        final ProcessMaster master = new ProcessMaster();
        int N = 10;
        Integer ints[] = new Integer[N];
        int expected = 0;

        for (int i=1; i<=N; i++) {
            expected +=i;
            ints[i-1] = i;
        }

        TestSumTask task = new TestSumTask(ints);
        ResultSet<Integer> results = master.submitTask(task);
        Message<Integer> message = null;

        while ((message = results.next()) != null) {
            assertEquals(expected, message.getData().intValue());
        }

        results.close();
        master.shutdown();
    }

    /*
     * This will test that ResultSet.next() will correct pull more from the master
     * if its list of futures is empty
     */
    @Test
    public void testTaskWithMultiplePulls() {
        final ProcessMaster master = new ProcessMaster();
        int N = 10;

        TestTimesTask task = new TestTimesTask(N);
        ResultSet<Integer> results = master.submitTask(task);
        int count = 0;

        while (results.next() != null) {
            count++;
        }

        assertEquals(N, count);

        results.close();
        master.shutdown();
    }

    /*
     * This will test that the transform Function is correctly called, which
     * should reduce the produced to results to unique ones only
     */
    @Test
    public void testUniqueTransform() {
        final ProcessMaster master = new ProcessMaster();
        Task<int[]> task = new TestUniqueTask();
        List<Integer> numbers = new ArrayList<Integer>();

        ResultSet<int[]> results = master.submitTask(task);

        Message<int[]> message = null;
        while ((message = results.next()) != null) {
            int[] ints = message.getData();
            for (int i : ints) {
                numbers.add(i);
            }
        }

        assertEquals(8, numbers.size());

        results.close();
        master.shutdown();
    }

    /*
     * This will test that the transform Function is correctly called, which
     * should reduce the produced to results to unique ones only
     */
    @Test
    public void testCancel() {
        final ProcessMaster master = new ProcessMaster();
        int N = 50;
        int t = 10000;
        int count = 0;

        TestWaitTask task = new TestWaitTask(N, t);
        ResultSet<Integer> results = master.submitTask(task);

        results.cancel();

        while (results.next() != null) {
            count++;
        }

        // Should not be the same since we cancelled
        assertNotSame(N, count);

        results.close();
        master.shutdown();
    }
}
