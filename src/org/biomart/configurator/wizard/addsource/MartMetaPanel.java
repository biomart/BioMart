package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.dialects.DialectFactory;
import org.biomart.configurator.controller.dialects.McSQL;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.treelist.CheckBoxListModel;
import org.biomart.configurator.utils.treelist.FileCheckBoxNode;
import org.biomart.configurator.utils.treelist.LeafCheckBoxList;
import org.biomart.configurator.utils.treelist.LeafCheckBoxNode;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.McEventProperty;

public class MartMetaPanel extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MartMetaList martMetaList;
	private LeafCheckBoxList leafList;
	private final MartMetaModel model;
	private ASMartsPanel parent;
	
	public MartMetaPanel(ASMartsPanel parent) {
		this.parent = parent;
		model = new MartMetaModel();
		init();
	}
	
	private void init() {
		martMetaList = new MartMetaList();
		leafList = new LeafCheckBoxList();
		leafList.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				MartMetaPanel.this.parent.getWizard().updateButtonEnabled();
				
			}
			
		});
		martMetaList.setMetaModel(model);
		leafList.setMetaModel(model);
		this.model.addPropertyChangeListener(this);
		
		this.setLayout(new GridLayout());
		JPanel martsPanel = new JPanel(new BorderLayout());
		JLabel martLabel = new JLabel("choose source");
	    JScrollPane scrollPane = new JScrollPane(martMetaList);
	    scrollPane.setPreferredSize(new Dimension(300, 400));

	    martsPanel.add(scrollPane,BorderLayout.CENTER);
	    martsPanel.add(martLabel,BorderLayout.NORTH);
	   
	    this.add(martsPanel);

		JPanel dsPanel = new JPanel(new BorderLayout());
		JLabel dsLabel = new JLabel("  ");
    
	    JScrollPane listScrollPane = new JScrollPane(leafList);
	    //listScrollPane.setSize(scrollPane.getSize());
	    dsPanel.add(listScrollPane,BorderLayout.CENTER);
	    dsPanel.add(dsLabel,BorderLayout.NORTH);

	    this.add(dsPanel);

	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(evt.getPropertyName().equals(MartMetaModel.MART_ITEM_CHANGE)) {
			
		} else if(evt.getPropertyName().equals(MartMetaModel.RESTORE_ITEMS)) {
			@SuppressWarnings("unchecked")
			Collection<LeafCheckBoxNode> items = (Collection<LeafCheckBoxNode>)evt.getNewValue();
			this.leafList.setItems(items);
			this.parent.getWizard().updateButtonEnabled();
		} else if(evt.getPropertyName().equals(MartMetaModel.SET_ITEMS)) {
			FileCheckBoxNode node = (FileCheckBoxNode)evt.getNewValue();
			this.setSubNodes(node);
		}
	}
	
	public void updateList(List<MartInVirtualSchema> list) {
		List<LeafCheckBoxNode> nodeList = new ArrayList<LeafCheckBoxNode>();
		if(list!=null) {
			for(MartInVirtualSchema item:list) {
				FileCheckBoxNode node = new FileCheckBoxNode(item,false);
				nodeList.add(node);
			}
		}
		CheckBoxListModel model = (CheckBoxListModel)this.martMetaList.getModel();
		model.clear();
		for(LeafCheckBoxNode node: nodeList) {
			model.addElement(node);
		}	
	}
	
	public void setSubNodes(final FileCheckBoxNode node) {
		McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.TRUE);
		final SwingWorker<Void,Void> worker = new SwingWorker<Void,Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				try {
					MartInVirtualSchema martV = (MartInVirtualSchema)node.getUserObject();
					List<LeafCheckBoxNode> tables = null;
					if(martV.isURLMart()) {
						tables = MartMetaPanel.this.getSubNodesFromUrl(martV);
					} else {
						//db
						AddSourceWizardObject object = (AddSourceWizardObject)MartMetaPanel.this.parent.getWizardStateObject();
						DataLinkType dlt = object.getDlinkInfo().getDataLinkType();
						JdbcLinkObject conObject = martV.getJdbcLinkObject();
						if(dlt == DataLinkType.FILE)
							tables = getSubNodesFromDB_BC(conObject,martV.getSchema());		
						else
							tables = MartMetaPanel.this.getSubNodesFromDB(martV.getSchema(),martV);
					}
					MartMetaPanel.this.leafList.setItems(tables);	
					//save
					MartMetaPanel.this.saveCurrentSelectedDataSets(node);
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}finally {
					McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.FALSE);
				}
				return null;
			}
			
			 @Override
			 protected void done() {
				 McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.FALSE);
				 MartMetaPanel.this.martMetaList.getMetaModel().firePropertyChange(MartMetaModel.RESTORE_ITEMS, null, node.getSubNodes());
			 }
		};
		
		worker.execute();
	}
	
	private List<LeafCheckBoxNode> getSubNodesFromDB_BC(JdbcLinkObject conObject, String schemaName) {
		return DialectFactory.getDialect(conObject.getJdbcType()).
			getMetaTablesFromOldConfig(conObject, schemaName, true);
	}
	
	private List<LeafCheckBoxNode> getSubNodesFromDB(String schemaName, MartInVirtualSchema martV) {
		AddSourceWizardObject object = (AddSourceWizardObject)this.parent.getWizardStateObject();
		DataLinkType dlt = object.getDlinkInfo().getDataLinkType();
		JdbcLinkObject conObject = object.getDlinkInfo().getJdbcLinkObject();
		if(dlt.equals(DataLinkType.SOURCE)) {
			McSQL mcsql = new McSQL();
			return mcsql.getTablesNodeFromSource(conObject, schemaName, false);
		}
		else if(dlt.equals(DataLinkType.TARGET)) {
			boolean useOldConfig = false;
			McSQL mcsql = new McSQL();
			if(mcsql.hasOldConfigInTarget(conObject, schemaName)) {
				int option = JOptionPane.showConfirmDialog(null,
					    "Do you want to use the existing config?", "",
					    JOptionPane.YES_NO_OPTION);
				if(option == 0) {
					useOldConfig = true;
				}else {
					useOldConfig = false;
				}
			}else {
				useOldConfig = false;
			}
			
			if(useOldConfig) {
				martV.setUseBCForDB(true);
				return DialectFactory.getDialect(conObject.getJdbcType()).
					getMetaTablesFromOldConfig(conObject, schemaName, true);
			}else {
				//get tables from sql
				List<String> tables = mcsql.getTablesFromTarget(conObject, schemaName);
				Collections.sort(tables);	
				List<LeafCheckBoxNode> tableNodes = new ArrayList<LeafCheckBoxNode>();
				for(String table: tables) {
					LeafCheckBoxNode cbn = new LeafCheckBoxNode(table,false);
					DatasetFromUrl dsUrl = new DatasetFromUrl();
					dsUrl.setName(table);
					dsUrl.setDisplayName(table);
					cbn.setUserObject(dsUrl);
					tableNodes.add(cbn);
				}
				return tableNodes;
			}			
		}
		return new ArrayList<LeafCheckBoxNode>();
	}

	private List<LeafCheckBoxNode> getSubNodesFromUrl(MartInVirtualSchema martV){
		List<LeafCheckBoxNode> dsList = new ArrayList<LeafCheckBoxNode>();
		List<DatasetFromUrl> dss;
		try {
			dss = McGuiUtils.INSTANCE.getDatasetsFromUrlForMart(martV);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, Resources.get("NODATASETS"), "error", JOptionPane.ERROR_MESSAGE);
			return dsList;
		}
		for(DatasetFromUrl ds: dss) {
			boolean b = ds.isSequence()?false:true;
			LeafCheckBoxNode cbn = new LeafCheckBoxNode(ds.getName(),b);
			cbn.setEnabled(b);
			cbn.setUserObject(ds);
			dsList.add(cbn);
		}
		return dsList;
	}

	private void saveCurrentSelectedDataSets(FileCheckBoxNode node) {
		node.clearTables();
		for(int i=0; i<this.leafList.getModel().getSize(); i++) {
			node.addTable((LeafCheckBoxNode)this.leafList.getModel().getElementAt(i));
		}
	}
	
	public Map<String,List<String>> get08MartList(boolean all) {
		Map<String,List<String>> mpList = new HashMap<String,List<String>>();
		for(Object obj: this.martMetaList.getCheckedValues()) {
			FileCheckBoxNode cbn = (FileCheckBoxNode)obj;

			List<String> list = new ArrayList<String>();
			for(LeafCheckBoxNode sub: cbn.getSubNodes()) {
				if(sub.isSelected()) {
					list.add(sub.getText());
				}
			}
			if(!list.isEmpty())
				mpList.put(cbn.getText(), list);
					
		}
		return mpList;
	}
	
	public List<MartInVirtualSchema> getMartsForDB(JdbcLinkObject conObject) {
		
		List<MartInVirtualSchema> result = new ArrayList<MartInVirtualSchema>();
		Pattern p = null;
		if(conObject.getPartitionRegex()!=null && conObject.getPtNameExpression()!=null)
		try {
			p = Pattern.compile(conObject.getPartitionRegex());
		} catch (final PatternSyntaxException e) {
			// Ignore and return if invalid.
			return new ArrayList<MartInVirtualSchema>();
		}

		Connection con = ConnectionPool.Instance.getConnection(conObject);
		if(con==null)
			return new ArrayList<MartInVirtualSchema>();
		try {
			DatabaseMetaData dmd = con.getMetaData();			
			ResultSet rs2 = conObject.useSchema()? dmd.getSchemas(): dmd.getCatalogs();
			//ResultSet rs2 = dmd.getSchemas();
			//clean all
			//check the first one is information_schema
//			rs2.next();
//			String schemaName = rs2.getString(1);
			
//			if(!"information_schema".equals(rs2.getString(1)))
//				this.treeItemStrList.add(rs2.getString(1));
			while (rs2.next()) {
				String schemaName = rs2.getString(1);
				String databaseName = conObject.useSchema()?conObject.getDatabaseName():schemaName;
				if(schemaName.equals("information_schema"))
					continue;
				if(conObject.getPartitionRegex()!=null && conObject.getPtNameExpression()!=null) {
					Matcher m = p.matcher(schemaName);
					if (m.matches()) {
						MartInVirtualSchema mart = new MartInVirtualSchema.DBBuilder().database(databaseName).schema(schemaName)
							.host(conObject.getHost()).port(conObject.getPort()).name(schemaName).displayName(schemaName)
							.type(conObject.getJdbcType().toString()).username(conObject.getUserName()).password(conObject.getPassword())
							.build();
						result.add(mart);
					}
				}else {
					MartInVirtualSchema mart = new MartInVirtualSchema.DBBuilder().database(databaseName).schema(schemaName)
					.host(conObject.getHost()).port(conObject.getPort()).name(schemaName).displayName(schemaName)
					.type(conObject.getJdbcType().toString()).username(conObject.getUserName()).password(conObject.getPassword()).build();
					result.add(mart);
				}
			}					
		} catch(SQLException ex) {
			ex.printStackTrace();
		}
		ConnectionPool.Instance.releaseConnection(conObject);
		return result;
	}

	
	/**
	 * if all=false, only selected tables return;
	 * @param all
	 * @return
	 */
	public Map<MartInVirtualSchema, List<DatasetFromUrl>> get07MartList(boolean all) {

		Map<MartInVirtualSchema, List<DatasetFromUrl>>dbInfo = new LinkedHashMap<MartInVirtualSchema, List<DatasetFromUrl>>();
		for(Object selected: this.martMetaList.getCheckedValues()) {
			FileCheckBoxNode cbn = (FileCheckBoxNode)selected;
			MartInVirtualSchema ms = (MartInVirtualSchema)cbn.getUserObject();
			dbInfo.put(ms,cbn.getDatasetsForUrl(all));			
		}
		return dbInfo;
	}
	
	public Map<MartInVirtualSchema, List<DatasetFromUrl>> getMartListForDB(boolean all) {
		Map<MartInVirtualSchema, List<DatasetFromUrl>>dbInfo = new LinkedHashMap<MartInVirtualSchema, List<DatasetFromUrl>>();
		for(Object selected: this.martMetaList.getCheckedValues()) {
			FileCheckBoxNode cbn = (FileCheckBoxNode)selected;
			MartInVirtualSchema ms = (MartInVirtualSchema)cbn.getUserObject();
			dbInfo.put(ms,cbn.getDatasetsForUrl(all));			
		}
		return dbInfo;
	}

	public void clear() {
		CheckBoxListModel model = (CheckBoxListModel)this.martMetaList.getModel();
		model.removeAllElements();
		CheckBoxListModel model2 = (CheckBoxListModel)this.leafList.getModel();
		model2.removeAllElements();
	}

	public boolean hasUserSelectedItems() {
		List<LeafCheckBoxNode> checkList = this.martMetaList.getCheckedValues();
		boolean valid = false;
		if(checkList.size() > 0) {		
			for(LeafCheckBoxNode checkBoxNode: checkList) {
				List<LeafCheckBoxNode> subNodes = checkBoxNode.getSubNodes();
				for(LeafCheckBoxNode subNode: subNodes) {
					if(subNode.isSelected()) {
						valid = true;
						break;
					}
				}
				if(valid)
					break;
			}
		}
		return valid;
	}
	
}