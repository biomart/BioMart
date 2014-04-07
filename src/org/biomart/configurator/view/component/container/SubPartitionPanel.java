package org.biomart.configurator.view.component.container;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class SubPartitionPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Mart mart;
	
	public SubPartitionPanel(Mart mart) {
		this.mart = mart;
		init();
	}
	
	private void init() {
		for(PartitionTable pt: mart.getPartitionTableList()) {
			if(pt.getPartitionType() == PartitionType.SCHEMA)
				continue;
			JLabel label = new JLabel(pt.getName());
			List<String> ptValue = this.getUniqueDatasetName(pt);
			JComboBox cb = new JComboBox();
			for(String s: ptValue) {
				cb.addItem(s);
			}
			this.add(label);
			this.add(cb);
		}
	}
	
	private List<String> getUniqueDatasetName(PartitionTable pt) {
		List<String> dss = new ArrayList<String>();
		for(int i=0; i<pt.getTotalRows(); i++) {
			String s = pt.getValue(i, 0);
			if(!dss.contains(s))
				dss.add(s);
		}
		return dss;
	}
	
}