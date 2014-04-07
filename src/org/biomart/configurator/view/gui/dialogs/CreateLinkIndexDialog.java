package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.linkIndices.LinkIndices;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;

public class CreateLinkIndexDialog extends JDialog implements ListSelectionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Mart mart;
	private JComboBox importableCB;
	private JTable datasetTable;
	private MyTableModel model;
	private JButton okButton;
	private JButton removeButton;
//	private JList datasetList;
	
	public CreateLinkIndexDialog(Mart mart) {
		this.mart = mart;
		this.init();
		this.setModal(true);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);		
	}
	
	private void init() {
		JPanel content = new JPanel(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		JPanel inputPanel = new JPanel(new BorderLayout());
		
		
		JPanel impPanel = new JPanel();
		JLabel importableLabel = new JLabel("Link: ");
		this.importableCB = new JComboBox();
		for(Link el: this.mart.getMasterConfig().getLinkList()) {
			if(el.getObjectStatus()!=ValidationStatus.VALID)
				continue;
			this.importableCB.addItem(el);
		}
		impPanel.add(importableLabel);
		impPanel.add(this.importableCB);
		
		importableCB.addActionListener (new ActionListener () {
		    public void actionPerformed(ActionEvent e) {
		        //reset table
		    	resetTable();
		    }
		});

		
		JPanel dsListPanel = new JPanel(new BorderLayout());
		List<List<String>> data = new ArrayList<List<String>>();
		
		for(Dataset ds: this.mart.getDatasetList()) {
			//check if linkindic already exist
			boolean b = McGuiUtils.INSTANCE.hasIndexInDataset(this.mart, ds.getName());
			List<String> row = new ArrayList<String>();
			row.add(ds.getName());
			row.add(Boolean.toString(b));
			data.add(row);
		}
		
		model = new MyTableModel(data);
		datasetTable = new JTable(model);
		datasetTable.getSelectionModel().addListSelectionListener(this);
		datasetTable.setShowGrid(true);
		datasetTable.setGridColor(Color.BLACK);
		
		JScrollPane sp = new JScrollPane(this.datasetTable);
		//sp.setPreferredSize(new Dimension(200,200));

		dsListPanel.add(sp, BorderLayout.CENTER);		
		
		JButton cancelButton = new JButton(Resources.get("CANCEL"));
		cancelButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				CreateLinkIndexDialog.this.setVisible(false);
			}
			
		});
		
		okButton = new JButton("Create");
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createLinkIndex();
				CreateLinkIndexDialog.this.setVisible(false);				
			}			
		});
		
		removeButton = new JButton("Remove");
		removeButton.setEnabled(false);
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeLinkIndex();
				CreateLinkIndexDialog.this.setVisible(false);
			}
			
		});
		buttonPanel.add(cancelButton);
		buttonPanel.add(removeButton);
		buttonPanel.add(okButton);		

		inputPanel.add(impPanel,BorderLayout.NORTH);
		inputPanel.add(dsListPanel, BorderLayout.CENTER);
		content.add(inputPanel, BorderLayout.CENTER);
		content.add(buttonPanel,BorderLayout.SOUTH);
		this.add(content);
		this.setTitle(Resources.get("CREATELINKINDEXTITLE"));
	}
	
	private void createLinkIndex() {
		final Object impObj = this.importableCB.getSelectedItem();
		
		int[] rows = this.datasetTable.getSelectedRows();
		if(this.importableCB.getSelectedItem() == null)
			return;
		if(rows == null || rows.length==0)
			return;
		final ArrayList<String> datasetList = new ArrayList<String>();
		for(int row: rows) {
			datasetList.add((String)model.getValueAt(row, 0));
		}
		//add progress bar
		final ProgressDialog progressMonitor = ProgressDialog.getInstance(this);				
		
		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {
			@Override
			protected void done() {
				// Close the progress dialog.
				progressMonitor.setVisible(false);
			}

			@Override
			protected Void doInBackground() throws Exception {
				try {
					progressMonitor.setStatus("getting link indices");
					LinkIndices li = mart.getLinkIndices();
					li.addIndex((Link)impObj,datasetList);
					//reset table
					resetTable();
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}finally {
					progressMonitor.setVisible(false);
				}
				return null;

			}
		};
		
		worker.execute();
		progressMonitor.start("getting link indices ...");

	}
	
	private void removeLinkIndex() {
		List<String> dsList = new ArrayList<String>();
		int[] rows = this.datasetTable.getSelectedRows();
		for(int row: rows) {
			dsList.add((String)this.datasetTable.getValueAt(row, 0));
		}
		//TODO also check ../registry/linkindices 
		File dir = null;
		try {
			dir = new File(McGuiUtils.INSTANCE.getDistIndicesDirectory().getCanonicalPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		String[] children = dir.list(); 
		if (children == null) { 
			// Either dir does not exist or is not a directory 
		} else { 
			for (String fileName: children) {
				if(fileName.indexOf(mart.getName()+"_") == 0) {
					//remove file
					//check dataset
					for(String ds: dsList) {
						if(fileName.indexOf(mart.getName()+"_"+ds+"__")==0) {
						try {
								if(!(new File(dir.getCanonicalPath()+"/"+fileName)).delete()) {
									Log.error("cannot remove file");
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}					
			}
		}
		//refresh table
		this.resetTable();
	}
	
	private void resetTable() {		
		MyTableModel myModel = (MyTableModel)this.datasetTable.getModel();
		List<List<String>> data = new ArrayList<List<String>>();
		
		for(Dataset ds: this.mart.getDatasetList()) {
			//check if linkindic already exist
			boolean b = McGuiUtils.INSTANCE.hasIndexInDataset(this.mart, ds.getName());
			List<String> row = new ArrayList<String>();
			row.add(ds.getName());
			row.add(Boolean.toString(b));
			data.add(row);
		}
		myModel.resetData(data);
	}
	
	class MyTableModel extends AbstractTableModel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String[] columnNames = new String[]{"dataset","indexed"};
		private List<List<String>> data;
		
		public MyTableModel(List<List<String>> data) {
			this.data = data;
		}
		
		@Override
		public String getColumnName(int col) {
	        return columnNames[col];
	    }

		
		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			// TODO Auto-generated method stub
			return data.get(row).get(col);
		}
		
		@Override
		public Class<?> getColumnClass(int c) {
	        return String.class;
	    }
		
		public void resetData(List<List<String>> data) {
			this.data = data;
		}

		
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(this.datasetTable.getSelectedRowCount()>0) 
		this.okButton.setEnabled(true);
		this.removeButton.setEnabled(true);		
	}

		
}