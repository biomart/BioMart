/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.common.exceptions;

/**
 * This is a basic {@link Exception} for all non-specific BioMart exceptions.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.5 $, $Date: 2007/10/03 10:41:01 $, modified by 
 * 			$Author: rh4 $
 * @since 0.5
 */
public class BioMartException extends RuntimeException {
	private static final long serialVersionUID = 1;

	/**
	 * Creates a new instance of <tt>BioMartException</tt> without detail
	 * message.
	 */
	public BioMartException() {
		super();
	}

	/**
	 * Constructs an instance of <tt>BioMartException</tt> with the specified
	 * detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public BioMartException(final String msg) {
		super(msg);
	}

	/**
	 * Constructs an instance of <tt>BioMartException</tt> with the specified
	 * detail message and cause.
	 * 
	 * @param msg
	 *            the detail message.
	 * @param t
	 *            the underlying cause.
	 */
	public BioMartException(final String msg, final Throwable t) {
		super(msg, t);
	}

	/**
	 * Constructs an instance of <tt>BioMartException</tt> with the specified
	 * cause.
	 * 
	 * @param t
	 *            the underlying cause.
	 */
	public BioMartException(final Throwable t) {
		super(t);
	}
}
