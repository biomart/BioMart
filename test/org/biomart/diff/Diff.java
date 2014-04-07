package org.biomart.diff;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;

public class Diff {
	public static void main(String args[]){
		String oldMartServiceURL = "http://localhost:8888/martservice/";
		String newMartSericeURL = "http://localhost:8888/martservice/";
		
		HashSet<String> oldAttributes = new HashSet<String>();
		HashSet<String> oldFilters = new HashSet<String>();

		getAllElements(oldMartServiceURL, oldAttributes, oldFilters);
		
		
		HashSet<String> newAttributes = new HashSet<String>();
		HashSet<String> newFilters = new HashSet<String>();

		getAllElements(newMartSericeURL, newAttributes, newFilters);
		
		HashSet<String> deletedAttributes = new HashSet<String>(oldAttributes);
		deletedAttributes.removeAll(newAttributes);
		for(String entry:deletedAttributes)
			System.out.println(entry + "\tDELETED");
		
		HashSet<String> addedAttributes = new HashSet<String>(newAttributes);
		addedAttributes.removeAll(oldAttributes);
		for(String entry:addedAttributes)
			System.out.println(entry + "\tADDED");
		
		HashSet<String> deletedFilters = new HashSet<String>(oldFilters);
		deletedFilters.removeAll(newFilters);
		for(String entry:deletedFilters)
			System.out.println(entry + "\tDELETED");
		
		HashSet<String> addedFilters = new HashSet<String>(newFilters);
		addedFilters.removeAll(oldFilters);
		for(String entry:addedFilters)
			System.out.println(entry + "\tADDED");
		
		if(deletedAttributes.isEmpty() && addedAttributes.isEmpty() && deletedFilters.isEmpty() && addedFilters.isEmpty())
			System.out.println("The two servers have no detectable differences");
	}

	private static void getAllElements(String martserviceURL1,
			HashSet<String> attributes1, HashSet<String> filters1) {
		try {
			// ----- Get Marts
			URL martsURL = new URL(martserviceURL1 + "xml/marts");
			//InputStream rstream = testSource.openStream();
			SAXBuilder builder = new SAXBuilder();
			Document martDoc = builder.build(martsURL);

			Element martRoot = martDoc.getRootElement();

			List martPointers = martRoot.getChildren("martpointer");
			Iterator mpIterator = martPointers.iterator();
			while (mpIterator.hasNext()) {
				Element martPointer = (Element) mpIterator.next();
				String martName = martPointer.getAttributeValue("name");
				String configName = martPointer.getAttributeValue("config");
				//System.err.println(martName + " (" + configName + ")");

				// ----- Get Datasets
				URL datasetsURL = new URL(martserviceURL1 + "xml/datasets/" + martName);
				Document datasetsDoc = builder.build(datasetsURL);

				Element datasetsRoot = datasetsDoc.getRootElement();

				List datasets = datasetsRoot.getChildren("dataset");
				Iterator datasetsIterator = datasets.iterator();
				while (datasetsIterator.hasNext()) {
					Element dataset = (Element) datasetsIterator.next();
					String datasetName = dataset.getAttributeValue("name");
					//System.err.println("\t" + datasetName);

					// ----- Get Attributes
					URL attributeURL = new URL(martserviceURL1 + "xml/attributes/" + martName + "?datasets=" + datasetName);
					Document attributeDoc = builder.build(attributeURL);

					Element attributeRoot = attributeDoc.getRootElement();

					Iterator attributeIterator = attributeRoot.getDescendants(new ElementFilter("attribute"));
					while (attributeIterator.hasNext()) {
						Element attribute = (Element) attributeIterator.next();
						String attributeName = attribute.getAttributeValue("name");
						attributes1.add(martName + "\t" + configName + "\t" + datasetName + "\t" + attributeName + "\tAttribute");
						//System.err.print("\t\tA: " + attributeName + " ");



					}
					// ----- Get Filters
					URL filterURL = new URL(martserviceURL1 + "xml/filters/" + martName + "?datasets=" + datasetName);
					Document filterDoc = builder.build(filterURL);

					Element filterRoot = filterDoc.getRootElement();

					Iterator filterIterator = filterRoot.getDescendants(new ElementFilter("filter"));
					while (filterIterator.hasNext()) {
						Element filter = (Element) filterIterator.next();
						String filterName = filter.getAttributeValue("name");
						//String filterType = filter.getAttributeValue("type");

						filters1.add(martName + "\t" + configName + "\t" + datasetName + "\t" + filterName + "\tFilter");
						//System.err.print("\t\tF: " + filterName + " ");

					}
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
