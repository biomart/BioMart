package org.biomart.configurator.wizard.addsource;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.controller.ObjectController;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.FileLinkObject;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.connection.URLConnection;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.wizard.WizardPanel;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


public class ASMartsPanel extends WizardPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String IDENTIFIER = "AS_MARTS";
	private JCheckBox groupCB;
	private JCheckBox rebuildlinkCB;
	private JCheckBox partitionCB;
	private MartMetaPanel martMetaPanel;


	public ASMartsPanel() {
		init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		JPanel topPanel = new JPanel();
		martMetaPanel = new MartMetaPanel(this);
		
		this.groupCB = new JCheckBox("group");
		this.rebuildlinkCB = new JCheckBox("rebuild link");
		this.partitionCB = new JCheckBox("partition");
		topPanel.add(groupCB);
		topPanel.add(rebuildlinkCB);
		topPanel.add(partitionCB);				
		
		this.add(topPanel,BorderLayout.SOUTH);
		this.add(martMetaPanel,BorderLayout.CENTER);
	}
	
	@Override
	public void saveWizardState() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizardStateObject();
		result.getDlinkInfo().setRebuildLink(this.rebuildlinkCB.isSelected());
		result.getDlinkInfo().setBCPartitioned(this.partitionCB.isSelected());
		DataLinkType type = result.getDlinkInfo().getDataLinkType();
		switch(type) {
		case URL:
			this.saveWizardStateForURL(result);
			break;
		case FILE:
			this.saveWizardStateForFile(result);
			break;
		default:
			this.saveWizardStateForDB(result);
		}
	}
	
	private void saveWizardStateForURL(AddSourceWizardObject object) {
		UrlLinkObject urlLinkObject = object.getDlinkInfo().getUrlLinkObject();
		object.getDlinkInfo().setSourceGrouped(this.groupCB.isSelected());
		if(urlLinkObject.isVersion8()) {
			urlLinkObject.setMpList(martMetaPanel.get08MartList(false));
		} else {
			urlLinkObject.setDsInfoMap(this.martMetaPanel.get07MartList(false));
		}	
		//save the profile
		if(object.getProfileName()!=null) {
			Document doc = object.getProfile();
			Element root = doc.getRootElement();
			Element urlElement = root.getChild(DataLinkType.URL.name());
			if(urlElement==null) {
				urlElement = new Element(DataLinkType.URL.name());
				root.addContent(urlElement);
			}
			Element profileElement = urlElement.getChild(object.getProfileName());
			if(profileElement == null) {
				profileElement = new Element(object.getProfileName());
				urlElement.addContent(0, profileElement);
			}
			profileElement.setAttribute("protocol", urlLinkObject.getProtocol());
			profileElement.setAttribute("host", urlLinkObject.getHost());
			profileElement.setAttribute("port", urlLinkObject.getPort());
			profileElement.setAttribute("path", urlLinkObject.getPath());
			profileElement.setAttribute("user", urlLinkObject.getUserName());
			profileElement.setAttribute("password", urlLinkObject.getPassword());
			profileElement.setAttribute("key", urlLinkObject.getKeys());
			profileElement.setAttribute("version", urlLinkObject.isVersion8()?"0.8":"0.7");
			
			if(urlElement.getContentSize()>10) {
				urlElement.removeContent(10);
			}
			//save doc
			File file = new File(Settings.classCacheDir,"cacheXml");
	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	     	try {
	    		FileOutputStream fos = new FileOutputStream(file);
	    		outputter.output(doc, fos);
	    		fos.close();
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		JOptionPane.showMessageDialog(null, Resources.get("SAVEXMLERROR"));
	    	}   	

		}
	}
	
	private void saveWizardStateForFile(AddSourceWizardObject object) {
		FileLinkObject fileLinkObject = object.getDlinkInfo().getFileLinkObject();
		fileLinkObject.setDsInfoMap(this.martMetaPanel.get07MartList(false));
		//save the profile
		if(object.getProfileName()!=null) {
			Document doc = object.getProfile();
			Element root = doc.getRootElement();
			Element urlElement = root.getChild(DataLinkType.FILE.name());
			if(urlElement==null) {
				urlElement = new Element(DataLinkType.FILE.name());
				root.addContent(urlElement);
			}
			Element profileElement = urlElement.getChild(object.getProfileName());
			if(profileElement == null) {
				profileElement = new Element(object.getProfileName());
				urlElement.addContent(0, profileElement);
			}
			profileElement.setAttribute("file", fileLinkObject.getFileName());

			
			if(urlElement.getContentSize()>10) {
				urlElement.removeContent(10);
			}
			//save doc
			File file = new File(Settings.classCacheDir,"cacheXml");
	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	     	try {
	    		FileOutputStream fos = new FileOutputStream(file);
	    		outputter.output(doc, fos);
	    		fos.close();
	    		MartController.getInstance().setChanged(false);
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		JOptionPane.showMessageDialog(null, Resources.get("SAVEXMLERROR"));
	    	} 
		}

	}

	private void saveWizardStateForDB(AddSourceWizardObject object) {
		JdbcLinkObject jdbcLinkObject = object.getDlinkInfo().getJdbcLinkObject();
		jdbcLinkObject.setDsInfoMap(this.martMetaPanel.getMartListForDB(false));
		//save the profile
		if(object.getProfileName()!=null) {
			Document doc = object.getProfile();
			Element root = doc.getRootElement();
			Element urlElement = root.getChild(DataLinkType.RDBMS.name());
			if(urlElement==null) {
				urlElement = new Element(DataLinkType.RDBMS.name());
				root.addContent(urlElement);
			}
			Element profileElement = urlElement.getChild(object.getProfileName());
			if(profileElement == null) {
				profileElement = new Element(object.getProfileName());
				urlElement.addContent(0, profileElement);
			}
			profileElement.setAttribute("protocol", jdbcLinkObject.getJdbcType().name());
			profileElement.setAttribute("myisam", Boolean.toString(jdbcLinkObject.isKeyGuessing()));
			profileElement.setAttribute("host", jdbcLinkObject.getHost());
			profileElement.setAttribute("port", jdbcLinkObject.getPort());
			profileElement.setAttribute("user", jdbcLinkObject.getUserName());
			profileElement.setAttribute("password", jdbcLinkObject.getPassword());
			profileElement.setAttribute("database", jdbcLinkObject.getDatabaseName());
			profileElement.setAttribute("source", Boolean.toString(object.getDlinkInfo().getDataLinkType() == DataLinkType.SOURCE));
			
			if(urlElement.getContentSize()>10) {
				urlElement.removeContent(10);
			}
			//save doc
			File file = new File(Settings.classCacheDir,"cacheXml");
	    	XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
	     	try {
	    		FileOutputStream fos = new FileOutputStream(file);
	    		outputter.output(doc, fos);
	    		fos.close();
	    		MartController.getInstance().setChanged(false);
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		JOptionPane.showMessageDialog(null, Resources.get("SAVEXMLERROR"));
	    	} 
		}

	}
	
	@Override
	public boolean validateWizard() {		
		return this.martMetaPanel.hasUserSelectedItems();
	}

	@Override
	public Object getNextPanelId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getBackPanelId() {
		AddSourceWizardObject result = (AddSourceWizardObject)this.getWizard().getModel().getWizardResultObject();
		Object object = null;
		DataLinkType type = result.getDlinkInfo().getDataLinkType();
		switch(type) {
		case URL:
			object = ASURLPanel.IDENTIFIER;
			break;
		case FILE:
			object = ASFilePanel.IDENTIFIER;
			break;
		default:
			object = ASDBPanel.IDENTIFIER;
		}
		return object;
	}

	
	@Override
	public void aboutToDisplayPanel(boolean next) {
		//use progressbar
		McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.TRUE);

		final SwingWorker<List<MartInVirtualSchema>,List<MartInVirtualSchema>> worker = 
			new SwingWorker<List<MartInVirtualSchema>,List<MartInVirtualSchema>>() {
			@Override
			protected List<MartInVirtualSchema> doInBackground() throws Exception {
				try {

					AddSourceWizardObject object = (AddSourceWizardObject)ASMartsPanel.this.getWizard().getModel().getWizardResultObject();
					List<MartInVirtualSchema> marts = ASMartsPanel.this.getMarts(object.getDlinkInfo());
					return marts;
				} catch (final Throwable t) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							StackTrace.showStackTrace(t);
						}
					});
				}finally {
					McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.FALSE);		
				}
				return null;

			}
			
			@Override
			protected void done() {
				try {
					List<MartInVirtualSchema> marts = get();
					ASMartsPanel.this.martMetaPanel.updateList(marts);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				McEventBus.getInstance().fire(McEventProperty.STATUSBAR_SHOW.toString(), Boolean.FALSE);
			}
		};
		
		worker.execute();
		//progressMonitor.start("processing ...");
	}
	
	private List<MartInVirtualSchema> getMarts(DataLinkInfo dlinkInfo) {
		List<MartInVirtualSchema> result = null;
		DataLinkType type = dlinkInfo.getDataLinkType();
		switch(type) {
		case URL:
			if(dlinkInfo.getUrlLinkObject().isVersion8())
				result = this.getMartsFromUrl08();
			else
				result = this.getMartsFromUrl07(dlinkInfo.getUrlLinkObject());
			break;
		case FILE:
			result = this.getMartsFromFile07(dlinkInfo.getFileLinkObject());
			break;
		default:
			result = this.getMartsFromDB(dlinkInfo.getJdbcLinkObject());	
		}		
		return result;
	}
	
	private List<MartInVirtualSchema> getMartsFromUrl07(UrlLinkObject urlLink) {
		String url = urlLink.getFullHost()+":"+urlLink.getPort()+urlLink.getPath();
		return URLConnection.getInstance().getMartsFromURL(url);
	}
	
	private List<MartInVirtualSchema> getMartsFromUrl08() {
		return null;
	}
	
	private List<MartInVirtualSchema> getMartsFromDB(JdbcLinkObject jdbcLink) {	
		return this.martMetaPanel.getMartsForDB(jdbcLink);
	}
	
	private List<MartInVirtualSchema> getMartsFromFile07(FileLinkObject fileLink) {
		File file = new File(fileLink.getFileName());
		ObjectController oc = new ObjectController();
		List<MartInVirtualSchema> martList = null;
		try {
			martList = oc.getURLMartFromFile(file);
		} catch (JDOMException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Not a valid BioMart 0.7 registry file");
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Not a valid BioMart 0.7 registry file");
		}
		return martList;
	}

	@Override
	public void aboutToHidePanel(boolean next) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void backClear() {
		this.martMetaPanel.clear();
		
	}
	

	
}