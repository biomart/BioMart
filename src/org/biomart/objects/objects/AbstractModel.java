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
package org.biomart.objects.objects;

import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.biomart.configurator.utils.type.McNodeType;

public abstract class AbstractModel {

	private McNodeType type;
	
	//should remove later
/*	private final List<WeakReference<PropertyChangeListener>> globalListeners = 
		new ArrayList<WeakReference<PropertyChangeListener>>();

	private final Map<String, List<WeakReference<PropertyChangeListener>>> namedListeners = 
		new HashMap<String, List<WeakReference<PropertyChangeListener>>>();


	/**
	 * Add a global listener, which will be stored with a {@link WeakReference}.
	 * 
	 * @param listener
	 *            the listener.
	 *
	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		this.globalListeners.add(new WeakReference<PropertyChangeListener>(listener));
	}

	/**
	 * Add a property-specific listener, which will be stored with a
	 * {@link WeakReference}.
	 * 
	 * @param property
	 *            the property.
	 * @param listener
	 *            the listener.
	 *
	public void addPropertyChangeListener(final String property,
			final PropertyChangeListener listener) {
		if (!this.namedListeners.containsKey(property))
			this.namedListeners.put(property, new ArrayList<WeakReference<PropertyChangeListener>>());
		this.namedListeners.get(property).add(new WeakReference<PropertyChangeListener>(
				listener));
	}
*/

	public void setNodeType(McNodeType type) {
		this.type = type;
	}
	
	public McNodeType getNodeType() {
		return this.type;
	}

}
