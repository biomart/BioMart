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

package org.biomart.common.view.gui.dialogs;

import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;

/**
 * Displays pretty GUI stacktraces on demand.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.5 $, $Date: 2007/10/03 10:41:02 $, modified by 
 * 			$Author: rh4 $
 * @since 0.5
 */
public class StackTrace {
	/**
	 * Display a nice friendly stack trace window.
	 * 
	 * @param t
	 *            the throwable to display the stack trace for.
	 */
	public static void showStackTrace(final Throwable t) {
		// Log the error.
		if (t instanceof Error)
			Log.error(Resources.get("stackTraceTitle"), t);
		else
			Log.warn(Resources.get("stackTraceTitle"), t);

		// Create the main message.
		final int messageClass = t instanceof Error ? JOptionPane.ERROR_MESSAGE
				: JOptionPane.WARNING_MESSAGE;
		String mainMessage = t.getLocalizedMessage();
		if (mainMessage == null)
			mainMessage = "";

		// Missing message?
		if (mainMessage.length() == 0)
			mainMessage = Resources.get("missingException");

		// Too-long message?
		else if (mainMessage.length() > 100)
			mainMessage = mainMessage.substring(0, 100)
					+ Resources.get("truncatedException");

		// Prepend exception classname.
		final String throwableClass = t.getClass().getName();
		final int lastDot = throwableClass.lastIndexOf('.');
		mainMessage = throwableClass.substring(lastDot + 1) + ":\n"
				+ mainMessage;

		// Ask if they want to see the full stack trace (show the first line of
		// the stack trace as a hint).
		final int choice = JOptionPane.showConfirmDialog(null, new Object[] {
				mainMessage, Resources.get("stackTracePrompt") }, Resources
				.get("stackTraceTitle"), JOptionPane.YES_NO_OPTION);

		// Create and show the full stack trace dialog if they said yes.
		if (choice == JOptionPane.YES_OPTION) {
			// Extract the full stack trace.
			final StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			final String stackTraceText = sw.toString();

			// Build the text pane.
			final JEditorPane editorPane = new JEditorPane("text/plain",
					stackTraceText);

			// Put the editor pane in a scroll pane.
			final JScrollPane editorScrollPane = new JScrollPane(editorPane);
			editorScrollPane
					.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

			// Arbitrarily resize the scrollpane.
			editorScrollPane.setPreferredSize(new Dimension(600, 400));

			// Show the output.
			JOptionPane.showMessageDialog(null, editorScrollPane, Resources
					.get("stackTraceTitle"), messageClass);
		}
	}
}
