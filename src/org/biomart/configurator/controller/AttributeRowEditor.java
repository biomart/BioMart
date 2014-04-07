package org.biomart.configurator.controller;

import javax.swing.table.*;
import java.util.*;

public class AttributeRowEditor{
	private Map<Integer, TableCellEditor> editorMap;
	
    public AttributeRowEditor(){
    	editorMap = new HashMap<Integer,TableCellEditor>();
    }
    
    public void addEditorForRow(int row, TableCellEditor e ){
        editorMap.put(new Integer(row), e);
    }
    
    public void removeEditorForRow(int row){
        editorMap.remove(new Integer(row));
    }
    

    public TableCellEditor getEditor(int row) {
        return (TableCellEditor)editorMap.get(new Integer(row));
    }
 
    public void clear() {
    	this.editorMap.clear();
    }
}
