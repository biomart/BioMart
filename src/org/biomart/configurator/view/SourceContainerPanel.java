package org.biomart.configurator.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.objects.SourceContainer;

public class SourceContainerPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SourceContainer sourceContainer;
	public static Color lightblue = new Color(135, 206, 250);
	private AddSourceButton gbutton;
	
	public SourceContainerPanel(SourceContainer sc, boolean expanded) {
		this.sourceContainer = sc;
		init(sc, expanded);
	}
	
	private void init(SourceContainer value, boolean expanded) {
    	BorderLayout layout = new BorderLayout();
    	this.setLayout(layout);
    	JPanel westPanel = new JPanel();
    	Icon icon = null;
    	if(expanded) 
    		icon = McUtils.createImageIcon("images/expanded.gif");
    	else
    		icon = McUtils.createImageIcon("images/collapsed.gif");
    	JLabel label = new JLabel(value.getName(),icon, JLabel.CENTER);
    	westPanel.add(label);
    	this.add(westPanel,BorderLayout.WEST);
    	this.setBackground(lightblue);
    	westPanel.setBackground(lightblue);
    	if(value.isGrouped()) {
    		JToolBar tb = new JToolBar();
    		tb.setFloatable(false);
    		gbutton = new AddSourceButton("G");
    		tb.add(gbutton);
    		gbutton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("aaa");					
				}
    			
    		});
    		tb.setBackground(lightblue);
    		this.add(tb,BorderLayout.EAST);
    	}
    	this.setBorder(BorderFactory.createLineBorder(Color.BLACK));

	}
	
	
	@Override 
	public void processMouseEvent(final MouseEvent evt) {
		for(Component c: this.getComponents()) {
			if(c instanceof JToolBar) {
				for(Component subc: ((JToolBar) c).getComponents())
					((AddSourceButton)subc).processMouseEvent(evt);
			}
		}
	}
	
	public void handleClick(MouseEvent evt) {
		Component c = this.getComponentAt(evt.getX(),evt.getY());
		if(gbutton.getBounds().contains(evt.getX(),evt.getY())) {
			gbutton.doClick();
		}
	}


}