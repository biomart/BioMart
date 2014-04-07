package org.biomart.configurator.utils;

import java.util.List;
import java.util.Map;

import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;


/**
 * should merge with ConnectionObject class
 * @author yliang
 *
 */
public class UrlLinkObject {

	private Map<MartInVirtualSchema, List<DatasetFromUrl>> dsInfoMap;	
	private String protocol;
	private String host;
	private String port;
	private String path;
	private boolean isGrouped = false;
	private boolean version8;
	private String userName;
	private String password;
	private String keys;
	
	private Map<String,List<String>> mpMap;

	public void setDsInfoMap(Map<MartInVirtualSchema, List<DatasetFromUrl>> value) {
		this.dsInfoMap = value;
	}
	
	public Map<MartInVirtualSchema, List<DatasetFromUrl>> getDsInfoMap() {
		return this.dsInfoMap;
	}
		
	public void setHost(String host) {
		this.host = host;
	}
	public String getHost() {
		return host;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getPort() {
		return port;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getPath() {
		return path;
	}
	public void setGrouped(boolean b){
		this.isGrouped = b;
	}
	public boolean isGrouped() {
		return this.isGrouped;
	}




	public void setFullHost(String fullHost) {
		//set host 
		int index = fullHost.indexOf("://");
		this.host = fullHost.substring(index+3);	
		this.protocol = fullHost.substring(0,index);
	}

	public String getFullHost() {
		return this.protocol+"://"+this.host;
	}

	public void setVersion8(boolean version8) {
		this.version8 = version8;
	}

	public boolean isVersion8() {
		return version8;
	}

	public void setMpList(Map<String,List<String>> mpList) {
		this.mpMap = mpList;
	}

	public Map<String,List<String>> getMpList() {
		return mpMap;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setKeys(String keys) {
		this.keys = keys;
	}

	public String getKeys() {
		return keys;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getProtocol() {
		return protocol;
	}
	
}