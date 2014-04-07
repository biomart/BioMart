package org.biomart.objects.portal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.enums.EntryLayout;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class GuiContainer extends MartConfiguratorObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String iconName;
	private List<UserGroup> userList;
	private GuiType guiType;
	private EntryLayout entryLayout;
	private List<GuiContainer> guiContainerList;
	private List<MartPointer> martPointerList;
	
	
	public GuiContainer(String name) {
		super(name);
		this.setNodeType(McNodeType.GUICONTAINER);
		this.guiContainerList = new ArrayList<GuiContainer>();
		this.martPointerList = new ArrayList<MartPointer>();
		this.userList = new ArrayList<UserGroup>();
		//this.setGuiType(GuiType.MART_EXPLORER);
		//set gui type default to mart form
		this.setGuiType(GuiType.get("martform"));
	}
	
	public GuiContainer(Element element) {
		super(element);
		this.setNodeType(McNodeType.GUICONTAINER);
		this.guiContainerList = new ArrayList<GuiContainer>();
		this.martPointerList = new ArrayList<MartPointer>();
		this.userList = new ArrayList<UserGroup>();

		//set value
		//create sub guicontainer
		@SuppressWarnings("unchecked")
		List<Element> subGcElementList = element.getChildren(XMLElements.GUICONTAINER.toString());
		for(Element subGcElement: subGcElementList) {
			GuiContainer subGc = new GuiContainer(subGcElement);
			this.addGuiContainer(subGc);
		}
		@SuppressWarnings("unchecked")
		List<Element> mpElementList = element.getChildren(XMLElements.MARTPOINTER.toString());
		for(Element mpElement: mpElementList) {
			MartPointer mp = new MartPointer(mpElement);
			this.addMartPointer(mp);
		}
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.GUICONTAINER.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.INTERNALNAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.DISPLAYNAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.DESCRIPTION));
		element.setAttribute(XMLElements.HIDE.toString(), this.getPropertyValue(XMLElements.HIDE));
		element.setAttribute(XMLElements.INUSERS.toString(),McUtils.listToStr(this.userList, ","));
		if(entryLayout!=null) {
			element.setAttribute(XMLElements.ENTRYLAYOUT.toString(),this.entryLayout.toString());
		}
		if(this.guiType!=null) {
			element.setAttribute(XMLElements.GUITYPE.toString(), this.guiType.toString());
		}

		//save guiContainer recursively
		for(GuiContainer subContainer: this.guiContainerList) {
			Element subElement = subContainer.generateXml();
			element.addContent(subElement);
		}
		
		for(MartPointer martPointer: this.martPointerList) {
			Element martPointerElement = martPointer.generateXml();
			element.addContent(martPointerElement);
		}
		return element;
	}
	
	public void addGuiContainer(GuiContainer guiContainer) {
		/*
		for(GuiContainer subcon : this.getGuiContainerList()){
			if(subcon.getName().equals(guiContainer.getName())){
				for(GuiContainer subsubcon : guiContainer.getGuiContainerList())
					subcon.addGuiContainer(subsubcon);
				return;
			}
		}*/
		guiContainer.setParent(this);
		String nextName = McGuiUtils.INSTANCE.getNextGuiContainerName(guiContainer.getName());
		guiContainer.setName(nextName);
		//guiContainer.setDisplayName(nextName);
		this.guiContainerList.add(guiContainer);
	}
	
	public void removeGuiContainer(GuiContainer guiContainer) {
		this.guiContainerList.remove(guiContainer);
	}
	
	public void addMartPointer(MartPointer martPointer) {
		this.martPointerList.add(martPointer);
		martPointer.setParent(this);
	}
	

	public void removeMartPointer(MartPointer martPointer) {
		this.martPointerList.remove(martPointer);
	}
	
	public List<GuiContainer> getGuiContainerList() {
		return this.guiContainerList;
	}
	
	public GuiContainer getGCByNameRecursively(String name) {
		for(GuiContainer gc: this.getAllGuiContainerRecursively()) {
			if(gc.getName().equals(name))
				return gc;
		}
		return null;
	}
	
	public Collection<GuiContainer> getAllGuiContainerRecursively() {
		Set<GuiContainer> gcSet = new HashSet<GuiContainer>();
		gcSet.addAll(this.guiContainerList);
		for(GuiContainer gc: this.guiContainerList) {
			gcSet.addAll(gc.getAllGuiContainerRecursively());
		}
		return gcSet;
	}
	
	/**
	 * get the martpointer list under this guiContainer, not include the sub guicontainer
	 * @return
	 */
	public List<MartPointer> getMartPointerList() {
		return this.martPointerList;
	}
	
	
	public MartPointer getMartPointerByName(String name) {
		for(MartPointer mp: this.martPointerList) {
			if(mp.getName().equals(name))
				return mp;
		}
		return null;
	}
	
	public MartPointer getMartPointerByNameRecursively(String name) {
		for(MartPointer mp: this.getAllMartPointerListResursively()) {
			if(mp.getName().equals(name))
				return mp;
		}
		return null;
	}
	
	public List<MartPointer> getAllMartPointerListResursively() {
		List<MartPointer> martPointerList = new ArrayList<MartPointer>();
		martPointerList.addAll(this.martPointerList);

		for(GuiContainer gc : this.guiContainerList) {
			martPointerList.addAll(gc.getAllMartPointerListResursively());
		}
		return martPointerList;		
	}
	
	public void setGuiType(GuiType guiLayout) {
		this.guiType = guiLayout;
		this.setProperty(XMLElements.GUITYPE, this.guiType.toString());
	}
	
	public boolean isReportContainer(){
		return this.guiType == GuiType.get("martreport");
	}
	public GuiType getGuiType() {
		return this.guiType;
	}
	
	public void setEntryLayout(EntryLayout configLayout) {
		this.entryLayout = configLayout;
		this.setProperty(XMLElements.ENTRYLAYOUT, configLayout==null?"":configLayout.toString());
	}
	
	public EntryLayout getEntryLayout() {
		return this.entryLayout;
	}
	
	public void addUser(UserGroup user) {
		if(!this.userList.contains(user)) {
			this.userList.add(user);
			this.setProperty(XMLElements.INUSERS, McUtils.listToStr(this.userList, ","));
		}
	}
	
	public void removeUser(UserGroup user) {
		this.userList.remove(user);
		this.setProperty(XMLElements.INUSERS, McUtils.listToStr(this.userList, ","));
	}
	
	public void setIconName(String value) {
		this.iconName = value;
	}
	
	public String getIconName() {
		return this.iconName;
	}
	
	public GuiContainer getRootGuiContainer() {
		MartConfiguratorObject mcObject = this.parent;
		if(mcObject instanceof Portal)
			return this;
		boolean isGC = true;
		while(isGC) {
			if(mcObject.getParent() instanceof Portal) {
				isGC = false;
			} else
				mcObject = mcObject.getParent();
		}
		return (GuiContainer) mcObject;
	}
	
	public boolean isLeaf() {
		if(!this.martPointerList.isEmpty())
			return true;
		else if(this.guiContainerList.isEmpty()) 
			return true;
		else
			return false;
	}
	
	public boolean isRoot() {
		return this.getName().equals("root") && !(this.getParent() instanceof GuiContainer);
	}
	
	public boolean hasSubContainer() {
		return !this.guiContainerList.isEmpty();
	}
	/**
	 * get all the mart pointer recursively for the user
	 * @param user
	 * @return
	 */
	public List<MartPointer> getMartPointerList(UserGroup user) {
		List<MartPointer> martPointerList = new ArrayList<MartPointer>();
		for(MartPointer mp: this.martPointerList) {
			martPointerList.add(mp);
		}
		for(GuiContainer gc : this.guiContainerList) {
			martPointerList.addAll(gc.getMartPointerList(user));
		}
		return martPointerList;
	}
	
	public List<MartPointer> getMartPointerListforMart(Mart mart) {
		List<MartPointer> martPointerList = new ArrayList<MartPointer>();
		for(MartPointer mp: this.martPointerList) {
			if(mp.getMart().equals(mart))
				martPointerList.add(mp);
		}
		for(GuiContainer gc : this.guiContainerList) {
			martPointerList.addAll(gc.getMartPointerListforMart(mart));
		}
		return martPointerList;		
	}
	
	public List<MartPointer> getMartPointerListforMartName(String mart) {
		List<MartPointer> martPointerList = new ArrayList<MartPointer>();
		for(MartPointer mp: this.martPointerList) {
			if(mp.getPropertyValue(XMLElements.MART).equals(mart))
				martPointerList.add(mp);
		}
		for(GuiContainer gc : this.guiContainerList) {
			martPointerList.addAll(gc.getMartPointerListforMartName(mart));
		}
		return martPointerList;		
	}
	
	public Set<MartPointer> getMartPointerListByMartConfig(Mart mart, Config config) {
		Set<MartPointer> martPointerList = new HashSet<MartPointer>();
		for(MartPointer mp: this.martPointerList) {
			if(mp.getMart().equals(mart) && mp.getConfig().equals(config))
				martPointerList.add(mp);
		}
		for(GuiContainer gc : this.guiContainerList) {
			martPointerList.addAll(gc.getMartPointerListByMartConfig(mart,config));
		}
		return martPointerList;				
	}


	@Override
	public void synchronizedFromXML() {
		this.entryLayout = EntryLayout.valueFrom(this.getPropertyValue(XMLElements.ENTRYLAYOUT));
		this.guiType = GuiType.get(this.getPropertyValue(XMLElements.GUITYPE));
		String usersStr = this.getPropertyValue(XMLElements.INUSERS);
		if(!McUtils.isStringEmpty(usersStr)) {
			String[] users = this.getPropertyValue(XMLElements.INUSERS).split(",");
			for(String userStr: users) {
				UserGroup user = ((Portal)this.getRootGuiContainer().getParent()).getUsers().getUserGroupByName(userStr);
				if(user!=null)
					this.addUser(user);
			}
		}
		//sub guicontainer
		for(GuiContainer subgc: this.guiContainerList) {
			subgc.synchronizedFromXML();
		}
		//martpointer
		for(MartPointer mp: this.martPointerList) {
			mp.synchronizedFromXML();
		}
		//martpointer subcomponent
		for(MartPointer mp: this.martPointerList) {
			mp.synchronizeSubComponentFromXML();
		}
		this.setObjectStatus(ValidationStatus.VALID);
	}
	
	public boolean isActivatedInUser(UserGroup user) {
		if(user==null || user.isHidden()) {
			return false;
		}
		return this.userList.contains(user);
	}

	public GuiContainer getReportGuiContainer(){
		for(GuiContainer gc: this.getGuiContainerList()){
			if(gc.isReportContainer())
				return gc;
		}
		return null;
	}
}