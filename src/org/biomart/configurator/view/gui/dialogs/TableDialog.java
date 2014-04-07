package org.biomart.configurator.view.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreeNode;
import org.biomart.common.resources.Settings;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.TreeNodeHandler;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.jdomUtils.McViewsFilter;
import org.biomart.configurator.jdomUtils.XMLTreeCellRenderer;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.McNodeType;
import org.biomart.configurator.utils.type.McViewType;
import org.biomart.configurator.view.AttributeTable;
import org.biomart.configurator.view.MartConfigTree;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Mart;

public class TableDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final private Mart mart;
	private MartConfigTree tree;
	
	public TableDialog(JDialog parent, Mart mart) {
		super(parent);
		this.mart = mart;
		init();
	}
	
	private void init() {
		if(Boolean.parseBoolean(Settings.getProperty("checkorphancolumn"))) {
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
						progressMonitor.setStatus("checking orphan columns ...");
						checkOrphanColumn();
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
			progressMonitor.start("processing ...");
		}
		this.setLayout(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		JButton naiveButton = new JButton(McUtils.createImageIcon("images/search.gif"));
		naiveButton.setToolTipText("create naive attributes and filters for orphan columns");
		naiveButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				doNaive();
			}
		});
		naiveButton.setEnabled(Boolean.parseBoolean(Settings.getProperty("checkorphancolumn")));
		toolBar.add(naiveButton);
		
		this.add(toolBar,BorderLayout.NORTH);
		this.add(this.createTreePanel(),BorderLayout.CENTER);
		this.setModal(true);
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(screenSize.width-100,screenSize.height - 100);
		this.setLocationRelativeTo(null);
		this.tree.setSelectionRow(0);
		this.tree.expandRow(0);
		this.setVisible(true);
	}
	
	private void checkOrphanColumn() {
		for(DatasetTable dst: mart.getDatasetTables()) {
			for(Column dsc: dst.getColumnList()) {
				DatasetColumn dscol = (DatasetColumn)dsc;
				if(!dscol.hasReferences()) {
					dscol.setOrphan(true);
					dst.setOrphan(true);
				}				
			}			
		}
	}
	
	private void doNaive() {
		TreeNodeHandler tnh = new TreeNodeHandler();
		for(DatasetTable dst: mart.getDatasetTables()) {
			if(dst.isOrphan()) {
				for(Column dsc: dst.getColumnList()) {
					DatasetColumn dscol = (DatasetColumn)dsc;
					if(dscol.isOrphan()) {
						tnh.requestNewAttribute(dscol);
					}
				}							
			}
		}
		this.tree.getModel().nodeStructureChanged((TreeNode)this.tree.getModel().getRoot());
	}
	
	private JPanel createTreePanel() {
		JPanel panel = new JPanel(new BorderLayout());
		McTreeNode node = new McTreeNode(mart);
		tree = new MartConfigTree(node,false,true,true,true,this);
		ToolTipManager.sharedInstance().registerComponent(tree);
		Map<McNodeType, Map<String, String>> filters = McGuiUtils.INSTANCE.getFilterMap(McViewType.TARGET);
		McViewsFilter filter = new McViewsFilter(filters);
		tree.getModel().setFilters(filter);
		node.synchronizeNode();
		
        XMLTreeCellRenderer treeCellRenderer = new XMLTreeCellRenderer();
        tree.setCellRenderer(treeCellRenderer);
        JScrollPane ltreeScrollPane = new JScrollPane(tree);
        
        AttributeTable attrTable = new AttributeTable(tree,this);
        tree.setAttributeTable(attrTable);
        JScrollPane tableScroll = new JScrollPane(attrTable);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        		ltreeScrollPane,tableScroll);
        splitPane.setResizeWeight(0.5);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(400);
        panel.add(splitPane,BorderLayout.CENTER);
		return panel;
	}
	
}