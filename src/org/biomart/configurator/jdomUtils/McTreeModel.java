package org.biomart.configurator.jdomUtils;

import java.util.Collection;
import java.util.Collections;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.biomart.common.resources.Settings;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;

public class McTreeModel extends DefaultTreeModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private McViewsFilter filter;

	public McTreeModel(McTreeNode root) {
		super(root);
	}

	public void setFilters(McViewsFilter filter) {
		this.filter = filter;
	}

	@Override
	public void insertNodeInto(MutableTreeNode newChild,
			MutableTreeNode parent, int index) {
		super.insertNodeInto(newChild, parent, index);
		// insert into the object model
		MartConfiguratorObject childObj = ((McTreeNode) newChild).getObject();
		if (childObj.getNodeType().equals(McNodeType.MART)) {

		} else if (childObj.getNodeType().equals(McNodeType.ATTRIBUTE)
				|| childObj.getNodeType().equals(McNodeType.FILTER)) {
			Container parentObj = (Container) ((McTreeNode) parent).getObject();
			if (childObj.getNodeType().equals(McNodeType.ATTRIBUTE))
				parentObj.addAttribute((Attribute) childObj);
			else
				parentObj.addFilter((Filter) childObj);
		}
	}

	private void insertNodeInto(MutableTreeNode newChild,
			MutableTreeNode parent, int index, boolean up) {
		super.insertNodeInto(newChild, parent, index);
		MartConfiguratorObject childObj = ((McTreeNode) newChild).getObject();
		if (childObj instanceof MartPointer) {
			// if(childObj.getNodeType().equals(McNodeType.MARTPOINTER)) {
			// move the order in the registry.martList
			GuiContainer parentObj = (GuiContainer) ((McTreeNode) parent)
					.getObject();
			int currentIndex = parentObj.getMartPointerList().indexOf(childObj);
			if (up)
				Collections.swap(parentObj.getMartPointerList(), currentIndex,
						currentIndex - 1);
			else
				Collections.swap(parentObj.getMartPointerList(), currentIndex,
						currentIndex + 1);
		} else if (childObj instanceof Container) {
			Container parentObj = (Container) ((McTreeNode) parent).getObject();
			int currentIndex = parentObj.getContainerList().indexOf(childObj);
			if (up)
				Collections.swap(parentObj.getContainerList(), currentIndex,
						currentIndex - 1);
			else
				Collections.swap(parentObj.getContainerList(), currentIndex,
						currentIndex + 1);
		} else if (childObj instanceof Attribute) {
			Container parentObj = (Container) ((McTreeNode) parent).getObject();
			int currentIndex = parentObj.getAttributeList().indexOf(childObj);
			if (up)
				Collections.swap(parentObj.getAttributeList(), currentIndex,
						currentIndex - 1);
			else
				Collections.swap(parentObj.getAttributeList(), currentIndex,
						currentIndex + 1);
		} else if (childObj instanceof Filter) {
			Container parentObj = (Container) ((McTreeNode) parent).getObject();
			int currentIndex = parentObj.getFilterList().indexOf(childObj);
			if (up)
				Collections.swap(parentObj.getFilterList(), currentIndex,
						currentIndex - 1);
			else
				Collections.swap(parentObj.getFilterList(), currentIndex,
						currentIndex + 1);
		}
	}

	@Override
	public int getChildCount(Object parent) {
		McTreeNode node = (McTreeNode) parent;
		int realCount = node.getChildCount();
		int count = 0;
		for (int i = 0; i < realCount; i++) {
			Object child = node.getChildAt(i);
			if (child instanceof McTreeNode) {
				if (((McTreeNode) child).getObject().isHidden()
						&& "1".equals(Settings
								.getProperty("hidemaskedcomponent")))
					continue;
				if (filter == null)
					count++;
				else if (!this.filter.isFiltered((McTreeNode) child))
					count++;
			}
		}
		return count;
	}

	@Override
	public Object getChild(Object parent, int index) {
		McTreeNode node = (McTreeNode) parent;
		int originalCount = node.getChildCount();
		int current = -1;
		for (int i = 0; i < originalCount; i++) {
			Object child = node.getChildAt(i);
			if (child instanceof McTreeNode) {
				if (((McTreeNode) child).getObject().isHidden()
						&& "1".equals(Settings
								.getProperty("hidemaskedcomponent")))
					continue;
				if (filter == null)
					current++;
				else if (!this.filter.isFiltered((McTreeNode) child))
					current++;
				if (current == index)
					return child;
			}
		}

		return null;
	}

	public boolean moveNode(McTreeNode node, int step) {
		McTreeNode parent = (McTreeNode) node.getParent();
		int count = parent.getChildCount();
		int currentIndex = parent.getIndex(node);
		int newIndex = currentIndex + step;
		if (newIndex > count - 1 || newIndex < 0)
			return false;
		// hack for attribute and filter, attributes should always in front of
		// filters
		if (node.getObject() instanceof Attribute) {
			int attCount = ((Container) parent.getObject()).getAttributeList()
					.size();
			if (newIndex > attCount - 1)
				return false;
		} else if (node.getObject() instanceof Filter) {
			int attCount = ((Container) parent.getObject()).getAttributeList()
					.size();
			if (newIndex < attCount)
				return false;
		}
		this.removeNodeFromParent(node);
		boolean up = (step > 0) ? false : true;
		this.insertNodeInto(node, parent, newIndex, up);
		return true;
	}

	public void addFilter(McNodeType type, Collection<String> conditions) {
		this.filter.addFilter(type, conditions);
	}
	public void addFilter(String type){
		this.filter.addFilter(type);
	}
	
	public void removeFilter(McNodeType type) {
		this.filter.removeFilter(type);
	}
	public void removeFilter(String type){
		this.filter.removeFilter(type);
	}
}