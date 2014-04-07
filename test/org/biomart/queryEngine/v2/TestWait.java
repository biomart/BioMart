package org.biomart.queryEngine.v2;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * This job will sleep (block) for the specified amount of
 * seconds, then return the input value.
 */
public class TestWait<T> extends TestIdentity<T> {
    private final int secondsToWait;

    public TestWait(int seconds) {
        this.secondsToWait = seconds;
    }

    @Override
    public T apply(T t) {
        try {
            Thread.sleep(secondsToWait);
        } catch (InterruptedException e) {
            // nothing
        } finally {
            return t;
        }
    }
}
