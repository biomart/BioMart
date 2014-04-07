package org.biomart.processors;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.biomart.common.exceptions.TechnicalException;

public class JGUtils {
	public static List<String> splitLine(String separator, String line) {
		return new ArrayList<String>(Arrays.asList(line.split(separator, -1)));	// -1 to account for trailing empty values (see String documentation)
	}
	
	public static List<List<String>> parseUrlContentToListStringList(URL url, String separator) throws TechnicalException {
		return parseUrlContentToListStringList(url, separator, null);
	}

	public static List<List<String>> parseUrlContentToListStringList(URL url, String separator, Integer maxLines) throws TechnicalException {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			List<List<String>> listList = new ArrayList<List<String>>();
			String line = null;
			int lineCount = 0;
			boolean capped = maxLines!=null && maxLines>0;
			while ((line = in.readLine()) != null) {
				if (!(line.length()==0)) {			
					listList.add(splitLine(separator, line));
					lineCount++;
					if (capped && lineCount>=maxLines) {
						break;
					}
				}
			}
			in.close();
			return listList;
		} catch (IOException e) {
			throw new TechnicalException(e);
		}
	}

	public static List<List<String>> parseFileToListStringList(
			String filename) {
		List<List<String>> parsedData = new ArrayList<List<String>>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = in.readLine()) != null) {
				if (!(line.length()==0)) {			
					parsedData.add(JGUtils.splitLine("\t", line));
				}
			}
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			parsedData = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			parsedData = null;
		} 
		return parsedData;
	}
	public static LinkedHashMap sortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
				.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		LinkedHashMap result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry)it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}

