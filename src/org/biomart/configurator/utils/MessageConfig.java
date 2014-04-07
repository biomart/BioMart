package org.biomart.configurator.utils;

import java.util.HashMap;
import java.util.Map;

import org.biomart.configurator.utils.type.MessageType;

public class MessageConfig {
	private static MessageConfig instance;
	
	private Map<String,MessageType> messageConfig;
	
	public static MessageConfig getInstance() {
		if(instance == null) {
			instance = new MessageConfig();
		}
		return instance;
	}
	
	private MessageConfig() {
		this.messageConfig = new HashMap<String,MessageType>();
		this.messageConfig.put("rebuildlink", MessageType.NO);
	}
	
	public MessageType showDialog(String messageType) {
		if("2".equals(System.getProperty("api"))) //ignore testing
			return MessageType.YESFORALL;
		return messageConfig.get(messageType);
	}
	
	public void put(String key, MessageType value) {
		this.messageConfig.put(key, value);
	}
	
	public MessageType get(String key) {
		return this.messageConfig.get(key);
	}
	
}