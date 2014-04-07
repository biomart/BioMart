package org.biomart.queryEngine.v2;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jhsu
 *
 * Job statuses passed in the Message object.
 */
public enum Status {
    SUCCESS(0),
    ERROR(100),
    CANCELLED(200),
    DONE(300);

    private static final Map<Integer,Status> lookup = new HashMap<Integer,Status>();

    static {
        for(Status s : EnumSet.allOf(Status.class))
            lookup.put(s.getCode(), s);
    }

    private int code;

    private Status(int code) {
        this.code = code;
    }

    public int getCode() { return code; }

    public static Status get(int code) {
        return lookup.get(code);
    }
}
