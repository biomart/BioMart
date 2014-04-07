package org.biomart.objects.objects;

import org.biomart.common.exceptions.AssociationException;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class RelationTarget extends Relation {

	public RelationTarget(Element element) {
		super(element);
		this.setNodeType(McNodeType.RELATION);
	}
	
	public RelationTarget(Key pk, Key fk, Cardinality cardinality) throws AssociationException {
		super(pk,fk,cardinality);
		this.setNodeType(McNodeType.RELATION);
	}
	
	@Override
	/**
	 * for a source relation, need a mart parameter because a source can be used in 
	 * multiple mart. no needed in target relation.
	 * @param dataset
	 * @return
	 */
	public boolean isSubclassRelation(String mart) {
		DatasetTable firstT = (DatasetTable)this.getFirstKey().getTable();
		DatasetTable secondT = (DatasetTable)this.getSecondKey().getTable();
		if(firstT.getType()!=DatasetTableType.DIMENSION && secondT.getType()!=DatasetTableType.DIMENSION)
			return true;
		else
			return false;
	}
	
}