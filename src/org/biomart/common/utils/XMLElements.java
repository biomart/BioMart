package org.biomart.common.utils;

public enum XMLElements {
	//element
	ID("id"),
	PORTAL ("portal"),
	USERS ("users"),
	USER ("user"),
	GROUP ("group"),
	MARTREGISTRY ("martregistry"),
	MART ("mart"),
	SOURCESCHEMAS("sourceschemas"),
	SOURCESCHEMA ("sourceschema"),
	TABLES ("tables"),
	 TABLE  ("table"),
	 COLUMN  ("column"),
	 LINKINDEXES  ("linkindices"),
	 LINKINDEX  ("linkindex"),
	 GUICONTAINER ("guicontainer"),
	 MARTPOINTER  ("martpointer"),
	 PROCESSORGROUP  ("processorgroup"),
	 PROCESSOR  ("processor"),
	 DATASET ("dataset"),

	 CONFIG ("config"),
	 PARTITIONTABLE ("partitiontable"),
	 CONTAINER ("container"),
	 FILTER("filter"),
	 ATTRIBUTE("attribute"),
	 ANNOTATIONS("annotations"),
	 IMPORTABLE("importable"),
	 EXPORTABLE("exportable"),
	 ITEM("item"),
	 RELATIONS("relations"),
	 RELATION("relation"),
	 PRIMARYKEY("primarykey"),
	 FOREIGNKEY("foreignkey"),
	 LINK("link"),
	 OPTIONS("options"),
	 TRANSFORM("transform"),
	 SOURCECONTAINERS("sourcecontainers"),
	 SOURCECONTAINER("sourcecontainer"),
	 DINO("dino"),

	//attribute
	 HIDE("hide"),
	 NAME("name"),
	 DISPLAYNAME("displayname"),
	 DESCRIPTION("description"),
	 VERSION("version"),
	 INTERNALNAME("internalname"),
	 MAXCONTAINERS("maxcontainers"),
	 MAXATTRIBUTES("maxattributes"),
	 INDEPENDENTQUERYING("independentquerying"),
	 SPLITON("spliton"),
	 OPERATION("operation"),
	 DATAFILE("datafile"),
	 FUNCTION("function"),

	 UNIQUEID("uniqueid"),
	 DRIVERCLASSNAME("driverclassname"),
	 URL("url"),
	 DATABASENAME("databasename"),
	 SCHEMANAME("schemaname"),
	 USERNAME("username"),
	 PASSWORD("password"),
	 MASKED("masked"),
	 KEYGUESSING("keyguessing"),

	 OPTIMISER("optimiser"),
	 INDEXOPTIMISED("indexoptimised"),
	 CENTRALTABLE("centraltable"),
	 TYPE("type"),

	 ROW("row"),
	 COL("col"),
	 ROWS("rows"),
	 COLS("cols"),

	 POINTER("pointer"),
	 GUITYPE("guitype"),
	 ICONS("icons"),
	 ICON("icon"),
	 DATASETS("datasets"),
	 CONTAINERS("containers"),
	 ENTRYLAYOUT("entrylayout"),
	 FILTERLIST("filterlist"),
	 DEFAULT("default"),
	 ORDERBY("orderby"),
	 FILTERS("filters"),
	 ATTRIBUTES("attributes"),
	 LINKVERSION("linkversion"),
	 EXPORTABLEDATASETS("exportabledatasets"),
	 IMPORTABLEDATASETS("importabledatasets"),
	 IMPORTABLEINMART("importableinmart"),
	 EXPORTABLEINMART("exportableinmart"),
	 EXPORT("export"),
	 LINKVERSIONS("linkversions"),
	 INPARTITIONS("inpartitions"),
	 QUALIFIER("qualifier"),
	 POINTEDATTRIBUTE("pointedattribute"),
	 POINTEDFILTER("pointedfilter"),
	 POINTEDMART("pointedmart"),
	 POINTEDCONFIG("pointedconfig"),
	 POINTEDDATASET("pointeddataset"),
	 ONLY("only"),
	 EXCLUDED("excluded"),
	 ATTRIBUTELIST("attributelist"),
	 LINKOUTURL ("linkouturl"),
	 VALUE("value"),
	 REFCONTAINER("refcontainer"),
	 DATA("data"),
	 METAINFO("metainfo"),
	 DATASETDISPLAYNAME ("datasetdisplayname"),
	 DATASETHIDEVALUE ("datasethidevalue"),
     SUBCLASSOF ("subclassof"),

	 FIRSTTABLE ("firsttable"),
	 FIRSTKEY ("firstkey"),
	 SECONDTABLE ("secondtable"),
	 SECONDKEY ("secondkey"),
	 INUSERS ("inusers"),
	 OPENID ("openid"),
	 SUBPARTITION("subpartition"),
	 MULTIFILE ("multifile"),
	 MASTER ("master"),
	 READONLY ("readonly"),
	 VIRTUAL ("virtual"),
	 SELECTEDTABLES ("selectedtables"),
	 SEARCHFROMTARGET ("searchfromtarget"),
	 LOCATION ("location"),
	 DEPENDSON("dependson"),
	 RDF("rdf"),
	 RDFCLASS("rdfclass"),
	 REQUIRED ("required"),
	 DATATYPE("datatype"),
	 POINTERINSOURCE("pointerinsource"),
	//value
	 TRUE_VALUE("true"),
	 FALSE_VALUE("false"),
	 NONE("none"),

	 PARTITIONPREFIX("p"),
	 PARTITIONCOLUMNPREFIX("c"),
	 TABPROCESSORGROUP("Tabular"),
	 SEQUENCESPROCESSORGROUP("Sequences"),
	 GRAPHSPROCESSORGROUP("Graphs"),

	 HTMLPROCESSOR("HTML"),
	 CSVPROCESSOR("CSV"),
	 XLSPROCESSOR("XLS"),
	 TSVPROCESSOR("TSV"),
	 FASTAPROCESSOR("FASTA"),
	 GFFPROCESSOR("GFF"),
	 KAPLAPROCESSOR("KAPLA"),

	 //gui only
	 VISIBLEMODIFIED("visiblemodified"),
	 ERROR("error");

	private String value;

	 XMLElements(String value) {
		 this.value = value;
	 }

	 public String toString() {
		 return this.value;
	 }

	 public static XMLElements valueFrom(String value) {
		 for(XMLElements xe: XMLElements.values()) {
			 if(xe.toString().equalsIgnoreCase(value))
				 return xe;
		 }
		 return null;
	 }
}