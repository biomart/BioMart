package org.biomart.common.utils;

public class PartitionUtils {
	public static int CONNECTION = 0;
	public static int DATABASE = 1;
	public static int SCHEMA = 2;
	public static int USERNAME = 3;
	public static int PASSWORD = 4;
	public static int DATASETNAME = 5;
	public static int HIDE = 6;
	public static int DISPLAYNAME = 7;
	public static int VERSION = 8;
	public static int KEY = 9;
	// 1 yes
	// 0 no,  which means innodb for mysql, for update
	public static int KEYGUESSING = 10; 
	public static int MARTDATABASE = 11;
	public static int MARTSCHEMA = 12;
	public static int MARTUSERNAME = 13;
	public static int MARTPASSWORD = 14;
	
	
	public static int DSM_DATASETNAME = 0;
	public static int DSM_DISPLAYNAME = 1;
	public static int DSM_CONNECTION = 2;
	public static int DSM_DATABASE = 3;
	public static int DSM_SCHEMA = 4;
	public static int DSM_HIDE = 5;
	public static int DSM_KEY = 6;
	
}