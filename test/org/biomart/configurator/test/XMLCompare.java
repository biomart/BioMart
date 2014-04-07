package org.biomart.configurator.test;

import java.util.Arrays;
import java.util.List;
import org.biomart.common.resources.Log;
import org.biomart.configurator.utils.McUtils;
import org.jdom.Attribute;
import org.jdom.Element;

public class XMLCompare {
	
	public boolean compare(Element e1,Element e2) {
		@SuppressWarnings("unchecked")
		List<Element> children1 = e1.getChildren();
		@SuppressWarnings("unchecked")
		List<Element> children2 = e2.getChildren();
		//check the size of children
		if(children1.size()!=children2.size()) {
			Log.error("child size not equal "+e1.getName() + " "+e1.getAttributeValue("name"));
			return false;
		}
		
		//getting the right element
		for(Element element1: children1) {
			Element element2 = getElement(children2,element1);
			if(element2 == null) {
				Log.error("element not matched "+element1.getName() + " "+element1.getAttributeValue("name"));
				return false;
			}
			if(!compare(element1,element2))
				return false;
		}
		
		//compare the text
		String t1 = e1.getText();
		String t2 = e2.getText();
		if(t1!=null ) {
			if(e1.getName().equals("row") && t1.indexOf("jdbc")==0) {
				//skip because the password is not comparable
			}else {				
				if(!t1.equals(t2)) {
					Log.error("text not equal "+e1.getName()+ " "+e1.getAttributeValue("name"));
					return false;
				}
			}
		} else if(t2!=null) {
			Log.error("text not equal "+e2.getName()+ " "+e2.getAttributeValue("name"));
			return false;		
		}
		
		//compare all attributes	
		@SuppressWarnings("unchecked")
		List<Attribute> al1 = e1.getAttributes();
		@SuppressWarnings("unchecked")
		List<Attribute> al2 = e2.getAttributes();
		if(al1.size()!=al2.size()) {
			Log.error("attribute size not equal "+e1.getName()+" "+e1.getAttributeValue("name"));
			return false;
		}
		
		for(Attribute a1: al1) {
			Attribute a2 = e2.getAttribute(a1.getName().toLowerCase());
			if(a2 == null) {
				Log.error("attribute not match "+a1.getName() + " in "+ e1.getName()+" "+e1.getAttributeValue("name"));
				return false;
			}
			//inpartitions is set, the order may not be the same.
			if((a1.getName().equals("inpartitions") || a1.getName().equals("attributelist")
					|| a1.getName().equals("filterlist")) && !McUtils.isStringEmpty(a1.getValue()) && 
					!McUtils.isStringEmpty(a2.getValue())) {
				String s1 = a1.getValue();
				String s2 = a2.getValue();
				String[] _s1 = s1.split(",");
				String[] _s2 = s2.split(",");
				if(_s1.length!=_s2.length) {
					Log.error("list not match " + a1.getName());
					return false;
				}
				if(!Arrays.asList(_s1).containsAll(Arrays.asList(_s2))) {
					Log.error("list not match " + a1.getName());
					return false;					
				}
			} //ignore the mart id for now 
			else if(a1.getName().equals("id") && e1.getName().equals("mart")) {
				//do nothing
			}//ignore master for now
			else if(a1.getName().equals("rdf")) {
				//ignore rdf for now
			}else if(a1.getName().equals("password")){
				//ignore password compare
			}else if(McUtils.hasPartitionBinding(a1.getValue())) {
				//ignore the partition value, since the column (p0c?) may be different
			}
			
			else if(!a1.getValue().equals(a2.getValue())) {
				Log.error("attribute not match "+a1.getName() + " in "+ e1.getName()+" "+e1.getAttributeValue("name"));
				return false;				
			}
		}		
		return true;
	}
	
	public Element getElement(List<Element> elementList, Element otherElement) {
		for(Element element: elementList) {
			if(!element.getName().equals(otherElement.getName()))
				continue;
			if(null!=otherElement.getAttributeValue("name")) {
				if(otherElement.getAttributeValue("name").equals(element.getAttributeValue("name"))) {
					return element;
				}					
			} else if(null!=otherElement.getAttributeValue("id")) {
				if(otherElement.getAttributeValue("id").equals(element.getAttributeValue("id"))) {
					return element;
				}			
			} else if(null!=otherElement.getAttribute("data")) {
				if(otherElement.getAttributeValue("data").equals(element.getAttributeValue("data"))) {
					return element;
				}							
			} else if(null!=otherElement.getAttributeValue("value")) {
				if(otherElement.getAttributeValue("value").equals(element.getAttributeValue("value"))) {
					return element;
				}	
			}
			else
				return element;
		}
		return null;
	}
	
}