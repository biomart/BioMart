package org.biomart.configurator.test.category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.biomart.configurator.test.SettingsForTest;
import org.mortbay.log.Log;


public class TestOpenOldXML extends TestAddingSource {

	@Override
	public boolean test() {
		this.testNewPortal();
		this.testOpenXML(testName);
		this.testSaveXML(testName);
		return this.compareXML(testName);
	}

	public static final String XML_ELEMENT_ROW = "<row ";
	public static final int PARTITION_TABLE__SOURCE_DATABASE__PASSWORD_COLUMN_INDEX = 4;	// 14 or 15 for target databases?
	public static final String PIPE_REGEX = "[|]";
	
	public boolean compareXML(String testcase) {
		String fileName1 = SettingsForTest.getSourceXMLPath(testcase);
		String fileName2 = SettingsForTest.getSavedXMLPath(testcase);
		
		File file1 = new File(fileName1);
		File file2 = new File(fileName2);
		
		Boolean match = null;
		try {
			match = checkMatch(file1, file2);
		} catch (FileNotFoundException e) {
			match = false;
			e.printStackTrace();
		} catch (IOException e) {
			match = false;
			e.printStackTrace();
		}
		return match;
	}

	private boolean checkMatch(File file1, File file2) throws FileNotFoundException, IOException {
		FileReader fileReader1 = new FileReader(file1);
		FileReader fileReader2 = new FileReader(file2);
		
		BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
		BufferedReader bufferedReader2 = new BufferedReader(fileReader2);
		
		String line1 = null;
		String line2 = null;
		String previousLine1 = null;
		String previousLine2 = null;
		List<String> list1 = null;
		List<String> list2 = null;
		int lineCount = 0;
		
		boolean match = true;	// unless set differently
		while (true) {
			line1 = bufferedReader1.readLine();
			line2 = bufferedReader2.readLine();
			if (line1!=null && line2!=null) {
				if (!line1.equals(line2)) {
					line1 = line1.trim();
					line2 = line2.trim();
					if (line1.endsWith("\r")) {
						line1 = line1.substring(0, line1.length()-2);
					}
					if (line2.endsWith("\r")) {
						line2 = line2.substring(0, line2.length()-2);
					}
					
					// if not an encrypted password issue then just a mismatch
					if (!line1.startsWith(XML_ELEMENT_ROW) || !line2.startsWith(XML_ELEMENT_ROW)) {
						match = false;
						break;
					}
					// account for encrypted password mismatch
					// format is expected to be like: <row id="0">jdbc:mysql://dcc-db.oicr.on.ca:3306/|dcc_tcgaLAML4e|dcc_tcgaLAML4e|dcc_web|cf7d3611a49a45114d46e980dc7fca0ca95c6e93304dc797787d9a389d84ed5945ecca4e9c7e7fcc70d5957b47e0075e|cnv_tcgaLAML|true|Acute Myeloid Leukemia (TCGA, US)|||||||||NCBI36|sample_tcgaLAML|sample_tcgaLAML|tcgaLAML</row>
					else {
						String string = "potential mismatch on " + lineCount + "\n" +
								"\tline1 = " + line1 + "\n" +
								"\tline2 = " + line2 + "\n";
						String message = string;
						Log.info(message);
						System.out.println(message);
						
						String[] split1 = line1.split(PIPE_REGEX);
						String[] split2 = line2.split(PIPE_REGEX);
						split1[PARTITION_TABLE__SOURCE_DATABASE__PASSWORD_COLUMN_INDEX] = "";	// so that we ignore the password column in the equality check
						split2[PARTITION_TABLE__SOURCE_DATABASE__PASSWORD_COLUMN_INDEX] = "";
						list1 = new ArrayList<String>(Arrays.asList(split1));
						list2 = new ArrayList<String>(Arrays.asList(split2));
						if (!list1.equals(list2)) {
							match = false;
							break;
						}
						list1 = null;
						list2 = null;
					}
					
				}
			} else if (line1==null && line2==null) {
				break;
			} else {
				match = false;
				break;
			}
			previousLine1 = line1;
			previousLine2 = line2;
			lineCount++;
		}
		bufferedReader1.close();
		bufferedReader2.close();
		
		fileReader1.close();
		fileReader2.close();
		
		String message = match ? 
				"match! (" + lineCount + " lines)" :
				"mismatch...!\n" +
					"\tlineCount = " + lineCount + "\n" +
					"\t\t" + "previousLine1 = " + previousLine1 + "\n" +
					"\t\t" + "previousLine2 = " + previousLine2 + "\n" +
					"\t\t" + "line1 = " + line1 + "\n" +
					"\t\t" + "line2 = " + line2 + "\n" +
					"\t\t" + "list1 = " + list1 + "\n" +
					"\t\t" + "list2 = " + list2;
		Log.debug(message);
		System.out.println(message);
		return match;
	}
	
}