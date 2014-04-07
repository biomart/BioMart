package org.biomart.queryEngine.v2;

/**
 *
 * @author jhsu
 *
 * Message contains the result data from an executed Job. It also contains extra
 * information about its execution.
 */
public class Message<T> {
    // Keep track of who this message is from/for
    protected String taskId;
    protected String jobId;
    protected String workerId;

    // Indicates success or error, cancelled, etc.
    protected Status status;

    // Any accompanying message, useful when error occurred
    protected String message;

    // Future object that contains the data (may block)
    protected T data;

    public Message() {}
    public Message(String jobId, Status status, String message, T data) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.data = data;
    }

    /*
     * Try to get computation data from future object
     * If we get exception, return null
     */
    public T getData() {
        return data;
    }

    /*
     * Getters
     */
    public Status getStatus() {
        return status;
    }
    public String getMessage() {
        return message;
    }
}
