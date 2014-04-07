package org.biomart.configurator.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.update.UpdateMartModel;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.JdbcType;
import org.biomart.configurator.view.menu.McMenus;
import org.biomart.oauth.persist.Accessor;
import org.biomart.oauth.persist.Consumer;
import org.biomart.oauth.provider.core.SimpleOAuthProvider;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.start.AppConfig;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 *  update command line interface
 *
 */
public class UpdateCLI {
	
	private String fileName;
	
	public static void main(String[] args) {
		if(args.length<2) {
			System.exit(0);
		}
  		Resources.setResourceLocation("org/biomart/configurator/resources");  	
        Settings.loadGUIConfigProperties();
        Settings.loadAllConfigProperties();
		UpdateCLI cli = new UpdateCLI();
		try {
			cli.update(args[0], args[1]);
		} catch (MartBuilderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void update(String registryFile, String configFile) throws MartBuilderException {
		this.fileName = registryFile;
		Element root = this.openConfigFile(configFile);
		String openid = root.getChild("oauth").getAttributeValue("openid");
		String key = null;
		try {
			key = this.generateOAuthKey(openid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(key == null) {
			System.out.println("no key generated");
			System.exit(1);
		}
		
		this.updateDatasets(root.getChild("datasets"));
		String fileName = this.saveXML();
		this.saveOAuth(fileName, key);
		this.changePropertyFile(fileName);
	}
	
	private void updateDatasets(Element element) throws MartBuilderException {
		MartRegistry registry = this.openXML(this.fileName);
		@SuppressWarnings("unchecked")
		List<Element> dsElements = element.getChildren();
		UpdateMartModel updateModel = new UpdateMartModel();
		for(Element dsElement: dsElements) {
			String dsName = dsElement.getAttributeValue("name");
			String database = dsElement.getAttributeValue("database");
			String host = dsElement.getAttributeValue("dbhost");
			String port = dsElement.getAttributeValue("dbport");
			String user = dsElement.getAttributeValue("dbuser");
			String password = dsElement.getAttributeValue("password");
			//connect to database
			String tmplate = JdbcType.MySQL.getUrlTemplate();
			tmplate = tmplate.replaceAll("<HOST>", host);
			tmplate = tmplate.replaceAll("<PORT>", port);

			JdbcLinkObject conObj = new JdbcLinkObject(tmplate,database,database,user,password,
					JdbcType.MySQL,"","",true);
			McSQL mcsql = new McSQL();			
			List<String> martList = mcsql.getMainTableInfo(conObj);
			for(String martStr: martList) {
				String tmpMartStr = martStr;
				if(martStr.equals("hsapiens_gene_ensembl"))
					tmpMartStr = "gene_ensembl";
				Mart mart = registry.getMartByName(tmpMartStr);
				if(mart!=null) {
					Log.info("updating mart "+martStr);
					//change connection parameters for dataset
					String datasetname = martStr+"_"+dsName;
					PartitionTable pt = mart.getSchemaPartitionTable();
					int row = pt.getRowNumberByDatasetName(datasetname);
					if(row!=-1) {
						pt.setValue(row, PartitionUtils.DATABASE, database);
						pt.setValue(row, PartitionUtils.SCHEMA, database);
						pt.setValue(row, PartitionUtils.CONNECTION,conObj.getConnectionBase());
						pt.setValue(row, PartitionUtils.USERNAME, user);
						pt.setValue(row, PartitionUtils.PASSWORD, password);
						pt.setValue(row, PartitionUtils.HIDE, Boolean.toString(false));
						List<Dataset> dsList = new ArrayList<Dataset>();
						dsList.add(mart.getDatasetByName(datasetname));
						updateModel.updateDatasets(dsList, true);
						Log.info("updating dataset "+datasetname);
					} else {
						Log.info("cannot find dataset "+datasetname);
					}
				}else {
					Log.info("cannot find mart "+martStr);
				}
			}
		}

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

	private void changePropertyFile(String registryFile) {
		File file = new File(registryFile);
		String tmpName = file.getName();
		int index = tmpName.lastIndexOf(".");
		String keyFile = null;
		if(index>0) {
			tmpName = tmpName.substring(0,index);
			keyFile = file.getParent()+File.separator+"."+tmpName;
		}

		String propertyFile = System.getProperty("biomart.properties");
    	List<String> lines = new ArrayList<String>();
       	try {
       		BufferedReader input =  new BufferedReader(new FileReader(new File(propertyFile)));
       		try {
       	        String line = null; //not declared within while loop
       	        /*
       	        * readLine is a bit quirky :
       	        * it returns the content of a line MINUS the newline.
       	        * it returns null only for the END of the stream.
       	        * it returns an empty String if two newlines appear in a row.
       	        */
       	        while (( line = input.readLine()) != null){
       	        	if(line.indexOf("biomart.registry.file")>=0) {
       	        		lines.add("biomart.registry.file="+registryFile+"\n");
       	        	} else if(line.indexOf("biomart.registry.key.file")>=0) {
       	        		lines.add("biomart.registry.key.file="+keyFile+"\n");
       	        	}else
       	        		lines.add(line+"\n");
       	        }
       	      }
       	      finally {
       	        input.close();
       	      }
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Writer output = null;
		try {
			try {
			output = new BufferedWriter(new FileWriter(new File(propertyFile)));
	    	for(String str: lines) 
	    		output.write(str);
			} finally {
				output.close();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
	private Element openConfigFile(String fileName) {
		File file = new File(fileName);
		Document document = null;
		try {
			SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
			document = saxBuilder.build(file);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return document.getRootElement();
	}

	private String saveXML() {		
		int index = this.fileName.lastIndexOf(".");
		String prefix = this.fileName.substring(0, index);
		String savedFile = prefix + "-"+ McUtils.getCurrentTimeString()+".xml";
		File file = new File(savedFile);
		McMenus.getInstance().requestSavePortalToFile(file, false);
		return file.getAbsolutePath();
	}

	private void saveOAuth(String fileName, String value) {
		int index = this.fileName.lastIndexOf(".");
		String prefix = this.fileName.substring(0, index);
		String savedFile = prefix + ".oauth.key";		
		File file = new File(savedFile);
		FileWriter fos;
		try {
			fos = new FileWriter(file);
			fos.write(value);
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String generateOAuthKey(String openid) throws IOException, OAuthException {	
		File current = new File(".");
		String properties = current.getCanonicalPath()+File.separator+"biomart.properties";
        System.setProperty("biomart.properties", properties);

		AppConfig.loadProperties();
               
        String consumerKey = System.getProperty("oauth.consumer.key");
        String consumerSecret = System.getProperty("oauth.consumer.secret");
        if (consumerSecret.startsWith("OBF:")) {
            consumerSecret = org.mortbay.jetty.security.Password.deobfuscate(consumerSecret);
        }
        if (consumerKey != null && consumerSecret != null) {
            Log.info(String.format("Populating default BioMart OAuth consumer: key=%s", consumerKey));
            OAuthConsumer oauthConsumer = new OAuthConsumer("oob", consumerKey, consumerSecret, null);
            oauthConsumer.setProperty("name", "BioMart");
            oauthConsumer.setProperty("description", "The default BioMart OAuth consumer");
            Consumer c = new Consumer(oauthConsumer);
            c.save();
            SimpleOAuthProvider.loadConsumers();
            
            OAuthAccessor oa = new OAuthAccessor(c.oauthConsumer);
            Accessor a = new Accessor(openid, oa);
            SimpleOAuthProvider.generateRequestToken(oa);
            SimpleOAuthProvider.generateAccessToken(oa);
            SimpleOAuthProvider.markAsAuthorized(oa, openid);
            a.save();
            
            return c.getKey()+","+c.getSecret()+","+oa.accessToken+","+oa.tokenSecret;
        }
        return null;
        
 
	}
	
	
}