package org.biomart.objects.objects;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.portal.Portal;
import org.jdom.Document;
import org.jdom.Element;


public class MartRegistry extends MartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = 4555425904982314129L;

	private Portal portal = null;
	private final List<Mart> martList;
	private SourceContainers sourcecontainers;

	public MartRegistry(String name) {
		super(name);
		this.martList = new ArrayList<Mart>();
		this.setNodeType(McNodeType.MARTREGISTRY);
		this.setParent(null);
	}	
	
	/*
	 * for web api only
	 */
	public MartRegistry(Document xmlDoc, String keyFile) {
		this(xmlDoc.getRootElement());	
		if(!McUtils.isStringEmpty(keyFile)) {
			BufferedReader input;
			try {
				input = new BufferedReader(new FileReader(keyFile));
				String key = input.readLine();
				McUtils.setKey(key);
				input.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		MartController.getInstance().requestCreateRegistryFromXML(this,xmlDoc);
	}

	private MartRegistry(Element registryElement) {
		super(registryElement);
		this.martList = new ArrayList<Mart>();
		this.setNodeType(McNodeType.MARTREGISTRY);
		this.setParent(null);
	}
	
	public void setPortal(Portal portal) {
		if(portal!=null) {
			this.portal = portal;
			portal.setParent(this);
		}
	}
	public void addMart(Mart mart) {
		if(!this.martList.contains(mart)) {			
			this.martList.add(mart);
			mart.setParent(this);
			MartController.getInstance().setChanged(true);
		}
	}
	
	public void removeMart(Mart mart) {
		//before remove mart also clear all links that related to the mart
		Config masterConfig = mart.getMasterConfig();
		if(masterConfig!=null)
			for(Link l : masterConfig.getLinkList()){
				if(l.getPointedMart() != null){
					List<Config> pconfigs = l.getPointedMart().getConfigList();
					for(Config c : pconfigs){
						Link link = McUtils.getOtherLink(l);//McUtils.getLink(c,currentLink.getParentConfig());
						
			    		if(null!=link){
			    			c.removeLink(link);
			    		}			
					}
				}
			}
		
		//remove options
		
		if(this.martList.remove(mart)) {
			MartController.getInstance().setChanged(true);
		}
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public Mart getMartByName(String name) {
		for(Mart mart: this.martList) {
			if(mart.getName().equals(name))
				return mart;
		}
		return null;
	}
		
	public Document generateXmlDocument() throws FunctionalException {
		return new Document(this.generateXml());
	}
	
	public List<Document> generateMultiXmlDocuments() throws FunctionalException {
		List<Document> dlist = new ArrayList<Document>();
		Element element = new Element(XMLElements.MARTREGISTRY.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(), this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.MULTIFILE.toString(), XMLElements.TRUE_VALUE.toString());
		
		element.addContent(this.portal.generateXml());	
		dlist.add(new Document(element));
		
		for (Mart mart : this.martList) {
			dlist.add(new Document(mart.generateXml()));
		}
		//add options element
		if(Options.getInstance().getOptionRootElement()!=null)
			dlist.add(new Document(Options.getInstance().getOptionRootElement()));
		
		return dlist;
	}
	
	public Element generateXml() throws FunctionalException {
		//clean options
		Options.getInstance().clear();
		try{
			Element element = new Element(XMLElements.MARTREGISTRY.toString());
			element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
			element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
			element.setAttribute(XMLElements.DISPLAYNAME.toString(), this.getPropertyValue(XMLElements.DISPLAYNAME));
			element.setAttribute(XMLElements.DESCRIPTION.toString(), this.getPropertyValue(XMLElements.DESCRIPTION));
			//element.setAttribute(XMLElements.HIDE.toString(), this.getPropertyValue(XMLElements.HIDE));
			
			element.addContent(this.portal.generateXml());	
			element.addContent(this.sourcecontainers.generateXml());
			for (Mart mart : this.martList) {
				element.addContent(mart.generateXml());
			}
			return element;
		}catch(NullPointerException npe){
			npe.printStackTrace();
		}
		return null;		
	}

	/**
	 * get all marts including hidden marts
	 * @return
	 */
	public List<Mart> getMartList() {
		return this.martList;
	}
	
	@Deprecated
	public Dataset getDatasetByName(String datasetName) {
		List<Dataset> dsList = new ArrayList<Dataset>();
		for(Mart mart: this.getMartList()) {
			for(Dataset ds: mart.getDatasetList())
				if(ds.getName().equals(datasetName))
					dsList.add(ds);
		}
		if(dsList.isEmpty())
			return null;
		else if(dsList.size()==1)
			return dsList.get(0);
		else {
			//check default;
			for(Dataset ds: dsList) {
				if(ds.getParentMart().getDefaultConfig().isDefaultConfig())
					return ds;
			}
			return dsList.get(0);
		}			
	}

	public Dataset getDatasetByName(String datasetName, String config) {
		if(config==null || config.trim().equals(""))
			return this.getDatasetByName(datasetName);
		for(Mart mart: this.getMartList()) {
			for(Dataset ds: mart.getDatasetList())
				if(ds.getName().equals(datasetName)){
					if(mart.getConfigByName(config)!=null)
						return ds;
				}
		}
		return null;
	}

    public Config getConfigByName(String name) {
		for(Mart mart: this.getMartList()) {
            Config config;
            if((config = mart.getConfigByName(name)) != null) {
                return config;
            }
		}
        return null;
    }

	
	public String getNextMartName(String baseName) {
		String tmpName = baseName;
		int i=1;
		while(this.getMartByName(tmpName)!=null) {
			tmpName = baseName + "_"+i++;
		}
		return tmpName;
	}

	
	public String getVersion() {
		return Resources.BIOMART_VERSION;
	}

	@Deprecated
    public void synchronizedFromXML() {    	
    	for(Mart mart: this.martList) {
    		mart.synchronizedFromXML();
    	}   	
    	//portal
    	this.portal.synchronizedFromXML();
    	//FIXME synchronization shouldnot set the status, only validation set the status
    	this.setObjectStatus(ValidationStatus.VALID);
    }
 

	@Override
	public List<MartConfiguratorObject> getChildren() {
		List<MartConfiguratorObject> children = new ArrayList<MartConfiguratorObject>();
		children.add(this.portal);
		for(Mart mart: this.martList) 
			children.add(mart);
		return children;
	}

	
	public List<Mart> getMartsInGroup(String group) {
		List<Mart> marts = new ArrayList<Mart>();
		for(Mart mart: this.martList) {
			if(group.equals(mart.getGroupName()))
				marts.add(mart);
			else if(McUtils.isStringEmpty(mart.getGroupName()) && group.equals(XMLElements.DEFAULT.toString()))
				marts.add(mart);
		}
		return marts;
	}

	public void setSourcecontainers(SourceContainers sourcecontainers) {
		this.sourcecontainers = sourcecontainers;
		this.sourcecontainers.setParent(this);
	}

	/*
	 * sourcecontainers should not be null;
	 */
	public SourceContainers getSourcecontainers() {
		return sourcecontainers;
	}
}
