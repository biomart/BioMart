package org.biomart.configurator.utils.type;

import org.biomart.common.utils.XMLElements;

public enum McNodeType {
	MARTREGISTRY (XMLElements.MARTREGISTRY.toString()),
	PORTAL (XMLElements.PORTAL.toString()),
	USERS (XMLElements.USERS.toString()),
	LINK (XMLElements.LINK.toString()),
	GUICONTAINER (XMLElements.GUICONTAINER.toString()),
	MARTPOINTER (XMLElements.MARTPOINTER.toString()),
	PROCESSORS (XMLElements.PROCESSORGROUP.toString()),
	PROCESSOR (XMLElements.PROCESSOR.toString()),
	USER (XMLElements.USER.toString()),
	GROUP (XMLElements.GROUP.toString()),
	SCHEMA (XMLElements.SOURCESCHEMA.toString()),
	LINKINDEXES (XMLElements.LINKINDEXES.toString()),
	LINKINDEX (XMLElements.LINKINDEX.toString()),
	MART (XMLElements.MART.toString()),
	CONFIG(XMLElements.CONFIG.toString()),
	DATASET (XMLElements.DATASET.toString()),
	CONTAINER (XMLElements.CONTAINER.toString()), 
	PARTITIONTABLE (XMLElements.PARTITIONTABLE.toString()),
	TABLE (XMLElements.TABLE.toString()),
	SOURCETABLE(XMLElements.TABLE.toString()),
	COLUMN (XMLElements.COLUMN.toString()),
	PRIMARYKEY (XMLElements.PRIMARYKEY.toString()),
	FOREIGNKEY (XMLElements.FOREIGNKEY.toString()),
	FILTER (XMLElements.FILTER.toString()),
	ATTRIBUTE (XMLElements.ATTRIBUTE.toString()),
	RDFCLASS (XMLElements.RDFCLASS.toString()),
	IMPORTABLE (XMLElements.IMPORTABLE.toString()),
	EXPORTABLE (XMLElements.EXPORTABLE.toString()),
	RELATION (XMLElements.RELATION.toString()),
	SOURCERELATION (XMLElements.RELATION.toString()),
	SOURCECONTAINERS (XMLElements.SOURCECONTAINERS.toString()),
	SOURCECONTAINER (XMLElements.SOURCECONTAINER.toString());
	
	private String value;

	
	McNodeType(String value) {
		this.value = value;
	}
	


	public String toString() {
		return this.value;
	}
}