package org.biomart.queryEngine.v2;

import java.util.concurrent.Callable;
import java.util.UUID;
import org.biomart.common.resources.Log;


/**
 *
 * @author jhsu
 *
 * ProcessWorker is responsible for running the Job it receives from the
 * ProcessMaster. The call() method will fetch the result data from the Job,
 * and wrap it with a Message object.
 */
public final class ProcessWorker<F,T> implements Callable<Message<T>> {

    private final String id;
    private final String taskId;

    private Message<T> message;

    private Job<F,T> job;

    public ProcessWorker(String taskId, Job<F,T> job) {
        this.id = UUID.randomUUID().toString();
        this.job = job;
        this.taskId = taskId;
    }

    /*
     * Gets assigned a job and executes it
     * Once done, emit results to master
     */
    @Override
    public final Message<T> call() {
        message = new Message<T>();
        // Set worker and job IDs
        message.workerId = id;
        message.taskId = taskId;
        message.jobId = job.getId();

        // Check we have a service and job
        if (job == null) {
            message.status = Status.ERROR;
            message.message = "No job set on task";
        } else {
            try {
                Log.debug("Waiting for message from job: " + job.getId());

                // Set message with Future object
                message.status = Status.SUCCESS;
                message.data = job.run();

                Log.debug("Got message from job: " + job.getId());

            } catch (Exception e) {
                Log.error("Error during Worker.run()", e);
                message.status = Status.ERROR;
                message.message = e.getMessage();
            }
        }

        return message;
    }

    /*
     * Setters and getters
     */
    public final String getId() {
        return id;
    }

    /*
     * May not be set if this worker has not run yet
     */
    public final Message<T> getMessage() {
        return message;
    }
}
