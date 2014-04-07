package org.biomart.processors;

import java.util.ArrayList;
import java.util.List;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.MartPointer;
import org.jdom.Element;

/**
 *
 * @author Yong Liang
 *
 * The object to encapsulate various processor in a group.
 * The group contains individual processors that are similar in the behaviour and
 * can be perceived as formatters for results.
 */
public class ProcessorGroup extends MartConfiguratorObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final List<Processor> processorList;
	
    /**
     *
     * @param martPointer
     * @param name
     */
    public ProcessorGroup(MartPointer martPointer, String name) {
		super(name);
		this.parent = martPointer;
		this.setNodeType(McNodeType.PROCESSORS);
		this.processorList = new ArrayList<Processor>();
	}
	
    /**
     *
     * @param element
     */
    public ProcessorGroup(Element element) {
		super(element);
		this.setNodeType(McNodeType.PROCESSORS);
		this.processorList = new ArrayList<Processor>();
		List<Element> pElementList = element.getChildren(XMLElements.PROCESSOR.toString());
		for(Element pElement: pElementList) {
			Processor p = new Processor(pElement);
			this.addProcessor(p);
		}
	}
	
    /**
     *
     * @param processor
     */
    public void addProcessor(Processor processor) {
		this.processorList.add(processor);
		processor.setParent(this);
	}
	
    /**
     *
     * @param name
     * @return
     */
    public Processor getProcessorByName(String name) {
		for(Processor p: this.processorList) {
			if(p.getName().equals(name))
				return p;
		}
		return null;
	}
	
    /**
     *
     * @return
     */
    public List<Processor> getProcessorList() {
		return this.processorList;
	}
	
	public Element generateXml() {
		Element element = new Element(XMLElements.PROCESSORGROUP.toString());
		element.setAttribute(XMLElements.NAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.INTERNALNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DISPLAYNAME.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DESCRIPTION.toString(),this.getPropertyValue(XMLElements.NAME));
		element.setAttribute(XMLElements.DEFAULT.toString(),this.getPropertyValue(XMLElements.DEFAULT));
		for(Processor p: this.processorList) {
			element.addContent(p.generateXml());
		}
		return element;
	}
	
	
    /**
     *
     * @return
     */
    public MartPointer getMartPointer() {
		return (MartPointer)this.parent;
	}
	
    /**
     *
     * @param p
     */
    public void setDefaultProcessor(Processor p) {
		if(p!=null)
			this.setProperty(XMLElements.DEFAULT, p.getName());
		else
			this.setProperty(XMLElements.DEFAULT, "");
	}
	
    /**
     *
     * @return
     */
    public Processor getDefaultProcessor() {
		return this.getProcessorByName(this.getPropertyValue(XMLElements.DEFAULT));
	}


    /**
     *
     */
    @Override
	public void synchronizedFromXML() {
		for(Processor p: this.processorList) {
			p.synchronizedFromXML();
		}
		this.setObjectStatus(ValidationStatus.VALID);
	}
}