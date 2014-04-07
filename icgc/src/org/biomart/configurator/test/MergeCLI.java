package org.biomart.configurator.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class MergeCLI {
	
	private String originalRegistryFile;
	private String updatedRegistryFile;
	private String datasetRoot;
	
	private HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>> updatedInfo = new HashMap<String, HashMap<String,HashMap<String,ArrayList<String>>>>();
	
	public static void main(String[] args) {
		if(args.length<3) {
			System.exit(0);
		}
  		Resources.setResourceLocation("org/biomart/configurator/resources");  	
        Settings.loadGUIConfigProperties();
        Settings.loadAllConfigProperties();
        
        MergeCLI mergeCLI = new MergeCLI();
        
        mergeCLI.update(args[0], args[1], args[2]);
	}
	
	private void update(String originalRegistryFile, String updatedRegistryFile, String datasetRoot){
		this.originalRegistryFile = originalRegistryFile;
		this.updatedRegistryFile = updatedRegistryFile;
		this.datasetRoot = datasetRoot;

		Element updatedOptions = this.loadUpdatedTables();

		MartRegistry originalRegistry = this.openXML(this.originalRegistryFile);
		
		Element originalOptions = Options.getInstance().getOptionRootElement();
		
		Element mergedOptions = this.mergeOptions(originalOptions, updatedOptions);

		Options.getInstance().setOptions(mergedOptions);

		for(Mart originalMart : originalRegistry.getMartList()){
			Dataset originalDS = originalMart.getDatasetBySuffix(this.datasetRoot);
			if(null != originalDS){
				String originalDSname = originalDS.getName();

				if(this.updatedInfo.containsKey(originalMart.getName())){
					HashMap<String, ArrayList<String>> updatedTables = this.updatedInfo.get(originalMart.getName()).get(originalDSname);

					for(DatasetTable originalTable : originalMart.getDatasetTables()){

						ArrayList<String> updatedColumns = updatedTables.get(originalTable.getName());

						if(null != updatedColumns){
							originalTable.addInPartitions(originalDSname);

							for(Column originalColumn : originalTable.getColumnList()){
								if(updatedColumns.contains(originalColumn.getName())){
									originalColumn.addInPartitions(originalDSname);
								} else {
									originalColumn.removeFromPartitions(originalDSname);
								}
							}
						} else {
							originalTable.removeFromPartition(originalDSname);
						}
					}
				}
			}
		}
		
		for(String martName : this.updatedInfo.keySet()){
			Mart originalMart = originalRegistry.getMartByName(martName);
			if(originalMart !=null){
				HashMap<String, HashMap<String, ArrayList<String>>> datasets = this.updatedInfo.get(martName);
				for(String dsName : datasets.keySet()){
					// Dataset originalDS = originalMart.getDatasetByName(dsName);
					// if(originalDS != null){ 
					HashMap<String, ArrayList<String>> tables = datasets.get(dsName);
					for(String tableName : tables.keySet()){
						DatasetTable originalTable = originalMart.getTableByName(tableName);
						if(originalTable == null){
							 originalTable = new DatasetTable(originalMart, tableName, DatasetTableType.DIMENSION);
							 originalTable.addInPartitions(dsName);
							 originalMart.addTable(originalTable);
						}
						
						ArrayList<String> columns = tables.get(tableName);
						for(String column : columns){
							if(originalTable.getColumnByName(column) == null){
								DatasetColumn newColumn = new DatasetColumn(originalTable, column);
								originalTable.addColumn(newColumn);
								newColumn.addInPartitions(dsName);
							}
						}
					}
					//}
				}
			}
			MartController.getInstance().createNaiveForOrphanColumn(originalMart);
		}
		
		this.saveXML();
	}
	
	@SuppressWarnings("unchecked")
	private Element mergeOptions(Element originalOptions, Element updatedOptions) {
		
		HashMap<String, Element> updatedOptionMap = new HashMap<String, Element>();
		
		for(Element mart : (List<Element>)updatedOptions.getChildren()){
			for (Element config : (List<Element>)mart.getChildren()){
				for (Element filter : (List<Element>)config.getChildren()){
					for (Element dataset : (List<Element>)filter.getChildren()){
						if(dataset.getAttributeValue("name").endsWith(this.datasetRoot)){
							String martName = mart.getAttributeValue("name");
							String configName = config.getAttributeValue("name");
							String filterName = filter.getAttributeValue("name");
							updatedOptionMap.put(martName + ";" + configName + ";" + filterName, dataset);
						}
					}
				}
			}
		}
		
		for(Element mart : (List<Element>)originalOptions.getChildren()){
			for (Element config : (List<Element>)mart.getChildren()){
				for (Element filter : (List<Element>)config.getChildren()){
					for (Element dataset : (List<Element>)filter.getChildren()){
						String martName = mart.getAttributeValue("name");
						String configName = config.getAttributeValue("name");
						String filterName = filter.getAttributeValue("name");
						if(dataset.getAttributeValue("name").endsWith(this.datasetRoot)){
							dataset.detach();
						}
						Element replacementDataset = updatedOptionMap.get(martName + ";" + configName + ";" + filterName);
						if(dataset!=null){
							filter.addContent(replacementDataset);
						}
					}
				}
			}
		}
		
		return originalOptions;
	}

	private Element loadUpdatedTables(){
		MartRegistry updatedRegistry = this.openXML(this.updatedRegistryFile);
		
		for(Mart updatedMart : updatedRegistry.getMartList()){
			Dataset updatedDS = updatedMart.getDatasetBySuffix(this.datasetRoot);
			HashMap<String, HashMap<String, ArrayList<String>>> updatedDatasets = new HashMap<String, HashMap<String,ArrayList<String>>>();
			
			if(null != updatedDS){
				String dsName = updatedDS.getName();
				HashMap<String, ArrayList<String>> updatedTables = new HashMap<String, ArrayList<String>>();
				
				for(DatasetTable updatedTable : updatedMart.getDatasetTablesForDataset(dsName)){
					ArrayList<String> updatedColumns = new ArrayList<String>();
					
					for(Column updatedColumn : updatedTable.getColumnList(dsName)){
						updatedColumns.add(updatedColumn.getName());
					}
					
					updatedTables.put(updatedTable.getName(), updatedColumns);
				}
				
				updatedDatasets.put(dsName, updatedTables);
			}
			this.updatedInfo.put(updatedMart.getName(), updatedDatasets);
		}
		
		return Options.getInstance().getOptionRootElement();
	}
	
	private MartRegistry openXML(String registryFile) {
		MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
		File file = new File(registryFile);
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
			return registry;
		}
		
		MartController.getInstance().requestCreateRegistryFromXML(registry, document);
		return registry;
	}

	private String saveXML() {		
		int index = this.originalRegistryFile.lastIndexOf(".");
		String prefix = this.originalRegistryFile.substring(0, index);
		String savedFile = prefix + "-"+ McUtils.getCurrentTimeString()+".xml";
		File file = new File(savedFile);
		McMenus.getInstance().requestSavePortalToFile(file, false);
		return file.getAbsolutePath();
	}
}
