package org.biomart.objects.enums;

import org.biomart.processors.ProcessorInterface;
import org.biomart.processors.TSV;

public enum ProcessorType {
	HTML    ("HTML"),
	TSV		("TSV"),	
	CSV		("CSV"),
	FASTA	("FASTA");
	
	private String value = null;
	private ProcessorType() {
		this(null);
	}
	private ProcessorType(String value) {
		this.value = value;
	}
	public String getValue() {
		return this.value;
	}
	public static ProcessorType fromValue(String value) {
		for (ProcessorType processorType : values()) {
			if (processorType.value.equals(value)) {
				return processorType;
			}
		}
		return null;
	}
	@Override
	public String toString() {
		return getValue();
	}
	
	public static ProcessorInterface fromProcessorType(ProcessorType processorType) {
		if (ProcessorType.TSV.equals(processorType)) {
			return new TSV();
		} else
		return null;	//TODO
	}
}
