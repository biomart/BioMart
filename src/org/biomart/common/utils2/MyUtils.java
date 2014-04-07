package org.biomart.common.utils2;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.biomart.common.constants.Constants;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.exceptions.TechnicalException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;


/**
 * Should be split into appropritate Utils classes
 * @author anthony cros
 */
public class MyUtils {

	/* -------------------------------------- Constants -------------------------------------- */

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String FILE_SEPARATOR_OPPOSITE = "\\".equals(FILE_SEPARATOR) ? "/" : "\\";
	public static final String INFO_SEPARATOR = "_";	
	public static final String TAB_SEPARATOR = "\t";
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String URL_SEPARATOR = "/";
	
	public static final String PARENT_FOLDER_PATH = "/home/anthony/javaIO" + FILE_SEPARATOR;
	public static final String FILES_PATH = PARENT_FOLDER_PATH + "_Files" + FILE_SEPARATOR;
	public static final String INPUT_FILES_PATH = PARENT_FOLDER_PATH + "_InputFiles" + FILE_SEPARATOR;
	public static final String OUTPUT_FILES_PATH = PARENT_FOLDER_PATH + "_OutputFiles" + FILE_SEPARATOR;
	
	public static final String CONSOLE_OUTPUT_REDIRECT = OUTPUT_FILES_PATH + "Console";
	
	public static final String TEXT_FILE_EXTENSION = ".txt";
	public static final String MIDI_FILE_EXTENSION = ".mid";
	
	public static final String DASH_LINE  = "-------------------------------------------------------------------------------------";
	public static final String EQUAL_LINE = "=====================================================================================";
	public static final String STAR_LINE  = "*************************************************************************************";
	public static final String ERROR_LINE = "#####################################################################################";
	
	
	public static StringBuffer copyUrlContentToStringBuffer(URL url) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		StringBuffer stringBuffer = new StringBuffer();
		String line = null;
		while ((line = in.readLine()) != null) {
			stringBuffer.append(line + MyUtils.LINE_SEPARATOR);
		}
		in.close();
		return stringBuffer;
	}
	
	
	/**
	 * Accounts for a potention empty tokens at the end
	 */
	public static List<String> splitLine(String separator, String line) {
		return new ArrayList<String>(Arrays.asList(line.split(separator, -1)));	// -1 to account for trailing empty values (see String documentation)
	}


	





	public static boolean isEmpty(String string) {
		return string.length()==0;	// so it throws NullPointerException if string is null (just like string.isEmpty() would)
	}


	
}
