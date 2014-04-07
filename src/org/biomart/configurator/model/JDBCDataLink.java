/*
 Copyright (C) 2006 EBI
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the itmplied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.biomart.configurator.model;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * should replaced by datalink object
 * @author yliang
 *
 */
public interface JDBCDataLink {

	/**
	 * Gets the data source database name.
	 * 
	 * @return the data source database name.
	 */
	public String getDataLinkDatabase();

	/**
	 * Sets the data source database name.
	 * 
	 * @param databaseName
	 *            the data source database name.
	 */
	public void setDataLinkDatabase(String databaseName);

	/**
	 * Gets the data source schema name.
	 * 
	 * @return the data source schema name.
	 */
	public String getDataLinkSchema();

	/**
	 * Sets the data source schema name.
	 * 
	 * @param schemaName
	 *            the data source schema name.
	 */
	public void setDataLinkSchema(String schemaName);


	/**
	 * Returns a JDBC connection connected to this database using the data
	 * supplied to all the other methods in this interface.
	 * 
	 * @param overrideDataLinkSchema
	 *            the schema to connect to, if any. <tt>null</tt> is used
	 *            where the default main schema is suitable.
	 * @return the connection for this database.
	 * @throws SQLException
	 *             if there was any problem connecting.
	 */
	public Connection getConnection()
			throws SQLException;

	/**
	 * Getter for the name of the driver class, eg.
	 * <tt>com.mysql.jdbc.Driver</tt>
	 * 
	 * @return the name of the driver class.
	 */
	public String getDriverClassName();

	/**
	 * Gets the JDBC URL.
	 * 
	 * @return the JDBC URL.
	 */
	public String getUrl();

	/**
	 * Gets the password. May be <tt>null</tt> which would indicate that
	 * no password is required.
	 * 
	 * @return the password.
	 */
	public String getPassword();

	/**
	 * Gets the username.
	 * 
	 * @return the username.
	 */
	public String getUsername();
	

}
