package org.biomart.dino.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.biomart.common.utils.XMLElements;
import org.biomart.dino.Binding;
import org.biomart.dino.annotations.Func;
import org.biomart.dino.tests.fixtures.TestDino;
import org.biomart.objects.objects.Element;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.QueryElementType;
import org.junit.Before;
import org.junit.Test;

public class BindingTest {

    List<Field> fields;
    List<QueryElement> qes;
    List<Element> els;
    final String stAnn = "first", sdAnn = "second";
    Map<String, Element> m;
    
    final Class<TestDino> testDinoClass = TestDino.class;

    @Before
    public void setUp() {
        // Field class cannot be mocked :(

        Field[] fs = org.biomart.dino.tests.fixtures.MetaData.class
                .getDeclaredFields();
        Element e1 = mock(Element.class), e2 = mock(Element.class), 
                e3 = mock(Element.class);
        QueryElement q1 = mock(QueryElement.class), q2 = mock(QueryElement.class), 
                q3 = mock(QueryElement.class);

        when(q1.getElement()).thenReturn(e1);
        when(q2.getElement()).thenReturn(e2);
        when(q3.getElement()).thenReturn(e3);
        when(e1.getPropertyValue(XMLElements.FUNCTION)).thenReturn(stAnn);
        when(e2.getPropertyValue(XMLElements.FUNCTION)).thenReturn(sdAnn);
        when(e3.getPropertyValue(XMLElements.FUNCTION)).thenReturn("foo");

        fields = Arrays.asList(fs);
        qes = new ArrayList<QueryElement>();
        qes.add(q1);
        qes.add(q2);
        qes.add(q3);
        
        els = new ArrayList<Element>();
        els.add(e1);
        els.add(e2);
        els.add(e3);

        m = new HashMap<String, Element>();
        m.put(stAnn, e1);
        m.put(sdAnn, e2);
    }

    @Test
    public void setBindingsTest() {
        Binding md = new Binding();

        assertFalse(fields.isEmpty());

        md.setBindings(fields, qes);
        Map<String, Element> binding = md.getBindings();

        assertTrue(binding.size() != 0);
        assertEquals(m, binding);
    }
    
//    @Test
//    public void setBindingsByElementTest() {
//        Binding md = new Binding();
//        md.setBindingsByElement(fields, els);
//        Map<String, Element> binding = md.getBindings();
//
//        assertTrue(binding.size() != 0);
//        assertEquals(m, binding);
//    }
    
//    @Test(expected = ValidationException.class)
//    public void checkBindingTest() {
//        Binding md = new Binding();
//        els.remove(0);
//        md.setBindingsByElement(fields, els);
//        md.checkBinding(fields);
//
//    }
    
    @Test
    public void getAnnotatedFieldsTest() {
        List<Field> fields = Binding.getAnnotatedFields(testDinoClass);
        assertEquals("only two fields are annotated", 2, fields.size());
        Func f = fields.get(0).getAnnotation(Func.class), s = fields.get(1)
                .getAnnotation(Func.class);
        assertEquals("first", f.id());
        assertFalse(f.optional());
        assertEquals("second", s.id());
        assertTrue(s.optional());
    }

    @Test
    public void setFieldValuesTest() throws IllegalArgumentException,
            IllegalAccessException {
        String otherFilter = "other filter", otherAttribute = "other attribute", first = "first", second = "second", stVal = "1st";
        Element e = null;
        QueryElement qe;
        TestDino dino = new TestDino();
        List<Field> fds = Binding.getAnnotatedFields(testDinoClass);
        List<QueryElement> expectedBoundEls = new ArrayList<QueryElement>(), boundEls = null;

        List<QueryElement> l = new ArrayList<QueryElement>();

        // I'm adding these to have a more generic case
        e = TestSupport.mockAttributeElement(XMLElements.FUNCTION,
                otherAttribute, otherAttribute);
        l.add(TestSupport.mockQE(e, QueryElementType.ATTRIBUTE));

        e = TestSupport.mockFilterElement(XMLElements.FUNCTION, otherFilter,
                null);
        // notice that i'm not mocking the getFilterValues method here.
        l.add(TestSupport.mockQE(e, QueryElementType.FILTER));

        e = TestSupport.mockAttributeElement(XMLElements.FUNCTION, second,
                second);
        l.add(qe = TestSupport.mockQE(e, QueryElementType.ATTRIBUTE));
        expectedBoundEls.add(qe);

        e = TestSupport.mockAttributeElement(XMLElements.FUNCTION, first, null);
        l.add(qe = TestSupport.mockQE(e, QueryElementType.FILTER));
        when(qe.getFilterValues()).thenReturn(stVal);
        expectedBoundEls.add(qe);

        boundEls = Binding.setFieldValues(dino, fds, l);

        assertEquals(stVal, dino.getF1());
        assertEquals(second, dino.getF2());

        Iterator<QueryElement> it = boundEls.iterator();
        while (it.hasNext()) {
            qe = it.next();
            assertTrue(expectedBoundEls.contains(qe));
        }

        // assertEquals("it returned the proper QueryElement elements",
        // expectedBoundEls, boundEls);
    }

}
