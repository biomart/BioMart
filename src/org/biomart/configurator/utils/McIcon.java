package org.biomart.configurator.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

public class McIcon {
	  private static final int ICON_SIZE = 8;
	  private static final int HIGHLIGHT_SIZE = 12;

	  public static final Icon VIEW_ICON = new Icon() {
		    public int getIconHeight() {
		      return ICON_SIZE;
		    }

		    public int getIconWidth() {
		      return ICON_SIZE;
		    }

		    public void paintIcon(Component c, Graphics g, int x, int y) {
		      Color oldColor = g.getColor();

		      g.setColor(new Color(70, 70, 70));
		      g.fillRect(x, y, ICON_SIZE, ICON_SIZE);

		      g.setColor(new Color(100, 230, 100));
		      g.fillRect(x + 1, y + 1, ICON_SIZE - 2, ICON_SIZE - 2);

		      g.setColor(oldColor);
		    }
		  };
		  
		  public static final Icon HIGHLIGHT_ICON = new Icon() {
			    public int getIconHeight() {
			      return HIGHLIGHT_SIZE;
			    }

			    public int getIconWidth() {
			      return HIGHLIGHT_SIZE;
			    }

			    public void paintIcon(Component c, Graphics g, int x, int y) {
			      Color oldColor = g.getColor();

			      g.setColor(Color.RED);
			      g.fillRect(x, y, HIGHLIGHT_SIZE, HIGHLIGHT_SIZE);

			      g.setColor(Color.RED);
			      g.fillRect(x + 1, y + 1, HIGHLIGHT_SIZE - 2, HIGHLIGHT_SIZE - 2);

			      g.setColor(oldColor);
			    }
			  };


		  /**
		   * Custom view button icon.
		   */
		  public static final Icon BUTTON_ICON = new Icon() {
		    public int getIconHeight() {
		      return ICON_SIZE;
		    }

		    public int getIconWidth() {
		      return ICON_SIZE;
		    }

		    public void paintIcon(Component c, Graphics g, int x, int y) {
		      Color oldColor = g.getColor();

		      g.setColor(Color.BLACK);
		      g.fillOval(x, y, ICON_SIZE, ICON_SIZE);

		      g.setColor(oldColor);
		    }
		  };

}