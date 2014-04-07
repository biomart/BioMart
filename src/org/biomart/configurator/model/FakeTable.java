package org.biomart.configurator.model;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;
import org.jdom.Element;

public class FakeTable extends Table {
	private FakeSchema schema;
	
	public FakeTable(String name, FakeSchema schema) {
		super(name);
		this.schema = schema;
	}

	@Override
	public SourceSchema getSchema() {		
		return this.schema;
	}

	@Override
	public Element generateXml() throws FunctionalException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void synchronizedFromXML() {
	}

	@Override
	public boolean hasSubPartition() {
		// TODO Auto-generated method stub
		return false;
	}

}