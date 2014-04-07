package org.biomart.configurator.view.component.container;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.view.dnd.MartDropTargetListener;
import org.biomart.configurator.view.menu.ContextMenuConstructor;
import org.biomart.objects.objects.SourceContainer;

public class ActionPanel extends JPanel implements MouseListener {  
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final SourceContainer sourceContainer; 
    private Font font;  
    private boolean selected;  
    private BufferedImage open, closed;  
    private Rectangle target;  
    private final int  OFFSET = 30,  PAD    =  5;  
    public static Color lightblue = new Color(135, 206, 250);
     
	ActionPanel(SourceContainer sc, MouseListener ml, boolean selected)  {  
	    this.sourceContainer = sc;  	    
	    this.setBackground(ActionPanel.lightblue);
	    addMouseListener(ml);  
	    font = new Font("sans-serif", Font.PLAIN, 12); 
	    this.selected = selected;  
	    this.setPreferredSize(new Dimension(200,20));  
	    createImages();  
	    setRequestFocusEnabled(true);  
	    this.addMouseListener(this);
	    this.setDropTarget(new DropTarget(ActionPanel.this, new MartDropTargetListener()));
	}  
     
	public void toggleSelection()  {  
		selected = !selected;  
		repaint();  
	}  
	
 
	protected void paintComponent(Graphics g)  {  
		super.paintComponent(g);  
		Graphics2D g2 = (Graphics2D)g;  
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  
                          RenderingHints.VALUE_ANTIALIAS_ON);  
		int h = getHeight();  
		if(selected)  
			g2.drawImage(open, PAD, 0, this);  
		else  
			g2.drawImage(closed, PAD, 0, this);  
		g2.setFont(font);  
		FontRenderContext frc = g2.getFontRenderContext();  
		LineMetrics lm = font.getLineMetrics(this.sourceContainer.getName(), frc);  
		float height = lm.getAscent() + lm.getDescent();  
		float x = OFFSET;  
		float y = (h + height)/2 - lm.getDescent();  
		g2.drawString(this.sourceContainer.getName(), x, y);  
	}  
 
	private void createImages()  {  
		int w = 20;  
		int h = getPreferredSize().height;  
		target = new Rectangle(2, 0, 20, 18);  
		open = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);  
		Graphics2D g2 = open.createGraphics();  
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  
                          RenderingHints.VALUE_ANTIALIAS_ON);  
		g2.setPaint(ActionPanel.lightblue);  
		g2.fillRect(0,0,w,h);  
		int[] x = { 2, w/2, 18 };  
		int[] y = { 4, 15,   4 };  
		Polygon p = new Polygon(x, y, 3);  
		g2.setPaint(Color.green.brighter());  
		g2.fill(p);  
		g2.setPaint(Color.blue.brighter());  
		g2.draw(p);  
		g2.dispose();  
		closed = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);  
		g2 = closed.createGraphics();  
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  
                          RenderingHints.VALUE_ANTIALIAS_ON);  
		g2.setPaint(ActionPanel.lightblue);  
		g2.fillRect(0,0,w,h);  
		x = new int[] { 3, 13,   3 };  
		y = new int[] { 4, h/2, 16 };  
		p = new Polygon(x, y, 3);  
		g2.setPaint(Color.red);  
		g2.fill(p);  
		g2.setPaint(Color.blue.brighter());  
		g2.draw(p);  
		g2.dispose();  
	}  
	
	public Rectangle getMouseArea() {
		return this.target;
	}
	
	public String getTitle() {
		return this.sourceContainer.getName();
	}
	
	public SourceContainer getSourceContainer() {
		return this.sourceContainer;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.isPopupTrigger())
			this.handleMouseClick(e);		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger())
			this.handleMouseClick(e);		
	}
	
	private void handleMouseClick(MouseEvent e) {
		JPopupMenu menu = ContextMenuConstructor.getInstance().getContextMenu(this,"sourcecontainer",false);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

} 