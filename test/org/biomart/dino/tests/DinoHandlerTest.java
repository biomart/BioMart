package org.biomart.dino.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.dino.DinoHandler;
import org.biomart.dino.tests.fixtures.TestDino;
import org.biomart.objects.objects.Element;
import org.biomart.dino.tests.TestSupport;
import org.biomart.dino.annotations.Func;
import org.biomart.dino.dinos.Dino;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.QueryElementType;
import org.junit.Test;

public class DinoHandlerTest {
    final String dinoClassName = "org.biomart.dino.tests.fixtures.TestDino";
    final Class<TestDino> testDinoClass = TestDino.class;

    @Test
    public void getDinoClassTest() {
        try {
            Class<? extends Dino> klass = DinoHandler
                    .getDinoClass(dinoClassName);
            assertEquals("returned the proper class", dinoClassName,
                    klass.getName());
        } catch (ClassNotFoundException e) {
            fail("getDinoClass has raised an exception");
        }
    }

    @Test(expected = ClassNotFoundException.class)
    public void getDinoClassNotFoundTest() throws ClassNotFoundException {
        String className = "wat";
        @SuppressWarnings("unused")
        Class<? extends Dino> klass = DinoHandler.getDinoClass(className);
    }

    @Test(expected = RuntimeException.class)
    public void getDinoClassCastFailTest() throws ClassNotFoundException {
        String className = "java.lang.String";
        @SuppressWarnings("unused")
        Class<? extends Dino> klass = DinoHandler.getDinoClass(className);
    }

    @Test
    public void getDinoInstanceTest() {
        Dino dino = null;
        try {
            dino = DinoHandler.getDinoInstance(TestDino.class);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("returns the proper instance", dino.getClass(),
                testDinoClass);
    }


}
