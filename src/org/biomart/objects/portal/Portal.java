package org.biomart.objects.portal;

import java.io.Serializable;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.jdom.Element;

public class Portal extends MartConfiguratorObject implements Serializable {

	private static final long serialVersionUID = 3193550598191848391L;

	private Users users = null;
	private Aliases aliases = null;
	private LinkIndices linkIndices;
	private GuiContainer rootGuiContainer;
		
	public Portal(Element portalElement) {
		super(portalElement);
		this.setNodeType(McNodeType.PORTAL);
		Element userElement = portalElement.getChild(XMLElements.USERS.toString());
		if(userElement!=null) {
			this.users = new Users(userElement);
			this.users.setParent(this);
		}
		//linkindices
		Element linkIndicesElement = portalElement.getChild(XMLElements.LINKINDEXES.toString());
		if(linkIndicesElement!=null) {
			this.linkIndices = new LinkIndices(linkIndicesElement);
			this.linkIndices.setParent(this);
		}
		//create guicontainer
		Element rootGCElement = portalElement.getChild(XMLElements.GUICONTAINER.toString());
		GuiContainer rootGC = new GuiContainer(rootGCElement);
		this.setRootGuiContainer(rootGC);
	}
	
	public void addUser(UserGroup user) {
		this.users.addUserGroup(user);
	}
	
	public void addLinkIndex(LinkIndex index) {
		this.linkIndices.addLinkIndex(index);
	}
	
	public Users getUsers() {
		return users;
	}
	public void setUsers(Users users) {
		this.users = users;
	}
	public Aliases getAliases() {
		return aliases;
	}
	public void setAliases(Aliases aliases) {
		this.aliases = aliases;
	}
	public LinkIndices getLinkIndices() {
		return linkIndices;
	}
	public void setLinkIndices(LinkIndices li) {
		this.linkIndices = li;
	}

	
	public GuiContainer getRootGuiContainer() {
		return this.rootGuiContainer;
	}

	public void setRootGuiContainer(GuiContainer rootGuiContainer) {
		this.rootGuiContainer = rootGuiContainer;
		this.rootGuiContainer.setParent(this);
	}



	@Override
	public String toString() {
		return this.getName();
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.PORTAL.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.NAME));
		//element.setAttribute(XMLElements.HIDE.toString(), this.getPropertyValue(XMLElements.HIDE));

		element.addContent(users.generateXml());
		
		if (this.aliases!=null) {
			element.addContent(aliases.generateXml());
		}

		element.addContent(linkIndices.generateXml());
		
		if (this.rootGuiContainer!=null) {
			element.addContent(rootGuiContainer.generateXml());
		}
		return element;
	}

	public void synchronizedFromXML() {
		//linkindices
		if(this.linkIndices!=null)
			this.linkIndices.synchronizedFromXML();
		//root guicontainer
		this.rootGuiContainer.synchronizedFromXML();

		if(this.users!=null)
			this.users.synchronizedFromXML();

		this.setObjectStatus(ValidationStatus.VALID);
	}
	
	public void importPortal(Element portal, MartRegistry registry){
		GuiContainer root = new GuiContainer(portal);
		/*
		for(MartPointer mp:	root.getAllMartPointerListResursively()){
			if(mp.getConfig() == null)
				mp.getGuiContainer().removeMartPointer(mp);
		
		}*/
		/*GuiContainer gc = null;
		for(GuiContainer gcs : this.rootGuiContainer.getGuiContainerList()){
			if(gcs.getDisplayName().equals("import")){
				gc = gcs;
				break;
			}
		}
		if(gc == null)
			gc = new GuiContainer("import");*/
		//gc.addGuiContainer(root);
		for(GuiContainer gcs : root.getGuiContainerList()){
			if(gcs.getName().equals("root")){
				gcs.setName("import");
				gcs.setDisplayName("import");
				
			}
			this.rootGuiContainer.addGuiContainer(gcs);
		}
		//this.rootGuiContainer.addGuiContainer(gc);
	}
}

