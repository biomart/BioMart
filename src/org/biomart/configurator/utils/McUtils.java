package org.biomart.configurator.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.ImageIcon;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.Base64OutputStream;
import org.biomart.common.utils.InstallCert;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.JoinTable;
import org.biomart.configurator.model.SelectFromTable;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.utils.type.PortableType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.oauth.rest.OAuthSigner;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Exportable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Importable;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.SourceSchema;
import org.biomart.objects.objects.Table;
import org.biomart.objects.objects.VirtualLink;
import org.biomart.objects.portal.User;
import org.biomart.objects.portal.UserGroup;
import org.biomart.registry.HttpCentralRegistryProxy;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

import sun.tools.jconsole.ProxyClient;
import sun.tools.jconsole.ProxyClient.Snapshot;
import sun.tools.jconsole.ProxyClient.SnapshotMBeanServerConnection;

public class McUtils {

	public static final String NEW_LINE = System.getProperty("line.separator");
	
	private static final int MIN_PORT_NUMBER = 0;
	private static final int MAX_PORT_NUMBER = Integer.MAX_VALUE;
	//private static Calendar calendar = new GregorianCalendar();
	private static int containerId = 0;
	private static String key;
	private static boolean testingMode = false; 
	
	public static void setTestingMode(boolean b) {
		testingMode = b;
	}
	
	public static boolean isTestingMode() {
		return testingMode;
	}

	public static String getKey() {
		return key;
	}
	
	public static String getCurrentTimeString() {
		Format formatter = new SimpleDateFormat("yyyy-mm-dd-HH-mm-ss");
		Date date = new Date();
		return formatter.format(date);
	}

