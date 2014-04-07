    package org.biomart.processors;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;

/**
 * Just an empty shell for now (so we can reference it)
 * @author anthony
 *
 * DEPRECATED - SHOULD BE GONE. dont understand why this has got 52 references
 * in the project space - scary!
 */
public class Processor extends MartConfiguratorObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Container container;
	private List<String> exportList;
	
	public Processor(ProcessorGroup processors, String name) {
		super(name);
		this.parent = processors;
		processors.addProcessor(this);
		this.setNodeType(McNodeType.PROCESSOR);
		this.exportList = new ArrayList<String>();
	}
	
	public Processor(Element element) {
		super(element);
		this.setNodeType(McNodeType.PROCESSOR);
		this.exportList = new ArrayList<String>();
	}
	
	public Container getContainer() {
		return this.container;
	}
	
	public ProcessorGroup getProcessors() {
		return (ProcessorGroup)this.parent;
	}
	
	public void setContainer(Container container) {
		this.container = container;
		if(container!=null)
			this.setProperty(XMLElements.CONTAINERS, container.getName());
		else
			this.setProperty(XMLElements.CONTAINERS, "");
	}
	
	public void addExportFormatter(String value) {
		this.exportList.add(value);
		this.setProperty(XMLElements.EXPORT, McUtils.StrListToStr(this.exportList, ","));
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.PROCESSOR.toString());
		element.setAttribute(XMLElements.NAME.toString(), this.getName());
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.NAME));
		if(this.container!=null)
			element.setAttribute(XMLElements.CONTAINERS.toString(),this.container.getName());
		else
			element.setAttribute(XMLElements.CONTAINERS.toString(),"");

		element.setAttribute(XMLElements.EXPORT.toString(),this.getPropertyValue(XMLElements.EXPORT));
		return element;
	}



	@Override
	public void synchronizedFromXML() {
		MartPointer mp = this.getProcessors().getMartPointer();
		Container c = mp.getConfig().getRootContainer().getContainerByNameResursively(this.getPropertyValue(XMLElements.CONTAINERS));
		this.setContainer(c);
		this.setObjectStatus(ValidationStatus.VALID);
	}
}
