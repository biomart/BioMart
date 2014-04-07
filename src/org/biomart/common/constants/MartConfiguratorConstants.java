package org.biomart.common.constants;


import java.util.regex.Pattern;

import org.biomart.common.utils2.MyUtils;

public class MartConfiguratorConstants {
	
	public static final int HASH_SEED1 = 7;
	public static final int HASH_SEED2 = 31;
	
	public static final String LIST_ELEMENT_SEPARATOR = ",";
	
	public static final String MAIN_PARTITION_TABLE_DEFAULT_NAME = "0";
	public static final String MAIN_TABLES_MAIN_PARTITION_REFERENCE_SEPARATOR = ".";

	public static final int DEFAULT_PARTITION_TABLE_ROW = 0;
	public static final int PARTITION_TABLE_MAIN_COLUMN = 0;
	public static final int MAIN_PARTITION_TABLE_MAIN_COLUMN = PARTITION_TABLE_MAIN_COLUMN;
	public static final int DIMENSION_TABLE_PARTITION_TABLE_MAIN_COLUMN = PARTITION_TABLE_MAIN_COLUMN;
	
	public static final String PARTITION_TABLE_EMPTY_VALUE = "";
	
	public static final String ROOT_CONTAINER_NAME = "rootContainer";
	public static final String ROOT_CONTAINER_DISPLAY_NAME = "Root";
	
	// Find better names
	public static final String RANGE_PARTITION_TABLE_PREFIX = "P";
	public static final String RANGE_PARTITION_TABLE_REFERENCE_START = "(";
	public static final String RANGE_PARTITION_TABLE_REFERENCE_END = ")";
	public static final String RANGE_RANGE_START = "[";
	public static final String RANGE_RANGE_END = "]";
	public static final String RANGE_COLUMN_PREFIX = "C";
	public static final String RANGE_ROW_PREFIX = "R";
	public static final String RANGE_INTRA_SEPARATOR = ":";
	
	public static final String PARTITION_REFERENCE_PATTERN_STRING1 = "\\" + RANGE_PARTITION_TABLE_REFERENCE_START;
	public static final String PARTITION_REFERENCE_PATTERN_STRING2 = RANGE_PARTITION_TABLE_PREFIX;
	public static final String PARTITION_REFERENCE_PATTERN_STRING3 = "\\w*"; // any name for the partitionTable
	public static final String PARTITION_REFERENCE_PATTERN_STRING4 = RANGE_COLUMN_PREFIX;
	public static final String PARTITION_REFERENCE_PATTERN_STRING5 = "\\d*"; // any column number
	public static final String PARTITION_REFERENCE_PATTERN_STRING6 = "\\" + RANGE_PARTITION_TABLE_REFERENCE_END;
	public static final Pattern PARTITION_REFERENCE_PARTITION_TABLE_NAME_PREFIX_PATTERN = Pattern.compile(
			PARTITION_REFERENCE_PATTERN_STRING1 +
			PARTITION_REFERENCE_PATTERN_STRING2);
	public static final Pattern PARTITION_REFERENCE_PARTITION_TABLE_NAME_SUFIX_PATTERN = Pattern.compile(
			PARTITION_REFERENCE_PATTERN_STRING4 +
			PARTITION_REFERENCE_PATTERN_STRING5 +
			PARTITION_REFERENCE_PATTERN_STRING6);
	public static final Pattern PARTITION_REFERENCE_COLUMN_NUMBER_PREFIX_PATTERN = Pattern.compile(
			PARTITION_REFERENCE_PATTERN_STRING1 +
			PARTITION_REFERENCE_PATTERN_STRING2 + 
			PARTITION_REFERENCE_PATTERN_STRING3 + 
			PARTITION_REFERENCE_PATTERN_STRING4);
	public static final Pattern PARTITION_REFERENCE_COLUMN_NUMBER_SUFIX_PATTERN = Pattern.compile(PARTITION_REFERENCE_PATTERN_STRING6);
	public static final Pattern PARTITION_REFERENCE_PATTERN = Pattern.compile(
			PARTITION_REFERENCE_PATTERN_STRING1 +
			PARTITION_REFERENCE_PATTERN_STRING2 + 
			PARTITION_REFERENCE_PATTERN_STRING3 + 
			PARTITION_REFERENCE_PATTERN_STRING4 + 
			PARTITION_REFERENCE_PATTERN_STRING5 + 
			PARTITION_REFERENCE_PATTERN_STRING6);
	
	public static final String PART_SUPER_SPECIFIC_PARAMETER_VALUE_ASSIGNOR = "=";
	public static final String PART_SUPER_SPECIFIC_PARAMETER_VALUE_DELIMITER = "\"";
	public static final String PART_SUPER_SPECIFIC_DISPLAY_NAME_PARAMETER_NAME = "dN";
	
	public static final String NAMING_CONVENTION_MAIN_TABLE_NAME = "main";
	
	public static final String FILTER_LOGICAL_OPERATOR_OR = "OR";
	public static final String FILTER_LOGICAL_OPERATOR_AND = "AND";
	
	//public static final String CASCADE_DATA_INDENTATION = MyUtils.TAB_SEPARATOR;
	public static final String DATA_INFO_SEPARATOR = MyUtils.TAB_SEPARATOR;
	
	public static final String MULTIPLE_FILTER_VALUE_1 = "1";
	public static final String MULTIPLE_FILTER_VALUE_N = "N";
	public static final String MULTIPLE_FILTER_VALUE_ALL = "ALL";
	public static final String PARTITION_TABLE_ROW_WILDCARD = "*";
	public static final int PARTITION_TABLE_ROW_WILDCARD_NUMBER = -1;
	
	// Filter data
	public static final String XML_ELEMENT_PART = "part";
	public static final String XML_ELEMENT_ATTRIBUTE_PART_NAME = "name";
	public static final String XML_ELEMENT_CASCADE_CHILD = "cascadeChild";
}
