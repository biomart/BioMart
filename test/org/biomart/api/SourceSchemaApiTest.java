package org.biomart.api;

import java.util.List;

import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Mart;
import org.junit.Test;
import static org.junit.Assert.*;

public class SourceSchemaApiTest {
	private static Portal _portal;
	private static Mart _mart;
	
	static {
        try {
	        MartRegistryFactory factory = new XmlMartRegistryFactory("./testdata/sourceschemaapi.xml", null);
	        _portal = new Portal(factory, "anonymous");
	        _mart = _portal.getMarts("default").get(0);
        } catch(Exception e) {
            fail("Exception initializing registry");
        }
    }
	
	@Test
	public void testSourceColHide() {
		List<Attribute> atts = _mart.getAttributes("vega58hs",true);
    	boolean containInvalidAttribute = false;
    	for(Attribute a : atts){
    		if(a.getName().equals("transcript__translation__start_exon_id_1066")){
    			containInvalidAttribute = true;
    		}
    	}
    	assertTrue(!containInvalidAttribute);
	}
	
	@Test
	public void testTargetColHide() {
		List<Attribute> atts = _mart.getAttributes("vega58hs",true);
    	boolean containInvalidAttribute = false;
    	for(Attribute a : atts){
    		if(a.getName().equals("transcript__translation__analysis_id_1021")){
    			containInvalidAttribute = true;
    		}
    	}
    	assertTrue(!containInvalidAttribute);
	}
}
