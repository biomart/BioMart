package org.biomart.queryEngine;

/**
 *
 * @author Anthony Cros, Syed Haider
 *
 * Various object types that are used by a generic QueryElement object.
 * This is over engineered, should be deprecated at some point.
 */
public enum QueryElementType {
    ATTRIBUTE,
    FILTER,
    EXPORTABLE_ATTRIBUTE,	// if MS and reversing order, will need to get counterpart Filter
    IMPORTABLE_FILTER;		// if SQL and reversing order, will need to get counterpart Attribute
}
