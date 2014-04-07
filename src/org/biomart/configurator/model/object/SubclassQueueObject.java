package org.biomart.configurator.model.object;

import java.util.List;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Relation;

public class SubclassQueueObject {
	private List<DatasetColumn> cclist;
	private Relation rc;
	

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
		return cclist;
	}
	
}