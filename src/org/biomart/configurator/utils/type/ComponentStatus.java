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

package org.biomart.configurator.utils.type;


/**
 * Represents the status of any component with regard to how the system came to
 * know about it.
 * 
 */
public enum ComponentStatus {
	MODIFIED ("MODIFIED"),
	HANDMADE ("HANDMADE"),
	INFERRED ("INFERRED"),
	INFERRED_INCORRECT ("INFERRED_INCORRECT");
	
	ComponentStatus(String name) {
		this.name = name;
	}

	private final String name;


	public String toString() {
		return this.name;
	}
}
