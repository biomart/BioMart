package org.biomart.configurator.model.object;

import java.util.List;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.Relation;

public class DimensionQueueObject {
	private List<DatasetColumn> dsclist;
	private Relation rc;
	

	public void setRelation(Relation rc) {
		this.rc = rc;
	}

	public Relation getRelation() {
		return rc;
	}

	public void setDatasetColumnlist(List<DatasetColumn> dsclist) {
		this.dsclist = dsclist;
	}

	public List<DatasetColumn> getDatasetColumnlist() {
		return dsclist;
	}
}