package org.biomart.objects.portal;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.jdom.Element;

public class LinkIndices extends MartConfiguratorObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<LinkIndex> linkIndexList;
	
	public LinkIndices(Portal portal) {
		super(XMLElements.LINKINDEXES.toString());
		this.setNodeType(McNodeType.LINKINDEXES);
		this.linkIndexList = new ArrayList<LinkIndex>();
		this.parent = portal;
		portal.setLinkIndices(this);
	}
	
	public LinkIndices(Element element) {
		super(element);
		this.setNodeType(McNodeType.LINKINDEXES);
		this.linkIndexList = new ArrayList<LinkIndex>();
	}
	
	public void addLinkIndex(LinkIndex linkIndex) {
		this.linkIndexList.add(linkIndex);
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.LINKINDEXES.toString());
		for(LinkIndex linkIndex: this.linkIndexList) {
			element.addContent(linkIndex.generateXml());
		}
		return element;
	}
	
	public List<LinkIndex> getLinkIndexList() {
		return this.linkIndexList;
	}

	@Override
	public void synchronizedFromXML() {
	}

	
}