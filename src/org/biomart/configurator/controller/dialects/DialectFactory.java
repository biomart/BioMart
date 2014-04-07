package org.biomart.configurator.controller.dialects;

import org.biomart.configurator.utils.type.JdbcType;

public class DialectFactory {
	public static DatabaseDialect getDialect(JdbcType type) {
		DatabaseDialect dialect = null;
		switch (type) {
		case MySQL: 
			dialect = new MySQLDialect();
			break;
		case MSSQL:
			dialect = new MsSQLDialect();
			break;
		case DB2:
			dialect = new DB2Dialect();
			break;
		case PostGreSQL:
			dialect = new PgDialect();
			break;
		case Oracle:
			dialect = new OracleDialect();
			break;
		}
		return dialect;
	}
}