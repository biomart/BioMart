package org.biomart.configurator.view.component;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;


public class LinkTableComponent extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String, JLabel> impComponent;
	private Map<String, JLabel> expComponent;
	private final Color impBGColor = Color.YELLOW;
	private final Color expBGColor = Color.ORANGE;
	private final Color proBGColor = Color.GREEN;
	
	public LinkTableComponent(Config config) {
		impComponent = new HashMap<String, JLabel>();
		expComponent = new HashMap<String, JLabel>();
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		int y=0;
		c.gridx = 0;
		c.gridy = y;

		this.setBorder(new EtchedBorder());
		JLabel dsName = new JLabel(config.getName());
		this.add(dsName,c);
		
		c.fill = GridBagConstraints.HORIZONTAL;
		
/*		List<Element> impList = JDomUtils.searchElementList(treeNode.getNode(), Resources.get("IMPORTABLE"),null);
		List<Element> importableList = JDomUtils.getCurrentConfigElements(impList);
		for(Element importableE: importableList) {
			y++;
			c.gridy = y;
			ImportableComponent label = new ImportableComponent("importable: "+importableE.getAttributeValue(Resources.get("NAME")));
			this.impComponent.put(importableE.getAttributeValue(Resources.get("NAME")), label);
			this.add(label,c);
		}
		
		List<Element> processorList = JDomUtils.searchElementList(treeNode.getNode(), Resources.get("PROCESSOR"),null);
		List<Element> proList = JDomUtils.getCurrentConfigElements(processorList);
		*/
/*		for(Element processorE: proList) {
			y++;
			c.gridy = y;
			ProcessorComponent label = new ProcessorComponent("processor: "+processorE.getAttributeValue(Resources.get("NAME")));
			this.impComponent.put(processorE.getAttributeValue(Resources.get("NAME")), label);
			this.add(label,c);
		}*/

/*		List<Element> expList = JDomUtils.searchElementList(treeNode.getNode(), Resources.get("EXPORTABLE"), null);
		List<Element> exportableList = JDomUtils.getCurrentConfigElements(expList);
		for(Element exportableE: exportableList) {
			y++;
			c.gridy = y;
			ExportableComponent label = new ExportableComponent("exportable: "+exportableE.getAttributeValue(Resources.get("NAME")));
			this.expComponent.put(exportableE.getAttributeValue(Resources.get("NAME")), label);
			this.add(label,c);				
		}*/

	}
	
	public JLabel getImpComponent(String name) {
		return impComponent.get(name);
	}
	
	public JLabel getExpComponent(String name) {
		return expComponent.get(name);
	}
	
	public Map<String, JLabel> getImpComponents() {
		return this.impComponent;
	}
	
	public Map<String, JLabel> getExpComponents() {
		return this.expComponent;
	}
	
	
/*	public Set<LinkComponent> getLinks(LinkTableComponent ltComp) {
		boolean linked = false;
		Set<LinkComponent> linkSet = new HashSet<LinkComponent>();
		for(String linkName: ltComp.getExpComponents().keySet()) {
			if(this.impComponent.containsKey(linkName)) {
				linked = true;
				LinkComponent lc = new LinkComponent(this.impComponent.get(linkName),ltComp.getExpComponent(linkName));
				linkSet.add(lc);
				break;
			}				
		}
		
		for(String linkName: ltComp.getImpComponents().keySet()) {
			if(this.expComponent.containsKey(linkName)) {
				linked = true;
				LinkComponent lc = new LinkComponent(ltComp.getImpComponent(linkName), this.expComponent.get(linkName));
				linkSet.add(lc);
			}
		}
		
		if(linked)
			return linkSet;
		else
			return null;
	}*/
	
}