/**
 * 
 */
package org.biomart.configurator.utils;

import java.util.List;

import org.biomart.common.resources.ErrorMessage;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.PartitionColumn;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.Portal;
import org.biomart.processors.Processor;
import org.biomart.processors.ProcessorGroup;


public class Validation {
	private static Validation instance;
	
	private Validation() {};
	
	public static ValidationStatus validate(MartConfiguratorObject mcObj, boolean quick) {
		if(instance == null)
			instance = new Validation();
		
		ValidationStatus vs = ValidationStatus.VALID;
		if(mcObj instanceof MartRegistry) {
			vs = instance.validateMartRegistry((MartRegistry)mcObj,quick);
		} else if(mcObj instanceof Portal) {
			vs = instance.validatePortal((Portal)mcObj,quick);
		} else if(mcObj instanceof GuiContainer) {
			vs = instance.validateGuiContainer((GuiContainer)mcObj,quick);
		} else if(mcObj instanceof Processor) {
			vs = instance.validateProcessor((Processor)mcObj,quick);
		} else if(mcObj instanceof ProcessorGroup) {
			vs = instance.validateProcessorGroup((ProcessorGroup)mcObj,quick);
		} else if(mcObj instanceof MartPointer) {
			vs = instance.validateMartPointer((MartPointer)mcObj,quick);
		}
		else if(mcObj instanceof Mart) {
			vs = instance.validateMart((Mart)mcObj,quick);
		} else if(mcObj instanceof Config) {
			vs = instance.validateConfig((Config)mcObj,quick);
		} else if(mcObj instanceof Link) {
			vs = instance.validateLink((Link)mcObj,quick);
		} else if(mcObj instanceof PartitionTable) {
			vs = instance.validatePartitionTable((PartitionTable)mcObj,quick);
		} else if(mcObj instanceof Relation) {
			vs = instance.validateRelation((Relation)mcObj,quick);
		} else if(mcObj instanceof Column) {
			vs = instance.validateColumn((Column)mcObj,quick);
		} else if(mcObj instanceof DatasetTable) {
			vs = instance.validateDatasetTable((DatasetTable)mcObj,quick);
		} else if(mcObj instanceof Key) {
			vs = instance.validateKey((Key)mcObj,quick);
		}
		else if(mcObj instanceof Container) {
			vs = instance.validateContainer((Container)mcObj,quick);
		}
		else if(mcObj instanceof Attribute){ 
			vs = instance.validateAttribute((Attribute)mcObj);
		} else if(mcObj instanceof Filter) {
			vs = instance.validateFilter((Filter)mcObj);
		}
		return vs;
	}
	
	public static ValidationStatus revalidate(MartConfiguratorObject object) {
		if(instance == null)
			instance = new Validation();

		ValidationStatus oldvs = object.getObjectStatus();
		ValidationStatus newvs = Validation.validate(object,false);
		if(oldvs == newvs)
			return newvs;
		MartConfiguratorObject parent = object.getParent();
		if(newvs == ValidationStatus.INVALID) {
			while(parent!=null) {
				parent.setObjectStatus(ValidationStatus.INVALID);
				parent = parent.getParent();
			}
		} else {
			//need to update the status of parent object			
			if(parent!=null)
				instance.quickValidate(parent);
		}
		return newvs;
	}
	
	private void quickValidate(MartConfiguratorObject object) {
		ValidationStatus oldvs = object.getObjectStatus();
		ValidationStatus newvs = Validation.validate(object,true);
		if(oldvs == newvs)
			return;
		MartConfiguratorObject parent = object.getParent();
		if(newvs == ValidationStatus.INVALID) {
			while(parent!=null) {
				parent.setObjectStatus(ValidationStatus.INVALID);
				parent = parent.getParent();				
			}
		} else {
			if(parent!=null)
				quickValidate(parent);
		}
	}


