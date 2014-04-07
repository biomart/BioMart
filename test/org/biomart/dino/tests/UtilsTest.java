package org.biomart.dino.tests;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.biomart.dino.Utils;
import org.biomart.objects.objects.Attribute;
import org.biomart.queryEngine.QueryElement;
import org.junit.Before;
import org.junit.Test;

public class UtilsTest {

	Attribute forId, forSpec, a;
	QueryElement qe;
	List<Attribute> aList;
	
	@Before
	public void setUp() {
		forId = mock(Attribute.class);
		forSpec = mock(Attribute.class);
		a = mock(Attribute.class);
		aList = new ArrayList<Attribute>();
		aList.add(forId); aList.add(forSpec);
		when(a.getAttributeList()).thenReturn(aList);
		qe = mock(QueryElement.class);
		when(qe.getElement()).thenReturn(a);
	}
	
	@Test
	public void getAttributeForEnsemblSpeciesIdTranslationTest() {
		Attribute gotA = Utils.getAttributeForEnsemblGeneIdTranslation(qe);
		assertEquals(forId, gotA);
	}
	
	@Test
	public void getAttributeForEnsemblSpeciesIdTranslationFailTest0() {
		aList.clear();
		Attribute gotA = Utils.getAttributeForEnsemblGeneIdTranslation(qe);
		assertNull(gotA);
	}
	
	@Test
	public void getAttributeForEnsemblSpeciesIdTranslationFailTest1() {
		when(a.getAttributeList()).thenReturn(new ArrayList<Attribute>());
		Attribute gotA = Utils.getAttributeForEnsemblGeneIdTranslation(qe);
		assertNull(gotA);
	}

	@Test
	public void getAttributeForEnsemblSpecieIdTranslationTest() {
		Attribute gotA = Utils.getAttributeForAnnotationRetrieval(qe);
		assertEquals(forSpec, gotA);
	}
	
	@Test
	public void getAttributeForEnsemblSpecieIdTranslationFailTest0() {
		aList.remove(0);
		Attribute gotA = Utils.getAttributeForAnnotationRetrieval(qe);
		assertNull(gotA);
	}
	
	@Test
	public void getAttributeForEnsemblSpecieIdTranslationFailTest1() {
		when(a.getAttributeList()).thenReturn(new ArrayList<Attribute>());
		Attribute gotA = Utils.getAttributeForAnnotationRetrieval(qe);
		assertNull(gotA);
	}

}
