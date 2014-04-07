package org.biomart.configurator.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.biomart.common.utils.XMLElements;
import org.biomart.objects.objects.Relation;

public class Relations {
	private Map<String,List<Relation>> firstTableRelations;
	private Map<String,List<Relation>> secondTableRelations;
	
	public Relations() {
		this.firstTableRelations = new HashMap<String,List<Relation>>();
		this.secondTableRelations = new HashMap<String,List<Relation>>();
	}
	
	public void addRelation(Relation r) {
		List<Relation> rs = this.firstTableRelations.get(r.getPropertyValue(XMLElements.FIRSTTABLE));
		if(null==rs) {
			rs = new ArrayList<Relation>();
			this.firstTableRelations.put(r.getPropertyValue(XMLElements.FIRSTTABLE), rs);
		}
		rs.add(r);
		
		List<Relation> rs2 = this.secondTableRelations.get(r.getPropertyValue(XMLElements.SECONDTABLE));
		if(null==rs2) {
			rs2 = new ArrayList<Relation>();
			this.secondTableRelations.put(r.getPropertyValue(XMLElements.SECONDTABLE), rs2);
		}
		rs2.add(r);	
	}
	
	public void removeRelation(Relation r) {
		List<Relation> rs = this.firstTableRelations.get(r.getPropertyValue(XMLElements.FIRSTTABLE));
		if(null!=rs) {
			rs.remove(r);
		}
		
		List<Relation> rs2 = this.secondTableRelations.get(r.getPropertyValue(XMLElements.SECONDTABLE));
		if(null!=rs2) {
			rs2.remove(r);
		}
	}
	
	public Collection<Relation> getRelations() {
		List<Relation> result = new ArrayList<Relation>();
		for(Map.Entry<String, List<Relation>> entry: this.firstTableRelations.entrySet()) {
			result.addAll(entry.getValue());
		}
		Collections.sort(result);
		return result;
	}
	
	public Collection<Relation> getRelationByFirstTable(String tableName) {
		List<Relation> result = this.firstTableRelations.get(tableName);
		if(result==null)
			result = new ArrayList<Relation>();
		Collections.sort(result);
		return result;
	}
	
	public Collection<Relation> getRelationBySecondTable(String tableName) {
		List<Relation> result = this.secondTableRelations.get(tableName);
		if(result==null)
			result = new ArrayList<Relation>();
		Collections.sort(result);
		return result;	
	}
}