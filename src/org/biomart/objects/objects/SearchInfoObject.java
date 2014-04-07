package org.biomart.objects.objects;

import org.biomart.common.utils.XMLElements;

public class SearchInfoObject {
	private boolean like;
	private String value;
	private String typeStr;
	private boolean caseSensitive;
	private int sType;
	private String scopeStr;
	
	public SearchInfoObject(String value, String typeStr, boolean cs, boolean like, int sType, String scopeStr) {
		this.value = value;
		this.typeStr = typeStr;
		this.caseSensitive = cs;
		this.like = like;
		this.sType = sType;
		this.scopeStr = scopeStr;
	}


	public boolean isLike() {
		return like;
	}


	public String getValue() {
		return value;
	}


	public String getTypeStr() {
		return typeStr;
	}

	public String getScopeStr() {
		return this.scopeStr;
	}
	
 	public boolean isCaseSensitive() {
		return caseSensitive;
	}
	
	public XMLElements getSearchName() {
		if(sType == 0)
			return XMLElements.NAME;
		else if(sType == 1)
			return XMLElements.INTERNALNAME;
		else
			return XMLElements.DISPLAYNAME;
	}
	
}