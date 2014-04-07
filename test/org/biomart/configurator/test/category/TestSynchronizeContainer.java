package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.jdom.Element;

public class TestSynchronizeContainer extends TestSynchronize {
	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
		this.testAddMart(testName);
		this.addConfigs();
		this.testAddContainer();
		
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}

	private void testAddContainer() {
		// TODO Auto-generated method stub
		Mart mart = this.getMart();
		Element e = SettingsForTest.getTestCase(testName);
		@SuppressWarnings("unchecked")
		List<Element> createElements = e.getChildren("create");
		for(Element createElement: createElements) {
			String configName = createElement.getAttributeValue("config");
			String type = createElement.getAttributeValue("type");
			String name = createElement.getAttributeValue("name");
			String parent = createElement.getAttributeValue("parent");

			Config config = mart.getConfigByName(configName);
			if(type.equals("container")){
				//create filter list
				Container container = new Container(name);				
				//add attribute list to the naive config
				if(parent.equals("root")){
					config.getRootContainer().addContainer(container);
				}else {
					config.getContainerByName(parent).addContainer(container);
				}
			}
			
		}
	}
}
