package org.biomart.objects.objects;

import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.PortableType;

public class Importable extends ElementList {

	public Importable(Config config, String name) {
		super(config, name, PortableType.IMPORTABLE);
	}
	
	public Importable(org.jdom.Element element) {
		super(element);
		this.setNodeType(McNodeType.IMPORTABLE);
		this.portableType = PortableType.IMPORTABLE;
	}
}