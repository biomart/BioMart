package org.biomart.configurator.view.gui.diagrams;

import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import org.biomart.common.resources.Resources;
import org.biomart.configurator.jdomUtils.JDomUtils;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.component.LinkTableComponent;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.jdom.Element;

public class LinkedPortableDiagram extends JLayeredPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MartConfiguratorObject object;
	private Set<LinkComponent> linkSet;
	//for showing all portable
//	private List<LinkTableComponent> ltList;
	
	
	/**
	 * if showall, the node is martRegistry, else the node is dataset
	 * @param node
	 * @param showAll
	 */
	public LinkedPortableDiagram(MartConfiguratorObject object, boolean showAll) {
		//add current config
		this.object = object;
		if(showAll) {
			JPanel content = this.showAllPortable();
			//content.setBounds(McViews.getInstance().getView(IdwViewType.SCHEMA).getBounds());
			this.add(content);

		}
		else {
			JPanel content = this.showLinkedPortable();
			//content.setBounds(McViews.getInstance().getView(IdwViewType.SCHEMA).getBounds());
			this.add(content);			
		}
	}
	

	private JPanel showLinkedPortable() {
		Config config = (Config) this.object;
		JLabel linkedImpLabel = null;
		JLabel linkedExpLabel = null;
		JPanel content = new JPanel();
		FlowLayout flo = new FlowLayout(FlowLayout.LEFT,50,10);
		content.setLayout(flo);
		LinkTableComponent currentDsPanel = new LinkTableComponent(config);
		content.add(currentDsPanel);
						
		List<ElementList> impList = config.getImportableList();
		List<ElementList> expList = config.getExportableList();
				
		List<Mart> allMarts = config.getMart().getMartRegistry().getMartList();
		//find all configs 
		List<Config> configList = new ArrayList<Config>();
		for(Mart mart: allMarts) {
			configList.addAll(mart.getConfigList());
		}
		
		linkSet = new HashSet<LinkComponent>();
		
		for(Config tmpConf: configList) {
			//don't show itself again
			if(tmpConf.equals(config))
				continue;

			ElementList linkedImp = null;
			ElementList linkedExp = null;
			
			List<ElementList> linkedImps = tmpConf.getImportableList();
			List<ElementList> linkedExps = tmpConf.getExportableList();
			
			boolean linked = false;			
			
			for(ElementList imp: linkedImps) {
				for(ElementList currentExp: linkedExps) {
					if(imp.getName().equalsIgnoreCase(
							currentExp.getName())) {
						linked = true;
						//remember the importable/exportable
						linkedExpLabel = currentDsPanel.getExpComponent(currentExp.getName());
						linkedImp = imp;
						linkedExp = currentExp;
						break;
					}
				}
			}
			
			if(linked) {
				LinkTableComponent linkedPanel = new LinkTableComponent(config);
				content.add(linkedPanel);
				linkedImpLabel = linkedPanel.getImpComponent(linkedImp.getName());
				LinkComponent lc = new LinkComponent(linkedImpLabel,linkedExpLabel);
				linkSet.add(lc);
			} else {
			
				for(ElementList exp: linkedExps) {
					for (ElementList currentImp: linkedImps) {
						if(exp.getName().equalsIgnoreCase(
								currentImp.getName())) {
							linked = true;
							//remember the importable/exportable
							linkedImp = currentImp;
							linkedExp = exp;
							linkedImpLabel = currentDsPanel.getImpComponent(exp.getName());
							break;
						}
					}
				}
				if(linked) {
					LinkTableComponent linkedPanel = new LinkTableComponent(config);
					content.add(linkedPanel);		
					linkedExpLabel = linkedPanel.getExpComponent(linkedExp.getName());
					LinkComponent lc = new LinkComponent(linkedImpLabel,linkedExpLabel);
					linkSet.add(lc);					
				}
			}
		} 
		return content;
	}
	
	private JPanel showAllPortable() {
		linkSet = new HashSet<LinkComponent>();
//		ltList = new ArrayList<LinkTableComponent>();
		JPanel content = new JPanel();
		FlowLayout flo = new FlowLayout(FlowLayout.LEFT,50,10);
		content.setLayout(flo);
/*		List<JDomNodeAdapter> dsNodeList = JDomUtils.searchElementS(node.getNode(), Resources.get("DATASET"), null);

		for(JDomNodeAdapter dsNode: dsNodeList) {
			LinkTableComponent dsPanel = new LinkTableComponent(dsNode);
			ltList.add(dsPanel);
			content.add(dsPanel);
		}
		//add linkSet
		for(int i=0; i<ltList.size(); i++) {
			for(int j=i+1; j<ltList.size(); j++) {
				Set<LinkComponent> lcSet = ltList.get(i).getLinks(ltList.get(j));
				if(lcSet!=null)
					linkSet.addAll(lcSet);
			}
		}
	*/	
		return content;
	}

	
	public Set<LinkComponent> getLinkComponents() {
		return this.linkSet;
	}
	

}