package org.biomart.configurator.view.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.biomart.common.utils.McEvent;
import org.biomart.common.utils.McEventListener;
import org.biomart.configurator.utils.type.McEventProperty;

public class JStatusBar extends JPanel {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    /**
     * The font of the components in the status bar
     */
    private Font statusFont = new Font( "Arial", Font.PLAIN, 12 );

    /**
     * The label that contains the main text of the status bar
     */
    private JLabel textLabel;
    private JProgressBar progressbar;


    /**
     * Creates a new JStatusBar. If you use this constructor, be sure to eventually call init() to configure
     * the status bar components.
     */
    public JStatusBar() {
    	init();
    }


    /**
     * Initializes the JStatusBar with the specified main component and list of secondary components (in
     * which the secondaryComponents list may be null if there are no secondary components)
     * 
     * @param textLabel            The main component, which is presented on the left side of the 
     *                                 status bar and occupies all unused space
     * @param secondaryComponents    A list of secondary components which are stored and accessed via
     *                                 a 0-indexed list through the getSecondaryComponent() method
     */
    public void init() {
        // Save the main component
        this.textLabel = new JLabel();
        this.progressbar = new JProgressBar();
        this.progressbar.setIndeterminate(true);
        // Configure the JStatusBar to use a BorderLayout
        setLayout(new BorderLayout() );
        // The main component goes in the center region, the others go to the right
        textLabel.setFont( statusFont );
        add(buildPanel( textLabel ), BorderLayout.CENTER );
        // Add the new JPanel to the EAST of the outer panel 
        this.add( buildPanel(progressbar), BorderLayout.EAST );   
        this.showProgressBar(false);
    }

    /**
     * Returns a reference to the main component of the status bar
     * 
     * @return            A reference to the main component
     */
    public JLabel getMainComponent() {
        return textLabel;
    }


    /**
     * Builds a status panel container, which is indicated by a lowered bevel border
     * 
     * @param            The component to add to the newly constructed panel
     * @return            A JPanel that is left justified with a lowered bevel border
     */
	  private JPanel buildPanel( JComponent component ) {
		      // Create a left-justified flow panel
		    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		    // Set the border to look de-pressed
		    panel.setBorder(BorderFactory.createEtchedBorder());
		
		    // Add our component to the panel
		    panel.add( component );
		
		    // Return the panel
		    return panel;
	  }
	  
	  private void showProgressBar(boolean b) {
		  this.progressbar.setVisible(b);
		  this.revalidate();
	  }
	  
	  private void setText(String text) {
		  this.textLabel.setText(text);
		  this.revalidate();
	  }

	@McEventListener
	public void update(McEvent<?> event) {
		String property = event.getProperty();
		if(property.equals(McEventProperty.STATUSBAR_SHOW.toString())) {
			Boolean b = (Boolean)event.getSource();
			this.showProgressBar(b);
		}else if(property.equals(McEventProperty.STATUSBAR_UPDATE.toString())) {
			String text = (String)event.getSource();
			this.setText(text);
		}
	}
}