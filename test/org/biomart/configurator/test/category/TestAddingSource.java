package org.biomart.configurator.test.category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.test.SettingsForTest;
import org.biomart.configurator.test.XMLCompare;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.portal.UserGroup;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class TestAddingSource extends McTestCategory {
	
	public Mart getMart() {
		Mart mart = null;
		List<Mart> ml = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
		if(ml.size()>0)
			mart = ml.iterator().next();
		return mart;
	}
	
	public void testAddMart(String testcase) {
		List<DataLinkInfo> dliList = SettingsForTest.getDataLinkInfo(testcase);
		for(DataLinkInfo dli: dliList) {
			dli.setIncludePortal(true);
			UserGroup ug = SettingsForTest.getUserGroup(testcase);
			ObjectController oc = new ObjectController();
			try {
				oc.initMarts(dli, ug, XMLElements.DEFAULT.toString());
			} catch (MartBuilderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void testNewPortal() {
		McGuiUtils.INSTANCE.setRegistry(null);
		Options.getInstance().setOptions(null);
		McViews.getInstance().clean();
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		File newfile = new File("conf/xml/untitled.xml");
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(newfile);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			return;
		}
		try {
			McMenus.getInstance().requestCreateObjects(registry, document);
		} catch (MartBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testOpenXML(String testcase){
		McGuiUtils.INSTANCE.setRegistry(null);
		Options.getInstance().setOptions(null);
		McViews.getInstance().clean();
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		String filePath = SettingsForTest.getBaseXMLPath(testcase);
		File file = new File(filePath);
		//set key file
		String tmpName = file.getName();
		int index = tmpName.lastIndexOf(".");
		if(index>0)
			tmpName = tmpName.substring(0,index);
		String keyFileName = file.getParent()+File.separator+"."+tmpName;
		BufferedReader input;
		String key=null;
		try {
			input = new BufferedReader(new FileReader(keyFileName));
			key = input.readLine();
			input.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.error("key file not found");
			//if key file no found generate one

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		McUtils.setKey(key);
		
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		MartController.getInstance().requestCreateRegistryFromXML(registry, document);
	}
	
	public void testSaveXML(String testcase) {
		String filePath = SettingsForTest.getSavedXMLPath(testcase);
		File file = new File(filePath);
		McMenus.getInstance().requestSavePortalToFile(file, false);
	}
	
	public boolean compareXML(String testcase) {
		String file1 = SettingsForTest.getSourceXMLPath(testcase);
		String file2 = SettingsForTest.getSavedXMLPath(testcase);
		Document document1 = null;
		Document document2 = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document1 = saxBuilder.build(file1);
			document2 = saxBuilder.build(file2);
		}
		catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage());
			return false;
		}
		
		Element root1 = document1.getRootElement();
		Element root2 = document2.getRootElement();
		XMLCompare xc = new XMLCompare();
		return xc.compare(root1,root2);

	}

	@Override
	public boolean test() {
		this.testNewPortal();
		this.testAddMart(testName);
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}
}