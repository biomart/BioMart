package org.biomart.objects.objects;

import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.PortableType;

public class Exportable extends ElementList {

	public Exportable(Config config, String name) {
		super(config, name, PortableType.EXPORTABLE);
	}
	
	public Exportable(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.EXPORTABLE);
		this.portableType = PortableType.EXPORTABLE;
	}
	
}