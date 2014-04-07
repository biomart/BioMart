package org.biomart.configurator.model.object;

import java.util.List;
import java.util.Map;

import org.biomart.configurator.utils.FileLinkObject;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.type.DataLinkType;

public class DataLinkInfo {
	private DataLinkType type;
	private boolean includePortal;
	private boolean isSourceGrouped;
	private boolean rebuildlink;
	private boolean partitioned;
	//for target only
	private boolean useOldConfig;
	private boolean targetTableNamePartitioned;
	
	//for JDBC connection
	private JdbcLinkObject jdbcLinkObject;
	private Map<String,List<String>> selectedTablesMap;
	private Map<String,List<String>> allTablesMap;
	
	//for URL connection
	private UrlLinkObject urlLinkObject;
	//for file
	private FileLinkObject fileLinkObject;
	
	public DataLinkInfo(DataLinkType type) {
		this.type = type;
		this.useOldConfig = false;
	}
	
	public void setUseOldConfigFlag(boolean b) {
		this.useOldConfig = b;
	}
	
	public boolean getUseOldConfigFlag() {
		return this.useOldConfig;
	}
	
	public void setType(DataLinkType type) {
		this.type = type;
	}
	
	public void setSelectedTables(Map<String,List<String>> tables) {
		this.selectedTablesMap = tables;
	}
		
	public void setJdbcLinkObject(JdbcLinkObject object) {
		this.jdbcLinkObject = object;
	}
	
	public JdbcLinkObject getJdbcLinkObject() {
		return this.jdbcLinkObject;
	}

	public Map<String,List<String>> getSelectedTablesMap() {
		return selectedTablesMap;
	}

	public void setAllTables(Map<String,List<String>> allTablesMap) {
		this.allTablesMap = allTablesMap;
	}

	public Map<String,List<String>> getAllTablesMap() {
		return allTablesMap;
	}

	public DataLinkType getDataLinkType() {
		return this.type;
	}
	
	public boolean isPartitioned() {
		boolean isPT = false;
		switch(this.type) {
		case SOURCE:
		case TARGET:
			if(null!=this.jdbcLinkObject.getPartitionRegex() && !"".equals(this.jdbcLinkObject.getPartitionRegex())
					&& null!=this.jdbcLinkObject.getPtNameExpression() && !"".equals(this.jdbcLinkObject.getPtNameExpression()))
				isPT = true;
			else
				isPT = false;
			break;
		}
		return isPT;
	}
	//for URL 
	public UrlLinkObject getUrlLinkObject() {
		return this.urlLinkObject;
	}

	
	public void setUrlLinkObject(UrlLinkObject value) {
		this.urlLinkObject = value;
	}

	public void setIncludePortal(boolean includePortal) {
		this.includePortal = includePortal;
	}

	public boolean isIncludePortal() {
		return includePortal;
	}

	public void setTargetTableNamePartitioned(boolean targetTableNamePartitioned) {
		this.targetTableNamePartitioned = targetTableNamePartitioned;
	}

	public boolean isTargetTableNamePartitioned() {
		return targetTableNamePartitioned;
	}

	public void setSourceGrouped(boolean isSourceGrouped) {
		this.isSourceGrouped = isSourceGrouped;
	}

	public boolean isSourceGrouped() {
		return isSourceGrouped;
	}
	
	public void setRebuildLink(boolean b) {
		this.rebuildlink = b;
	}
	
	public boolean isRebuildLink() {
		return this.rebuildlink;
	}
	//for file

	public void setFileLinkObject(FileLinkObject value) {
		this.fileLinkObject = value;
	}
	
	public FileLinkObject getFileLinkObject() {
		return this.fileLinkObject;
	}


	public void setBCPartitioned(boolean partitioned) {
		this.partitioned = partitioned;
	}
	
	public boolean isBCPartitioned() {
		return this.partitioned;
	}
}