/*
 
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

package org.biomart.runner.exceptions;

/**
 * This refers to exceptions where something has failed during a job test.
 * 
 */
public class TestException extends MartRunnerException {
	private static final long serialVersionUID = 1;

	/**
	 * Constructs an instance of <tt>TestException</tt> with the specified
	 * detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public TestException(final String msg) {
		super(msg);
	}

	/**
	 * Constructs an instance of <tt>TestException</tt> with the specified
	 * detail message and initial cause.
	 * 
	 * @param msg
	 *            the detail message.
	 * @param t
	 *            the initial cause.
	 */
	public TestException(final String msg, final Throwable t) {
		super(msg, t);
	}

	/**
	 * Constructs an instance of <tt>TestException</tt> with the specified
	 * initial cause.
	 * 
	 * @param t
	 *            the initial cause.
	 */
	public TestException(final Throwable t) {
		super(t);
	}
}
