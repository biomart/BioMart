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

package org.biomart.runner.view.cli;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;

/**
 * This abstract class provides some useful common stuff for launching any
 * BioMart Java CLI appliaction.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.4 $, $Date: 2007/07/11 13:12:30 $, modified by $Author:
 *          rh4 $
 * @since 0.6
 */
public abstract class BioMartCLI {
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance of MartBuilder. You can customise the
	 * look-and-feel by speciying a configuration property called
	 * <tt>lookandfeel</tt>, which contains the classname of the
	 * look-and-feel to use. Details of where this file is can be found in
	 * {@link Settings}.
	 */
	protected BioMartCLI() {
		// Load our cache of settings.
		Settings.load();
	}

	/**
	 * Starts the application.
	 */
	protected void launch() {
		// Start the application.
		Log.info("Launching command line application");
		System.out.println("..." + Settings.getApplication() + " started.");
		// Start work.
		boolean moreInput = true;
		while (moreInput)
			try {
				moreInput = this.poll();
			} catch (final Throwable t) {
				System.err.println(Resources.get("cliExceptionHeader"));
				t.printStackTrace(System.err);
				System.err.println(Resources.get("cliExceptionFooter"));
				moreInput = true;
			}
		// All done.
		this.requestExitApp();
	}

	/**
	 * Do whatever has to be done in this program. If an exception needs to be
	 * reported, throw it. Control will be returned to this method until it
	 * returns a value other than <tt>true</tt>. This method should only
	 * handle one action at a time.
	 * 
	 * @return <tt>true</tt> if it expects to be called again after returning.
	 *         <tt>false</tt> if it does not and the program can exit now.
	 * @throws Throwable
	 *             with any exception that needs reporting to the user and
	 *             cannot be handled within the method itself. If an exception
	 *             is thrown, it is treated as though the method returned
	 *             <tt>true</tt> and so the method will be called again once
	 *             the exception has been handled.
	 */
	protected abstract boolean poll() throws Throwable;

	/**
	 * Requests the application to exit, allowing it to ask for permission from
	 * the user first if necessary.
	 */
	public void requestExitApp() {
		Log.info("Normal exit requested");
		if (this.confirmExitApp()) {
			Log.info("Normal exit granted");
			System.exit(0);
		} else
			Log.info("Normal exit denied");
	}

	/**
	 * Override this method if you wish to ask the user for confirmation.
	 * 
	 * @return <tt>true</tt> if it is OK to exit, and <tt>false</tt> if the
	 *         user asks not to.
	 */
	public boolean confirmExitApp() {
		return true;
	}
}
