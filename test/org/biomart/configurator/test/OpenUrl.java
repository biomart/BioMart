package org.biomart.configurator.test;

import java.io.File;
import java.io.IOException;

public class OpenUrl {
	public static void main(String args[]) {
		//hardcode the url for now
		String path = null;
		String url = "/testdata/report/index.html";
		File file = new File(".");
		try {
			path = "file:///"+file.getCanonicalPath()+url;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if( !java.awt.Desktop.isDesktopSupported() ) {
            System.exit(0);
        }

        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
            System.exit(0);
        }


        try {
            java.net.URI uri = new java.net.URI(path);
            desktop.browse(uri);
        }
        catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }        

	}
}