package org.biomart.configurator.view.gui.dialogs;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.McEventListener;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.view.gui.diagrams.SchemaDiagram;
import org.biomart.configurator.view.gui.diagrams.TargetDiagram;
import org.biomart.objects.objects.Mart;

public class SchemaDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Mart mart;
	private JTabbedPane tab;
	
	public SchemaDialog(Mart mart) {
		this.mart = mart;
		init();
		this.setModal(true);
		this.pack();
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    this.setSize(new Dimension(screenSize.width-100,screenSize.height - 100));
		this.setLocationRelativeTo(null);
		this.setVisible(true);	
	}
	
	private void init() {
		tab = new JTabbedPane();
		//swap source tab and target tab
		if(mart.hasSource()) {
			SchemaDiagram sd = new SchemaDiagram(this, this.mart);
			JScrollPane ssp = new JScrollPane(sd);
			tab.addTab("source", ssp);
		}
		TargetDiagram td = new TargetDiagram(this,this.mart);
		JScrollPane sp = new JScrollPane(td);
		tab.addTab("target", sp);
		
		this.add(tab);
		
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
        		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_SOURCEDIAGRAM.toString(), this);
        		McEventBus.getInstance().removeListener(McEventProperty.REFRESH_TARGETDIAGRAM.toString(), this);
        		McEventBus.getInstance().removeListener(McEventProperty.REBUILD_MART.toString(), this);
            }
          });

		McEventBus.getInstance().addListener(McEventProperty.REFRESH_SOURCEDIAGRAM.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REFRESH_TARGETDIAGRAM.toString(), this);
		McEventBus.getInstance().addListener(McEventProperty.REBUILD_MART.toString(), this);
	}
	
	private SchemaDiagram getSourceDiagram() {
		if(tab.getTabCount()<2)
			return null;
		JScrollPane ssp = (JScrollPane)tab.getComponent(1);
		return (SchemaDiagram)ssp.getViewport().getComponent(0);
	}
	
	private TargetDiagram getTargetDiagram() {
		JScrollPane tsp = null;
		if(tab.getTabCount()<2)
			tsp = (JScrollPane)tab.getComponent(0);
		else
			tsp = (JScrollPane)tab.getComponent(1);
		return (TargetDiagram)tsp.getViewport().getComponent(0);
	}
	
	@McEventListener
	public void update(McEvent<?> event) {
		String property = event.getProperty();
		if(property.equals(McEventProperty.REFRESH_SOURCEDIAGRAM.toString())) {
			
		} else if(property.equals(McEventProperty.REFRESH_TARGETDIAGRAM.toString())) {
			TargetDiagram td = this.getTargetDiagram();
			td.recalculateDiagram();
		} else if(property.equals(McEventProperty.REBUILD_MART.toString())) {
			Mart mart = (Mart)event.getSource();
			MartController.getInstance().rebuildMartFromSource(mart);
			//refresh target diagram
			TargetDiagram td = this.getTargetDiagram();
			td.recalculateDiagram();
			//refresh GUI icons
			Validation.validate(mart,false);
		}
	}
}