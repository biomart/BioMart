/**
 * 
 */
package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.jdom.Element;

/**
 * @author lyao
 *
 */
public class TestSynchronizeFilterList extends TestSynchronize {
	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
		this.testAddMart(testName);
		this.addConfigs();
		this.testFilterList();
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}

	private void testFilterList() {
		Mart mart = this.getMart();			
		Element e = SettingsForTest.getTestCase(testName);
		@SuppressWarnings("unchecked")
		List<Element> createElements = e.getChildren("create");
		for(Element createElement: createElements) {
			String configName = createElement.getAttributeValue("config");
			String type = createElement.getAttributeValue("type");
			String name = createElement.getAttributeValue("name");
			String displayName = createElement.getAttributeValue("displayname");

			Config config = mart.getConfigByName(configName);
			if(type.equals("filterlist")){
				//create filter list
				Filter filterList = new Filter(name,displayName);
				@SuppressWarnings("unchecked")
				List<Element> attrs = createElement.getChildren("filter");
				for(Element att : attrs){
					Filter filter = config.getFilterByName(att.getAttributeValue("name"), null);
					filterList.addFilter(filter);					
				}
				//add attribute list to the naive config
				config.getRootContainer().addFilter(filterList);
			}
			//sync with master config			
			config.syncWithMasterconfig();
			
		}
		@SuppressWarnings("unchecked")
		List<Element> renameElements = e.getChildren("rename");
		for(Element renameElement: renameElements) {
			String configName = renameElement.getAttributeValue("config");
			String type = renameElement.getAttributeValue("type");
			String name = renameElement.getAttributeValue("name");
			String newvalue = renameElement.getAttributeValue("newvalue");
			Config config = mart.getConfigByName(configName);
			Filter filter = config.getFilterByName(name, new ArrayList<String>());
			List<Filter> clist = McGuiUtils.INSTANCE.findFilterInOtherConfigs(filter);			
			
			MartController.getInstance().renameFilter(filter, newvalue, null);
			if(config.isMasterConfig()) {				
				for(Filter c: clist) {
					
					MartController.getInstance().renameFilter(c, newvalue, null);
				}
			}
		}		
	}
}
