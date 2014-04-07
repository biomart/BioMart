package org.biomart.configurator.view.idwViews;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.biomart.configurator.model.McModel;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.McView;
import org.biomart.configurator.view.component.MartComponent;
import org.biomart.configurator.view.component.container.ContainerComponent;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.SourceContainer;
import org.biomart.objects.objects.SourceContainers;

public class McViewSourceGroup extends McView {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SourceContainers scs;
	private MartComponent lastpoint;

	public McViewSourceGroup(String title, Icon icon, Component component,
			McModel model, IdwViewType type) {
		super(title, icon, component, model, type);	
		this.createMainPanelLayer();
	}
	
	private void createMainPanelLayer() {
		JPanel panel = new JPanel(new BorderLayout());
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		JScrollPane sp = new JScrollPane(mainPanel);
		panel.add(sp,BorderLayout.CENTER);
        this.setComponent(panel);
	}
	
	public void showSource(MartRegistry registry) {
		this.scs = registry.getSourcecontainers();
		this.refreshGui();
	}
		
	public void addGroup() {
		String groupName = JOptionPane.showInputDialog("Source container name");
		if(!McUtils.isStringEmpty(groupName)) {
			//check groupname
			if(this.scs.getSourceContainerByName(groupName)!=null) {
				JOptionPane.showMessageDialog(this, "Source container name conflict");
				return;
			}
			SourceContainer sc = new SourceContainer(groupName);
			this.scs.addSourceContainer(sc);
			this.refreshGui();
		}
	}
	
	public void deleteGroup(String name) {
		this.scs.removeSourceContainer(name);
		this.refreshGui();
	}
	
	public boolean groupCanDelete(String name) {
/*		if(groups.size()==1)
			return false;
		else if(groups.get(name).size()>0)
			return false;
		return true;*/
		return false;
	}
	
	public void refreshGui() {
		List<Mart> selMarts = this.getSelectedMarts();
		JPanel panel = this.getMainPanel();
		for(Component c : panel.getComponents()){
			if(c instanceof ContainerComponent){
				ContainerComponent cc = (ContainerComponent)c;
				SourceContainer sc = cc.getSourceContainer();
				sc.setExpanded(cc.getExpandingArea().isVisible());
			}
		}
		panel.removeAll();
		panel.repaint();
        GridBagConstraints gbc = new GridBagConstraints();  
        gbc.weightx = 1.0; 
        gbc.anchor = GridBagConstraints.PAGE_START; 
        gbc.fill = GridBagConstraints.HORIZONTAL;  
        gbc.gridwidth = GridBagConstraints.REMAINDER;  

		for(SourceContainer sc: this.scs.getSourceContainerList()) {
			ContainerComponent cc = new ContainerComponent(sc,sc.isExpanded(),sc.isGrouped());
			panel.add(cc,gbc);
			for(Mart mart: sc.getMartList()) {
				MartComponent mc = new MartComponent(mart);
				cc.addComponent(mc);
			}
		}

        //JLabel padding = new JLabel();  
        gbc.weighty = 1.0;  
        //panel.add(padding, gbc); 
		JLabel padding = new JLabel();
		gbc.fill = GridBagConstraints.NONE;
		panel.add(padding, gbc);
		
		panel.revalidate();
		
		this.setSelectedMarts(selMarts);
	}
	
	
	public void clean() {
		this.scs = null;
		this.lastpoint = null;
		this.getMainPanel().removeAll();
		this.getMainPanel().repaint();
	}
	
	private List<MartComponent> getAllMartComponents() {
		List<MartComponent> result = new ArrayList<MartComponent>();
		JPanel topPanel = this.getMainPanel();
		Component[] cs = topPanel.getComponents();
		for(Component c: cs) {
			if(c instanceof ContainerComponent) {
				ContainerComponent cc = (ContainerComponent)c;
				JPanel expandedPanel = (JPanel)cc.getExpandingArea();
				Component[] subcs = expandedPanel.getComponents();
				for(Component subc: subcs) {
					if(subc instanceof MartComponent) {
						result.add((MartComponent)subc);
					}
				}
			}
		}
		return result;
	}
	
	public void unselectOthers(MartComponent myself) {
		for(MartComponent mc : this.getAllMartComponents()) {
			if(mc.equals(myself))
				continue;
			mc.setSelected(false);
			mc.setBorder(BorderFactory.createLineBorder(mc.getBackground()));
		}
	}
	
