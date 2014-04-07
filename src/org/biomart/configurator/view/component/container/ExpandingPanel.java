package org.biomart.configurator.view.component.container;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;


public class ExpandingPanel extends JPanel implements MouseListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private void togglePanelVisibility(ActionPanel ap)  {  
       if(this.isShowing())  
           this.setVisible(false);  
       else  
           this.setVisible(true);  
       ap.getParent().validate();  
	}  


	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
	public void mousePressed(MouseEvent e) {
       ActionPanel ap = (ActionPanel)e.getSource();  
       if(ap.getMouseArea().contains(e.getPoint())) {  
           ap.toggleSelection();  
           togglePanelVisibility(ap);  
       }  		
	}
	
	public void mouseReleased(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
}

}