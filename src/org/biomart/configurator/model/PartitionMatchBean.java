package org.biomart.configurator.model;

public class PartitionMatchBean {
	private PTCellObject source;
	private PTCellObject target;
	private boolean check;
	
	public void setSource(PTCellObject source) {
		this.source = source;
	}
	
	public PTCellObject getSource() {
		return source;
	}

	public void setTarget(PTCellObject target) {
		this.target = target;
	}

	public PTCellObject getTarget() {
		return target;
	}

	public void setCheck(boolean check) {
		this.check = check;
	}

	public boolean isCheck() {
		return check;
	}	
	
}