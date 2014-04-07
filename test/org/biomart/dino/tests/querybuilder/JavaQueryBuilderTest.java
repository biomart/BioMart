package org.biomart.dino.tests.querybuilder;

import org.biomart.api.Portal;
import org.biomart.api.Query;
import org.biomart.dino.querybuilder.JavaQueryBuilder;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.biomart.dino.querybuilder.QueryBuilder;
import org.junit.Before;
import org.junit.Test;

public class JavaQueryBuilderTest {
	QueryBuilder builder;
	Query q;
	Query.Dataset d;

	@Before
	public void setUp() {
		q = mock(Query.class);
		d = mock(Query.Dataset.class);
		when(q.addDataset(anyString(), anyString())).thenReturn(d);
		
		Portal portal = mock(Portal.class);
		
		builder = new JavaQueryBuilder(portal);
	}
	
	private void initTestHelper(QueryBuilder builder) {
		assertFalse(builder.hasHeader());
		assertEquals("false", builder.getClient());
		assertEquals("TSV", builder.getProcessor());
		assertEquals(-1, builder.getLimit());
		assertEquals("", builder.getDatasetName());
		assertEquals("", builder.getDatasetConfig());
	}
	
	@Test
	public void initialStateTest() {
		initTestHelper(builder);
	}
	
//	@Test
//	public void getResultsTest() {
//		ByteArrayOutputStream o = new ByteArrayOutputStream();
//		builder.getResults(o);
//		verify(q).getResults(o);
//	}
	
	@Test
	public void setHeaderTest() {
		builder.setHeader(true);
//		verify(q).setHeader(true);
		assertTrue(builder.hasHeader());
	}
	
	@Test
	public void setClientTest() {
		builder.setClient("wat?");
//		verify(q).setClient("wat?");
		assertEquals("wat?", builder.getClient());
	}
	
	@Test
	public void setProcessorTest() {
		builder.setProcessor("CSV");
//		verify(q).setProcessor("CSV");
		assertEquals("CSV", builder.getProcessor());
	}

	@Test
	public void setLimitTest() {
		builder.setLimit(42);
//		verify(q).setLimit(42);
		assertEquals(42, builder.getLimit());
	}
	
	@Test
	public void setDatasetTest() {
		builder.setDataset("ninni", "cucchiaio");
//		verify(q).addDataset("ninni", "cucchiaio");
		assertEquals("ninni", builder.getDatasetName());
		assertEquals("cucchiaio", builder.getDatasetConfig());
	}
	
	@Test(expected = NullPointerException.class)
	public void addFilterFailTest() {
		builder.addFilter("n", "v");
	}
	
	@Test
	public void addFilterTest() {
		builder.setDataset("ninni", "cucchiaio");
		builder.addFilter("n", "v");
//		verify(q).addDataset("ninni", "cucchiaio");
//		verify(d).addFilter("n", "v");
	}
	
	@Test
	public void addAttributeTest() {
		builder.setDataset("ninni", "cucchiaio");
		builder.addAttribute("n");
//		verify(q).addDataset("ninni", "cucchiaio");
//		verify(d).addAttribute("n");
	}
	
//	@Test
//	public void getXmlTest() {
//		String xml = builder.getXml(), xmlq;
////		verify(q).getXml();
//		xmlq = q.getXml();
//		assertEquals(xml, xmlq);
//	}
	
	@Test
	public void initTest() {
		builder.setClient("foo")
			.setHeader(false)
			.setLimit(2)
			.setDataset("a", "b")
			.setProcessor("CSV")
			.addAttribute("a")
			.addFilter("f", "v");
		
		builder.init();
		initTestHelper(builder);
		
	}
}




























