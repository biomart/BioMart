package org.biomart.configurator.boolMove;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class BoolMove {
	public static void main(String args[]){
		String fullInput = "/Users/jonathanguberman/Documents/workspace/biomart-java/conf/xml/IcgcPortalUpdate.xml";
		String fullOutput = "/Users/jonathanguberman/Documents/workspace/biomart-java/conf/xml/BoolTest.xml";
		try {
			SAXBuilder builder = new SAXBuilder();
			Document martDoc;
			martDoc = builder.build(new File(fullInput));

			Element martRoot = martDoc.getRootElement();

			List marts = martRoot.getChildren("mart");
			Iterator martIterator = marts.iterator();
			while (martIterator.hasNext()) {
				Element mart = (Element) martIterator.next();

				List configs = mart.getChildren("config");
				Iterator configIterator = configs.iterator();
				while ( configIterator.hasNext()){
					Element config = (Element) configIterator.next();

					HashMap<String, Element> booleanContainer = new HashMap<String, Element>();
					HashMap<String, HashSet<Element>> filterLists = new HashMap<String, HashSet<Element>>();
					HashSet<Element> newBools = new HashSet<Element>();

					HashMap<String, Element> attributeByName = new HashMap<String, Element>();
					HashMap<String, String> filterFieldByName = new HashMap<String, String>();
					HashMap<String, String> filterTableByName = new HashMap<String, String>();

					Iterator attributeIterator = config.getDescendants(new ElementFilter("attribute"));
					while(attributeIterator.hasNext()){
						Element attribute = (Element) attributeIterator.next();
						attributeByName.put(attribute.getAttributeValue("name"), attribute);
					}

					Iterator filterIterator = config.getDescendants(new ElementFilter("filter"));
					while(filterIterator.hasNext()){
						Element filter = (Element) filterIterator.next();
						if(attributeByName.get(filter.getAttributeValue("attribute"))!=null){
							filterFieldByName.put(filter.getAttributeValue("name"), attributeByName.get(filter.getAttributeValue("attribute")).getAttributeValue("field"));
							filterTableByName.put(filter.getAttributeValue("name"), attributeByName.get(filter.getAttributeValue("attribute")).getAttributeValue("table"));
						}
					}

					Iterator elementsIterator = config.getDescendants(new ElementFilter("attribute").or(new ElementFilter("filter")));
					while(elementsIterator.hasNext()){
						Element queryElement = (Element) elementsIterator.next();
						Element doubleParent = queryElement.getParentElement().getParentElement();
						String field = queryElement.getAttributeValue("field");
						if(field==null)
							field = filterFieldByName.get(queryElement.getAttributeValue("name"));
						String table = queryElement.getAttributeValue("table");
						if(table==null)
							table = filterTableByName.get(queryElement.getAttributeValue("name"));
						if(field!=null){

							if(field.endsWith("_bool")){
								if(!doubleParent.getAttributeValue("name").equals("newFilter") && !doubleParent.getAttributeValue("name").equals("newAttribute")){
									//System.out.println(field);
									booleanContainer.put(table + "*" + field.split("__")[0],queryElement.getParentElement());
								} else {
									newBools.add(queryElement);
									//System.out.println(field);
								}
							}
						}
						String filterList = queryElement.getAttributeValue("filterlist");

						if(filterList!=null && !filterList.equals("") && !doubleParent.getAttributeValue("name").equals("newFilter")){
							String[] listMembers = filterList.split(",");
							for(String listMember : listMembers){
								String fieldName = filterFieldByName.get(listMember);
								String tableName = filterTableByName.get(listMember);
								if(fieldName!=null && fieldName.endsWith("_bool")){
									String prefix = tableName + "*" + fieldName.split("__")[0];
									HashSet<Element> parentFilters = filterLists.get(prefix);
									if(parentFilters==null)
										parentFilters = new HashSet<Element>();
									parentFilters.add(queryElement);
									filterLists.put(prefix, parentFilters);
								}
							}
						}

					}
					for(Element queryElement : newBools){
						String fieldName = queryElement.getAttributeValue("field");
						if(fieldName==null)
							fieldName = filterFieldByName.get(queryElement.getAttributeValue("name"));
						
						String tableName = queryElement.getAttributeValue("table");
						if(tableName==null)
							tableName = filterTableByName.get(queryElement.getAttributeValue("name"));
						
						String prefix = tableName + "*" + fieldName.split("__")[0];
						Element parent = booleanContainer.get(prefix);
						HashSet<Element> parentFilters = filterLists.get(prefix);
						if(parent!=null){
							queryElement.detach();
							parent.addContent(queryElement);
							//System.out.println(prefix + " moved to " + parent.getAttributeValue("name"));

						}
						if(parentFilters!=null){
							for(Element parentFilter : parentFilters){
								//System.out.println("LIST ADDED");
								String filterList = parentFilter.getAttributeValue("filterlist");
								filterList = filterList + "," + queryElement.getAttributeValue("name");
								parentFilter.setAttribute("filterlist", filterList);
							}
						}
					}
				}
			}
			XMLOutputter serializer = new XMLOutputter();
			BufferedWriter outputStream = new BufferedWriter(new FileWriter(fullOutput));
			serializer.output(martDoc, outputStream);

		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
