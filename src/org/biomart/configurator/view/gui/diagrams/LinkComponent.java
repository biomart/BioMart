package org.biomart.configurator.view.gui.diagrams;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import javax.swing.JLabel;
import javax.swing.JPanel;



public class LinkComponent extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel importable;
	private JLabel exportable;
	private final Color defaultColor = Color.RED;
	private final int pad = 5;
	private final int curvePad = 20;
		
	public LinkComponent(JLabel imp, JLabel exp) {
		//schema is not used anymore
//		McViewSchema schema = (McViewSchema)McViews.getInstance().getView(IdwViewType.SCHEMA);
//		this.setBounds(schema.getBounds());
		this.importable = imp;
		this.exportable = exp;
	}
	

	public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        //check who is on the left?
        Container c1 = this.importable.getParent();
        Container c2 = this.exportable.getParent();
        int x1, x2, y1, y2;
        
        if(c1.getLocation().x > c2.getLocation().x) {
        	//exportable is on the left
        	x1 = c2.getLocation().x + c2.getWidth();
        	y1 = c2.getLocation().y + this.exportable.getHeight()/2 + this.exportable.getLocation().y;
        	x2 = c1.getLocation().x;
        	y2 = c1.getLocation().y + this.importable.getHeight()/2 + this.importable.getLocation().y;
        } else if(c1.getLocation().x < c2.getLocation().x) {
        	//importable is on the left
        	x1 = c1.getLocation().x + c1.getWidth();
        	y1 = c1.getLocation().y + this.importable.getHeight()/2 + this.importable.getLocation().y;
        	x2 = c2.getLocation().x;
        	y2 = c2.getLocation().y + this.exportable.getHeight()/2 + this.exportable.getLocation().y;
        	
        } else {
        	x1 = c1.getLocation().x + c1.getWidth();
        	y1 = c1.getLocation().y + this.importable.getHeight()/2 + this.importable.getLocation().y;
        	x2 = c2.getLocation().x + c2.getWidth();
        	y2 = c2.getLocation().y + this.exportable.getHeight()/2 + this.exportable.getLocation().y;
        }
		final GeneralPath path = new GeneralPath(
				GeneralPath.WIND_EVEN_ODD, 4);
		path.moveTo(x1, y1);
		path.lineTo(x1+pad, y1);
		path.quadTo(x1+curvePad,y1+curvePad,x2-pad,y2);
		path.lineTo(x2, y2);
        g2.setColor(defaultColor);
        g2.draw(path);

	}
	
	@Override
	public int hashCode() {
	   final int PRIME = 31;
	   return PRIME + this.importable.hashCode() + this.exportable.hashCode();
	}
	/**
	 * true if both importable and exportable are equals
	 */
	@Override
	public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if(!(obj instanceof LinkComponent))
        	return false;
        return ((LinkComponent)obj).importable.equals(this.importable) &&
        	((LinkComponent)obj).exportable.equals(this.exportable);
        	
	}
}