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
 * This is a basic {@link Exception} for all listener-related exceptions during 
 * mart construction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.3 $, $Date: 2007/10/03 10:41:02 $, modified by 
 * 			$Author: rh4 $
 * @since 0.7
 */
public class ListenerException extends ConstructorException {
	private static final long serialVersionUID = 1;

	/**
	 * Creates a new instance of <tt>ListenerException</tt> without detail
	 * message.
	 */
	public ListenerException() {
		super();
	}


	/**
	 * Constructs an instance of <tt>ListenerException</tt> with the specified
	 * detail message and cause.
	 * 
	 * @param msg
	 *            the detail message.
	 * @param t
	 *            the underlying cause.
	 */
	public ListenerException(final String msg, final Throwable t) {
		super(msg, t);
	}

	/**
	 * Constructs an instance of <tt>ListenerException</tt> with the specified
	 * cause.
	 * 
	 * @param t
	 *            the underlying cause.
	 */
	public ListenerException(final Throwable t) {
		super(t);
	}
}
