package org.biomart.api.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author jhsu
 */
@XmlRootElement(name="estimate")
public class CountEstimate {
	public int entries;
}
