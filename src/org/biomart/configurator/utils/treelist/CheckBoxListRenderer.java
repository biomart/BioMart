package org.biomart.configurator.utils.treelist;

import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * A specialized CellRenderer for the CheckBoxList
 * 
 */
public class CheckBoxListRenderer extends JCheckBox implements 
        ListCellRenderer {

    /** for serialization */
    private static final long serialVersionUID = 1059591605858524586L;

    /**
     * Return a component that has been configured to display the specified 
     * value.
     * 
     * @param list	The JList we're painting.
     * @param value	The value returned by list.getModel().getElementAt(index).
     * @param index	The cells index.
     * @param isSelected	True if the specified cell was selected.
     * @param cellHasFocus	True if the specified cell has the focus.
     * @return 		A component whose paint() method will render the 
     * 			specified value.
     */
    public Component getListCellRendererComponent(JList list,
            Object value, int index, boolean isSelected,
            boolean cellHasFocus) {

        setText(value.toString());
        setSelected(((LeafCheckBoxList) list).getChecked(index));
        this.setEnabled(((LeafCheckBoxList) list).isEnabled(index));
        setBackground(isSelected ? list.getSelectionBackground()
                : list.getBackground());
        setForeground(isSelected ? list.getSelectionForeground()
                : list.getForeground());
        setFocusPainted(false);

        return this ;
    }
}
