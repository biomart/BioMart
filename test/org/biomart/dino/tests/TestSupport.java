package org.biomart.dino.tests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.StringUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Element;
import org.biomart.objects.objects.Filter;
import org.biomart.queryEngine.QueryElement;
import org.biomart.queryEngine.QueryElementType;

public class TestSupport {

    public static final String sep = System.getProperty("file.separator");
    
	public static Attribute 
	mockAttributeElement(XMLElements key, String pvalue, String name) {
		Attribute e = mock(Attribute.class);
		baseElementMock(e, key, pvalue, name);
		return e;
	}
	
	public static Filter 
	mockFilterElement(XMLElements key, String pvalue, String name) {
		Filter e = mock(Filter.class);
		baseElementMock(e, key, pvalue, name);
		return e;
	}
	
	public static void
	baseElementMock(Element e, XMLElements key, String pvalue, String name) {
		when(e.getPropertyValue(key)).thenReturn(pvalue);
		when(e.getName()).thenReturn(name == null ? "mock name" : name);
	}
	
	public static QueryElement mockQE(Element e, QueryElementType type) {
		QueryElement qe = mock(QueryElement.class);
		when(qe.getElement()).thenReturn(e);
		when(qe.getType()).thenReturn(type);
		return qe;
	}
	
	public static String fixtureDir() {
	    final String s = System.getProperty("file.separator");
	    return StringUtils.join(new String[] {
	            System.getProperty("user.dir"), "test", "org", "biomart", "dino",
	            "tests", "fixtures"
	    }, s);
	}
	
//	public static Method getMethod(Class<?> klass, String mName, Class<?>... args) {
//	    Method method = klass.getDeclaredMethod(mName, args);
//        method.setAccessible(true);
//        
//        return method;
//	}
//	
//	public static Object callMethod(Method m, Object obj, Object... args) {
//	    return m.invoke(obj, args);
//	}
}
