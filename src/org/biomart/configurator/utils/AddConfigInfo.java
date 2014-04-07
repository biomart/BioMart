package org.biomart.configurator.utils;

public class AddConfigInfo {
	private String name;
	private boolean doNaive;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDoNaive(boolean doNaive) {
		this.doNaive = doNaive;
	}

	public boolean isDoNaive() {
		return doNaive;
	}
}