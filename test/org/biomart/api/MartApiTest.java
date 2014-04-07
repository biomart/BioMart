package org.biomart.api;

import java.util.List;

import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Filter;
import org.biomart.api.lite.Mart;
import org.junit.Test;
import static org.junit.Assert.*;

public class MartApiTest {
	private static Portal _portal;
	private static Mart _mart;

    static {
        try {
        MartRegistryFactory factory = new XmlMartRegistryFactory("./testdata/martapi.xml", null);
        _portal = new Portal(factory, "anonymous");
        _mart = _portal.getMarts("default").get(0);
        } catch(Exception e) {
            fail("Exception initializing registry");
        }
    }
    
    @Test
    public void testInvalidAttribute(){
    	List<Attribute> atts = _mart.getAttributes("hsapiens_gene_vega",true);
    	boolean containInvalidAttribute = false;
    	for(Attribute a : atts){
    		if(a.getName().equals("gene_exon")){
    			containInvalidAttribute = true;
    		}else if(a.getName().equals("cdna")) {
    			containInvalidAttribute = true;
    		}else if(a.getName().equals("coding")) {
    			containInvalidAttribute = true;
    		}
    	}
    	assertTrue(!containInvalidAttribute);
    }
    
    @Test
    public void testInvalidFilter(){
    	List<Filter> filters = _mart.getFilters("hsapiens_gene_vega", true, true);
    	boolean containInvalidFilter = false;
    	for(Filter filter : filters) {
    		if(filter.getName().equals("band_start")) {
    			containInvalidFilter = true;
    		}else if(filter.getName().equals("band_end")) {
    			containInvalidFilter = true;
    		}else if(filter.getName().equals("mark_start")){
    			containInvalidFilter = true;
    		}else if(filter.getName().equals("mark_end")){
    			containInvalidFilter = true;
    		}
    	}
    	assertTrue(!containInvalidFilter);
    }
    
    @Test
    public void testHideAttribute(){
    	List<Attribute> atts = _mart.getAttributes("hsapiens_gene_vega",false,true);
    	boolean containHideAttribute = false;
    	for(Attribute a : atts){
    		if(a.getName().equals("exon_id")){
    			containHideAttribute = true;
    		}else if(a.getName().equals("codon_table_id")) {
    			containHideAttribute = true;
    		}else if(a.getName().equals("transcript_id_key")) {
    			containHideAttribute = true;
    		}
    	}
    	assertTrue(!containHideAttribute);
    	
    	atts = _mart.getAttributes("hsapiens_gene_vega",true,true);
    	containHideAttribute = false;
    	for(Attribute a : atts){
    		if(a.getName().equals("exon_id")){
    			containHideAttribute = true;
    		}else if(a.getName().equals("codon_table_id")) {
    			containHideAttribute = true;
    		}else if(a.getName().equals("transcript_id_key")) {
    			containHideAttribute = true;
    		}
    	}
    	assertTrue(containHideAttribute);
    }
    
    @Test
    public void testHideFilter(){
    	List<Filter> filters = _mart.getFilters("hsapiens_gene_vega", false, false);
    	boolean containHideFilter = false;
    	for(Filter filter : filters) {
    		if(filter.getName().equals("gene_id")) {
    			containHideFilter = true;
    		}else if(filter.getName().equals("transcript_id")) {
    			containHideFilter = true;
    		}
    	}
    	assertTrue(!containHideFilter);
    	
    	filters = _mart.getFilters("hsapiens_gene_vega", true, true);
    	containHideFilter = false;
    	for(Filter filter : filters) {
    		if(filter.getName().equals("gene_id")) {
    			containHideFilter = true;
    		}else if(filter.getName().equals("transcript_id")) {
    			containHideFilter = true;
    		}
    	}
    	assertTrue(containHideFilter);
    }
}
