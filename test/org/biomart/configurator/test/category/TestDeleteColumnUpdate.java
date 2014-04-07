package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.update.UpdateMartModel;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;
import org.jdom.Element;

public class TestDeleteColumnUpdate extends TestAddingSource {
	@Override
	public boolean test() {
		this.testNewPortal();
		this.testOpenXML(testName);
		//update
		this.update();
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}
	
	private void update() {
		UpdateMartModel updateModel = new UpdateMartModel();
		Mart mart = this.getMart();	
		//replace the password
		PartitionTable pt = mart.getSchemaPartitionTable();
		for(int i=0; i<pt.getTotalRows(); i++) {
			pt.setValue(i, PartitionUtils.PASSWORD, "Bi0M4rt");
		}
		Element element = SettingsForTest.getTestCase(testName);
		Element updateElement = element.getChild("updatedataset");
		String dsName = updateElement.getAttributeValue("name");
		List<Dataset> dss = new ArrayList<Dataset>();
		dss.add(mart.getDatasetByName(dsName));
		updateModel.updateDatasets(dss, false);

	}

}