	private ValidationStatus validateContainer(Container con, boolean quick){
		ValidationStatus validate = ValidationStatus.VALID;
		String errorMessage = null;
		for(Attribute attribute: con.getAttributeList()) {
			validate = quick? attribute.getObjectStatus(): this.validateAttribute(attribute);
			if(validate == ValidationStatus.INVALID) {
				errorMessage = ErrorMessage.get("60001");
				if(quick && validate == ValidationStatus.INVALID) {
					break;
				}
			}
		}
		
		if(!(quick && validate == ValidationStatus.INVALID)) {
			for(Filter filter: con.getFilterList()) {
				validate = quick? filter.getObjectStatus(): this.validateFilter(filter);
				if(validate == ValidationStatus.INVALID) {
					errorMessage = ErrorMessage.get("60001");
					if(quick && validate == ValidationStatus.INVALID) {
						break;
					}
				}
			}
		}
		
		if(!(quick && validate == ValidationStatus.INVALID)) {
			for(Container c: con.getContainerList()) {
				ValidationStatus tmp = quick? c.getObjectStatus(): this.validateContainer(c,quick);
				if(tmp.compareTo(validate)>0) {
					validate = tmp;
					errorMessage = ErrorMessage.get("60001");
					if(quick && validate == ValidationStatus.INVALID) {
						break;
					}
				}
			}
		}

		con.setObjectStatus(validate);
		con.setProperty(XMLElements.ERROR,errorMessage);
		return validate;
	}
	
	private ValidationStatus validateConfig(Config config,boolean quick){
		ValidationStatus validate =  quick? config.getRootContainer().getObjectStatus():
			this.validateContainer(config.getRootContainer(),quick);
		config.setObjectStatus(validate);
		if(validate!=ValidationStatus.VALID)
			config.setProperty(XMLElements.ERROR, ErrorMessage.get("60001"));
		return validate;
	}
		
	private  ValidationStatus validateDatasetTable(DatasetTable datasetTable,boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		for(Column col: datasetTable.getColumnList()) {
			ValidationStatus tmp = quick? col.getObjectStatus(): this.validateColumn((DatasetColumn)col,quick);
			if(tmp.compareTo(result)>0) {
				result = tmp;
				if(quick && result == ValidationStatus.INVALID)
					break;
			}
		}

		if(!(quick && result == ValidationStatus.INVALID))  {
			if(datasetTable.getPrimaryKey()!=null) {
				ValidationStatus tmp = quick? datasetTable.getPrimaryKey().getObjectStatus(): 
					this.validateKey(datasetTable.getPrimaryKey(),quick);
				if(tmp.compareTo(result)>0) 
					result = tmp;
			}
		}
		if(!(quick && result == ValidationStatus.INVALID))  {
			//fk
			for(ForeignKey fk: datasetTable.getForeignKeys()) {
				ValidationStatus tmp = quick? fk.getObjectStatus(): this.validateKey(fk,quick);
				if(tmp.compareTo(result)>0) {
					result = tmp;			
				}
			}
		}
		
		datasetTable.setObjectStatus(result);
		if(result!=ValidationStatus.VALID)
			datasetTable.setProperty(XMLElements.ERROR, ErrorMessage.get("60001"));
		return result;
	}
		
	private ValidationStatus validateKey(Key key,boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		return result;
	}
	
	private ValidationStatus validateLink(Link link, boolean quick){
		String errorMessage = null;
		if(link.getPointedConfig() == null) {
			errorMessage = ErrorMessage.get("10011");
			link.setObjectStatus(ValidationStatus.INVALID);
			link.setProperty(XMLElements.ERROR,errorMessage);
			return link.getObjectStatus();
		}
		//synch pointed dataset, for now just set the name,
				
		String attributes = link.getPropertyValue(XMLElements.ATTRIBUTES);
		String[] _atts = attributes.split(",");
		for(String attStr: _atts) {
			Attribute att = ((Config)link.getParentConfig()).getAttributeByName(attStr,null);
			if(att == null) {
				link.setObjectStatus(ValidationStatus.INVALID);
				link.setProperty(XMLElements.ERROR, "attributes invalid");
				return link.getObjectStatus();
			}
		}
		
		String filters = link.getPropertyValue(XMLElements.FILTERS);
		String[] _filters = filters.split(",");
		for(String filStr: _filters) {
			Filter filter = ((Config)link.getParentConfig()).getFilterByName(filStr, null);
			if(filter == null) {
				link.setObjectStatus(ValidationStatus.INVALID);
				link.setProperty(XMLElements.ERROR, "filters invalid");
				return link.getObjectStatus();				
			}
		}
		link.setObjectStatus(ValidationStatus.VALID);
		return link.getObjectStatus();
	}
	
