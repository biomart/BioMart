package org.biomart.configurator.utils.treelist;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.configurator.wizard.addsource.MartMetaModel;


public class LeafCheckBoxList extends JList {

    /** for serialization */
    private static final long serialVersionUID = -4359573373359270258L;

    private MartMetaModel metaModel;
    /**
     * initializes the list with an empty CheckBoxListModel
     */

    public LeafCheckBoxList() {
        super ();
        CheckBoxListModel  model = new CheckBoxListModel();
        setModel(model);
        setCellRenderer(new CheckBoxListRenderer());
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new MouseAdapter() {
        	@Override
            public void mousePressed(MouseEvent e) {
            	if(e.isPopupTrigger()) {
            		handleMouseClick(e);
            	} else {
	                int index = locationToIndex(e.getPoint());
	                if (index != -1) {
	                    setChecked(index, !getChecked(index));
	                    repaint();
	                }
            	}
            }
        	
        	@Override
        	public void mouseReleased(MouseEvent e) {
        		if(e.isPopupTrigger())
        			handleMouseClick(e);
        	}
        });

        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if ((e.getKeyChar() == ' ') && (e.getModifiers() == 0)) {
                    int index = getSelectedIndex();
                    setChecked(index, !getChecked(index));
                    e.consume();
                    repaint();
                }
            }
        });

    }

    private void handleMouseClick(MouseEvent e) {
		JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"checkboxlist", false);
		menu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * returns the checked state of the element at the given index
     * 
     * @param index	the index of the element to return the checked state for
     * @return		the checked state of the specifed element
     */
    public boolean getChecked(int index) {
        return ((CheckBoxListModel) getModel()).getChecked(index);
    }

    /**
     * sets the checked state of the element at the given index
     * 
     * @param index	the index of the element to set the checked state for
     * @param checked	the new checked state
     */
    public void setChecked(int index, boolean checked) {
        ((CheckBoxListModel) getModel()).setChecked(index, checked);
    }

    /**
     * returns an array with the indices of all checked items
     * 
     * @return		the indices of all items that are currently checked
     */
    public int[] getCheckedIndices() {
        List<Integer> list;
        int[] result;
        int i;

        // traverse over model
        list = new ArrayList<Integer>();
        for (i = 0; i < getModel().getSize(); i++) {
            if (getChecked(i))
                list.add(new Integer(i));
        }

        // generate result array
        result = new int[list.size()];
        for (i = 0; i < list.size(); i++) {
            result[i] = ((Integer) list.get(i)).intValue();
        }

        return result;
    }
    

    public List<LeafCheckBoxNode> getCheckedValues() {
    	List<LeafCheckBoxNode> checkedList = new ArrayList<LeafCheckBoxNode>();
        for (int i = 0; i < getModel().getSize(); i++) {
            if (getChecked(i))
                checkedList.add((LeafCheckBoxNode)getModel().getElementAt(i));
        }
        return checkedList;
    }
    
    public void setItems(Collection<LeafCheckBoxNode> items) {
    	CheckBoxListModel model = (CheckBoxListModel)this.getModel();
    	model.removeAllElements();
    	
    	for(LeafCheckBoxNode item:items) {  
    		model.addElement(item);
    	}
    }
    
    public List<DatasetFromUrl> getItemsForUrl(boolean all) {
    	CheckBoxListModel model = (CheckBoxListModel)this.getModel();
    	List<DatasetFromUrl> result = new ArrayList<DatasetFromUrl>();
    	for(int i=0; i<model.getSize(); i++) {
    		LeafCheckBoxNode node = (LeafCheckBoxNode)model.get(i);
    		if(all)
    			result.add((DatasetFromUrl)node.getUserObject());
    		else if(node.isSelected())
    			result.add((DatasetFromUrl)node.getUserObject());
    	}
    	return result;    	
    }
    
    /**
     * if all=false, only add those selected
     * @param all
     * @return
     */
    public List<String> getItems(boolean all) {
    	DefaultListModel model = (DefaultListModel)this.getModel();
    	List<String> result = new ArrayList<String>();
    	for(int i=0; i<model.getSize(); i++) {
    		LeafCheckBoxNode node = (LeafCheckBoxNode)model.get(i);
    		if(all)
    			result.add(node.getText());
    		else if(node.isSelected())
    			result.add(node.getText());
    	}
    	return result;
    }


    
    public void checkAll(boolean checked) {
    	DefaultListModel model = (DefaultListModel)this.getModel();
    	for(int i=0; i<model.getSize(); i++) {
    		setChecked(i,checked);
    	}
    	repaint();
    }

	public void setMetaModel(MartMetaModel metaModel) {
		this.metaModel = metaModel;
	}

	public MartMetaModel getMetaModel() {
		return metaModel;
	}

	public boolean isEnabled(int index) {
		return ((CheckBoxListModel) getModel()).isEnabled(index);
	}
}
