package org.biomart.queryEngine.v2;

import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import org.biomart.common.resources.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.biomart.event.Emitter;


/**
 *
 * @author jhsu
 *
 * ProcessMaster is governs all tasks that need to be executed. Each Job from a
 * Task is assigned to a ProcessWorker and submitted to the ExecutorService.
 *
 * A ResultSet object is returned, which contains the response messages from
 * workers -- including the result data. When the message is accessed it MAY
 * cause the executing thread to block until the result is ready.
 *
 */
public class ProcessMaster extends Emitter<Message> {
    /*
     * Max size for bound number of threads we're able to create
     *
     * Shutdown delay measured in seconds, try for graceful shutdown when possible
     */
    private static final int DEFAULT_MAX_SIZE = 100;
    private static final int DEFAULT_SHUTDOWN_DELAY = 10;
    private static final int DEFAULT_RESULT_TIMEOUT = 30;

    private final int maxSize = DEFAULT_MAX_SIZE;
    private final int shutdownDelay = DEFAULT_SHUTDOWN_DELAY;
    private final int resultTimeout = DEFAULT_RESULT_TIMEOUT;

    private final ExecutorService service;

    // Keep track of jobs, workers, and result sets
    private final Map<String,Job> jobs = new HashMap<String,Job>();
    private final Map<String,ProcessWorker> workers = new HashMap<String,ProcessWorker>();
    private final Map<String,ResultSet> pendingResultSets = new HashMap<String,ResultSet>();

    public ProcessMaster() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setDaemon(false)
                .setNameFormat("BioMart-%d")
                .setThreadFactory(Executors.defaultThreadFactory())
                .build();

        service = Executors.newFixedThreadPool(maxSize, threadFactory);
    }

    /*
     * Submit a new task to be scheduled
     * Returns a list of rs that may block when read() is called
     */
    public synchronized <F,T> ResultSet<T> submitTask(Task<T> task) {
        if (service.isShutdown() || service.isTerminated()) {
            return null;
        }

        Log.debug("[Master] Submitting new task: " + task.getId());

        List<Job<F,T>> list = task.pullJobs();

        ResultSet<T> rs = new ResultSet<T>(this, task);
        rs.setTimeout(resultTimeout);

        List<ListenableFuture<Message<T>>> newFutures =  submitJobs(task.getId(), list);
        rs.addAll(newFutures);

        // Each task maps to exactly one result set
        pendingResultSets.put(task.getId(), rs);

        return rs;
    }

    public synchronized void removeResultSetForTask(String taskId) {
        ResultSet removed = pendingResultSets.remove(taskId);
        if (removed != null) {
            Log.debug("[Master] Removed ResultSet from master for task = " + taskId);
        }
    }

    /*
     * Takes a list of jobs and submits all
     */
    protected <F,T> List<ListenableFuture<Message<T>>> submitJobs(String taskId, List<Job<F,T>> list) {
        List<ListenableFuture<Message<T>>> newFutures = new ArrayList<ListenableFuture<Message<T>>>();

        for (Job job : list) {
            newFutures.add(
                submitJob(taskId, job)
            );
        }

        return newFutures;
    }

    /*
     * Submits a Job to be executed. If the Job depends on the data of another Job,
     * chain the rs together and return the Future.
     */
    private <F,T> ListenableFuture<Message<T>> submitJob(final String taskId, final Job<F,T> job) {
        ListenableFuture<Message<T>> future;

        // Need to chain current job to use output from the input job
        if (job.hasInputJob()) {
            ListenableFuture<Message<F>> inputFuture = submitJob(taskId, job.getInputJob());

            Function<Message<F>,ListenableFuture<Message<T>>> chainFunction = new Function<Message<F>,ListenableFuture<Message<T>>>() {
                @Override
                public ListenableFuture<Message<T>> apply(Message<F> message) {
                    F input = message.data;
                    job.setInput(input);
                    return createMessageFutureFromJob(taskId, job);
                }
            };

            future = Futures.chain(inputFuture, chainFunction);
        } else {
            // We can execute the job because input was preset
            future = createMessageFutureFromJob(taskId, job);
        }

        return future;
    }

    /*
     * Creates a ProcessWorker and submits it to the ExecutorService
     */
    private <T> ListenableFuture<Message<T>> createMessageFutureFromJob(final String taskId, final Job<?,T> job) {
        final ProcessWorker worker = new ProcessWorker(taskId, job);
        final ListenableFuture<Message<T>> future =  Futures.makeListenable(
                service.submit(worker)
            );

        // Make references to job and worker objects in case we need them later
        // for cancellations, etc.
        Log.debug("[Master] Adding job to map: " + job.getId());
        Log.debug("[Master] Adding worker to map: " + worker.getId());
        jobs.put(job.getId(), job);
        workers.put(worker.getId(), worker);

        // When the data returns we can remove job and worker object from maps
        future.addListener(new Runnable() {
            @Override
            public void run() {
                Log.debug("[Master] Removing job from map: " + job.getId());
                Log.debug("[Master] Removing worker from map: " + worker.getId());
                jobs.remove(job.getId());
                workers.remove(worker.getId());
            }
        }, MoreExecutors.sameThreadExecutor());

        return future;
    }

    /*
     * Cleanup on objects and shutdown the ExecutorService
     */
    public synchronized void shutdown() {
        Log.debug("[Master] Shutting down");

        // Clear out maps
        jobs.clear();
        workers.clear();
        pendingResultSets.clear();

        // Try to shutdown nicely... if delay time is reached, just shutdown everything
        // This will cancel any remaining tasks/futures
        service.shutdown();
        try {
            service.awaitTermination(shutdownDelay, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            service.shutdownNow();
        }
    }

}
