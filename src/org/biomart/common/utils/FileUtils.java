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

package org.biomart.common.utils;

import java.io.File;
import java.io.IOException;


public class FileUtils {

	/**
	 * Takes a directory (or file) and removes it, recursively emptying it if
	 * necessary.
	 * 
	 * @param file
	 *            the directory or file to remove.
	 * @throws IOException
	 *             if something goes wrong.
	 */
	public static void delete(final File file) throws IOException {
		if (file.isDirectory()) {
			final File[] files = file.listFiles();
			for (int i = 0; i < files.length; i++)
				FileUtils.delete(files[i]);
		}
		file.delete();
	}

	// Static class cannot be instantiated.
	private FileUtils() {
	}

}
