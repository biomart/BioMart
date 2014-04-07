package org.biomart.configurator.utils.type;


public enum Cardinality {

	/**
	 * Use this constant to refer to a relation with many values at the
	 * first key end.
	 */
	MANY_A ("1:M"),

	/**
	 * Use this constant to refer to a 1:1 relation.
	 */
	ONE ("1:1");



	private final String name;


	Cardinality(final String name) {
		this.name = name;
	}




	/**
	 * {@inheritDoc}
	 * <p>
	 * Always returns the name of this cardinality.
	 */
	public String toString() {
		return this.name;
	}
	
	public static Cardinality valueFrom(String value) {
		for(Cardinality c: Cardinality.values()) {
			if(c.name.equals(value))
				return c;
		}
		return null;
	}
}
