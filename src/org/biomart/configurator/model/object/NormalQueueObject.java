package org.biomart.configurator.model.object;

import java.util.ArrayList;
import java.util.List;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.Table;

public class NormalQueueObject {
	private Relation rc;
	private List<DatasetColumn> cclist;
	private Table tc;
	private TransformationUnit tu;
	private boolean makeDimension;
	private List<String> nextNameColumns;
	private List<String> nextNameColSuffixes;


	public NormalQueueObject() {
		this.cclist = new ArrayList<DatasetColumn>();
		this.nextNameColumns = new ArrayList<String>();
		this.nextNameColSuffixes = new ArrayList<String>();
	}
	
	public void setRelation(Relation rc) {
		this.rc = rc;
	}

	public Relation getRelation() {
		return rc;
	}
	
	public void setColumnList(List<DatasetColumn> cclist) {
		this.cclist = cclist;
	}
	
	public List<DatasetColumn> getColumnList() {
		return this.cclist;
	}

	public void setTable(Table tc) {
		this.tc = tc;
	}

	public Table getTable() {
		return tc;
	}

	public void setTransformationUnit(TransformationUnit tu) {
		this.tu = tu;
	}

	public TransformationUnit getTransformationUnit() {
		return tu;
	}

	public void setMakeDimension(boolean makeDimension) {
		this.makeDimension = makeDimension;
	}

	public boolean isMakeDimension() {
		return makeDimension;
	}


	public void setNextNameColumns(List<String> nextNameColumns) {
		this.nextNameColumns = nextNameColumns;
	}

	public List<String> getNextNameColumns() {
		return nextNameColumns;
	}

	public void setNextNameColSuffixes(List<String> nextNameColSuffixes) {
		this.nextNameColSuffixes = nextNameColSuffixes;
	}

	public List<String> getNextNameColSuffixes() {
		return nextNameColSuffixes;
	}


	
	
}