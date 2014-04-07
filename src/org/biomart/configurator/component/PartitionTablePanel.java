package org.biomart.configurator.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreeSelectionModel;

import org.biomart.configurator.view.AttTableCellRenderer;
import org.biomart.configurator.view.PartitionTableCellRenderer;
import org.biomart.configurator.view.PartitionTableHeadRenderer;
import org.biomart.configurator.view.menu.PtTableContextMenu;
import org.biomart.objects.objects.PartitionTable;



public class PartitionTablePanel extends JPanel implements TableModelListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private PtTableContextMenu popupMenu;
	private JTable table;
	private PartitionTable ptable;

	public PartitionTablePanel(PartitionTable ptable, int startcol, int endcol) {
		this.ptable = ptable;
		this.init(startcol, endcol);
	}
	
	private void init(int start, int end) {
		this.setLayout(new BorderLayout());
		
		PtModel model = new PtModel(ptable,start,end);
		model.addTableModelListener(this);
		//McViewTree treeView = (McViewTree)McViews.getInstance().getView(IdwViewType.MCTREE);
		//model.addTableModelListener(treeView.getMcTree());
		JTable tmpTable = new JTable(model);
		table = this.autoResizeColWidth(tmpTable, model);
		table.setShowGrid(true);
		table.setGridColor(Color.BLACK);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);		
		
		MouseListener popupListener = new PopupListener();
		table.addMouseListener(popupListener);
		table.getTableHeader().addMouseListener(popupListener);
		//add headrenderer
		table.getTableHeader().setDefaultRenderer(new PartitionTableHeadRenderer());
		table.setPreferredScrollableViewportSize(table.getPreferredSize());
		table.setDefaultRenderer(Object.class, new PartitionTableCellRenderer());
		JScrollPane scrollPane = new JScrollPane(table);

		this.add(scrollPane,BorderLayout.CENTER);	
		
		this.setBorder(new EtchedBorder());
	}
	
	private void createContextMenu(int row, int col) {
		popupMenu = new PtTableContextMenu(this.table, ptable, row, col);
	}
	
	
	class PopupListener implements MouseListener {
		public void mousePressed(MouseEvent e) {
			showPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			showPopup(e);
		}

		private void showPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				PartitionTablePanel.this.createContextMenu(table.rowAtPoint(e.getPoint()),table.columnAtPoint(e.getPoint()));			
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}
	}


	public void tableChanged(TableModelEvent e) {
		this.table.setPreferredScrollableViewportSize(this.table.getPreferredSize());		
	}
	

	private JTable autoResizeColWidth(JTable table, PtModel model) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(model);
 
        int margin = 5;
        for (int i = 0; i < table.getColumnCount(); i++) {
            int  vColIndex = i;
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
            TableColumn  col = colModel.getColumn(vColIndex);
            int width  = 0;
 
            // Get width of column header
            TableCellRenderer renderer = col.getHeaderRenderer(); 
            if (renderer == null) {
                renderer = table.getTableHeader().getDefaultRenderer();
            } 
            Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
            width = comp.getPreferredSize().width;
            // Get maximum width of column data
            for (int r = 0; r < table.getRowCount(); r++) {
                renderer = table.getCellRenderer(r, vColIndex);
                comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false,
                        r, vColIndex);
                width = Math.max(width, comp.getPreferredSize().width);
            } 
            // Add margin
            width += 2 * margin;
            // Set the width
            col.setPreferredWidth(width);
        }
 
        ((DefaultTableCellRenderer) table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(
            SwingConstants.LEFT);
 
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }
}