	public static void gc() {
		new Thread("MartConfigurator.gc") {
			public void run() {
				try {
					SnapshotMBeanServerConnection server = Snapshot.newSnapshot(ManagementFactory.getPlatformMBeanServer());
					MemoryMXBean memoryMBean = ManagementFactory.newPlatformMXBeanProxy(server,ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
					memoryMBean.gc();
				} catch (UndeclaredThrowableException e) {
					// Ignore
				} catch (IOException e) {
					// Ignore
				}
			}
		}.start();
	}
	
	public static String StrListToStr(List<String> list, String separator) {
		if(list==null || list.size()==0)
			return "";
		else {
			StringBuffer res = new StringBuffer();
			res.append(list.get(0));
			for(int i=1;i<list.size();i++) {
				if(list.get(i).indexOf("|")>=0) {
					String tmp = list.get(i).replaceAll("\\|", " or ");
					list.set(i, tmp);
				//	list.get(i).replaceAll("|", " or ");
				}
				res.append(separator);
				res.append(list.get(i));
			}
			return res.toString();
		}
	}

	public static  String listToStr(List<? extends MartConfiguratorObject> list, String separator) {
		if(list==null || list.size()==0)
			return "";
		else {
			StringBuffer res = new StringBuffer();
			res.append(list.get(0).getName());
			for(int i=1;i<list.size();i++) {
				res.append(separator);
				res.append(list.get(i).getName());
			}
			return res.toString();
		}
	}

	/**
	 * Necessary in order to order mixed case strings properly.
	 * 	with default Comparator, the following sequence:	a_, A_, ab, AB
	 * 	would end up being ordered as:						AB, A_, a_, ab
	 *  while it should be:									A_, a_, AB, ab
	 * -> see DCCTEST-491
	 * @author Anthony Cros
	 */
	public static final Comparator<String> BETTER_STRING_COMPARATOR = new Comparator<String>() {
		public int compare(String string1, String string2) {
			return string1.toLowerCase().compareTo(string2.toLowerCase());	// assumes no null 
		}		
	};
	
	public static long getCurrentTime() {
		Calendar calendar = new GregorianCalendar();
		return calendar.getTimeInMillis();
	}

	public static String getRegValue(String reg, String expression, String input) {
		Pattern p = Pattern.compile(reg);
		Matcher m = p.matcher(input);
		return m.replaceAll(expression);
	}
    /** Returns an ImageIcon, or null if the path was invalid. */
    public static ImageIcon createImageIcon(String path) {
         return new ImageIcon(path);
    }

    public static int getNextContainerId() {
    	return containerId++;
    }

    public static String getPartitionTableName(String value) {
    	int index = value.indexOf(XMLElements.PARTITIONCOLUMNPREFIX.toString());
    	//remove "("
    	return value.substring(1,index);
    }
    
    
    public static int getPartitionColumnValue(String value) {
    	int index = value.indexOf(XMLElements.PARTITIONCOLUMNPREFIX.toString());
    	String colStr = value.substring(index+1, value.length()-1);
    	//remove the ")"
    	int ret = -1;
    	try{
    		ret = Integer.parseInt(colStr);
    	}catch(Exception e) {
    		//TODO
    		return -1;
    	}
    	return ret;
    }

    public static List<String> extractPartitionReferences(String value) {
        List<String> list = new ArrayList<String>();
        if (null!=value) {
        	String pat = "\\(p\\d*c\\d*\\)";
        	
            Pattern pattern = Pattern.compile(pat);
            Matcher m = pattern.matcher(value);
            while (m.find()) {
                int start = m.start();
                int end = m.end();
                list.add(value.substring(0, start));
                list.add(value.substring(start, end));
                value = value.substring(end);
                m = pattern.matcher(value);
            }
            list.add(value);
        } else {
            list.add("");    // null becomes an empty value
        }
        return list;
    }
    
    public static boolean hasPartitionBinding(String value) {
    	return (McUtils.extractPartitionReferences(value).size()>1);
    }
    
    /**
     * if the partition value is not valid, return null;
     * assume that list size >1;
     * @param pt
     * @param row
     * @param list
     * @return
     */
    public static String replacePartitionReferences(PartitionTable pt, int row, List<String> list) {
    	StringBuffer sb = new StringBuffer();
    	int i=-1;
    	for(String item: list) {
    		i++;
    		if(i % 2 == 0)
    			sb.append(item);
    		else {
    			int col = McUtils.getPartitionColumnValue(item);
    			String value = pt.getValue(row, col);
    			if(value==null || value.equals(""))
    				return null;
    			sb.append(value);
    		}
    	}
    	return sb.toString();
    }

    public static String getRealName(PartitionTable pt, int row, String name) {
    	return replacePartitionReferences(pt, row, extractPartitionReferences(name));
    }
    
    public static String getRealName(String name, Dataset ds) {
		List<String> ptRefList = McUtils.extractPartitionReferences(name);
		//if has partition references
		if(ptRefList.size()>1) {
			//assume only one partition for now
			String ptRef = ptRefList.get(1);
			String ptName = McUtils.getPartitionTableName(ptRef);
			if(ds == null || ds.getParentMart() == null)
			{
				return name;
			}
			PartitionTable pt = ds.getParentMart().getPartitionTableByName(ptName);
			if(pt == null) {
				return name; 
			}else {
				//use first row
				for(int i=0; i<pt.getTotalRows(); i++) {
					String dsName = null;
					if(pt.getPartitionType() == PartitionType.SCHEMA) 
						dsName = pt.getValue(i, PartitionUtils.DATASETNAME);
					else
						dsName = pt.getValue(i, 0);
					if(!ds.getName().contains(dsName))
						continue;
					
					String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
					if(realName !=null) {
						return realName;
					}
				}
/*				//find the row number
				int row = pt.getRowNumberByDatasetName(ds.getName());
					String realName = McUtils.replacePartitionReferences(pt, row, ptRefList);
					return realName;
*/				
			}
		} else
			return name;
		return name;
    }
    
    public static int getFirstRowInPT(String name, Dataset ds) {
		List<String> ptRefList = McUtils.extractPartitionReferences(name);
		//if has partition references
		if(ptRefList.size()>1) {
			String ptRef = ptRefList.get(1);
			String ptName = McUtils.getPartitionTableName(ptRef);
			PartitionTable pt = ds.getParentMart().getPartitionTableByName(ptName);
			if(pt == null) 
				return -1; 
			else {
				//use first row
				for(int i=0; i<pt.getTotalRows(); i++) {
					String dsName = null;
					if(pt.getPartitionType() == PartitionType.SCHEMA) 
						dsName = pt.getValue(i, PartitionUtils.DATASETNAME);
					else
						dsName = pt.getValue(i, 0);
					if(!ds.getName().contains(dsName))
						continue;
					
					String realName = McUtils.replacePartitionReferences(pt, i, ptRefList);
					if(realName !=null) {
						return i;
					}
				}				
			}
		}
		return -1;			
    }

    /*
     * check null, and trim string
     */
    public static boolean isStringEmpty(String value) {
    	if(value == null)
    		return true;
    	else if(value.trim().length() == 0)
    		return true;
    	else 
    		return false;
    }

    public static org.jdom.Element findChildElementByAttribute(org.jdom.Element element, String att, String value) {
    	@SuppressWarnings("unchecked")
    	List<org.jdom.Element> elist = element.getChildren();
    	for(org.jdom.Element e: elist) {
    		if(value.equals(e.getAttributeValue(att))) 
    			return e;
    	}
    	return null;
    }

    public static String encrypt(String source) throws Exception {
    	if(McUtils.isStringEmpty(key))
    		return source;
    	byte[] raw = hexStringToByteArray(key);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = null;
		cipher = Cipher.getInstance("AES");
       
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] encrypted =
			     cipher.doFinal(source.getBytes());		
    	return asHex(encrypted);
    }
    
