package org.biomart.queryEngine.v2;

/**
 *
 * @author jhsu
 *
 * For testing purposes.
 *
 * Increments the input integer i by the integer c.
 */
public class TestIncrement extends Job<Integer,Integer> {
    private final Integer c;

    public TestIncrement(Integer c) {
        this.c = c;
    }

    @Override
    public Integer apply(Integer i) {
        return c + i;
    }
}
