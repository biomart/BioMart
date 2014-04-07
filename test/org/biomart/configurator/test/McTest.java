package org.biomart.configurator.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Settings;
import org.biomart.configurator.test.category.McTestCategory;
import org.biomart.configurator.utils.McUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * manual run:
 * 		java -cp "`DIR=./lib && find ${DIR?} -name '*.jar' | tr "\n" ":"`.:./bin" -Xmx2048m -Xms2048m [-Dtest.subset=true] org.junit.runner.JUnitCore org.biomart.configurator.test.McTest
 */
@RunWith(value = McParameterized.class)
public class McTest {

	public static final boolean TURN_ON_TIMING = false;
	private Element testcaseElement;
	public static boolean SUBSET_TEST = "true".equals(System.getProperty("test.subset"));
	public static String configxml = "./conf/xml/TestConfig.xml";

    @BeforeClass
    public static void runBeforeClass() {
    	System.setProperty("api","2");
    	//load the config xml and init for MC
    	MartConfigurator.initForWeb();
    	//init log
    	File home = new File(System.getProperty("user.home"), ".biomart");
    	Log.configure(Settings.MARTCONFIGURATOR, new File(home, Settings.MARTCONFIGURATOR));
		Settings.loadGUIConfigProperties();
		SettingsForTest.loadConfigXML(configxml);		
    }
    
	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> query = new ArrayList<Object[]>();
		SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
		try {
			Document document = saxBuilder.build(configxml);
			@SuppressWarnings("unchecked")
			List<org.jdom.Element> xmlElementList = document.getRootElement().getChildren("testcase");
			for(org.jdom.Element element: xmlElementList) {
				//get the first xml for now
				String ignoreStringValue = element.getAttributeValue("ignore");
				String subsetStringValue = element.getAttributeValue("subset");
				boolean ignore = "true".equals(ignoreStringValue);
				boolean subset = "true".equals(subsetStringValue);
				if(ignore) {	// prevails on everything
					continue;
				} else if (!SUBSET_TEST || subset) {	// either full tests or part of the subset 
					Element[] eArray = new Element[]{element};
					query.add(eArray);
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return query;
	}
	
	@Test
	public void test() {
		if (McTest.TURN_ON_TIMING) McUtils.timing1();
		McTestCategory test = McTestCategory.getCategory(this.testcaseElement.getAttributeValue("name"));
		assertTrue(test.test());
		if (McTest.TURN_ON_TIMING) McUtils.timing2();
	}
	
	public McTest(Element testcaseElement) {
		this.testcaseElement = testcaseElement;
	}
}