package org.biomart.queryEngine.v2;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.biomart.common.exceptions.BioMartTimeoutException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 *
 * ResultSet contains one or more results from Job execution
 *
 * Calling the next() method may block if result is not yet ready
 */
public class ResultSet<T> {
    private static final int DEFAULT_TIMEOUT = 30;

    private final Queue<ListenableFuture<Message<T>>> futures = new LinkedBlockingQueue<ListenableFuture<Message<T>>>();
    private final ProcessMaster master;
    private final Task<T> task;
    private int timeout = DEFAULT_TIMEOUT;

    public ResultSet(ProcessMaster master, Task<T> task) {
        this.master = master;
        this.task = task;
    }

    protected void add(ListenableFuture<Message<T>> future) {
        this.futures.add(future);
    }

    protected void addAll(List<ListenableFuture<Message<T>>> futures) {
        this.futures.addAll(futures);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // This may block
    public Message<T> next() {
        ListenableFuture<Message<T>> future = futures.poll();

        // No more futures, try pulling more from master
        if (future == null) {
            List<ListenableFuture<Message<T>>> more = master.submitJobs(task.getId(), task.pullJobs());
            if (more.isEmpty()) {
                close();
                return null;
            }
            this.futures.addAll(more);
            return next();
        }

        // Check if task has been cancelled already
        if (!future.isCancelled()) {
            try {
                return future.get(timeout, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new BioMartTimeoutException(e);
            } catch (InterruptedException e) {
                Log.debug("Interrupted in ResultSet.next()", e);
            } catch (ExecutionException e) {
                Log.debug("Problem during execution in ResultSet.next()", e);
            }
        }
        return null;
    }

    /*
     * Tries to cancel all the future objects. Cancel may not succeed.
     */
    public void cancel() {
        for (Future future : futures) {
            boolean cancelled = future.cancel(true);
            Log.debug("Cancelled Future for task: " + task.getId() + " (success=" + cancelled + ")");
        }
        close();
    }

    /*
     * Let master know to remove this ResultSet
     */
    public void close() {
        master.removeResultSetForTask(task.getId());
    }

    public String getTaskId() {
        return task.getId();
    }

    // Whether more results can be read
    public boolean hasMore() {
        return !futures.isEmpty();
    }
}
