package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.jdom.Element;

public class TestDragAndDropFilter extends TestSynchronize {
	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
    	this.testAddMart(testName);
    	this.addConfigs();
    	this.dragAndDropFilter();
    	this.testSaveXML(testName);
    	return this.compareXML(testName);
	}

	private boolean dragAndDropFilter() {
		// TODO Auto-generated method stub
		Mart mart = this.getMart();
		Element e = SettingsForTest.getTestCase(testName);
		@SuppressWarnings("unchecked")
		List<Element> renameElements = e.getChildren("move");
		for(Element renameElement: renameElements) {
			String configName = renameElement.getAttributeValue("config");
			String type = renameElement.getAttributeValue("type");
			String name = renameElement.getAttributeValue("name");
			String offset = renameElement.getAttributeValue("offset");
			Config config = mart.getConfigByName(configName);
			if(type.equals("filter")){
				Filter filter = config.getFilterByName(name, new ArrayList<String>());
				//reorder filter 
				List<Filter> filters = filter.getParentContainer().getFilterList();
				int index = filters.indexOf(filter);
				int shift = Integer.parseInt(offset);
				int desIndex = index+shift;
				if(index >=0 && index <filters.size() && desIndex >=0 && desIndex <filters.size() ){
					filters.add(desIndex, filter);
					
					if(index > desIndex){
						filters.remove(index + 1);
					}else{
						filters.remove(index);
					}
				}
			}
		}
		
		
		return true;
	}
}
