package org.biomart.objects.objects;

import javax.swing.JOptionPane;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.Cardinality;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.jdom.Element;

public class RelationSource extends Relation {

	public RelationSource(Element element) {
		super(element);
		this.setNodeType(McNodeType.SOURCERELATION);
	}
	
	public RelationSource(Key pk, Key fk, Cardinality cardinality) throws AssociationException {
		super(pk,fk,cardinality);
		this.setNodeType(McNodeType.SOURCERELATION);
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	/*
	 * TODO make it generic to relation and sourcerelation
	 */
	public void synchronizedFromXML() {

		Table ft = ((SourceSchema)this.getParent()).getTableByName(this.getPropertyValue(XMLElements.FIRSTTABLE));
		Table st = ((SourceSchema)this.getParent()).getTableByName(this.getPropertyValue(XMLElements.SECONDTABLE));

		PrimaryKey fKey = ft.getPrimaryKey();
		ForeignKey sKey = st.getForeignKeyByName(this.getPropertyValue(XMLElements.SECONDKEY));
		
		String firstKeyName = this.getPropertyValue(XMLElements.FIRSTKEY);
		if (fKey == null || !fKey.getName().equals(firstKeyName) || sKey == null) {
			//should not go here
			Log.debug("**** relation error: CHECK in mart "+this.getParent().getName() +  
					" ft "+this.getPropertyValue(XMLElements.FIRSTTABLE) + " fk "+
					this.getPropertyValue(XMLElements.FIRSTKEY) + " st "+st.getName() + " sk " +
					this.getPropertyValue(XMLElements.SECONDKEY) + 
					" (" + (fKey == null) + ", " + (sKey == null) + ", " + (fKey==null ? null : fKey.getName()) + ")"
					);
			JOptionPane.showMessageDialog(null, "invalid relation found");
		} else {	
			try {
				this.setOriginalCardinality(Cardinality.valueFrom(this.getPropertyValue(XMLElements.TYPE)));
				this.setCardinality(Cardinality.valueFrom(this.getPropertyValue(XMLElements.TYPE)));
				this.setStatus(ComponentStatus.INFERRED);
			} catch (AssociationException e) {
				e.printStackTrace();
			} 
		}
		
		this.setObjectStatus(ValidationStatus.VALID);

	}
	
}