	private ValidationStatus validateMart(Mart mart, boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		//validate datasettable
		for(DatasetTable dst: mart.getDatasetTables()) {
			ValidationStatus tmp = quick? dst.getObjectStatus(): this.validateDatasetTable(dst,quick);
			if(tmp.compareTo(result)>0) {
				result = tmp;
				if(result == ValidationStatus.INVALID && quick)
					break;
			}
		}
		if(!(quick && result==ValidationStatus.INVALID)) {
			//validate relation
			for(Relation relation: mart.getRelations()) {
				ValidationStatus tmp = quick? relation.getObjectStatus(): this.validateRelation(relation, quick);
				if(tmp.compareTo(result)>0) {
					result = tmp;
					if(result == ValidationStatus.INVALID && quick)
						break;					
				}
			}
		}
		if(!(quick && result==ValidationStatus.INVALID)) {
			//validate configs
			for(Config conf: mart.getConfigList()) {
				ValidationStatus tmp = quick? conf.getObjectStatus(): this.validateConfig(conf,quick);
				if(tmp.compareTo(result)>0) {
					result = tmp;
					if(result == ValidationStatus.INVALID && quick)
						break;
				}
			}
		}
		
		mart.setObjectStatus(result);
		if(result!=ValidationStatus.VALID)
			mart.setProperty(XMLElements.ERROR,ErrorMessage.get("60001"));
		return result;
	}
	
	/**
	 * in a martregistry validation, portal part is indepent with marts part, which means
	 * if a mart is invalid, a martpointer pointing to the mart can be valid.
	 * @param mr
	 * @return
	 */
	private ValidationStatus validateMartRegistry(MartRegistry mr, boolean quick){
		ValidationStatus validate = quick? mr.getPortal().getObjectStatus(): this.validatePortal(mr.getPortal(),quick);
		if(!(quick && validate == ValidationStatus.INVALID)) {
	    	for(Mart mart: mr.getMartList()) {
	    		ValidationStatus tmp = quick? mart.getObjectStatus(): this.validateMart(mart,quick);
	    		if(tmp.compareTo(validate)>0) {
	    			validate = tmp;
	    			if(quick)
	    				break;
	    		}
	    	}
		}
    	
    	mr.setObjectStatus(validate);
    	if(validate!=ValidationStatus.VALID)
    		mr.setProperty(XMLElements.ERROR,ErrorMessage.get("60001"));
    	return validate;
	}
	
	private ValidationStatus validatePartitionTable(PartitionTable pt, boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		int totalrow = pt.getTotalRows();
		for(PartitionColumn pc: pt.getPartitionColumns()) {
			if(pc.getColumnList().size()!=totalrow) {
				result = ValidationStatus.INVALID;
				break;
			}
		}

		pt.setObjectStatus(result);
		return result;
	}
	
	
	private ValidationStatus validateGuiContainer(GuiContainer gc, boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		if(gc.isLeaf()) {
			for(MartPointer mp: gc.getMartPointerList()) {
				ValidationStatus tmp = quick? mp.getObjectStatus(): this.validateMartPointer(mp,quick);
				if(tmp.compareTo(result)>0) {
					result = tmp;
					if(quick && result == ValidationStatus.INVALID) 
						break;
				}
			}			
		}else {
			for(GuiContainer guic: gc.getGuiContainerList()) {
				ValidationStatus tmp = quick? guic.getObjectStatus(): this.validateGuiContainer(guic,quick);
				if(tmp.compareTo(result)>0) {
					result = tmp;
					if(quick && result == ValidationStatus.INVALID) 
						break;
				}
			}
		}
		gc.setObjectStatus(result);
		if(result!=ValidationStatus.VALID)
			gc.setProperty(XMLElements.ERROR, ErrorMessage.get("60001"));

		return result;
	}
	
