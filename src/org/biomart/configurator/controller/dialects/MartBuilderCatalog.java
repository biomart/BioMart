package org.biomart.configurator.controller.dialects;

/**
 * A simple class to store database and schema names for both source and target
*/
public class MartBuilderCatalog {	// TODO better name?
	private String sourceDatabaseName;
	private String sourceSchemaName;
	private String targetDatabaseName;
	private String targetSchemaName;
	public MartBuilderCatalog(String sourceDatabaseName,
			String sourceSchemaName, String targetDatabaseName,
			String targetSchemaName) {
		super();
		this.sourceDatabaseName = sourceDatabaseName;
		this.sourceSchemaName = sourceSchemaName;
		this.targetDatabaseName = targetDatabaseName;
		this.targetSchemaName = targetSchemaName;
	}
	@Override
	public String toString() {
		return "[" + sourceDatabaseName + ", " + sourceSchemaName + ", " + targetDatabaseName + ", " + targetSchemaName + "]";
	}
	public String getSourceDatabaseName() {
		return sourceDatabaseName;
	}
	public void setSourceDatabaseName(String sourceDatabaseName) {
		this.sourceDatabaseName = sourceDatabaseName;
	}
	public String getSourceSchemaName() {
		return sourceSchemaName;
	}
	public void setSourceSchemaName(String sourceSchemaName) {
		this.sourceSchemaName = sourceSchemaName;
	}
	public String getTargetDatabaseName() {
		return targetDatabaseName;
	}
	public void setTargetDatabaseName(String targetDatabaseName) {
		this.targetDatabaseName = targetDatabaseName;
	}
	public String getTargetSchemaName() {
		return targetSchemaName;
	}
	public void setTargetSchemaName(String targetSchemaName) {
		this.targetSchemaName = targetSchemaName;
	}
}