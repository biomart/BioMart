package org.biomart.configurator.utils.type;


public enum JdbcType {
	MySQL("MySQL","com.mysql.jdbc.Driver", false,false,true),
	PostGreSQL("PostGreSQL","org.postgresql.Driver",false,true,false),
	Oracle("Oracle","oracle.jdbc.driver.OracleDriver",true,true,false),
	MSSQL("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver",false,true,true),
	DB2("DB2","com.ibm.db2.jcc.DB2Driver",true,true,false);
	
	private String name;
	private String driverClassName;
	private boolean upppcase;
	private boolean useSchema;
	private boolean useDbName;
	
	JdbcType(String name, String driverClass, boolean uppercase, boolean useSchema, boolean useDbName) {
		this.name = name;
		this.driverClassName = driverClass;
		this.upppcase = uppercase;
		this.useSchema = useSchema;
		this.useDbName = useDbName;
	}
	
	public String toString() {
		return getName();
	}
	public String getName() {
		return this.name;
	}
	
	public String getDefaultPort() {
		switch(this) {
		case PostGreSQL:
			return "5432";
		case Oracle:
			return "1521";
		case MSSQL:
			return "1509";
		case DB2:
			return "50000";
		default:
			return "3306";
		}			
	}
	
	public String getDefaultHost() {
		switch(this) {
		case PostGreSQL:
			return "biomartdb-dev.res.oicr.on.ca";
		case Oracle:
			return "biomartdb-dev.res.oicr.on.ca";
		case MSSQL:
			return "arektest.res.oicr.on.ca";
		case DB2:
			return "biomartdb-dev.res.oicr.on.ca";
		default: 
			return "";
		}
	}
	
	public String getDriverClassName() {
		return this.driverClassName;
	}
	
	public String getUrlTemplate() {
		switch(this) {
		case PostGreSQL:
			return "jdbc:postgresql://<HOST>:<PORT>/";
		case Oracle:
			return "jdbc:oracle:thin:@<HOST>:<PORT>/";
		case MSSQL:
			return "jdbc:sqlserver://<HOST>:<PORT>";
		case DB2:
			return "jdbc:db2://<HOST>:<PORT>/";
		default:
			return "jdbc:mysql://<HOST>:<PORT>/";		
		}		
	}
	
	public static JdbcType valueFrom(String value) {
		for(JdbcType type: JdbcType.values()) {
			if(value.equals(type.driverClassName))
					return type;
		}
		return null;
	}

	public static JdbcType valueLike(String value) {
		
			if(value.startsWith("jdbc:mysql"))
				return JdbcType.MySQL;
			else if(value.startsWith("jdbc:postgres"))
				return JdbcType.PostGreSQL;
			else if(value.startsWith("jdbc:oracle"))
				return JdbcType.Oracle;
			else if(value.startsWith("jdbc:sqlserver"))
				return JdbcType.MSSQL;
			else if(value.startsWith("jdbc:db2"))
				return JdbcType.DB2;
		
		return null;
	}

	public static JdbcType valueByName(String value) {
		for(JdbcType type: JdbcType.values()) {
			if(value.equalsIgnoreCase(type.getName()))
				return type;
		}
		return null;
	}
	
	public boolean isUpperCase() {
		return this.upppcase;
	}

	public boolean useSchema() {
		return this.useSchema;
	}
	
	public boolean useDbName() {
		return this.useDbName;
	}
}