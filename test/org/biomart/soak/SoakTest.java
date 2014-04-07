package org.biomart.soak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class SoakTest {
	// This determines whether all values for multi-valued filters are tested (true), or only the first (false)
	private static boolean testValues = false;
	
	public static void main(String args[]){
		String martserviceURL = System.getProperty("test.soak.url", "http://localhost:8888/martservice/");
		HashSet<String> failInfo = new HashSet<String>();
		URLCodec safeURL = new URLCodec();
			
		int martNumber = 8; // Starts at 1, not 0
		try {
			// ----- Get Marts
			URL martsURL = new URL(martserviceURL + "marts");
			//InputStream rstream = testSource.openStream();
			SAXBuilder builder = new SAXBuilder();
			Document martDoc = builder.build(martsURL);
			
			Element martRoot = martDoc.getRootElement();

			List martPointers = martRoot.getChildren("mart");
			Iterator mpIterator = martPointers.iterator();
			//Set testCounter to -1 to test all marts and ignore martNumber
			int testCounter = -1;
			

			while (mpIterator.hasNext()) {

				Element martPointer = (Element) mpIterator.next();

				if(testCounter >= 0){
					testCounter++;
					if(testCounter!=martNumber)
						continue;
				}

				String martName = martPointer.getAttributeValue("name");
				/*if(!martName.equals("hsapiens_gene_ensembl_1"))
					continue;*/
				String configName = martPointer.getAttributeValue("config");
				System.err.println(martName + " (" + configName + ")");

				// ----- Get Datasets
				URL datasetsURL = new URL(martserviceURL + "datasets?mart=" + martName);
				Document datasetsDoc = builder.build(datasetsURL);

				Element datasetsRoot = datasetsDoc.getRootElement();

				List datasets = datasetsRoot.getChildren("dataset");
				Iterator datasetsIterator = datasets.iterator();
				while (datasetsIterator.hasNext()) {
					Element dataset = (Element) datasetsIterator.next();
					String datasetName = dataset.getAttributeValue("name");
					/*if(!datasetName.equals("hsapiens_gene_ensembl_TSPLung4b"))
						continue;*/
					System.err.println("\t" + datasetName);

					// ----- Get Attributes
					URL attributeURL = new URL(martserviceURL + "attributes?config=" + configName + "&datasets=" + datasetName);
					Document attributeDoc = builder.build(attributeURL);

					Element attributeRoot = attributeDoc.getRootElement();

					List attributes = attributeRoot.getChildren("attribute");
					Iterator attributeIterator = attributes.iterator();
					String goodAtt = null;
					while (attributeIterator.hasNext()) {
						Element attribute = (Element) attributeIterator.next();
						goodAtt = checkAttribute(martserviceURL, failInfo,
								safeURL, martName, configName, datasetName,
								goodAtt, attribute);

					}
					// ----- Get Filters
					URL filterURL = new URL(martserviceURL + "filters?config=" + configName + "&datasets=" + datasetName);
					Document filterDoc = builder.build(filterURL);

					Element filterRoot = filterDoc.getRootElement();

					List filters = filterRoot.getChildren("filter");
					Iterator filterIterator = filters.iterator();
					while (filterIterator.hasNext()) {
						Element filter = (Element) filterIterator.next();
						checkFilter(martserviceURL, failInfo, safeURL,
								martName, configName, datasetName, goodAtt,
								filter);


					}
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EncoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(String entry : failInfo){
			System.out.println(entry);
		}
		
		if(failInfo.size()==0)
			System.out.println("Success! " + martNumber);
	}

	private static void checkFilter(String martserviceURL,
			HashSet<String> failInfo, URLCodec safeURL, String martName,
			String configName, String datasetName, String goodAtt,
			Element filter) throws MalformedURLException, EncoderException {

		if(goodAtt==null){
			System.err.println("NO VALID ATTRIBUTES");
			return;
		}
		List subFilters = filter.getChildren("filter");
		if(subFilters.size()>0){
			
			Iterator subFiltersIterator = subFilters.iterator();
			while(subFiltersIterator.hasNext()){
				Element subFilter = (Element) subFiltersIterator.next();
				checkFilter(martserviceURL, failInfo, safeURL, martName, configName, datasetName, goodAtt, subFilter);
			}
		} else {
			String filterName = filter.getAttributeValue("name");
			String filterType = filter.getAttributeValue("type");

			System.err.print("\t\tF: " + filterName + " ");

			String configString;
			if(configName==null)
				configString = "";
			else
				configString = "config=\"" + configName + "\"";

			if(filterType.equals("simple") || filterType.equals("text") || filterType.equals("upload") || filterType.equals("boolean")){
				boolean failFlag = false;
				URL queryURL = new URL(martserviceURL + "results?query=" + safeURL.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query client=\"webbrowser\" processor=\"TSVX\" limit=\"1\" header=\"0\"><Dataset name=\"" + datasetName + "\" " + configString + "><Filter name=\"" + filterName + "\" value=\"only\"/><Attribute name=\"" + goodAtt +"\"/></Dataset></Query>"));
				if(filterType.equals("text"))
					queryURL = new URL(martserviceURL + "results?query=" + safeURL.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query client=\"webbrowser\" processor=\"TSVX\" limit=\"1\" header=\"0\"><Dataset name=\"" + datasetName + "\" " + configString + "><Filter name=\"" + filterName + "\" value=\"1\"/><Attribute name=\"" + goodAtt +"\"/></Dataset></Query>"));
				BufferedReader queryResults;
				try {
					queryResults = new BufferedReader(new InputStreamReader(queryURL.openStream()));


					String resultLine;
					while ((resultLine = queryResults.readLine()) != null){
						if(resultLine.contains("error"))
							failFlag = true;
					}
					if(failFlag){
						System.err.println("FAILED");
						System.err.println(queryURL);
						failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tFILTER\t" + filterName + "\t" + queryURL);

					}
					else{
						System.err.println("PASSED");
					}
					queryResults.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("FAILED");
					System.err.println(queryURL);
					failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tFILTER\t" + filterName + "\t" + queryURL);

				}
			} else if(filterType.startsWith("singleSelect") || filterType.startsWith("multiSelect")) {
				System.err.println("");
				List values = filter.getChildren("value");
				Iterator valueIterator = values.iterator();
				while(valueIterator.hasNext()){
					Element value = (Element) valueIterator.next();
					String valueName = value.getAttributeValue("name");

					System.err.print("\t\t\tV: " + valueName + " ");

					boolean failFlag = false;

					URL queryURL = new URL(martserviceURL + "results?query=" + safeURL.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query client=\"webbrowser\" processor=\"TSVX\" limit=\"1\" header=\"0\"><Dataset name=\"" + datasetName + "\" " + configString + "><Filter name=\"" + filterName + "\" value=\"" + valueName + "\"/><Attribute name=\"" + goodAtt +"\"/></Dataset></Query>"));
					BufferedReader queryResults;
					try {
						queryResults = new BufferedReader(new InputStreamReader(queryURL.openStream()));


						String resultLine;
						while ((resultLine = queryResults.readLine()) != null){
							if(resultLine.contains("error"))
								failFlag = true;
						}
						if(failFlag){
							System.err.println("FAILED");
							System.err.println(queryURL);
							failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tFILTER\t" + filterName + "\t" + queryURL);

						}
						else{
							System.err.println("PASSED");
						}
						queryResults.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.err.println("FAILED");
						System.err.println(queryURL);
						failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tFILTER\t" + filterName + "\t" + queryURL);


					}
					if(!testValues)
						break;
				}
			} else {
				System.err.println("TYPE " + filterType + " UNSUPPORTED");
				failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tUNSUPPORTED\t" + filterName + "\t");
				//System.exit(-1);
			}
		}
	}

	private static String checkAttribute(String martserviceURL,
			HashSet<String> failInfo, URLCodec safeURL, String martName,
			String configName, String datasetName, String goodAtt,
			Element attribute) throws MalformedURLException, EncoderException {
		List subAttributes = attribute.getChildren("attribute");
		if(subAttributes.size()==0){
			String attributeName = attribute.getAttributeValue("name");
			if(attribute.getAttributeValue("value","").equals("")){
				System.err.print("\t\tA: " + attributeName + " ");
				boolean failFlag = false;
				String configString;
				if(configName==null)
					configString = "";
				else
					configString = "config=\"" + configName + "\"";


				URL queryURL = new URL(martserviceURL + "results?query=" + safeURL.encode("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE Query><Query client=\"webbrowser\" processor=\"TSVX\" limit=\"1\" header=\"0\"><Dataset name=\"" + datasetName + "\" " + configString + "><Attribute name=\"" + attributeName +"\"/></Dataset></Query>"));
				BufferedReader queryResults;
				try {
					queryResults = new BufferedReader(new InputStreamReader(queryURL.openStream()));


					String resultLine;
					while ((resultLine = queryResults.readLine()) != null){
						if(resultLine.contains("error"))
							failFlag = true;
					}
					if(failFlag){
						System.err.println("FAILED");
						System.err.println(queryURL);
						failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tATTRIBUTE\t" + attributeName + "\t" + queryURL);
					}
					else{
						System.err.println("PASSED");
						goodAtt = attributeName;
					}
					queryResults.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("FAILED");
					System.err.println(queryURL);
					failInfo.add(martName + "\t" + configName + "\t" + datasetName + "\tATTRIBUTE\t" + attributeName + "\t" + queryURL);
				}
				//System.exit(0);
			} else {
				System.err.println("\t\tA: " + attributeName + " NOT TESTED (PSEUDO)");
			}
		}
		else{
			Iterator subAttributesIterator = subAttributes.iterator();
			while(subAttributesIterator.hasNext()){
				Element subAttribute = (Element) subAttributesIterator.next();
				goodAtt = checkAttribute(martserviceURL, failInfo,
						safeURL, martName, configName, datasetName,
						goodAtt, subAttribute);
			}
		}
		return goodAtt;
	}
}
