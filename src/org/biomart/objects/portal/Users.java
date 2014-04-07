package org.biomart.objects.portal;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class Users extends MartConfiguratorObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<UserGroup> userList;
	
	
	public Users(Portal portal) {
		super(XMLElements.USERS.toString());
		this.parent = portal;
		this.userList = new ArrayList<UserGroup>();
		this.setNodeType(McNodeType.USERS);
		portal.setUsers(this);
	}
	
	public Users(org.jdom.Element element) {
		super(element);
		this.userList = new ArrayList<UserGroup>();
		this.setNodeType(McNodeType.USERS);
		@SuppressWarnings("unchecked")
		List<Element> ugElementList = element.getChildren(XMLElements.GROUP.toString());

		for(Element userElement: ugElementList) {
			UserGroup user = new UserGroup(userElement);
			this.addUserGroup(user);
		}	

	}
	
	public org.jdom.Element generateXml() {
		Element element = new Element(XMLElements.USERS.toString());
		for(UserGroup user: this.userList) {
			element.addContent(user.generateXml());
		}
		return element;
	}
	
	public void addUserGroup(UserGroup user) {
		if(!this.userList.contains(user)) {
			this.userList.add(user);
			user.setParent(this);
		}
	}
	
	public void removeUserGroup(UserGroup ug) {
		this.userList.remove(ug);
	}
	
	public UserGroup getUserGroupByName(String name) {
		for(UserGroup user: userList) {
			if(user.getName().equals(name))
				return user;
		}
		return null;
	}
	
	public User getUserByName(String name) {
		for(UserGroup ug: userList) {
			for(User user: ug.getUserList()) {
				if(user.getName().equals(name))
					return user;
			}
		}
		return null;
	}
	
	public User getUserByOpenId(String name) {
		for(UserGroup ug: userList) {
			for(User user: ug.getUserList()) {
				if(user.getPropertyValue(XMLElements.OPENID).equals(name))
					return user;
			}
		}
		return null;
	}
	
	public List<UserGroup> getUserList() {
		return this.userList;
	}

	public void synchronizedFromXML() {
		for(UserGroup user: this.userList) {
			user.synchronizedFromXML();
		}
		
		this.setObjectStatus(ValidationStatus.VALID);
	}

}