	public MartComponent getComponentByMart(Mart mart) {
		for(MartComponent mc: this.getAllMartComponents()) {
			if(mc.getMart().equals(mart)) {
				return mc;
			}
		}
		return null;
	}
	

	public void setHighlight(Mart mart, Color color) {
		MartComponent mc = this.getComponentByMart(mart);
		if(mc!=null)
			mc.setBackground(color);
	}
	
	public void renameGroup(String oldName, String newName) {
		SourceContainer sc = this.scs.getSourceContainerByName(oldName);
		if(sc!=null)
			sc.setName(newName);
	}

	// check if have source
	public boolean hasSource() {
		return !this.getAllMartComponents().isEmpty();
	}

	/*
	 * only two components, one is toolbar, another is jscrollpane
	 */
	public JPanel getMainPanel() {
		JPanel component = (JPanel)this.getComponent();
		for(Component c: component.getComponents()) {
			if(c instanceof JScrollPane) {
				JScrollPane sp = (JScrollPane)c;
				return (JPanel)sp.getViewport().getComponent(0);
			}
		}
		return null;
	}

	public List<Mart> getSelectedMarts() {
		List<Mart> result = new ArrayList<Mart>();
		for(MartComponent mc: this.getSelectedComponents()) {
			result.add(mc.getMart());
		}
		return result;
	}
	
	public void setSelectedMarts(List<Mart> marts) {
		for(MartComponent mc: this.getAllMartComponents()) {
			if(marts.contains(mc.getMart())) {
				mc.setSelected(true);
				mc.setBorder(BorderFactory.createLineBorder(Color.RED));
			}
		}
	}
	
	public List<MartComponent> getSelectedComponents() {
		List<MartComponent> result = new ArrayList<MartComponent>();
		for(MartComponent mc: this.getAllMartComponents()) {
			if(mc.isSelected())
				result.add(mc);
		}
		return result;
	}

	public void setLastpoint(MartComponent lastpoint) {
		this.lastpoint = lastpoint;
	}

	public MartComponent getLastpoint() {
		return lastpoint;
	}
	
	public List<MartComponent> getComponentsBetween(MartComponent mc0, MartComponent mc1) {
		List<MartComponent> result = new ArrayList<MartComponent>();
		List<MartComponent> all = this.getAllMartComponents();
		int i0 = all.indexOf(mc0);
		int i1 = all.indexOf(mc1);
		int low = i0>i1?i1:i0;
		int high = i0>i1?i0:i1;

		for(int i=0; i<all.size(); i++) {
			if(i>=low && i<=high)
				result.add(all.get(i));
		}
		return result;		
	}

	public MartComponent findMartComponent() {
		Mart mart = this.askUserForMart();
		if(mart!=null) {
			MartComponent mc = this.getComponentByMart(mart);
			return mc;
		}
		return null;

	}
	
	public void scrollToComponent(MartComponent mc) {
		JPanel component = (JPanel)this.getComponent();
		for(Component c: component.getComponents()) {
			if(c instanceof JScrollPane) {
				JScrollPane sp = (JScrollPane)c;
				sp.getViewport().setViewPosition(mc.getLocation());
			}
		}
		
	}
	
	private Mart askUserForMart() {
		final List<String> martnames = new ArrayList<String>();
		MartRegistry registry = (MartRegistry)this.scs.getParent();
		for (final Mart mart: registry.getMartList() ){
			martnames.add(mart.getName());
		}
		Collections.sort(martnames);

		final JComboBox martChoice = new JComboBox();
		for(String martname: martnames) {
			martChoice.addItem(martname);
		}
		
		JOptionPane.showMessageDialog(this, martChoice, "find mart", JOptionPane.QUESTION_MESSAGE, null);

		// Return the choice.
		return registry.getMartByName((String)martChoice.getSelectedItem());

	}

	public void unHighlightAllComponent() {
		for(MartComponent mc: this.getAllMartComponents()) {
			mc.setBackground(mc.getParent().getBackground());
		}
	}

	public void collapseall() {
		JPanel panel = this.getMainPanel();
		for(Component c : panel.getComponents()){
			if(c instanceof ContainerComponent){
				ContainerComponent cc = (ContainerComponent)c;
				cc.getExpandingArea().setVisible(false);
			}
		}
		this.refreshGui();
	}
}