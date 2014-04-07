package org.biomart.configurator.test.category;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Element;

public class TestAddReport extends TestAddingSource {

	@Override
	public boolean test() {
		this.testNewPortal();
		this.testOpenXML(testName);
		addReports();
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}
	
	private void addReports() {
		Mart mart = null;
		List<Mart> ml = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
		if(ml.size()>0)
			mart = ml.iterator().next();
		if(mart == null)
			return;
		UserGroup ug = SettingsForTest.getUserGroup(testName);
		ObjectController oc = new ObjectController();
		List<String> configs = this.getNewNaiveConfigs();
		for(String config: configs) {
			GuiContainer gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().
				getRootGuiContainer().getGCByNameRecursively("report");

			Config con = oc.addReportConfigFromMaster(mart, config, ug);
			String attributeName = SettingsForTest.getAttribute(testName);
			Attribute attribute = mart.getMasterConfig().getAttributeByName(attributeName, new ArrayList<String>());
			oc.initReportConfig(con, attribute, gc);
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
	
}