package org.biomart.configurator.test.category;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.update.UpdateMartModel;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;
import org.jdom.Element;

public class TestUpdate extends TestAddingSource {
	
	public void testUpdate() throws MartBuilderException{
		UpdateMartModel updateModel = new UpdateMartModel();
		Mart mart = this.getMart();
		PartitionTable pt = mart.getSchemaPartitionTable();
		
		Element element = SettingsForTest.getTestCase(testName);
		Element updateElement = element.getChild("updatedatabase");
		String database = updateElement.getAttributeValue("database");
		String schema = updateElement.getAttributeValue("schema");
		
		for(int i=0;i<pt.getTotalRows();i++) {
			pt.setValue(i, PartitionUtils.DATABASE, database);
			pt.setValue(i, PartitionUtils.SCHEMA, schema);
		}
		
		//updateModel.updateMart(mart);
		updateModel.updateDatasets(mart.getDatasetList(), false);
	}

	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
		this.testAddMart(testName);
		//this.testSaveXML(testName);
		//this.testOpenXML(testName);
		try {
			this.testUpdate();
		} catch (MartBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}
	
}
