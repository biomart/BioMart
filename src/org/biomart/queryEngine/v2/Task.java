package org.biomart.queryEngine.v2;

import java.util.List;
import java.util.UUID;
import org.biomart.event.Emitter;

/**
 *
 * @author jhsu
 *
 */
public abstract class Task<T> extends Emitter<Message<T>> {
    private final String id;

    public Task() {
        id = UUID.randomUUID().toString();
    }

    /*
     * Returns jobs required by this task
     * If this task is done, then return empty list
     */
    public abstract <E> List<Job<E,T>> pullJobs();

    /*
     * Getters and setters
     */
    public final String getId() {
        return id;
    }
}
