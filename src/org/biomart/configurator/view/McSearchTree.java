package org.biomart.configurator.view;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.biomart.configurator.jdomUtils.McTreeNode;
import org.biomart.configurator.view.component.SourceConfigPanel;
import org.biomart.configurator.view.gui.dialogs.ConfigDialog;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;


public class McSearchTree extends JTree {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MartConfigTree martTree;
	private SourceConfigPanel scp;
	
	public McSearchTree(McTreeNode node, MartConfigTree martTree, SourceConfigPanel scp) {
		super(node);
		this.martTree = martTree;
		this.scp = scp;
		this.putClientProperty("JTree.lineStyle", "Angled");
		this.setEditable(false);
		this.setShowsRootHandles(true);
		
		this.addMouseListener(new MouseAdapter ()  {
            public void mousePressed (MouseEvent e)  {
            }

            public void mouseReleased(MouseEvent e)  {
                if (e.getClickCount () == 2)  {
                    doubleClick (e);
                }
            }
		});
	}
	
	private void doubleClick(MouseEvent e) {
		TreePath path  = this.getPathForLocation(e.getX(),e.getY());
		if(path == null )
			return;
		McTreeNode node = (McTreeNode) path.getLastPathComponent();
		if(node == null) 
			return;
		if(node.getUserObject() instanceof MartRegistry ||
				node.getUserObject() instanceof Mart) {
			return;
		}
		
		if(this.martTree==null) {
			//check if the selected node is in current source tree
			Config userConfig = node.getObject().getParentConfig();
			if(!userConfig.equals(this.scp.getSourceConfig())) {
				this.scp.updateSourceConfig(userConfig);
			}
			TreePath tp = this.scp.getConfigTree().getPath(node);
			if(tp!=null) {
				ConfigDialog cd = (ConfigDialog)SwingUtilities.getAncestorOfClass(ConfigDialog.class, this.scp);
				if(!cd.isSourceConfigVisible())
					cd.showSourceConfig(true);
				this.scp.getConfigTree().scrollPathToVisible(tp);
				this.scp.getConfigTree().setSelectionPath(tp);
			}
		}else {
			TreePath tp = this.martTree.getPath(node);
			if(tp!=null) {
				this.martTree.scrollPathToVisible(tp);
				this.martTree.setSelectionPath(tp);
			}
		}

	}
}