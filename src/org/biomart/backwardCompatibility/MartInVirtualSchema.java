package org.biomart.backwardCompatibility;

import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.type.JdbcType;

public class MartInVirtualSchema {
	//common
	private final String database;
	private final String defaultStr;
	private final String displayName;
	private String host;
	private final String includeDatasets;
	private final String martUser;
	private final String name;
	private final String port;
	private final String visible;
	private final boolean isURL;

	//url
	private final String redirect;
	private final String serverVirtualSchema;
	private final String path;
	//db
	private final String schema;
	private final String username;
	private final String password;
	private final String type;
	private boolean useBCForDB;
	
	

	public static class URLBuilder {
		private String database = "";
		private String defaultStr = "";
		private String displayName = "";
		private String host = "";
		private String includeDatasets = "";
		private String martUser = "";
		private String name = "";
		private String path = "";
		private String port = "";
		private String serverVirtualSchema = "";
		private String visible = "";
		private String redirect = "";

		
		public URLBuilder database(String value) {
			database = value;
			return this;
		}
		
		public URLBuilder defaultValue(String value) {
			defaultStr = value;
			return this;
		}
		
		public URLBuilder displayName(String value) {
			displayName = value;
			return this;
		}
		
		public URLBuilder host(String value) {
			host = value;
			return this;
		}
		
		public URLBuilder includeDatasets(String value) {
			includeDatasets = value;
			return this;
		}
		
		public URLBuilder martUser(String value) {
			martUser = value;
			return this;
		}
		
		public URLBuilder name(String value) {
			name = value;
			return this;
		}
		
		public URLBuilder path(String value) {
			path = value;
			return this;
		}
		
		public URLBuilder port(String value) {
			port = value;
			return this;
		}
		
		public URLBuilder serverVirtualSchema(String value) {
			serverVirtualSchema = value;
			return this;
		}
		
		public URLBuilder visible(String value) {
			visible = value;
			return this;
		}
		
		public MartInVirtualSchema build() {
			return new MartInVirtualSchema(this);
		}
		
	}
	

	public static class DBBuilder {
		private String database = "";
		private String defaultStr = "";
		private String displayName = "";
		private String host = "";
		private String includeDatasets = "";
		private String martUser = "";
		private String name = "";
		private String path = "";
		private String port = "";
		private String visible = "";
		private String schema = "";
		private String username = "";
		private String password = "";
		private String type = "";
		
		public DBBuilder database(String value) {
			database = value;
			return this;
		}
		
		public DBBuilder defaultValue(String value) {
			defaultStr = value;
			return this;
		}
		
		public DBBuilder displayName(String value) {
			displayName = value;
			return this;
		}
		
		public DBBuilder host(String value) {
			host = value;
			return this;
		}
		
		public DBBuilder includeDatasets(String value) {
			includeDatasets = value;
			return this;
		}
		
		public DBBuilder martUser(String value) {
			martUser = value;
			return this;
		}
		
		public DBBuilder name(String value) {
			name = value;
			return this;
		}
		
		public DBBuilder port(String value) {
			port = value;
			return this;
		}

		
		public DBBuilder visible(String value) {
			visible = value;
			return this;
		}
		
		public DBBuilder schema(String schema) {
			this.schema = schema;
			return this;
		}
		
		public DBBuilder username(String username) {
			this.username = username;
			return this;
		}
		
		public DBBuilder password(String password) {
			this.password = password;
			return this;
		}
		
		public DBBuilder type(String type) {
			this.type = type;
			return this;
		}
		
		public MartInVirtualSchema build() {
			return new MartInVirtualSchema(this);
		}
		
	}
	

	private MartInVirtualSchema(URLBuilder builder) {
		database = builder.database;
		defaultStr = builder.defaultStr;
		displayName = builder.displayName;
		host = builder.host;
		includeDatasets = builder.includeDatasets;
		martUser = builder.martUser;
		name = builder.name;
		path = builder.path;
		port = builder.port;
		serverVirtualSchema = builder.serverVirtualSchema;
		visible = builder.visible;
		redirect = builder.redirect;
		schema = "";
		username = "";
		password = "";
		type = "";
		this.isURL = true;
	}
	
	private MartInVirtualSchema(DBBuilder builder) {
		database = builder.database;
		defaultStr = builder.defaultStr;
		displayName = builder.displayName;
		host = builder.host;
		includeDatasets = builder.includeDatasets;
		martUser = builder.martUser;
		name = builder.name;
		path = builder.path;
		port = builder.port;
		serverVirtualSchema = "";
		visible = builder.visible;
		redirect = "";
		schema = builder.schema;
		username = builder.username;
		password = builder.password;
		type = builder.type;
		this.isURL = false;
	}
	


	public String getDatabase() {
		return database;
	}

	public String getDefaultStr() {
		return defaultStr;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getHost() {
		return host;
	}
	
	public void setHost(String host) {
		this.host = host;
	}

	public String getIncludeDatasets() {
		return includeDatasets;
	}

	public String getMartUser() {
		return martUser;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getPort() {
		return port;
	}

	public String getServerVirtualSchema() {
		return serverVirtualSchema;
	}

	public boolean isRedirect() {
		return "1".equals(redirect);
	}

	public boolean isVisible() {
		if(this.visible.equals("1"))
			return true;
		else
			return false;
	}

	public String getSchema() {
		return schema;
	}
	
	public String getUserName() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public boolean isURLMart() {
		return this.isURL;
	}
	
	public String getType() {
		return this.type;
	}
	
	public JdbcLinkObject getJdbcLinkObject() {
		//db
		JdbcType type = JdbcType.valueByName(this.getType());
		String tmplate = type.getUrlTemplate();
		tmplate = tmplate.replaceAll("<HOST>", this.getHost());
		tmplate = tmplate.replaceAll("<PORT>", this.getPort());

		JdbcLinkObject conObject = new JdbcLinkObject(tmplate,this.getDatabase(),this.getSchema(),
				this.getUserName(),this.getPassword(),type,"","",true);
		return conObject;
	}

	public void setUseBCForDB(boolean useBCForDB) {
		this.useBCForDB = useBCForDB;
	}

	public boolean isUseBCForDB() {
		return useBCForDB;
	}
}