    public static String decrypt(String encrypted) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    	if(McUtils.isStringEmpty(key))
    		return encrypted;
    	byte[] raw = hexStringToByteArray(key);
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = null;
		cipher = Cipher.getInstance("AES");
       
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] source = cipher.doFinal(hexStringToByteArray(encrypted));		
    	return new String(source);    	
    }
    
    private static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
    }
    
    /**
     * Turns array of bytes into string
     *
     * @param buf	Array of bytes to convert to hex string
     * @return	Generated hex string
     */
    public static String asHex (byte buf[]) {
      StringBuffer strbuf = new StringBuffer(buf.length * 2);
      int i;

      for (i = 0; i < buf.length; i++) {
    	  if (((int) buf[i] & 0xff) < 0x10)
    		  strbuf.append("0");
       	strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
      }

      return strbuf.toString();
     }

    public static void setKey(String value) {
    	key = value;
    }
    
    public static List<org.biomart.objects.objects.Link> getLinks(Dataset ds1, Config c1, Dataset ds2, Config c2) {
    	List<org.biomart.objects.objects.Link> result = new ArrayList<org.biomart.objects.objects.Link>();
    	List<org.biomart.objects.objects.Link> allLinks = c1.getLinkList();
    	for(org.biomart.objects.objects.Link link: allLinks) {
    		if(c2.equals(link.getPointedConfig())) {
    			//check if the datasets match
    			String dsStr = link.getPropertyValue(XMLElements.DATASETS);
    			if(McUtils.hasPartitionBinding(dsStr)) {
    				String realName = McUtils.getRealName(dsStr, ds1);
    				if(realName.indexOf(ds2.getName())>=0)
    					result.add(link);
    			}else
    				result.add(link);
    		}
    	}
    	return result;
    }
  
    public static List<VirtualLink> getPortableList(Dataset ds1, Config c0, Dataset ds2, Config c2) {
    	List<VirtualLink> vlinkList = new ArrayList<VirtualLink>(); 
    	//create exp/imp from link, links are always master to master
    	//get the master config for c1
    	Config masterc1 = c0.getMart().getMasterConfig();
    	List<Link> linkList = masterc1.getLinkList();
    	for(Link link: linkList) {
    		if(link.getObjectStatus() != ValidationStatus.VALID)
    			continue;
    		if(!masterc1.equals(link.getParentConfig()))
    			continue;

    		if(!c2.getMart().getName().equals(link.getPropertyValue(XMLElements.POINTEDMART)))
    			continue;
    		String portableName =  link.getName()+"_"+link.getParentConfig().getMart().getName();
    		ElementList exp = new Exportable(masterc1,portableName);
    		for(Attribute att: link.getAttributeList()) {
    			exp.addAttribute(att);
    		}
    		ElementList imp = new Importable(c2,portableName);
    		boolean filterExist = true;

    		for(Filter filter: link.getFilterList()) {
    			//TODO check c2.equals(link.getPointedConfig())? is not used
    			//Filter newFilter = c2.equals(link.getPointedConfig())? filter: c2.getFilterByName(ds2, filter.getName(), true);
    			Filter newFilter = c2.getMart().getMasterConfig().getFilterByName(ds2, filter.getName(), true);
    			if(newFilter!=null)
    				imp.addFilter(filter);
    			else {
    				filterExist = false;
    				break;
    			}
    		}
    		if(filterExist && exp.inPartition(ds1.getName()) && imp.inPartition(ds2.getName())) {
	    		VirtualLink vlink = new VirtualLink(exp,imp);
				vlinkList.add(vlink);
    		}
    	}
    	return vlinkList;
    }
    

    public static String base64Encode(String s) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        Base64OutputStream out = new Base64OutputStream(bOut);
        try {
          out.write(s.getBytes());
          out.flush();
        } catch (IOException exception) {
        }
        return bOut.toString();
    }

    public static Document getDocumentFromUrl(String url, String userName, String password) {
		URL registryURL = null;
		URLConnection connection = null;
		try {
			registryURL = new URL(url);
			connection = registryURL.openConnection();
			//if(!McUtils.isStringEmpty(userName)) {
			if(url.indexOf("https://")==0) {
//				InstallCert ic = new InstallCert();
//				ic.test(url);
				String input = userName + ":" + password;
		        String encoding = McUtils.base64Encode(input);
		        connection.setRequestProperty("Authorization", "Basic "
		            + encoding);
			}
			connection.connect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
		SAXBuilder builder = new SAXBuilder();
		Document document = null;
		try {
			document = builder.build(connection.getInputStream());
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;
    }
    
    // helper method for scaling a image icon
	public static ImageIcon scale(Image src, double scale, ImageObserver observer) {
        int w = (int)(scale*src.getWidth(observer));
        int h = (int)(scale*src.getHeight(observer));
        int type = BufferedImage.TYPE_INT_ARGB;
        if(w == 0 || h == 0)
        	return null;
        BufferedImage dst = new BufferedImage(w, h, type);
        Graphics2D g2 = dst.createGraphics();
        g2.drawImage(src, 0, 0, w, h, observer);
        g2.dispose();
        return new ImageIcon(dst);
    }


	public static byte[] computeHash(String x)    
	  {
	     java.security.MessageDigest d =null;
	     try {
			d = java.security.MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	     d.reset();
	     d.update(x.getBytes());
	     return  d.digest();
	 }
	  
	public static String byteArrayToHexString(byte[] b){
	     StringBuffer sb = new StringBuffer(b.length * 2);
	     for (int i = 0; i < b.length; i++){
	       int v = b[i] & 0xff;
	       if (v < 16) {
	         sb.append('0');
	       }
	       sb.append(Integer.toHexString(v));
	     }
	     return sb.toString().toUpperCase();
	}
	
	public static Map<DatasetTable,List<Object>> getQcPathMap(Dataset ds) {
		Map<DatasetTable,List<Object>> qcPathMap = new HashMap<DatasetTable,List<Object>>();
		for(DatasetTable dsTable:ds.getParentMart().getDatasetTables()) {
			List<Object> list = getQCObjectList(dsTable);
			qcPathMap.put(dsTable, list);
		}
		return qcPathMap;  
	}
	  
	private static List<Object> getQCObjectList(DatasetTable table) {
		List<Object> result = new ArrayList<Object>();
		for(TransformationUnit tu:table.getTransformationUnits()) {
			if(tu instanceof JoinTable) {
				result.add(((JoinTable) tu).getSchemaRelation());
				result.add(((JoinTable) tu).getTable());
			}else if(tu instanceof SelectFromTable) {
				if(table.getType() == DatasetTableType.MAIN)
					result.add(((SelectFromTable) tu).getTable());
				else {
					//recursively add the source
					result.addAll(getQCObjectList(((DatasetTable)((SelectFromTable)tu).getTable())));
				}
			}
		}
		return result;
	}

	public static int getNextUniqueMartId(MartRegistry registry) {
		int x = 0;
		for(Mart mart: registry.getMartList()) {
			x = Math.max(x, mart.getUniqueId());
		}
		return x + 1;		
	}
	
	public static int getNextUniqueSchemaId(Mart mart) {
		int x = 0;
		for(SourceSchema ss: mart.getIncludedSchemas()) {
			x = Math.max(x, ss.getUniqueId());
		}
		return x+1;
	}
	
	public static int getNextUniqueTableId(SourceSchema schema) {
		int x = 0;
		for(Table table: schema.getTables()) {
			x = Math.max(x, table.getUniqueId());
		}
		return x+1;
	}

	public static Document buildDocument(String content) {
		SAXBuilder builder = new SAXBuilder();
		Document document = null;
		try {
			document = builder.build(new StringReader(content));
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;
	}

	public static String getUrlContentFromOAuth(String url, String keys) {
		String[] keyarray = keys.split(",");
		if(keyarray.length!=4)
			return null;
		OAuthRequest orequest = OAuthSigner.instance().buildRequest(url, keyarray[0],  keyarray[1],  keyarray[2],  keyarray[3]);		
		Response response = orequest.send();
		return response.getBody();
	}

	public static UserGroup getUserGroup(MartRegistry registry, String user, String password) {
		if(user.trim().equals("")) {
			//anonymous
			return (UserGroup)registry.getPortal().getUsers().getUserByName("anonymous").getParent();
		}
		List<UserGroup> ugList = registry.getPortal().getUsers().getUserList();
		for(UserGroup ug: ugList) {
			if(!McUtils.isStringEmpty(ug.getPropertyValue(XMLElements.LOCATION))) {
				if(HttpCentralRegistryProxy.getInstance().userExists(user,ug.getName(),ug.getPropertyValue(XMLElements.LOCATION)))
					return ug;
			}
		}
		//cannot find the usergroup
		//check all local
		User userObject = registry.getPortal().getUsers().getUserByName(user);
		if(userObject == null)
			userObject = registry.getPortal().getUsers().getUserByOpenId(user);
		if(userObject == null)
			userObject = registry.getPortal().getUsers().getUserByName("anonymous");
		UserGroup ug = (UserGroup)userObject.getParent();
		if(McUtils.isStringEmpty(ug.getPropertyValue(XMLElements.LOCATION)))
			return ug;
		return null;

	}

	public static <T> boolean isCollectionEmpty(Collection<T> collection) {
		if(collection == null || collection.isEmpty())
			return true;
		return false;
	}

	public static boolean hasLink(Config sconfig, Config tconfig){
		Config mastersConfig = sconfig.getMart().getMasterConfig();
		Config mastertConfig = tconfig.getMart().getMasterConfig();
		
		for(Link link : mastersConfig.getLinkList()){
			if(link.getPointedConfig() == null)
				continue;
			if(link.getPointedConfig().equals(mastertConfig))
				return true;
		}
		return false;				
	}
	
	public static Link getLink(Config sconfig, Config tconfig){
		Config mastersConfig = sconfig.getMart().getMasterConfig();
		Config mastertConfig = tconfig.getMart().getMasterConfig();
		
		for(Link link : mastersConfig.getLinkList()){
			if(link.getPointedConfig() == null || mastertConfig.getMart() == null)
				continue;
			//if(link.getPointedConfig().equals(tconfig.getMart().getMasterConfig()))
			if(link.getPointedMart().equals(mastertConfig.getMart()))
				return link;
		}
		return null;				
	}
	
	public static String getLinkName(Config sConfig,Config tConfig){
		StringBuilder targetName = new StringBuilder();
		//targetName.append(sConfig.getDisplayName());			
		targetName.append(sConfig.getMart().getName());
		targetName.append('-');
		//targetName.append(tConfig.getDisplayName());
		targetName.append(tConfig.getMart().getName());
		targetName.append("-link");
		return targetName.toString();
	}
	
	public static Link getOtherLink(Link link){
		if(link.getPointedConfig() == null)
			return null;
		Config otherMasterConfig = link.getPointedConfig().getMart().getMasterConfig();
		Config masterConfig = link.getParentConfig().getMart().getMasterConfig();
		String name = link.getName();
		String[] parts = name.split("_");
		String sBaseName = getLinkName(otherMasterConfig, masterConfig);
		String suffix = parts[parts.length-1];
		String otherName = "";
		try{
			Integer.parseInt(suffix);
			otherName = sBaseName + "_" + suffix;
		}catch(NumberFormatException nfe){
			otherName = sBaseName;
		}
		return otherMasterConfig.getLinkByName(otherName);
	}
	
	public static boolean isHttpServerAvailable(String url){
		try{
			//create the httpURLconnection
			URL newURL = new URL(url);
			HttpURLConnection connection = (HttpURLConnection)newURL.openConnection();
			
			connection.setRequestMethod("GET");
			//set timeout to be 15 sec
			connection.setReadTimeout(15*1000);
			connection.connect();
			
			return true;
		}
		catch(Exception e){
			Log.debug("checking url "+url + " ...");
			return false;
		}
	}
	
	public static boolean isPortAvailable(int port){
		
		    if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
		        throw new IllegalArgumentException("Invalid start port: " + port);
		    }

		    ServerSocket ss = null;
		    DatagramSocket ds = null;
		    try {
		        ss = new ServerSocket(port);
		        ss.setReuseAddress(true);
		        ds = new DatagramSocket(port);
		        ds.setReuseAddress(true);
		        return true;
		    } catch (IOException e) {
		    } finally {
		        if (ds != null) {
		            ds.close();
		        }

		        if (ss != null) {
		            try {
		                ss.close();
		            } catch (IOException e) {
		                /* should not be thrown */
		            }
		        }
		    }

		    return false;
	}
    
	/**
	 * intersection of the datasets
	 * subfilter and parentfilter should be in the same mart, if parentfilter is a pointer,
	 * subfilter should be a pointer too, and they should point to the same mart.
	 * if parentfilter is not a pointer, subfilter can be either pointer or non pointer
	 * 
	 */
	/*
	 * when the value is empty, return the first item that has pushaction. if it is empty, the web gui will disable the dropdown
	 * and never enable it back. TODO
	 */
	public static List<FilterData> getSubFilterData(Config config, Filter pFilter, String value, Collection<String> datasets, Filter subFilter) {
		List<FilterData> fds = new ArrayList<FilterData>();
		Filter tmpPFilter = pFilter;
		Config tmpConfig = config;
		Filter tmpSubFilter = subFilter;
		
		if(pFilter.isPointer() && pFilter.getPointedFilter()!=null && pFilter.getPointedConfing()!=null) {
			tmpPFilter = pFilter.getPointedFilter();
			tmpConfig = pFilter.getPointedConfing();
		}
		if(pFilter.isPointer() && subFilter.isPointer() && subFilter.getPointedFilter()!=null) {
			tmpSubFilter = subFilter.getPointedFilter();
		}
		if(!tmpConfig.equals(tmpSubFilter.getParentConfig()))
			return fds;
		
		//use master config
		if(!tmpConfig.isMasterConfig()) {
			tmpConfig = tmpConfig.getMart().getMasterConfig();
			tmpPFilter = tmpConfig.getFilterByName(tmpPFilter.getName(), null);
		}
		if(McUtils.isStringEmpty(value)) {
			/*
			 * get the first one that has subfilter
			 */
			for(FilterData fd: tmpPFilter.getOptionByDatasets(datasets)) {
				if(!McUtils.isCollectionEmpty(fd.getPushFilterOptions(subFilter.getName()))){
					fds.addAll(fd.getPushFilterOptions(subFilter.getName()));
					break;
				}					
			}
		} else {
			FilterData fd = tmpPFilter.getOptionByName(datasets,value);
			if(null==fd) 
				return fds;
			
			List<FilterData> subFds = fd.getPushFilterOptions(subFilter.getName());
			if(null!=subFds)
				fds.addAll(subFds);
		}
		
		return fds;
	}
	
	

	public static String getUniqueAttributeName(Config config, String baseName) {
		String tmpName = baseName;
		int i = 1;
		while (config.getAttributeByName(null, tmpName, true) != null) {
			tmpName = baseName + "_" + i++;
		}
		return tmpName;
	}

	public static String getUniqueFilterName(Config config, String baseName) {
        String tmpName = baseName;
        int i = 1;
        while (config.getFilterByName(null, tmpName, true) != null) {
            tmpName = baseName + "_" + i++;
        }
        return tmpName;
    }
	
	public static String getUniqueLinkName(Config config, String baseName){
		String tmpName = baseName;
		int i = 1;
		while (config.getLinkByName(tmpName) != null) {
			tmpName = baseName + "_" + i++;
		}
		return tmpName;
	}
	/**
	 * return a map<config2.dataset, config1.dataset>
	 * @param config1
	 * @param config2
	 * @return
	 */
	public static String getLinkedDatasetsForConfigs(Config config1, Config config2) {
		PartitionTable pt1 = config1.getMart().getSchemaPartitionTable();
		PartitionTable pt2 = config2.getMart().getSchemaPartitionTable();
		if(pt1.getTotalRows()>1 && pt2.getTotalRows()>1) {
			int newCol = pt2.addColumn("");
			for(int i=0; i<pt2.getTotalRows(); i++) {
				String ds2displayName = pt2.getValue(i, PartitionUtils.DISPLAYNAME);
				int rowInPt1 = getRowInPartitionByDisplayName(pt1, ds2displayName);
				String ds1Name = "";
				if(rowInPt1!=-1) {
					ds1Name = pt1.getValue(rowInPt1, PartitionUtils.DATASETNAME);
				}
				pt2.setValue(i, newCol, ds1Name);
			}
			return "(p0c"+newCol+")";
		}else if(pt1.getTotalRows() ==1){ //m:1
			return pt1.getValue(0,PartitionUtils.DATASETNAME);
		} else {		
			StringBuilder sb = new StringBuilder();
			for(int k=0; k<pt1.getTotalRows();k++) {
				if(k==0)
					sb.append(pt1.getValue(k, PartitionUtils.DATASETNAME));
				else
					sb.append(","+pt1.getValue(k, PartitionUtils.DATASETNAME));
			}
			return sb.toString();
			
		}
	}
	
	private static int getRowInPartitionByDisplayName(PartitionTable pt, String displayName) {
		for(int i=0; i<pt.getTotalRows(); i++) {
			if(displayName.equals(pt.getValue(i, PartitionUtils.DISPLAYNAME))) {
				return i;
			}
		}
		return -1;
	}

	public static List<String> getOtherDatasets(PartitionTable pt, List<String> datasets, String pointedDsName) {
		List<String> result = new ArrayList<String>();
		for(String inputDsStr: datasets) {
			int row = pt.getRowNumberByDatasetName(inputDsStr);
			if(row<0)
				continue;
			//String newValue = schemaPt.getValue(row, col);
			String newValue = McUtils.getRealName(pt, row, pointedDsName);
			//split by ","
			if(newValue == null) {
				Log.debug("check pointeddataset value");
				continue;
			}
			String[] _newDs = newValue.split(",");
			for(String item: _newDs) {
				result.add(item);
			}
		}
		return result;
	}

	public static String[] getOptionsDataFromString(String value) {
		String[] tmp =  value.split("(?<!\\\\)\\|",-1);
		for(int i=0; i<tmp.length; i++) {
			if(tmp[i].indexOf("\\|")>=0) {
				tmp[i]= tmp[i].replaceAll("\\\\\\|", "\\|");
			}
		}
		return tmp;
	}

	public static String getUniqueColumnName(Table table, String base) {
		// First we need to find out the base name, ie. the bit
		// we append numbers to make it unique, but before any
		// key suffix. If we appended numbers after the key
		// suffix then it would confuse MartEditor.
		String name = base;
		String suffix = "";
		String baseName = name;
		if (name.endsWith(Resources.get("keySuffix"))) {
			suffix = Resources.get("keySuffix");
			baseName = name.substring(0, name.indexOf(suffix));
		}
		// Now simply check to see if the name is used, and
		// then add an incrementing number to it until it is unique.
		int k = 1;
		for(Column cc: table.getColumnList()) {
			if(cc.getName().equals(name)) {
				name = baseName + "_"+k+suffix;
				k++;
			}
		}
		return name;
	}
	
	
	public static Filter findFilterInMaster(Filter filter) {
		Config sConfig = filter.getParentConfig();
		if(sConfig.isMasterConfig())
			return filter;
		else 
			return sConfig.getMart().getMasterConfig().getFilterByName(filter.getName(), new ArrayList<String>());
	}
	

	private static int loopCount = 0;
    private static Long timestamp1 = 0L;
    private static Long timestamp2 = 0L;
	public static void timing1() {
		timestamp1 = new Date().getTime();
		System.out.println("> " + loopCount + "\t" + timestamp1 + "\t\t" + (timestamp1-timestamp2) + "\n");
	}
	public static void timing2() {
		timestamp2 = new Date().getTime();
        System.out.println("< " + loopCount + "\t" + timestamp2 + "\t\t\t" + (timestamp2-timestamp1) + "\n\n");
        loopCount++;
	}
}