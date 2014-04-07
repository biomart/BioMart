/**
 * 
 */
package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.biomart.configurator.controller.DatasetListTransferHandler;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.PartitionTable;

/**
 * @author lyao
 *
 */
public class MatchDatasetDialog extends JDialog implements ActionListener, MouseListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Config sourceConfig;
	private Config targetConfig;
	private PartitionTable sourcePT;
	private PartitionTable targetPT;
	private List<Dataset> sourceDataset;
	private List<Dataset> targetDataset;
	private JList sourceList;
	private JList targetList;
	private JScrollPane sourcesp;
	private JScrollPane targetsp;
	private Map<Dataset,List<Dataset>> sourceDatasetMapping = new Hashtable<Dataset,List<Dataset>>();
	private Map<Dataset,List<Dataset>> targetDatasetMapping = new Hashtable<Dataset,List<Dataset>>();
	
	private boolean isCanceled = false;
	private boolean isOk = false;
	private int createdSourceCol;
	private int createdTargetCol;
	
	private DatasetListTransferHandler transferHandler;
	
	public MatchDatasetDialog(JDialog parent, Config sourceConfig, Config targetConfig) {
		//super(parent);
		this.sourceConfig = sourceConfig;
		this.targetConfig = targetConfig;
		this.sourceDataset = this.sourceConfig.getMart().getDatasetList();
		this.targetDataset = this.targetConfig.getMart().getDatasetList();
		this.sourcePT = this.sourceConfig.getMart().getSchemaPartitionTable();
		this.targetPT = this.targetConfig.getMart().getSchemaPartitionTable();
		//sort
		Collections.sort(this.sourceDataset);
		Collections.sort(this.targetDataset);
		
		init();
	}
	
	private void init(){
		if(this.sourceDataset.size() == 1 || this.targetDataset.size() == 1)
		{
			this.createColsInPT();
			this.setOk(true);
			this.setVisible(false);
		}else{
			this.setTitle("Dataset Match Dialog");
			this.add(this.createDatasetPanel());
			this.setModal(true);
			this.pack();
			this.setLocationRelativeTo(null);
			this.setVisible(true);
		}
	}
	
	private JPanel createDatasetPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setPreferredSize(new Dimension(200, 600));
		JPanel buttonPanel = new JPanel();

		JButton matchButton = new JButton("Auto match");
		matchButton.setActionCommand("automatch");
		matchButton.addActionListener(this);
		
		JButton okButton = new JButton("OK");
		okButton.setActionCommand("ok");
		okButton.addActionListener(this);
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("cancel");
		cancelButton.addActionListener(this);
		
		buttonPanel.add(matchButton);
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		DefaultListModel modelSource = new DefaultListModel();
		DefaultListModel modelTarget = new DefaultListModel();
				
		
		for(Dataset dataset : this.sourceDataset)
			modelSource.addElement(dataset.getName());		
				
		for(Dataset dataset : this.targetDataset)
			modelTarget.addElement(dataset.getName());
		
		transferHandler= new DatasetListTransferHandler(this);
		
		this.sourceList = new JList(modelSource);
		this.sourceList.setName("source");
		this.sourceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.sourceList.setDragEnabled(true);
		this.sourceList.setDropMode(DropMode.ON);
		this.sourceList.setTransferHandler(transferHandler);
		
		this.targetList = new JList(modelTarget);
		this.targetList.setName("target");
		this.targetList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.targetList.setDragEnabled(true);
		this.targetList.setDropMode(DropMode.ON);
		this.targetList.setTransferHandler(transferHandler);
		
		
		JLabel leftDsName = new JLabel(this.sourceConfig.getDisplayName(),JLabel.CENTER);
		JLabel rightDsName = new JLabel(this.targetConfig.getDisplayName(),JLabel.CENTER);
		
		JPanel leftDsPanel = new JPanel();
		JPanel rightDsPanel = new JPanel();
		
		sourcesp = new JScrollPane(this.sourceList);
		sourcesp.setPreferredSize(new Dimension(300, 400));
		targetsp = new JScrollPane(this.targetList);
		targetsp.setPreferredSize(new Dimension(300, 400));
		
			
		
		leftDsPanel.setLayout(new BoxLayout(leftDsPanel,BoxLayout.Y_AXIS));
		leftDsPanel.add(leftDsName);
		leftDsPanel.add(sourcesp);
		
		rightDsPanel.setLayout(new BoxLayout(rightDsPanel,BoxLayout.Y_AXIS));
		rightDsPanel.add(rightDsName);
		rightDsPanel.add(targetsp);
	
				
		panel.add(leftDsPanel,BorderLayout.WEST);
		panel.add(rightDsPanel,BorderLayout.EAST);
		panel.add(centerPanel,BorderLayout.CENTER);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		panel.setOpaque(true);
		return panel;
	}

	private void createColsInPT(){
		//create a col in the source partition table
		createdSourceCol = this.sourcePT.addColumn("");
		for(Dataset sourceDs : this.sourceDataset){
			int srow = this.sourcePT.getRowNumberByDatasetName(sourceDs.getName());
			StringBuilder value = new StringBuilder();
			List<Dataset> targetDSs = this.sourceDatasetMapping.get(sourceDs);
			if(this.sourceDataset.size() == 1 || this.targetDatasetMapping.isEmpty()){
				targetDSs = this.targetDataset;
			}
			if(targetDSs == null){
				continue;
			}
			for(Dataset targetDs : targetDSs) {
				value.append(targetDs.getName());
				if(targetDSs.indexOf(targetDs) != targetDSs.size()-1)
					value.append(',');
			}
			
			this.sourcePT.setValue(srow, createdSourceCol, value.toString());
		}
		//create a col in the target partition table
		createdTargetCol = this.targetPT.addColumn("");
		for(Dataset targetDs : this.targetDataset){
			int trow = this.targetPT.getRowNumberByDatasetName(targetDs.getName());
			StringBuilder value = new StringBuilder();
			List<Dataset> sourceDSs = this.targetDatasetMapping.get(targetDs);
			if(this.targetDataset.size() == 1 || this.targetDatasetMapping.isEmpty()){
				sourceDSs = this.sourceDataset;
			}
			if(sourceDSs == null){
				continue;
			}
			for(Dataset sourceDs : sourceDSs){
				value.append(sourceDs.getName());
				if(sourceDSs.indexOf(sourceDs) != sourceDSs.size()-1)
					value.append(',');
			}
			this.targetPT.setValue(trow, createdTargetCol, value.toString());
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if(e.getActionCommand() == "automatch"){
			this.autoMatch();
		}
		else if(e.getActionCommand() =="ok"){
			this.createColsInPT();
			this.setOk(true);
			this.setVisible(false);
		}
		else if(e.getActionCommand() == "cancel"){
			this.setCanceled(true);
			this.setVisible(false);
		}
	}
	
	public void createLink(int sourceIndex, int targetIndex){
		if(sourceIndex >= 0 && sourceIndex < this.sourceDataset.size()
				&& targetIndex >=0 && targetIndex < this.targetDataset.size()){
			Dataset sourceDs = this.sourceDataset.get(sourceIndex);
			Dataset targetDs = this.targetDataset.get(targetIndex);
			
			if(this.sourceDatasetMapping.containsKey(sourceDs)){
				//if dataset mapping already has the source mapping, then add to the target list
				this.sourceDatasetMapping.get(sourceDs).add(targetDs);
			}else{
				//create a new list and add target to the list
				List<Dataset> targetList = new ArrayList<Dataset>();
				targetList.add(targetDs);
				this.sourceDatasetMapping.put(sourceDs, targetList);
			}
			
			if(this.targetDatasetMapping.containsKey(targetDs)){
				//if dataset mapping already has the source mapping, then add to the target list
				this.targetDatasetMapping.get(targetDs).add(sourceDs);
			}else{
				//create a new list and add source to the list
				List<Dataset> sourceList = new ArrayList<Dataset>();
				sourceList.add(sourceDs);
				this.targetDatasetMapping.put(targetDs, sourceList);
			}
			
			this.repaint();
		}
	}
	private void autoMatch(){
		//clear mapping first
		this.sourceDatasetMapping.clear();
		//matching according to display names for source dataset
		for(Dataset sourceDs: this.sourceDataset){
			int targetIndex = Collections.binarySearch(this.targetDataset, sourceDs);
			if(targetIndex <0 || targetIndex >=this.targetDataset.size())
				continue;
			Dataset targetDs = this.targetDataset.get(targetIndex);
			//int targetIndex = this.targetDataset.indexOf(targetDs);
			if(this.sourceDatasetMapping.containsKey(sourceDs)){
				//if dataset mapping already has the source mapping, then add to the target list
				this.sourceDatasetMapping.get(sourceDs).add(targetDs);
			}else{
				//create a new list and add target to the list
				List<Dataset> targetList = new ArrayList<Dataset>();
				targetList.add(targetDs);
				this.sourceDatasetMapping.put(sourceDs, targetList);
			}
		}
		//do the same for target dataset
		for(Dataset targetDs: this.targetDataset){
			int sourceIndex = Collections.binarySearch(this.sourceDataset, targetDs);
			if(sourceIndex < 0 || sourceIndex >= this.sourceDataset.size())
				continue;
			Dataset sourceDs = this.sourceDataset.get(sourceIndex);
			
			if(this.targetDatasetMapping.containsKey(targetDs)){
				//if dataset mapping already has the source mapping, then add to the target list
				this.targetDatasetMapping.get(targetDs).add(sourceDs);
			}else{
				//create a new list and add source to the list
				List<Dataset> sourceList = new ArrayList<Dataset>();
				sourceList.add(sourceDs);
				this.targetDatasetMapping.put(targetDs, sourceList);
			}
		}
		this.repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.Window#paint(java.awt.Graphics)
	 */
	@Override
	public void paint(Graphics g) {
		// TODO Auto-generated method stub
		super.paint(g);
		Rectangle cellRect = this.sourceList.getCellBounds(0, 0);
		if(cellRect != null && !this.sourceDatasetMapping.isEmpty()){
			DefaultListModel sourceModel = (DefaultListModel)this.sourceList.getModel();
						
			Point p = this.getLocationOnScreen();
			for(int i=0;i<sourceModel.getSize();i++){		
				Point p1 = this.sourcesp.getLocationOnScreen();
				p1.translate(this.sourcesp.getWidth(), 0);
				//shift drawing lines to the middle of the list row
				p1.translate(0, cellRect.height/2 + i * cellRect.height);
				Dataset sourceDs = this.sourceDataset.get(i);
				if(!this.sourceDatasetMapping.containsKey(sourceDs))
					continue;
				int x1=p1.x;
				int y1 = p1.y;
				for(Dataset targetDs : this.sourceDatasetMapping.get(sourceDs)){
					int targetIndex = this.targetDataset.indexOf(targetDs);
					Point p2 = this.targetsp.getLocationOnScreen();					
					p2.translate(0, cellRect.height/2 + targetIndex * cellRect.height);					
					p1.x = x1 - p.x;
					p1.y = y1 - p.y;
					p2.x = p2.x - p.x;
					p2.y = p2.y - p.y;
					g.setColor(Color.red);
					g.drawLine(p1.x, p1.y, p2.x, p2.y);
				}
				
			}
		}
	}

	/**
	 * @return the isCanceled
	 */
	public boolean isCanceled() {
		return isCanceled;
	}

	/**
	 * @param isCanceled the isCanceled to set
	 */
	public void setCanceled(boolean isCanceled) {
		this.isCanceled = isCanceled;
	}

	/**
	 * @return the createdSourceCol
	 */
	public int getCreatedSourceCol() {
		return createdSourceCol;
	}

	/**
	 * @return the createdTargetCol
	 */
	public int getCreatedTargetCol() {
		return createdTargetCol;
	}

	/**
	 * @return the isOk
	 */
	public boolean isOk() {
		return isOk;
	}

	/**
	 * @param isOk the isOk to set
	 */
	public void setOk(boolean isOk) {
		this.isOk = isOk;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
