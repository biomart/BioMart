package org.biomart.objects.portal;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.resources.Log;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class UserGroup extends MartConfiguratorObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//only for group type
	private List<User> userList;
	private List<MartPointer> mpList;
	
	public UserGroup(String name, String displayName, String password) {
		super(name);
		if(McUtils.isStringEmpty(displayName))
			this.setDisplayName(name);
		else
			this.setDisplayName(displayName);
		this.setProperty(XMLElements.PASSWORD, password);
		this.mpList = new ArrayList<MartPointer>();
		this.userList = new ArrayList<User>();
		this.setNodeType(McNodeType.GROUP);
	}
	
	public UserGroup(Element element) {
		super(element);
		if(McUtils.isStringEmpty(element.getAttributeValue(XMLElements.DISPLAYNAME.toString()))) {
			this.setDisplayName(this.getName());
		}
		this.setNodeType(McNodeType.GROUP);
		this.userList = new ArrayList<User>();
		this.mpList = new ArrayList<MartPointer>();
		@SuppressWarnings("unchecked")
		List<Element> userElementList = element.getChildren(XMLElements.USER.toString());
		for(Element userElement: userElementList) {
			this.addUser(new User(userElement));
		}
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.GROUP.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.PASSWORD.toString(),this.getPropertyValue(XMLElements.PASSWORD));
		element.setAttribute(XMLElements.HIDE.toString(),this.getPropertyValue(XMLElements.HIDE));
		element.setAttribute(XMLElements.LOCATION.toString(),this.getPropertyValue(XMLElements.LOCATION));
		if(this.mpList.size()>0) {
			StringBuilder sb = new StringBuilder();
			sb.append(this.mpList.get(0).getConfig().getName());
			for(int i=1; i<this.mpList.size(); i++) {
				sb.append(","+this.mpList.get(i).getName());
			}
			element.setAttribute(XMLElements.CONFIG.toString(),sb.toString());
		}else
			element.setAttribute(XMLElements.CONFIG.toString(),"");
		//user
		for(User user: this.userList) {
			element.addContent(user.generateXml());
		}
		return element;		
	}
	
	public void addUser(User user) {
		if(!this.userList.contains(user)) {
			this.userList.add(user);
			user.setParent(this);
		}
	}
	
	public void removeUser(User user) {
		this.userList.remove(user);
	}

	
	public List<User> getUserList() {
		return this.userList;
	}
	

	public void synchronizedFromXML() {
		GuiContainer rootGc = ((Portal)this.getParent().getParent()).getRootGuiContainer();
		this.mpList.clear();
		//configs
		String configs = this.getPropertyValue(XMLElements.CONFIG);
		if(!McUtils.isStringEmpty(configs)) {
			String[] _configs = configs.split(",");
			for(String conStr: _configs) {
				MartPointer mp = rootGc.getMartPointerByNameRecursively(conStr);
				if(mp!=null)
					this.mpList.add(mp);
				else
					Log.debug("invalid config "+conStr);
			}
		}
		this.setObjectStatus(ValidationStatus.VALID);
	}
	
	public List<MartPointer> getMartPointers() {
		return this.mpList;
	}

	@Override
	public boolean equals(Object object) {
		if (this==object) {
			return true;
		}else if((object==null) || (object.getClass()!= this.getClass())) {
			return false;
		}
		UserGroup user = (UserGroup)object;
		return user.getName().equals(this.getName());
	}
	
	@Override
	public int hashCode() {
		return this.getName().hashCode();
	}
	
	public void addMartPointer(MartPointer mp) {
		this.mpList.add(mp);
	}

	public boolean isAnonymous() {
		return this.getName().equals("anonymous");
	}

}