	private ValidationStatus validateMartPointer(MartPointer mp,boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		String errorMessage = null;
		for(ProcessorGroup pg: mp.getProcessorGroupList()) {
			ValidationStatus tmp = quick? pg.getObjectStatus(): this.validateProcessorGroup(pg,quick);
			if(tmp.compareTo(result)>0) {
				result = tmp;
				errorMessage = ErrorMessage.get("60001");
				if(quick && result == ValidationStatus.INVALID) 
					break;
			}
		}
		
		if(mp.getMart()==null || mp.getConfig() == null) {
			result = ValidationStatus.INVALID;
			//override 60001 
			errorMessage = ErrorMessage.get("10001");
		}		

		
		mp.setObjectStatus(result);
		mp.setProperty(XMLElements.ERROR, errorMessage);
		return result;
	}
	
	private ValidationStatus validatePortal(Portal portal,boolean quick){
		ValidationStatus result = quick? portal.getRootGuiContainer().getObjectStatus():
			this.validateGuiContainer(portal.getRootGuiContainer(),quick);
		portal.setObjectStatus(result);
		if(result!=ValidationStatus.VALID)
			portal.setProperty(XMLElements.ERROR, ErrorMessage.get("60001"));
		return result;
	}
	
	private ValidationStatus validateProcessor(Processor processor,boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		if(processor.getContainer() == null) {
			result = ValidationStatus.INVALID;
		}
		processor.setObjectStatus(result);
		if(result!=ValidationStatus.VALID)
			processor.setProperty(XMLElements.ERROR, ErrorMessage.get("10002"));
		
		return result;
	}
	
	private ValidationStatus validateProcessorGroup(ProcessorGroup pg, boolean quick){
		ValidationStatus result = ValidationStatus.VALID;
		String errorMessage = null;
		for(Processor p: pg.getProcessorList()) {
			ValidationStatus tmp = quick? p.getObjectStatus(): this.validateProcessor(p,quick);
			if(tmp.compareTo(result)>0) {
				result = tmp;
				errorMessage = ErrorMessage.get("60001");
				if(quick && result == ValidationStatus.INVALID)
					break;
			}
		}
		if(pg.getDefaultProcessor() ==  null) {
			result = ValidationStatus.INVALID;
			//override 60001
			errorMessage = ErrorMessage.get("10003");
		}	
		pg.setObjectStatus(result);
		pg.setProperty(XMLElements.ERROR, errorMessage);		
		return result;
	}

	private ValidationStatus validateRelation(Relation r, boolean quick) {
		return ValidationStatus.VALID;
	}
	
	private ValidationStatus validateColumn(Column c,boolean quick) {
		return ValidationStatus.VALID;
	}
	
