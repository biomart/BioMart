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

package org.biomart.common.view.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.KeyboardFocusManager;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.biomart.common.view.gui.dialogs.StackTrace;

/**
 * This simple class wraps a thread, and displays an hourglass for as long as
 * that thread is running. It is synchronised so that if multiple threads are
 * running, only the first one will start the hourglass, and only the last one
 * to end will stop it.
 * <p>
 * The hourglass portion of the thread is run using
 * {@link SwingUtilities#invokeLater(java.lang.Runnable)}, so that it is
 * thread-safe for Swing and can safely work with the GUI. Any parts of the
 * process passed in that work with the GUI must also use this construct to
 * avoid concurrent modification problems.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.12 $, $Date: 2007/10/25 15:17:17 $, modified by
 *          $Author: rh4 $
 * @since 0.5
 */
public abstract class LongProcess {

	private static Component mainWindow;

	private static final Object lockObject = "My Hourglass Lock";

	private static int longProcessCount = 0;

	private static final Cursor HOURGLASS_CURSOR = new Cursor(
			Cursor.WAIT_CURSOR);

	private static final Cursor NORMAL_CURSOR = new Cursor(
			Cursor.DEFAULT_CURSOR);

	/**
	 * Tell the hourglass which window is the main window.
	 * 
	 * @param mainWindow
	 *            the main window.
	 */
	public static void setMainWindow(final Component mainWindow) {
		LongProcess.mainWindow = mainWindow;
	}

	/**
	 * Runs the current background task as defined by {@link #run()}. Whilst
	 * the task is running, the hourglass is shown over the currently active
	 * window.
	 */
	public void start() {

		new SwingWorker<Void,Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				// Which window needs the hourglass?
				final Component window = KeyboardFocusManager
						.getCurrentKeyboardFocusManager().getFocusedWindow();
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							synchronized (LongProcess.lockObject) {
								// Open the hourglass.
								if (LongProcess.longProcessCount++ == 0) {
									if (window != null)
										window
												.setCursor(LongProcess.HOURGLASS_CURSOR);
									if (!LongProcess.mainWindow.equals(window))
										LongProcess.mainWindow
												.setCursor(LongProcess.HOURGLASS_CURSOR);
								}
							}
						}
					});
					try {
						// Let the process run.
						LongProcess.this.run();
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								StackTrace.showStackTrace(t);
							}
						});
					}
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				} finally {
					// Decrease the number of processes currently running.
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								synchronized (LongProcess.lockObject) {
									// If that was the last one, stop the
									// hourglass.
									if (--LongProcess.longProcessCount <= 0) {
										if (window != null)
											window
													.setCursor(LongProcess.NORMAL_CURSOR);
										if (!LongProcess.mainWindow
												.equals(window))
											LongProcess.mainWindow
													.setCursor(LongProcess.NORMAL_CURSOR);
									}
								}
							}
						});
					} catch (final Throwable t) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								StackTrace.showStackTrace(t);
							}
						});
					}
				}
				// Nothing to return.
				return null;
			}
		}.execute();
	}

	/**
	 * Override this method to provide the long-process background behaviour.
	 * 
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	public abstract void run() throws Exception;
}
