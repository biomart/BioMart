package org.biomart.objects.objects;

public class VirtualLink {
	private ElementList exportable;
	private ElementList importable;
	
	public VirtualLink(ElementList exp, ElementList imp) {
		this.exportable = exp;
		this.importable = imp;
	}
	
	public ElementList getExportable() {
		return this.exportable;
	}
	
	public ElementList getImportable() {
		return this.importable;
	}
/*	
	@Override
	public int hashCode() {
		return exportable.hashCode()+importable.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == this)
			return true;
		else if(obj instanceof VirtualLink) {
			
		}
		return false;
	}*/
}