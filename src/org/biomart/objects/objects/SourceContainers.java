package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.List;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

public class SourceContainers extends MartConfiguratorObject {
	private List<SourceContainer> sourceContainerList;

	public SourceContainers(Element element) {
		super(element);
		this.setNodeType(McNodeType.SOURCECONTAINERS);
		sourceContainerList = new ArrayList<SourceContainer>();
		@SuppressWarnings("unchecked")
		List<Element> scElementList = element.getChildren();
		for(Element scElement: scElementList) {
			SourceContainer sc = new SourceContainer(scElement);
			this.addSourceContainer(sc);
		}
	}
	
	protected SourceContainers(String name) {
		super(name);
		this.setNodeType(McNodeType.SOURCECONTAINERS);
		sourceContainerList = new ArrayList<SourceContainer>();
	}
	
	public List<SourceContainer> getSourceContainerList() {
		return this.sourceContainerList;
	}
	
	public void addSourceContainerForOldXML(MartRegistry registry) {		
		for(Mart mart: registry.getMartList()) {
			String gname = mart.getGroupName();
			SourceContainer sc = this.getSourceContainerByName(gname);
			if(sc == null) {
				sc = new SourceContainer(gname);
				this.addSourceContainer(sc);
			}
		}

	}
	
	public void addSourceContainer(SourceContainer sc) {
		this.sourceContainerList.add(sc);
		sc.setParent(this);
	}
	
	public SourceContainer getSourceContainerByName(String name) {
		for(SourceContainer sc: this.sourceContainerList) {
			if(sc.getName().equals(name))
				return sc;
		}
		return null;
	}

	public boolean removeSourceContainer(String name) {
		SourceContainer sc = this.getSourceContainerByName(name);
		if(sc!=null) {
			return this.sourceContainerList.remove(sc);
		}
		return false;
	}
	
	@Override
	public Element generateXml() throws FunctionalException {
		Element element = new Element(XMLElements.SOURCECONTAINERS.toString());
		super.saveConfigurableProperties(element);
		for(SourceContainer sc: this.sourceContainerList) {
			element.addContent(sc.generateXml());
		}
		return element;
	}

	@Override
	public void synchronizedFromXML() {
		
	}
	
}