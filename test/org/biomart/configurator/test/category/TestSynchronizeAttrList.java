package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Element;

public class TestSynchronizeAttrList extends TestSynchronize {

	/* (non-Javadoc)
	 * @see org.biomart.configurator.test.category.TestAddingSource#test()
	 */
	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
		this.testAddMart(testName);
		this.addConfigs();
		testAttributeList();
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}


	private void testAttributeList(){
		
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
			if(type.equals("attributelist")){
				//create attribute list
				Attribute attrList = new Attribute(name,displayName);
				@SuppressWarnings("unchecked")
				List<Element> attrs = createElement.getChildren("attribute");
				for(Element att : attrs){
					Attribute attribute = config.getAttributeByName(att.getAttributeValue("name"), null);
					attrList.addAttribute(attribute);					
				}
				//add attribute list to the naive config
				config.getRootContainer().addAttribute(attrList);
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
			Attribute att = config.getAttributeByName(name, new ArrayList<String>());
			List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs(att);			
			
			MartController.getInstance().renameAttribute(att, newvalue, null);
			if(config.isMasterConfig()) {				
				for(Attribute c: clist) {
					
					MartController.getInstance().renameAttribute(c, newvalue, null);
				}
			}
		}
		
		
	}
	
}
