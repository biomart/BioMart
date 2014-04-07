package org.biomart.configurator.jdomUtils;

import java.util.LinkedHashMap;
import java.util.List;
import org.jdom.Element;
import org.biomart.configurator.model.object.PartitionColumn;
import org.biomart.configurator.utils.type.PartitionColumnType;
import org.biomart.objects.objects.PartitionTable;

public class JDomUtils {
	
	@SuppressWarnings("unchecked") //for jdom
	public static  LinkedHashMap<String,PartitionColumn> partitionTableElement2Table(PartitionTable table, Element partitionTable) {
		List<Element> rowList = partitionTable.getChildren();
		LinkedHashMap<String,PartitionColumn> data = new LinkedHashMap<String,PartitionColumn>();
		for(Element row: rowList) {
			String text = row.getText();
			String[] rowArray = text.split("\\|",-1);
			for(int i=0; i<rowArray.length; i++) {
				PartitionColumn pc = data.get(""+i);
				if(pc==null) {
					pc = new PartitionColumn(table,""+i,PartitionColumnType.CUSTOMIZED);
					data.put(""+i, pc);
				}
				pc.addNewRow(rowArray[i]);
			}	
		}
		return data;
	}





}