package org.biomart.event;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class EventTest {
    @Test
    public void testAddCallback(){
        final Integer i = (int)(Math.random() * 100);
        Emitter emitter = new Emitter<Integer>();
        Callback<Integer> callback = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                assertEquals(i, j);
            }
        };
        emitter.on("test", callback);
        emitter.emit("test", i);
    }

    @Test
    public void testAddCallbackOnce(){
        final Integer i = (int)(Math.random() * 100);
        Emitter emitter = new Emitter<Integer>();
        Callback<Integer> callback = new Callback<Integer>() {
            int count = 0;
            @Override
            public void call(String event, Integer j) {
                if (count++ > 0) {
                    fail("Should not fire more than once");
                }  else {
                    assertEquals(i, j);
                }
            }
        };
        emitter.once("test", callback);
        emitter.emit("test", i);
        emitter.emit("test", i);
    }

    @Test
    public void testRemoveCallback(){
        final Integer i = (int)(Math.random() * 100);
        Emitter emitter = new Emitter<Integer>();
        Callback<Integer> callback = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                fail("Should not have fired");
            }
        };
        emitter.on("test", callback);
        emitter.remove("test", callback);
        emitter.emit("test", i);
    }

    @Test
    public void testMultipleCallbacks(){
        final Integer i = (int)(Math.random() * 100);

        Emitter emitter = new Emitter<Integer>();

        Callback<Integer> callback1 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                assertEquals(i, j);
            }
        };

        Callback<Integer> callback2 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                fail("Should not have fired");
            }
        };

        Callback<Integer> callback3 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                assertEquals(i, j);
            }
        };

        emitter.on("test", callback1);
        emitter.on("test", callback2);
        emitter.on("test", callback3);
        emitter.remove("test", callback2);
        emitter.emit("test", i);
    }

    @Test
    public void testRemoveAllCallbacks(){
        final Integer i = (int)(Math.random() * 100);

        Emitter emitter = new Emitter<Integer>();

        Callback<Integer> callback1 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                fail("Should not have fired");
            }
        };

        Callback<Integer> callback2 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                fail("Should not have fired");
            }
        };

        Callback<Integer> callback3 = new Callback<Integer>() {
            @Override
            public void call(String event, Integer j) {
                fail("Should not have fired");
            }
        };

        emitter.on("test", callback1);
        emitter.on("test", callback2);
        emitter.on("test", callback3);
        emitter.removeAll("test");
        emitter.emit("test", i);
    }
}
