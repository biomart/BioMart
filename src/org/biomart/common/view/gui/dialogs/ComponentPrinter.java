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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

import org.biomart.common.resources.Log;
import org.biomart.common.view.gui.LongProcess;

/**
 * Prints any given component.
 * <p>
 * Based on code from <a
 * href="http://www.apl.jhu.edu/~hall/java/Swing-Tutorial/Swing-Tutorial-Printing.html">this
 * Swing Tutorial</a>.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.7 $, $Date: 2007/10/03 10:41:02 $, modified by 
 * 			$Author: rh4 $
 * @since 0.5
 */
public class ComponentPrinter implements Printable {

	private Component component;

	/**
	 * Constructs a component printer that is associated with the given mart
	 * tab.
	 * 
	 * @param component
	 *            the component to print.
	 */
	public ComponentPrinter(final Component component) {
		this.component = component;
	}

	/**
	 * Pops up a printing dialog, and if the user completes it correctly, prints
	 * the component.
	 */
	public void print() {
		final PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(this);
		if (printJob.printDialog())
			new LongProcess() {
				public void run() throws Exception {
					printJob.print();
				}
			}.start();
	}

	public int print(final Graphics g, final PageFormat pageFormat,
			final int pageIndex) {
		Log.debug("Printing page " + pageIndex);
		// Simple scale to reduce component size.
		final double scale = 0.5;
		// Work out pages required for the component we are drawing.
		final int pagesAcross = (int) Math.ceil(this.component.getWidth()
				* scale / pageFormat.getImageableWidth());
		final int pagesDown = (int) Math.ceil(this.component.getHeight()
				* scale / pageFormat.getImageableHeight());
		final int numPages = pagesAcross * pagesDown;
		// If we are beyond the last page, we are done.
		if (pageIndex >= numPages) {
			Log.debug("No such page - last page already printed.");
			return Printable.NO_SUCH_PAGE;
		}

		// Print the components.
		final Graphics2D g2d = (Graphics2D) g;
		// Translate our output to the printable area.
		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		// What page are we being asked to print?
		int pageXNum = pageIndex % pagesAcross;
		int pageYNum = pageIndex / pagesAcross;
		// Translate our output to focus on the required page.
		g2d.translate(pageFormat.getImageableWidth() * -pageXNum, pageFormat
				.getImageableHeight()
				* -pageYNum);
		g2d.setClip((int) pageFormat.getImageableWidth() * pageXNum,
				(int) pageFormat.getImageableHeight() * pageYNum,
				(int) pageFormat.getImageableWidth(), (int) pageFormat
						.getImageableHeight());
		// Scale our output down a bit as otherwise the objects are
		// huge on paper.
		g2d.scale(scale, scale);
		// Do the printing.
		this.component.print(g2d);
		Log.debug("Page printed");
		return Printable.PAGE_EXISTS;
	}
}
