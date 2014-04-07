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
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Element;

public class TestSynchronize extends TestAddingSource {

	@Override
	public boolean test() {
		this.testNewPortal();
		this.testAddMart(testName);
		this.addConfigs();
		this.changeObjectProperties();
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}
	
	public void addConfigs() {
		Mart mart = this.getMart();
		UserGroup ug = SettingsForTest.getUserGroup(testName);
		ObjectController oc = new ObjectController();
		List<String> configs = this.getNewNaiveConfigs();
		for(String config: configs) {
			GuiContainer gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().
				getRootGuiContainer().getGCByNameRecursively(XMLElements.DEFAULT.toString());
			oc.addConfigFromMaster(mart, config, ug, false, gc);
		}
	}
	
	private List<String> getNewNaiveConfigs() {
		List<String> configs = new ArrayList<String>();
		Element e = SettingsForTest.getTestCase(testName);
		@SuppressWarnings("unchecked")
		List<Element> configElements = e.getChildren("newconfig");
		for(Element configElement: configElements) {
			configs.add(configElement.getAttributeValue("name"));
		}
		return configs;
	}
	
	private void changeObjectProperties() {
		Mart mart = this.getMart();
		Element e = SettingsForTest.getTestCase(testName);
		@SuppressWarnings("unchecked")
		List<Element> renameElements = e.getChildren("rename");
		for(Element renameElement: renameElements) {
			String configName = renameElement.getAttributeValue("config");
			String type = renameElement.getAttributeValue("type");
			String name = renameElement.getAttributeValue("name");
			String newvalue = renameElement.getAttributeValue("newvalue");
			Config config = mart.getConfigByName(configName);
			Attribute att = config.getAttributeByName(name, new ArrayList<String>());
			MartController.getInstance().setProperty(att, XMLElements.DISPLAYNAME.toString(), newvalue, null);
			if(config.isMasterConfig()) {
				List<Attribute> clist = McGuiUtils.INSTANCE.findAttributeInOtherConfigs(att);
				for(Attribute c: clist) {
					MartController.getInstance().setProperty(c, XMLElements.DISPLAYNAME.toString(), newvalue, null);
				}
			}
		}
		@SuppressWarnings("unchecked")
		List<Element> deleteElements = e.getChildren("delete");
		for(Element deleteElement: deleteElements) {
			String configName = deleteElement.getAttributeValue("config");
			String type = deleteElement.getAttributeValue("type");
			String name = deleteElement.getAttributeValue("name");
			Config config = mart.getConfigByName(configName);
			Attribute att = config.getAttributeByName(name, new ArrayList<String>());
			att.getParentContainer().removeAttribute(att);
		}
		@SuppressWarnings("unchecked")
		List<Element> hideElements = e.getChildren("hide");
		for(Element hideElement: hideElements) {
			String configName = hideElement.getAttributeValue("config");
			String type = hideElement.getAttributeValue("type");
			String name = hideElement.getAttributeValue("name");
			Config config = mart.getConfigByName(configName);
			Attribute att = config.getAttributeByName(name, new ArrayList<String>());
			att.setHideValue(true);
		}
	}

}