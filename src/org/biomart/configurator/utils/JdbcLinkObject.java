package org.biomart.configurator.utils;

import java.util.List;
import java.util.Map;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.configurator.utils.type.JdbcType;

/**
 * Has all information for creating a JDBC connection
 * This is an immutable object. 
 *
 */
public class JdbcLinkObject {
	private String databaseName;
	private String userName;
	private String password;
	private String schemaName;
	private JdbcType type;
	private String regex;
	private String nameExpression;
	private boolean keyGussing;
	private String connectionBase;
	
	public String getJdbcUrl() {
		switch (type) {
		case MySQL:
			return this.connectionBase+databaseName;
		case MSSQL:
			if(McUtils.isStringEmpty(databaseName))
				return this.connectionBase;
			else
				return this.connectionBase+";databaseName="+databaseName+";"; 	// TODO better
		case DB2:
			return this.connectionBase+databaseName; // already has trailing "/"
		case PostGreSQL:
			return this.connectionBase+databaseName;
		case Oracle:
			return this.connectionBase+databaseName;
		}
		return this.connectionBase;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}
	
	public JdbcLinkObject(String url,String dbName, String schemaName,
			String userName, String pwd, JdbcType type, String regex, String expression,boolean keyGussing) {
		this.connectionBase = url;
		this.userName = userName;
		this.password = pwd;
		this.type = type;
		this.databaseName = dbName;
		this.schemaName = schemaName;
		this.regex = regex;
		this.nameExpression = expression;
		this.keyGussing = keyGussing;
	}
	public String toString() {
		return "databaseName = " + databaseName + 
			", " + "userName = " + userName + 
			", " + "password = " + password + 
			", " + "schemaName = " + schemaName + 
			", " + "type = " + type + 
			", " + "regex = " + regex + 
			", " + "nameExpression = " + nameExpression + 
			", " + "keyGussing = " + keyGussing + 
			", " + "connectionBase = " + connectionBase;
	}
	
	public JdbcType getJdbcType() {
		return this.type;
	}
	
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		
		if(obj == null)
			return false;
		
		if(this.getClass() != obj.getClass())
			return false;
		
		if(!(obj instanceof JdbcLinkObject))
			return false;
		
		JdbcLinkObject conObj = (JdbcLinkObject)obj;
		if(conObj.getJdbcType().equals(this.type) && 
				conObj.getConnectionBase().equals(this.connectionBase) && 
				conObj.getUserName().equals(this.userName) &&
				conObj.getPassword().equals(this.password) &&
				conObj.getDatabaseName().equals(this.databaseName))
			return true;
		else
			return false;
	}
	
	public int hashCode() {
        final int PRIME = 31;
        int result = PRIME + 
        	this.connectionBase.hashCode() + this.userName.hashCode() + this.password.hashCode() 
        	+ (this.databaseName==null? 0:this.databaseName.hashCode());
        return result;
	}

	public String getDatabaseName() {
		return databaseName;
	}
	
	public String getSchemaName() {
		return this.schemaName;
	}

	public String getPartitionRegex() {
		if(this.regex == null || "".equals(this.regex.trim()))
			return null;
		return this.regex;
	}
	
	public String getPtNameExpression() {
		if(this.nameExpression == null || "".equals(this.nameExpression.trim()))
			return null;
		return this.nameExpression;
	}
	
	public boolean isKeyGuessing() {
		if(type == JdbcType.MySQL)
			return this.keyGussing;
		else
			return false;
	}

	public String getConnectionBase() {
		return this.connectionBase;
	}

	public boolean useSchema() {
		return this.type.useSchema();
	}
	
	public boolean useDbName() {
		return this.type.useDbName();
	}

	public String getHost() {
		int index = 0;
		if(this.type == JdbcType.Oracle)
			index = this.connectionBase.indexOf("@");
		else
			index = this.connectionBase.indexOf("//")+2;
		String tmpStr = this.connectionBase.substring(index);
		String[] _str = tmpStr.split(":");
		if(this.type == JdbcType.Oracle) {
			return _str[0].substring(1);
		}else			
			return _str[0];
	}
	
	public String getPort() {
		int index = 0;
		if(this.type == JdbcType.Oracle)
			index = this.connectionBase.indexOf("@");
		else
			index = this.connectionBase.indexOf("//")+2;
		String tmpStr = this.connectionBase.substring(index);
		String[] _str = tmpStr.split(":");
		String[] tmpResult = _str[1].split("/");
		return tmpResult[0];
	}

	private Map<MartInVirtualSchema, List<DatasetFromUrl>> dsInfoMap;	
	private Map<MartInVirtualSchema, List<DatasetFromUrl>> fullDsInfoMap;


	public void setDsInfoMap(Map<MartInVirtualSchema, List<DatasetFromUrl>> value) {
		this.dsInfoMap = value;
	}
	
	public Map<MartInVirtualSchema, List<DatasetFromUrl>> getDsInfoMap() {
		return this.dsInfoMap;
	}

	public void setFullDsInfoMap(Map<MartInVirtualSchema, List<DatasetFromUrl>> fullDsInfoMap) {
		this.fullDsInfoMap = fullDsInfoMap;
	}

	public Map<MartInVirtualSchema, List<DatasetFromUrl>> getFullDsInfoMap() {
		return fullDsInfoMap;
	}

}