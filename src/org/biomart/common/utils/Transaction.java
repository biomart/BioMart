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

package org.biomart.common.utils;


/**
 * This static class provides methods which signal the beginning and end of a
 * transaction. It has no data - it is merely a pair of events and an associated
 * event handler queue.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.18 $, $Date: 2008/03/03 12:16:15 $, modified by
 *          $Author: rh4 $
 * @since 0.7
 */
public class Transaction {





	private static Transaction currentTransaction;


	/**
	 * Checks to see if there is currently a transaction in progress.
	 * 
	 * @return the current transaction, or <tt>null</tt> if there isn't one.
	 */
	public static Transaction getCurrentTransaction() {
		return Transaction.currentTransaction;
	}


	private boolean allowVisModChange;

	/**
	 * Create a new, empty transaction.
	 * 
	 * @param allowVisModChange
	 *            Does this transaction modify visible modification flags?
	 */
	public Transaction(final boolean allowVisModChange) {
		this.allowVisModChange = allowVisModChange;
	}

	/**
	 * Does this transaction modify visible modification flags?
	 * 
	 * @return <tt>true</tt> if it does.
	 */
	public boolean isAllowVisModChange() {
		return this.allowVisModChange;
	}
}
