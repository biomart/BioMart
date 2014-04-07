

package org.biomart.configurator.view.gui.diagrams;

import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;


/**
 * The diagram context receives notification to populate context menus in
 * {@link Diagram}s, or to change the colours of objects displayed in the
 * diagram. All objects in the diagram are passed to both methods at some point,
 * so anything displayed can be customised.
 */
public interface DiagramContext {
	/**
	 * Customise the appearance of a component that represents the given
	 * database object.
	 * 
	 * @param component
	 *            the component that represents the object.
	 * @param object
	 *            the database object we wish to customise this component to.
	 */
	public void customiseAppearance(JComponent component, Object object);

	/**
	 * Add items to a context menu for a given database object. Should add a
	 * separator first if the menu is not empty.
	 * 
	 * @param contextMenu
	 *            the context menu to add parameters to.
	 * @param object
	 *            the database object we wish to customise this menu to.
	 */
	public void populateContextMenu(JPopupMenu contextMenu, JComponent component, Object object);

	/**
	 * Add items to a context menu for a collection of database objects. Should
	 * add a separator first if the menu is not empty.
	 * 
	 * @param contextMenu
	 *            the context menu to add parameters to.
	 * @param selectedItems
	 *            the database objects we wish to customise this menu to.
	 * @param clazz
	 *            the type of objects in the selection.
	 */
	public void populateMultiContextMenu(JPopupMenu contextMenu,
			Collection selectedItems, Class clazz);

	/**
	 * Tests to see if the specified object is masked. This is used in the
	 * show/hide masked objects switch.
	 * 
	 * @param object
	 *            the object to test.
	 * @return <tt>true</tt> if it is masked.
	 */
	public boolean isMasked(Object object);
}
