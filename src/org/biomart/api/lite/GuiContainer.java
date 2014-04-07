package org.biomart.api.lite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.biomart.api.Jsoml;
import org.biomart.common.exceptions.FunctionalException;
import org.biomart.objects.enums.EntryLayout;
import org.biomart.objects.enums.GuiType;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;

@XmlRootElement(name="guiContainer")
@JsonPropertyOrder({"name", "displayName", "description", "guiType", "isHidden"})
public class GuiContainer extends LiteMartConfiguratorObject implements Serializable {

	private UserGroup user;
	private GuiType selectedGuiType;
    private boolean includeDatasets;
    private String parentDisplayName;

    private List<Mart> marts;

    public GuiContainer() {}
	
	protected GuiContainer(MartConfiguratorObject mcObject, UserGroup user, 
            GuiType selectedGT, boolean includeDatasets) {
        this(mcObject, user, selectedGT, includeDatasets, null);
    }

	protected GuiContainer(MartConfiguratorObject mcObject, UserGroup user, 
            GuiType selectedGT, boolean includeDatasets, String parentDisplayName) {
		super(mcObject);	
		this.user = user;
		this.selectedGuiType = selectedGT;
		this.guiContainerObject = (org.biomart.objects.portal.GuiContainer)mcObject;
        this.includeDatasets = includeDatasets;
        this.parentDisplayName = parentDisplayName;
		marts = new ArrayList<Mart>();
		//check user
		if(this.user != null) {
            String publicDefault = System.getProperty("public", "0");
            List<MartPointer> mpList = this.guiContainerObject.getMartPointerList();
            for(MartPointer mp: mpList) {
                //if the mart is public
                if("1".equals(publicDefault))
                    marts.add(new Mart(mp, this.includeDatasets,this.user));
                else if(mp.isActivatedInUser(this.user))
                    marts.add(new Mart(mp, this.includeDatasets,this.user));
            }
        }
	}

	private static final long serialVersionUID = 1963149805073116845L;
	
	private Integer row = null;
	private Integer col = null;
	private String icon = null;
	private org.biomart.objects.portal.GuiContainer guiContainerObject;

    @JsonIgnore
	public boolean isLeaf() {
		return this.guiContainerObject.isLeaf();
	}

    @JsonIgnore
	public Integer getRow() {
		return row;
	}

    @JsonIgnore
	public Integer getCol() {
		return col;
	}

    @JsonIgnore
	public GuiType getGuiType() {
		return this.guiContainerObject.getGuiType();
	}

    @XmlAttribute(name="guiType")
    @JsonProperty("guiType")
    public String getGuiTypeName() {
        GuiType guitype = this.guiContainerObject.getGuiType();
        if (guitype != null) {
            return guitype.getName();
        }
        return "";
    }

    @JsonIgnore
	public EntryLayout getEntryLayout() {
		return this.guiContainerObject.getEntryLayout();
	}

    @XmlElementWrapper(name="marts")
    @XmlElement(name="mart")
    @JsonProperty("marts")
	public List<Mart> getMartList() {
		return marts;
	}


    @JsonIgnore
	public String getIcon() {
		return icon;
	}

    @XmlElementWrapper(name="guiContainers")
    @XmlElement(name="guiContainer")
    @JsonProperty("guiContainers")
	public List<GuiContainer> getGuiContainerList() {
		List<org.biomart.objects.portal.GuiContainer> gcList = this.guiContainerObject.getGuiContainerList();
		List<GuiContainer> liteGcList = new ArrayList<GuiContainer>();
		for(org.biomart.objects.portal.GuiContainer gc: gcList) {
			if(gc.isActivatedInUser(this.user))
				if(this.selectedGuiType!=null) {
					if(gc.getGuiType() == this.selectedGuiType)
						liteGcList.add(new GuiContainer(gc, this.user,selectedGuiType, this.includeDatasets, this.getDisplayName()));
				}else
					liteGcList.add(new GuiContainer(gc, this.user, null, includeDatasets, this.getDisplayName()));
		}
		return liteGcList;
	}

    @XmlAttribute(name="isHidden")
    @JsonProperty("isHidden")
    public boolean isHidden() {
        return this.guiContainerObject.isHidden();
    }

	@Override
	protected Jsoml generateExchangeFormat(boolean xml) throws FunctionalException {
		Jsoml jsoml = new Jsoml(xml, super.getXMLElementName());
        GuiType l = this.getGuiType();
        EntryLayout el = this.getEntryLayout();
		
		jsoml.setAttribute("name", this.getName());
		jsoml.setAttribute("displayName", this.getDisplayName());
		jsoml.setAttribute("description", this.getDescription());

		jsoml.setAttribute("parentDisplayName", this.parentDisplayName);
		
        jsoml.setAttribute("isHidden", this.isHidden());
		jsoml.setAttribute("isLeaf", this.isLeaf());
		jsoml.setAttribute("row", this.row);
		jsoml.setAttribute("col", this.col);
		jsoml.setAttribute("guiType", l != null ? l.toString() : null);
		jsoml.setAttribute("entryType", el != null ? el.toString() : null);
		jsoml.setAttribute("icon", this.icon);
		if (!this.isLeaf()) {
			for (GuiContainer guiContainer : this.getGuiContainerList()) {
				jsoml.addContent(guiContainer.generateExchangeFormat(xml));				
			}
		} else {
			for (Mart mart : this.getMartList()) {
				jsoml.addContent(mart.generateExchangeFormat(xml));
			}
		}
		
		return jsoml;
	}


}