	private ValidationStatus validateAttribute(Attribute attribute) {
		//check pointer
		//if it is a pointer, the pointedElement should not be null
		if(attribute.isPointer()) {
			if(attribute.getPointedAttribute()!=null && attribute.getPointedAttribute().isPointer()) {
				attribute.setObjectStatus(ValidationStatus.INVALID);
				attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10004"));
				return ValidationStatus.INVALID;				
			}
			if(attribute.getPointedAttribute()== null || this.validateAttribute(attribute.getPointedAttribute())!=ValidationStatus.VALID) {
				attribute.setObjectStatus(ValidationStatus.INVALID);
				attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10004"));
				return ValidationStatus.INVALID;
			}
			if(!McUtils.hasLink(attribute.getParentConfig(), attribute.getPointedConfing())) {
				attribute.setObjectStatus(ValidationStatus.INVALID);
				attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10014"));
				return ValidationStatus.INVALID;
			}
/*			if(!McUtils.isStringEmpty(attr.getPropertyValue(XMLElements.COLUMN)) ||
				(!McUtils.isStringEmpty(attr.getPropertyValue(XMLElements.TABLE)))) {
				result = ValidationStatus.INVALID;
				errorMessage = ErrorMessage.get("10006");
			}*/
		} else if(!attribute.getParentConfig().isMasterConfig() &&
		        attribute.getParentConfig().getMart().getMasterConfig().getAttributeByName(attribute.getName(), null) == null) {
		    attribute.setObjectStatus(ValidationStatus.INVALID);
		    attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10005"));
		    return ValidationStatus.INVALID;

		} else if(attribute.isAttributeList()) {
			//it is valid if one of the attribute in the list is valid
/*			boolean b = false;
			for(Attribute att: attr.getAttributeList()) {
				if(this.validateAttribute(att)==ValidationStatus.VALID)
					b = true;
			}
			if(!b) {
				result = ValidationStatus.INVALID;
				errorMessage = ErrorMessage.get("10007");
			}*/
		} else if(attribute.isPseudoAttribute()) {
			
		} //normal attribute
		else {
			//normal attribute, check table and column
			if(attribute.getDatasetTable() == null || attribute.getDataSetColumn() == null) {
				attribute.setObjectStatus(ValidationStatus.INVALID);
				attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10008"));
				return ValidationStatus.INVALID;
			} else if(attribute.getDataSetColumn().isHidden()) {
				attribute.setObjectStatus(ValidationStatus.INVALID);
				attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10012"));
				return ValidationStatus.INVALID;				
			}
		}
		
		//check for link out url attributes validity
		if(!attribute.getLinkOutUrl().isEmpty()) {
			String linkOutURL = attribute.getLinkOutUrl(); 
			List<Attribute> refAtts = McGuiUtils.INSTANCE.getAttributesFromLinkOutUrl(linkOutURL, attribute.getParentConfig());
			for(Attribute refAtt : refAtts) {
				if(!attribute.getParentConfig().containAttributebyName(refAtt)){
					attribute.setObjectStatus(ValidationStatus.INVALID);
					attribute.setProperty(XMLElements.ERROR, ErrorMessage.get("10013"));
					return ValidationStatus.INVALID;
				}
			}
		}
		attribute.setObjectStatus(ValidationStatus.VALID);
		return ValidationStatus.VALID;
	}
	
	private ValidationStatus validateFilter(Filter filter) {
		ValidationStatus result = ValidationStatus.VALID;
		String errorMessage = null;
		//ignore validation if is hidden
		/*if(this.isHidden())
			return true;*/
		if(filter.isPointer()) {
			if(filter.getPointedFilter()== null || this.validateFilter(filter.getPointedFilter())!=ValidationStatus.VALID) {
				filter.setObjectStatus(ValidationStatus.INVALID);
				filter.setProperty(XMLElements.ERROR, ErrorMessage.get("10004"));
				return ValidationStatus.INVALID;
			}
			if(!McUtils.hasLink(filter.getParentConfig(), filter.getPointedConfing())) {
				filter.setObjectStatus(ValidationStatus.INVALID);
				filter.setProperty(XMLElements.ERROR, ErrorMessage.get("10014"));
				return ValidationStatus.INVALID;
			}
		} else if(!filter.getParentConfig().isMasterConfig() && 
                filter.getParentConfig().getMart().getMasterConfig().getFilterByName(filter.getName(), null) == null) {
            filter.setObjectStatus(ValidationStatus.INVALID);
            filter.setProperty(XMLElements.ERROR, ErrorMessage.get("10005"));
            return ValidationStatus.INVALID;
        } else if(filter.isFilterList()) {
			//it is valid if one of the filter in the list is valid
			boolean b = false;
			for(Filter fil: filter.getFilterList()) {
				if(this.validateFilter(fil)==ValidationStatus.VALID)
					b = true;
			}
			if(!b) {
				filter.setObjectStatus(ValidationStatus.INVALID);
				filter.setProperty(XMLElements.ERROR, ErrorMessage.get("10007"));
				return ValidationStatus.INVALID;
			}
		} else {
			//normal filter
			if(filter.getAttribute() == null || this.validateAttribute(filter.getAttribute())!=ValidationStatus.VALID) {
				filter.setObjectStatus(ValidationStatus.INVALID);
				filter.setProperty(XMLElements.ERROR,ErrorMessage.get("10009"));
				return ValidationStatus.INVALID;
			}
			
			//check for qualifiers
			if(filter.getQualifier() == null){
				filter.setProperty(XMLElements.ERROR,ErrorMessage.get("10010"));
				filter.setObjectStatus(ValidationStatus.INVALID);
				return ValidationStatus.INVALID;
			}

		}				
		filter.setObjectStatus(ValidationStatus.VALID);
		return ValidationStatus.VALID;
	}
	
	
}
