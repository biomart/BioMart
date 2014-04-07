package org.biomart.queryEngine;


/**
 *
 * @author Syed Haider, Jonathan Guberman
 *
 * Operator types used as filter qualifiers. Any new qualifier should be declared
 * here. People dont like me because i declared these as single characters believing
 * that it improves query performance in string matching.
 */
public enum OperatorType {
    /**
     *
     */
    E   ("="),
    LTE("<="),
    GTE(">="),
    LT("<"),
    GT(">"),
    LIKE    ("LIKE"),
    IS("IS"),
    RANGE("RANGE");
	
	private String value = null;

	private OperatorType(String value) {
		this.value = value;
	}
    /**
     *
     * @return
     */
    public String getValue() {
		return this.value;
	}
    /**
     *
     * @param value
     * @return
     */
    public static OperatorType valueFrom(String value) {
		for (OperatorType operatorType : values()) {
			if (operatorType.value.equals(value)) {
				return operatorType;
			}
		}
		//default =
		return OperatorType.E;
	}
    /**
     *
     * @return
     */
    @Override
	public String toString() {
		return getValue();
	}
}
