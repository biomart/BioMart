package org.biomart.configurator.view.idwViews;


import java.util.HashMap;
import java.util.Map;

import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.view.McView;
import org.ewin.common.util.Log;

import net.infonode.docking.util.DockingUtil;
import net.infonode.docking.util.ViewMap;

/**
 * a singleton class to store all views
 * it is not thread safe
 * FIXME may change later 
 * @author yliang
 *
 */
public class McViews {
	private Map<IdwViewType,McView> mcViews;
	//just to satisfy the example
	private ViewMap viewMap;
	
	private static McViews instance = null;
	
	private McViews() {
		mcViews = new HashMap<IdwViewType, McView>();
		viewMap = new ViewMap();
	}
	
	public static McViews getInstance() {
		if(instance == null)
			instance = new McViews();
		return instance;
	}
	
	public Map<IdwViewType, McView> getAllViews() {
		return instance.mcViews;
	}
	
	public McView getView(IdwViewType type) {
		return instance.mcViews.get(type);
	}
	
	public ViewMap getViewMap() {
		return instance.viewMap;
	}

	
	
	public void addView(McView view) {
		if(view instanceof McViewPortal) {
			instance.mcViews.put(IdwViewType.PORTAL, view);
			instance.viewMap.addView(IdwViewType.PORTAL.ordinal(), view);			
		}else if(view instanceof McViewSourceGroup) {
			instance.mcViews.put(IdwViewType.SOURCEGROUP, view);
			instance.viewMap.addView(IdwViewType.SOURCEGROUP.ordinal(), view);									
		}
		else
			Log.ERROR("error");
	}
	
	public void showView(IdwViewType type) {
		McView view = this.mcViews.get(type);
		if(view.getRootWindow()!=null)
			view.restoreFocus();
		else
			DockingUtil.addWindow(view, McGuiUtils.INSTANCE.getRootWindow());
	}

	public void clean() {
		if(instance.getView(IdwViewType.PORTAL)!=null)
			((McViewPortal)instance.getView(IdwViewType.PORTAL)).clean();
		if(instance.getView(IdwViewType.SOURCEGROUP)!=null)
			((McViewSourceGroup)instance.getView(IdwViewType.SOURCEGROUP)).clean();
		instance.mcViews.clear();
	}
}