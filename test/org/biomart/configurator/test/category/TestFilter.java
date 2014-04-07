/**
 * 
 */
package org.biomart.configurator.test.category;

import java.util.List;

import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Mart;

/**
 * @author lyao
 *
 */
public class TestFilter extends TestAddingSource {

	public boolean removeAttribute(String martName, String attrName){
		List<Mart> marts = McGuiUtils.INSTANCE.getRegistryObject().getMartList();
		Mart curMart = null ;
		for(Mart mart : marts){
			if(mart.getName().equals(martName)){
				curMart = mart;
			}
		}
		if(curMart == null)
			return false;
		
		for(Config config : curMart.getConfigList()){
			if(!config.isMasterConfig()){
				Attribute att = config.getAttributeByName(attrName, null);
				if(att == null){
					return false;
				}else{
					//remove attribute from derived config
					att.getParentContainer().removeAttribute(att);
				}
			}
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see org.biomart.configurator.test.category.TestAddingSource#test()
	 */
	@Override
	public boolean test() {
		// TODO Auto-generated method stub
		this.testNewPortal();
    	this.testAddMart(testName);
    	this.removeAttribute("gene_vega","chromosome_name");
    	this.testSaveXML(testName);
    	return this.compareXML(testName);
	}

}
