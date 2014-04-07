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
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.LongProcess;

/**
 * Saves any given component to an image file.
 * 
 * @author Richard Holland <holland@ebi.ac.uk>
 * @version $Revision: 1.7 $, $Date: 2007/10/03 10:41:02 $, modified by 
 * 			$Author: rh4 $
 * @since 0.5
 */
public class ComponentImageSaver {

	private Component component;

	/**
	 * Constructs a component saver that is associated with the given mart tab.
	 * 
	 * @param component
	 *            the component to save.
	 */
	public ComponentImageSaver(final Component component) {
		this.component = component;
	}

	private void save(final File file, final ImageSaverFilter format)
			throws IOException {
		// Create an image the same size as the component.
		final BufferedImage image = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration().createCompatibleImage(
						this.component.getWidth(), this.component.getHeight());
		// Render the component onto the image.
		Log.debug("Rendering graphics for saved image");
		final Graphics2D g2d = image.createGraphics();
		this.component.paint(g2d);
		// Save the image in the given format to the given filename.
		Log.debug("Writing saved image file");
		ImageIO.write(image, format.getFormat(), file);
	}

	/**
	 * Pops up a save-as dialog, and if the user completes it correctly, saves
	 * the component as an image file.
	 */
	public void save() {
		// Popup a save as dialog to request filename and image format.
		final JFileChooser fileChooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			public File getSelectedFile() {
				File file = super.getSelectedFile();
				if (file != null && !file.exists()) {
					String filename = file.getName();
					ImageSaverFilter filter = (ImageSaverFilter) this
							.getFileFilter();
					String extension = filter.getExtensions()[0];
					if (!filename.endsWith(extension)
							&& filename.indexOf('.') < 0)
						file = new File(file.getParentFile(), filename
								+ extension);
				}
				return file;
			}
		};
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.addChoosableFileFilter(new ImageSaverFilter("png",
				Resources.get("PNGFileFilterDescription"),
				new String[] { Resources.get("pngExtension") }));
		fileChooser.addChoosableFileFilter(new ImageSaverFilter("jpeg",
				Resources.get("JPEGFileFilterDescription"), new String[] {
						Resources.get("jpgExtension"),
						Resources.get("jpegExtension") }));
		if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
			// Call save() with the filename and format.
			new LongProcess() {
				public void run() throws Exception {
					ComponentImageSaver.this.save(
							fileChooser.getSelectedFile(),
							(ImageSaverFilter) fileChooser.getFileFilter());
				}
			}.start();
	}

	/**
	 * This class represents a filter for a particular image format.
	 */
	private class ImageSaverFilter extends FileFilter {

		private String description;

		private String[] extensions;

		private String format;

		/**
		 * The constructor accepts a format name, a description, and a list of
		 * matching extensions.
		 * 
		 * @param format
		 *            the format name.
		 * @param description
		 *            the description to put in the file chooser.
		 * @param extensions
		 *            the list of matching extensions.
		 */
		public ImageSaverFilter(final String format, final String description,
				final String[] extensions) {
			this.format = format;
			this.description = description;
			this.extensions = extensions;
		}

		public boolean accept(final File f) {
			if (f.isDirectory())
				return true;
			else {
				final String name = f.getName().toLowerCase();
				for (int i = 0; i < this.extensions.length; i++)
					if (name.endsWith(this.extensions[i]))
						return true;
			}
			return false;
		}

		public String getDescription() {
			return this.description;
		}

		/**
		 * Find out what extensions this filter accepts. The first extension in
		 * the list is the default one if you want to modify a file to be
		 * accepted by this filter.
		 * 
		 * @return the extensions this filter accepts.
		 */
		public String[] getExtensions() {
			return this.extensions;
		}

		/**
		 * Find out what image format this filter represents.
		 * 
		 * @return the format this filter represents.
		 */
		public String getFormat() {
			return this.format;
		}
	}
}
