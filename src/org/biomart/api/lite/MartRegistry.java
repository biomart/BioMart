package org.biomart.api.lite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.utils.McUtils;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.User;
import org.biomart.objects.portal.UserGroup;
import org.biomart.registry.HttpCentralRegistryProxy;

public class MartRegistry 
extends LiteMartConfiguratorObject implements Serializable
{

	private static final long serialVersionUID = 7098919356811373479L;
	private org.biomart.objects.objects.MartRegistry fullMRObject;
	private GuiContainer rootGuiContainer = null;
	private final UserGroup currentUser;

	
	public MartRegistry(org.biomart.objects.objects.MartRegistry martRegistry, String user, String password)  {
		super(martRegistry);
		this.fullMRObject = martRegistry;
		this.currentUser = McUtils.getUserGroup(this.fullMRObject, user, password);
	}

    public org.biomart.objects.objects.MartRegistry getFullRegistry() {
        return this.fullMRObject;
    }

	public GuiContainer getRootGuiContainer(String guitype, boolean includeDatasets) {
		GuiType gt = GuiType.get(guitype);
		return new GuiContainer(this.fullMRObject.getPortal().getRootGuiContainer(), this.currentUser, gt, includeDatasets);
	}
	public GuiContainer getRootGuiContainer(boolean includeDatasets) {
		return getRootGuiContainer(null, includeDatasets);
	}
	public GuiContainer getRootGuiContainer(String guitype) {
        return getRootGuiContainer(guitype, false);
    }
	public GuiContainer getRootGuiContainer() {
        return getRootGuiContainer(null, false);
    }
	
	public GuiContainer getGuiContainer(String guiContainerName, boolean includeDatasets) {
		GuiContainer guiContainer = null;
        GuiContainer root = this.getRootGuiContainer(includeDatasets);

		if (root!=null) {
			guiContainer = fetchGuiContainerRecursively(root, guiContainerName);
		}
		return guiContainer;
	}
	public GuiContainer getGuiContainer(String guiContainerName) {
        return getGuiContainer(guiContainerName, false);
    }

	private GuiContainer fetchGuiContainerRecursively(GuiContainer guiContainer, String guiContainerName) {	//TODO check
		if (guiContainerName.equals(guiContainer.getName())) {
			return guiContainer;
		} else {
			for (GuiContainer subGuiContainer : guiContainer.getGuiContainerList()) {
				GuiContainer guiContainerTmp =  fetchGuiContainerRecursively(subGuiContainer, guiContainerName);
				if (guiContainerTmp!=null) {
					return guiContainerTmp;
				}
			}
		}
		return null;
	}
	
	
	public Mart getMartByName(String martName) {
		List<Mart> martList = this.getMarts();
		for(Mart mart: martList) {
			if(mart.getName().equals(martName))
				return mart;
		}
		return null;
	}
	public Mart getMartByConfigName(String configName) {
		List<Mart> martList = this.getMarts();
		for(Mart mart: martList) {
			if(mart.getConfigName().equals(configName))
				return mart;
		}
		return null;
	}
	public List<Mart> getMartList() {
		return getMarts();
	}
	public List<Mart> getMartList(String leafGuiContainerName) {
		ArrayList<Mart> martList = new ArrayList<Mart>();
		if (this.rootGuiContainer!=null) {
			fetchMartListRecursively(martList, this.rootGuiContainer, leafGuiContainerName);
		}
		return martList;
	}
	private void fetchMartListRecursively(ArrayList<Mart> martList, GuiContainer guiContainer, String leafGuiContainerName) {
		if (guiContainer.isLeaf() && (leafGuiContainerName==null || leafGuiContainerName.equals(guiContainer.getName()))) {
			martList.addAll(guiContainer.getMartList());
		} else {
			for (GuiContainer subGuiContainer : guiContainer.getGuiContainerList()) {
				fetchMartListRecursively(martList, subGuiContainer, leafGuiContainerName);
			}
		}
	}
	
	/**
	 * assuming that dataset is unique in the registry
	 * this method is based on the following rules: 
	 * 1. all dataset(s) passed in should be in the same mart. 
	 * 2. dataset is unique in the system. 
	 * 3. if there is no lite mart based on the default config, return the first lite mart. 
	 * (no multiple lite marts using the same config).
	 * @return 
	 */
	public Mart getMartByDatasets(Collection<String> datasets) {
		if(datasets == null || datasets.isEmpty())
			return null;
		String ds = datasets.iterator().next();
		List<MartPointer> mpList = this.fullMRObject.getPortal().getRootGuiContainer().getAllMartPointerListResursively();
		MartPointer tmpMp = null;
		Mart tmpMart = null;
		for(MartPointer mp: mpList) {
			if(mp.getMart().getDatasetByName(ds)!=null) {
				//find the default one?
				if(mp.getConfig().isDefaultConfig()) {
					tmpMp = mp;
					break;
				} else {
					if(tmpMp == null) //use the first one
						tmpMp = mp;
				}
			}
		}
		if(tmpMp!=null)
			tmpMart = new Mart(tmpMp,true,this.currentUser);
		return tmpMart;
	}
	
	public Mart getMartPointer(String martPointerName) throws FunctionalException {
		List<Mart> martList = getMartList();
		for (Mart mart : martList) {
			if (mart.getName().equals(martPointerName)) {
				return mart;
			}
		}
		return null;
	}

	@Override
	protected Jsoml generateExchangeFormat(boolean xml)
			throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
		jsoml.setAttribute("name", super.getName());
		jsoml.setAttribute("displayName", super.getDisplayName());
		jsoml.setAttribute("description", super.getDescription());
		return jsoml;
	}

	public List<org.biomart.api.lite.Mart> getMarts() { return getMarts(false); }

	public List<org.biomart.api.lite.Mart> getMarts(boolean includeDatasets) {
		List<org.biomart.api.lite.Mart> martList = new ArrayList<org.biomart.api.lite.Mart>();
		if(this.currentUser == null)
			return martList;

		List<MartPointer> mpList = this.fullMRObject.getPortal().getRootGuiContainer().getAllMartPointerListResursively();

		for(MartPointer mp: mpList) {
			if(mp.isActivatedInUser(this.currentUser)) {
				martList.add(new org.biomart.api.lite.Mart(mp, includeDatasets,this.currentUser));
			}
		}
		return martList;
	}
	
	public String getGroupName() {
		return currentUser.getName();
	}

    public org.biomart.objects.objects.Dataset getDatasetByName(String name, String config) {
        return this.fullMRObject.getDatasetByName(name, config);
    }
}
