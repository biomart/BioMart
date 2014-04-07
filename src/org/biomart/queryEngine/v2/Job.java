package org.biomart.queryEngine.v2;

import com.google.common.base.Function;
import java.util.UUID;
import org.biomart.common.exceptions.BioMartQueryException;
import org.biomart.common.resources.Log;

/**
 *
 * @author jhsu
 */
public abstract class Job<F,T> {
    private final String id;
    private int retries = 3;
    private boolean allowRetry = false;

    private Function<T,T> transform;

    // Input can come from preset data or output from another job
    private F input;
    private Job<?,F> inputJob;

    public Job() {
        id = UUID.randomUUID().toString();
    }

    /*
     * Takes input F and transforms to output T
     */
    public abstract T apply(F input);

    /*
     * If a transform Function is defined, use it before passing the results back
     * Else just return the results
     */
    public final T run() {
        T t =  apply(input);
        if (transform == null) {
            return t;
        }
        return transform.apply(t);
    }

    public final T retry() {
        if (retries-- > 0) {
            Log.debug("Retrying job: " + id);
            return run();
        } else {
            Log.debug("Job failed (no more retries): " + id);
            throw new BioMartQueryException("Maximum retries reached");
        }
    }

    /*
     * Getters and setters
     */
    public final String getId() {
        return id;
    }

    public final Job<F,T> setAllowRetry(boolean allowRetry) {
        this.allowRetry = allowRetry;
        return this;
    }

    public final boolean getAllowRetry() {
        return allowRetry;
    }

    public final Job<F,T> setRetries(int retries) {
        this.retries = retries;
        return this;
    }

    public final Job<F,T> setTransform(Function<T,T> transform) {
        this.transform = transform;
        return this;
    }

    public final Function<T,T> getTransform() {
        return transform;
    }

    // Set input data directly
    public final Job<F,T> setInput(F input) {
        this.input = input;
        return this;
    }

    // Set input data to be the output of another job
    // Note that the type F has to match
    public final Job<F,T> setInput(Job<?,F> inputJob) {
        this.inputJob = inputJob;
        return this;
    }

    public final boolean hasInputJob() {
        return inputJob != null;
    }

    public final Job<?,F> getInputJob() {
        return inputJob;
    }
}
