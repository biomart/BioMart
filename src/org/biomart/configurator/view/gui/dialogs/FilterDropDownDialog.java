package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.McEventListener;
import org.biomart.configurator.controller.ListTransferHandler;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.Options;

public class FilterDropDownDialog extends JDialog implements ListSelectionListener, ActionListener, WindowListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Filter filter;
	private JList dsList;
	private JList optionList;
	private JButton updateButton;
	private JButton removeButton;
	private JButton upButton;
	private JButton downButton;
	private JButton sortButton;
	private JButton addButton;
	private Map<Dataset,List<FilterData>> optionDataMap;
	
	public FilterDropDownDialog(JDialog parent, Filter filter) {
		super(parent);
		this.filter = filter;
		this.optionDataMap = new HashMap<Dataset,List<FilterData>>();
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		
		updateButton = new JButton(Resources.get("UPDATE"));
		removeButton = new JButton(Resources.get("REMOVE"));
		addButton = new JButton(Resources.get("ADD"));
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		
		buttonPanel.add(cancelButton);
		cancelButton.setActionCommand(Resources.get("CANCEL"));
		cancelButton.addActionListener(this);
		
		buttonPanel.add(removeButton);
		removeButton.setActionCommand(Resources.get("REMOVE"));
		removeButton.addActionListener(this);
		
		buttonPanel.add(addButton);
		addButton.setActionCommand(Resources.get("ADD"));
		addButton.addActionListener(this);
		
		buttonPanel.add(updateButton);
		updateButton.setActionCommand(Resources.get("UPDATE"));
		updateButton.addActionListener(this);
		updateButton.setEnabled(false);
		
		JPanel inputPanel = new JPanel(new BorderLayout());
		JPanel dsInputPanel = new JPanel(new BorderLayout());
		JLabel datasetLabel = new JLabel("Dataset: ");
		dsInputPanel.add(datasetLabel,BorderLayout.NORTH);
		
		this.initDatasetList();
		JScrollPane dsScroller = new JScrollPane(dsList);
		dsScroller.setPreferredSize(new Dimension(250, 250));
		dsInputPanel.add(dsScroller,BorderLayout.SOUTH);
		
		this.initOptionList();
		JScrollPane optionScroller = new JScrollPane(optionList);
		optionScroller.setPreferredSize(new Dimension(250, 250));

		JPanel orderButtonPanel = new JPanel(new GridBagLayout());
		upButton = new JButton(Resources.get("UP"));
		upButton.setActionCommand(Resources.get("UP"));
		upButton.addActionListener(this);
		upButton.setEnabled(false);
		
		downButton = new JButton(Resources.get("DOWN"));
		downButton.setActionCommand(Resources.get("DOWN"));
		downButton.addActionListener(this);
		downButton.setEnabled(false);
		
		sortButton = new JButton("Sort");
		sortButton.setActionCommand("Sort");
		sortButton.addActionListener(this);
		sortButton.setEnabled(false);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		orderButtonPanel.add(upButton, c);
		c.gridy = 1;
		orderButtonPanel.add(downButton, c);
		c.gridy = 2;
		orderButtonPanel.add(sortButton, c);
		
		inputPanel.add(dsInputPanel,BorderLayout.WEST);
		inputPanel.add(optionScroller,BorderLayout.CENTER);
		inputPanel.add(orderButtonPanel,BorderLayout.EAST);
		
		content.add(buttonPanel,BorderLayout.SOUTH);
		content.add(inputPanel,BorderLayout.NORTH);
		this.add(content);
		this.setTitle("Filter DropDown Dialog");
		this.addWindowListener(this);
		//select the first item by default;
		if(dsList.getModel().getSize()>0) 
			dsList.setSelectedIndex(0);
		McEventBus.getInstance().addListener(McEventProperty.FILTEROPTION_CHANGED.toString(), this);
	}
	
	private void initDatasetList() {
		DefaultListModel dsModel = new DefaultListModel();
		for(Dataset ds: this.filter.getParentConfig().getMart().getDatasetList()) 
			dsModel.addElement(ds);
		this.dsList = new JList(dsModel);
		this.dsList.addListSelectionListener(this);
	}
	
	private void initOptionList() {
		//load all dataset options for this filter
		DefaultListModel model = new DefaultListModel();
		this.optionList = new JList(model);
		this.optionList.setDragEnabled(true);
		this.optionList.setDropMode(DropMode.INSERT);
		this.optionList.setTransferHandler(new ListTransferHandler());
		for(Dataset ds: this.filter.getParentConfig().getMart().getDatasetList()) {
			List<String> dsNameList = new ArrayList<String>();
			dsNameList.add(ds.getName());
			Filter mfilter = McUtils.findFilterInMaster(this.filter);
			if(null!=mfilter) {
				List<FilterData> optionList = mfilter.getFilterDataList(dsNameList);
				this.optionDataMap.put(ds, optionList);
			}
		}
		this.optionList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.optionList.addListSelectionListener(this);
		this.refreshDataList();
	}

	public void valueChanged(ListSelectionEvent evt) {
		boolean adjust = evt.getValueIsAdjusting();
		if(!adjust) {
			if(evt.getSource().equals(dsList)) {
				this.refreshDataList();
				this.updateButton.setEnabled(true);
				this.sortButton.setEnabled(this.dsList.getSelectedValues().length==1);				
			}
			//enable/disabable up/down button
			else if(evt.getSource().equals(optionList)) {
				boolean enabled = true;
				if(this.dsList.getSelectedValues().length!=1 || this.optionList.getSelectedValues().length<1)
					enabled = false;
				this.upButton.setEnabled(enabled);
				this.downButton.setEnabled(enabled);
				this.removeButton.setEnabled(enabled);
			}
		}
	}

	public void actionPerformed(ActionEvent evt) {
		if(evt.getActionCommand().equals(Resources.get("CANCEL"))) {
			this.setVisible(false);
			this.dispose();
		}else if(evt.getActionCommand().equals(Resources.get("UPDATE"))) {
			if(this.update()) {
				this.refreshDataList();
				this.save();
			}
		}else if(evt.getActionCommand().equals(Resources.get("REMOVE"))) {
			this.remove();
		}else if(evt.getActionCommand().equals(Resources.get("UP"))) {
			this.move(true);
		}else if(evt.getActionCommand().equals(Resources.get("DOWN"))) {
			this.move(false);
		}else if(evt.getActionCommand().equals("Sort")) {
			this.sort();
		}else if(evt.getActionCommand().equals(Resources.get("ADD"))) {
			this.add();
		}
	}
	
	private boolean remove() {
/*		Config config = this.filter.getParentConfig();
		Mart mart = config.getMart();
		Element options = McGuiUtils.INSTANCE.getOptionElement();
		//find mart
		Element martElement = McUtils.getFilterOptionMartElement(mart);

		if(martElement == null)
			return true;
		
		Element configElement = McUtils.getFilterOptionConfigElement(config, martElement);

		if(configElement == null)
			return true;
		//find filter
		
		List<Element> filterl = configElement.getChildren();
		Element filterElement = McUtils.getFilterOptionElement(config, filter);

		if(filterElement == null)
			return true;
		
		//get selected datasets and options	
		Object[] selectedDss = this.dsList.getSelectedValues();
		Object[] selectedOptions = this.optionList.getSelectedValues();
		for(Object ds: selectedDss) {
			Element dsElement = McUtils.getFilterOptionDatasetElement((Dataset)ds, filterElement);
			List<Element> optionElements = dsElement.getChildren();
			for(Object option: selectedOptions) {
				//remove the options from element
				for(Iterator<Element> it = optionElements.iterator(); it.hasNext();) {
					Element e = it.next();
					String data = e.getAttributeValue("data");
					String[] datas = data.split("\\|");
					if(datas.length>0 && datas[0].equals(((FilterData)option).getName())) {
						it.remove();
						break;
					}											
				}
			}
		}
		configElement.removeContent(filterElement);		*/
		Object[] selectedDss = this.dsList.getSelectedValues();
		Object[] selectedOptions = this.optionList.getSelectedValues();
		//remvoe from map
		for(Object obj: selectedDss) {
			List<FilterData> fdList = this.optionDataMap.get(obj);
			for(Object optionObj: selectedOptions) {
				fdList.remove(optionObj);
			}			
		}
		//remove from gui
		for(Object obj: selectedOptions) {
			((DefaultListModel)this.optionList.getModel()).removeElement(obj);
		}
		this.save();
		return true;
	}
	
	private boolean update() {
		Object[] selected = this.dsList.getSelectedValues();
		if(selected.length == 0)
			return false;
		//update each dataset
		for(Object sel: selected) {
			Dataset ds = (Dataset)sel;
			List<FilterData> fdl = null;
			try {
				fdl = this.filter.getOptionDataForDataset(ds);
			} catch (MartBuilderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Options.getInstance().updateFilterOptionElement(this.filter, ds, fdl);
			List<String> dsList = new ArrayList<String>();
			dsList.add(ds.getName());
			fdl = this.filter.getFilterDataList(dsList);
			this.optionDataMap.put(ds,fdl);
		}
		return true;
	}
	
	/**
	 * only save to master
	 * @return
	 */
	private boolean save() {
		/*
		 * update the filter element
		 */
		Mart mart = this.filter.getParentConfig().getMart();
		Config config = mart.getMasterConfig();
		Filter masterFilter = config.getFilterByName(this.filter.getName(), null);
		masterFilter.clearOptions();
		//add 
		for(Map.Entry<Dataset, List<FilterData>> entry: this.optionDataMap.entrySet()) {
			Dataset dataset = entry.getKey();
			if(null==entry.getValue())
				continue;
			List<FilterData> values = entry.getValue();

			if(!values.isEmpty()) {
				for(FilterData fd: values) {
					masterFilter.addOption(dataset.getName(), fd);
				}
			}
		}
		return true;
	}
	
	private void add() {
		FilterOptionInputDialog dialog = new FilterOptionInputDialog(this,true);
		if(dialog.getInputs().isEmpty())
			return;
		DefaultListModel model = (DefaultListModel) this.optionList.getModel();
		for(String s: dialog.getInputs()) {
			FilterData fd0 = new FilterData(s,s,false);
			model.addElement(fd0);
			
			//update optionMap
			Object[] objects = this.dsList.getSelectedValues(); 
			for(Object dsObj: objects) {
				FilterData fd = new FilterData(s,s,false);
				List<FilterData> datalist = this.optionDataMap.get(dsObj);
				if(!datalist.contains(fd))
					datalist.add(fd);
			}
		}
		this.optionList.setSelectedIndex(model.getSize()-1);
		this.save();
	}
		
	private void refreshDataList() {
		//non select means union, multiple select means intersection
		DefaultListModel model = (DefaultListModel) this.optionList.getModel();
		model.clear();
		Object[] selected = this.dsList.getSelectedValues();
		if(selected.length == 0) {
			for(List<FilterData> fdl : this.optionDataMap.values()) {
				if(null==fdl)
					continue;
				for(FilterData fd: fdl) {
					model.addElement(fd);
				}
			}
		} else {
			//FIXME intersection
			for(Object sel: selected) {
				for(FilterData fd: this.optionDataMap.get((Dataset)sel)) {
					model.addElement(fd);
				}
			}
		}
	}
	
	private void move(boolean up) {
		if(up) {
			int[] selected = this.optionList.getSelectedIndices();
			if(selected.length>0) {
				if(selected[0] == 0)
					return;
			}
			int[] newSelected = new int[selected.length];
			DefaultListModel model = (DefaultListModel)this.optionList.getModel();			
			for(int i=0; i<selected.length; i++) {
				Object obj = model.remove(selected[i]);
				model.insertElementAt(obj, selected[i]-1);
				newSelected[i] = selected[i]-1;
			}	
						
			this.optionList.setSelectedIndices(newSelected);
		} else {
			int[] selected = this.optionList.getSelectedIndices();
			DefaultListModel model = (DefaultListModel)this.optionList.getModel();
			if(selected.length>0) {
				if(selected[selected.length-1] == model.getSize() -1)
					return;
			}
			int[] newSelected = new int[selected.length];
			
			for(int i=0; i<selected.length; i++) {
				Object obj = model.remove(selected[i]);
				model.insertElementAt(obj, selected[i]+1);
				newSelected[i] = selected[i]+1;
			}

			
			this.optionList.setSelectedIndices(newSelected);
		}
		
		//update optionMap
		Object dsObj = this.dsList.getSelectedValue();
		List<FilterData> fdlist = new ArrayList<FilterData>();
		DefaultListModel model = (DefaultListModel)this.optionList.getModel();
		for(int i=0; i<model.getSize(); i++) {
			fdlist.add((FilterData)model.get(i));
		}
		this.optionDataMap.put((Dataset)dsObj, fdlist);
		this.save();
	}
	
	private void sort() {
		List<FilterData> sortList = new ArrayList<FilterData>();
		DefaultListModel model = (DefaultListModel)this.optionList.getModel();	
		for(int i=0; i<model.getSize(); i++) {
			sortList.add((FilterData)model.get(i));
		}
		Collections.sort(sortList);
		//get selected dataset name
		Dataset selectedDs = (Dataset)this.dsList.getSelectedValue();
		if(selectedDs == null)
			return;
		this.optionDataMap.put(selectedDs, sortList);
		model.clear();
		for(FilterData str: sortList) {
			model.addElement(str);
		}
		this.optionList.revalidate();
		this.save();
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
	}

	public void windowClosing(WindowEvent arg0) {
		McEventBus.getInstance().removeListener(McEventProperty.FILTEROPTION_CHANGED.toString(), this);			
	}

	public void windowDeactivated(WindowEvent arg0) {
		//McEventBus.getInstance().removeListener(McEventProperty.FILTEROPTION_CHANGED.toString(), this);	
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@McEventListener
	public void update(McEvent<?> event) {
		String property = event.getProperty();
		if(property.equals(McEventProperty.FILTEROPTION_CHANGED.toString())) {
			Dataset selectedDs = (Dataset)this.dsList.getSelectedValue();
			if(selectedDs == null)
				return;
			List<FilterData> changedList = new ArrayList<FilterData>();
			DefaultListModel model = (DefaultListModel)this.optionList.getModel();	
			for(int i=0; i<model.getSize(); i++) {
				changedList.add((FilterData)model.get(i));
			}
			//get selected dataset name
			this.optionDataMap.put(selectedDs, changedList);
			this.save();
		}
	}

}