package org.biomart.objects.enums;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.biomart.common.resources.Settings;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class GuiType {
	private static Map<String,GuiType> instance;
	
	public static GuiType get(String name) {
		if(instance == null) {
			instance = new HashMap<String,GuiType>();
			loadBasicGuiType();
			if("0".equals(System.getProperty("api"))) {
				if(Boolean.parseBoolean(Settings.getProperty("enableplugin")))
					loadPluginGuiType();
			}
			else
				loadPluginGuiType();
		}
		return instance.get(name);
	}
	
	private GuiType(String name, String displayName, String url) {
		this.name = name;
		this.displayName = displayName;
		this.url = url;
	}
	
	private static void loadBasicGuiType() {
		String basicGTxml = System.getProperty("org.biomart.baseDir", ".") + "/conf/xml/guitype.xml";
		File file = new File(basicGTxml);
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
		}catch(Exception e) {
			e.printStackTrace();
		}
		//parse the xml
		@SuppressWarnings("unchecked")
		List<Element> items = document.getRootElement().getChildren("item");
		for(Element item: items) {
			GuiType gt = new GuiType(item.getAttributeValue("name"),
					item.getAttributeValue("displayname"),item.getAttributeValue("url"));
			instance.put(gt.getName(), gt);
		}
	}
	
	private static void loadPluginGuiType() {
        // Traverse plugins directory
        File pluginsDir = new File ("./plugins");
        if(pluginsDir.exists()) {
            FileFilter fileFilter = new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            };
	    	 for (File file : pluginsDir.listFiles(fileFilter)) {
	    		 String filename = file.getName();
	             File propertyfile = new File(file, "guitype.properties");
	             if(propertyfile.exists()) {
	            	 Properties props = new Properties();
					try {
						FileInputStream fis = new FileInputStream(propertyfile);
		                props.load(fis); 
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                    if(props.getProperty("displayname") != null){
                        GuiType gt = new GuiType(filename,props.getProperty("displayname"),props.getProperty("url"));
                        instance.put(filename, gt);
                    }
	             }
	    	 }
        }
	}
	

	private String name;
	private String displayName;
	private String url;
	
	public String getName() {
		return this.name;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public String toString() {
		return this.name;
	}
	
	public static Collection<GuiType> values() {
		return instance.values();
	}
	
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof GuiType))
			return false;
		GuiType gt = (GuiType)object;
		if(gt.getName().equals(name))
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
