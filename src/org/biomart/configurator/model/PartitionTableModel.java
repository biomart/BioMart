package org.biomart.configurator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Element;

public class PartitionTableModel {

	private String name;	
	//Map<row,HashMap<col,value>>
	private Map<Integer, HashMap<Integer,String>> pt; 

	
	public PartitionTableModel(Element ptNode) {
		pt = new HashMap<Integer,HashMap<Integer,String>>();
		setPartitionTable(ptNode);
	}
	
	public String getCell(int row, int col) {
		return (pt.get(row)).get(col);
	}
	
	public int getRowSize() {
		return pt.values().size();
	}
	
	public int getColSize() {
		if(pt.get(0)==null) 
			return 0;
		else
			return pt.get(0).values().size();
	}
	
	public String getPartitionTableName() {
		return this.name;
	}
	
	private void setPartitionTable(Element ptNode) {
		this.name = ptNode.getAttributeValue("name");
		List<Element> cellList = ptNode.getChildren("cell");
		String row, col, cellName;
		Integer iRow=-1, iCol=-1;
		for(Element cell:cellList) {
			row = cell.getAttributeValue("row");
			col = cell.getAttributeValue("col");
			cellName = cell.getValue();
			try {
				iRow = Integer.parseInt(row);
				iCol = Integer.parseInt(col);
			}
			catch(Exception e) {
				e.printStackTrace();
				//FIXME: handle the exception
			}
			if (pt.get(iRow)!=null) {//has row already
				pt.get(iRow).put(iCol, cellName);
			} else {
				HashMap<Integer, String> colMap = new HashMap<Integer,String>();
				colMap.put(iCol, cellName);
				pt.put(iRow, colMap);
			}
		}
	}

	public List<String> getColumnOne() {
		List<String> columnOneValue = new ArrayList<String>();
		for(HashMap<Integer,String> hm:this.pt.values()){
			columnOneValue.addAll(hm.values());
		}
		return columnOneValue;
	}
	
	public Object[] getRowsList() {
		return this.pt.keySet().toArray();
	}
}