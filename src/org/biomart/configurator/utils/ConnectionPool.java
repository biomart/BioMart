package org.biomart.configurator.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Log;


/**
 * 
 * @author yliang
 *
 */
public enum ConnectionPool {
	Instance;
	
	private Map<JdbcLinkObject, Connection> connections;
	
	private ConnectionPool() {
		this.connections = new HashMap<JdbcLinkObject, Connection>();
	}
	/*
	 * if cannot find a connection object, create a new one and put it in pool
	 * if the conObject has databasename, the url will include it.
	 */
	public Connection getConnection(JdbcLinkObject conObject) throws MartBuilderException {
		Connection con = this.connections.get(conObject);
		if(con==null) {
			try {
				Class.forName(conObject.getJdbcType().getDriverClassName());
				con = DriverManager.getConnection(conObject.getJdbcUrl(),
						conObject.getUserName(),conObject.getPassword());			
			} catch(java.lang.ClassNotFoundException e) {
				Log.error("database error");
				throw new MartBuilderException("database connection error",e);
				//JOptionPane.showMessageDialog(null, "database connection error");				
			} catch (SQLException e) {
				e.printStackTrace();
				Log.error("database error");
				throw new MartBuilderException("database connection error",e);
				//JOptionPane.showMessageDialog(null, "database connection error");
			}
			this.connections.put(conObject, con);
		}
		return con;
	}
	
	public void releaseConnection(JdbcLinkObject conObject) {
		Connection con = this.connections.get(conObject);
		if(con != null) {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		this.connections.remove(conObject);			
	}

	public List<Map<String,String>> query(JdbcLinkObject conObject, String sql) throws MartBuilderException {
		List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		Connection con = null;
		try {
			Class.forName(conObject.getJdbcType().getDriverClassName());
			con = DriverManager.getConnection(conObject.getJdbcUrl(),
					conObject.getUserName(),conObject.getPassword());			
		} catch(java.lang.ClassNotFoundException e) {
			Log.error("database error");
			throw new MartBuilderException("database connection error",e);				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MartBuilderException("sql exception",e);
		} 
		Statement st;
		try {
			st = con.createStatement();
			ResultSet rs = st.executeQuery(sql);
			ResultSetMetaData metaData = rs.getMetaData();
			int count = metaData.getColumnCount()+1;
			while(rs.next()) {
				Map<String,String> item = new HashMap<String,String>();
				for(int i=1;i<count;i++) {
					//force it cast to string
					Object obj = rs.getObject(i);
					String value = "";
					if(obj != null)
						value = obj.toString();
					item.put(metaData.getColumnLabel(i), value);
				}
				result.add(item);
			}
			rs.close();
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MartBuilderException("sql exception",e);
		} 
		
		return result;
	}

}