package org.biomart.common.resources;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ErrorMessage {
	
	private final static ResourceBundle bundle = ResourceBundle
		.getBundle("org/biomart/common/resources/errors");

	public static String get(final String key) {
		String value = null;	
		try {
			value = ErrorMessage.bundle.getString(key);
		} catch (final MissingResourceException e) {
			value = null;
		}
		return value;
	}
	
	// Private means that this class is a static singleton.
	private ErrorMessage() {}
}