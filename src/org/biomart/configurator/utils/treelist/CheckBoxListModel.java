package org.biomart.configurator.utils.treelist;

import javax.swing.DefaultListModel;

public class CheckBoxListModel extends DefaultListModel {

    /** for serialization */
    private static final long serialVersionUID = 7772455499540273507L;

    /**
     * initializes the model with no data.
     */
    public CheckBoxListModel() {
        super ();
    }


    /**
     * Inserts the specified element at the specified position in this list.
     * 
     * @param index	index at which the specified element is to be inserted
     * @param element	element to be inserted
     */
    public void add(int index, Object element) {
        super.add(index, element);
    }

    /**
     * Adds the specified component to the end of this list.
     * 
     * @param obj 	the component to be added
     */
    public void addElement(Object obj) {
         super.addElement(obj);
    }





    /**
     * returns the checked state of the element at the given index
     * 
     * @param index	the index of the element to return the checked state for
     * @return		the checked state of the specifed element
     */
    public boolean getChecked(int index) {
        return ((LeafCheckBoxNode) super .getElementAt(index)).isSelected() &&
        	((LeafCheckBoxNode) super .getElementAt(index)).isEnabled();
    }

    /**
     * sets the checked state of the element at the given index
     * 
     * @param index	the index of the element to set the checked state for
     * @param checked	the new checked state
     */
    public void setChecked(int index, boolean checked) {
        ((LeafCheckBoxNode) super .getElementAt(index))
                .setSelected(checked);
    }

    public boolean isEnabled(int index) {
    	return ((LeafCheckBoxNode) super .getElementAt(index)).isEnabled();
    }
}