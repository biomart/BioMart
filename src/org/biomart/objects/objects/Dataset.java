package org.biomart.objects.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.biomart.common.exceptions.FunctionalException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.JdbcType;
import org.biomart.configurator.utils.type.McNodeType;
import org.jdom.Element;

/**
 * A Dataset associate to one partition of a Mart, it associate to one 
 * row in the partition table.
 * There is no setter for Dataset object, but some getter functions are depends on the 
 * partitiontable dynamically. So to change the dataset is to change the partitiontable.
 * A Dataset object is equal when its name and mart are equal.
 * TODO should it be immutable?
 *
 */
public class Dataset extends MartConfiguratorObject implements Serializable, Comparable<Dataset>{

	private static final long serialVersionUID = 3449474651475522505L;
	
	
	public Dataset(Mart parentMart, String name) {
		super(name);
		this.parent = parentMart;
		this.setNodeType(McNodeType.DATASET);
	}
	
	public Mart getParentMart() {
		return (Mart)this.parent;
	}
	
	public String getDino(String configName) {
		Log.debug(this.getClass().getName() + "#getDino("+configName+")");
		return this.getParentMart().getConfigByName(configName).getDino();
	}
	
	public Attribute getAttributeByName(String name, String configName, String userGroup){
		Attribute attObj =  this.getParentMart().getConfigByName(configName).getAttributeByName(this, name,true);
        Set<String> dsNames = new HashSet<String>();
        dsNames.add(this.getName());
        if (attObj != null && attObj.inUser(userGroup, dsNames))
            return attObj;
        return null;
	}
	
	public Filter getFilterByName(String name, String configName, String userGroup){
		Filter filtObj =  this.getParentMart().getConfigByName(configName).getFilterByName(this, name,true);
        Set<String> dsNames = new HashSet<String>();
        dsNames.add(this.getName());
        if (filtObj != null && filtObj.inUser(userGroup, dsNames))
            return filtObj;
        return null;
	}
	
	@Override
	public int hashCode() {
		return this.getParentMart().getName().hashCode()+this.getName().hashCode();
	}
	
	@Override
	public boolean equals(Object object) {
		if(object == this)
			return true;
		else if(!(object instanceof Dataset))
			return false;
		else 
			return ((Dataset)object).getParentMart().equals(this.getParentMart()) &&
				((Dataset)object).getName().equals(this.getName());
	}
	
	/**
	 * Get a DataLinkInfo object of current Dataset. A DataLinkInfo object has the information of 
	 * the data source.
	 * only for schema partition table
	 * TODO code clean up
	 * @return
	 */
	public DataLinkInfo getDataLinkInfo() {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return null;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return null;
		DataLinkInfo dli = null;
		//check if it is url or database
		String conStr = pt.getValue(row, 0);
		if(conStr.indexOf("jdbc")>=0) { //database 
			int col0 = 0;
			int col1 = this.getParentMart().searchFromTarget()?PartitionUtils.DATABASE:PartitionUtils.MARTDATABASE;
			int col2 = this.getParentMart().searchFromTarget()?PartitionUtils.SCHEMA:PartitionUtils.MARTSCHEMA;
			int col3 = 3;
			int col4 = 4;
			JdbcType type = JdbcType.valueLike(pt.getValue(row, col0));			
			dli = new DataLinkInfo(DataLinkType.TARGET);
			JdbcLinkObject jdbcLinkObject = new JdbcLinkObject(pt.getValue(row, col0),
					pt.getValue(row, col1),pt.getValue(row, col2),pt.getValue(row, col3),
					pt.getValue(row, col4),type,"","","0".equals(pt.getValue(row, PartitionUtils.KEYGUESSING))?false:true);
			dli.setJdbcLinkObject(jdbcLinkObject);
		}else {
			dli = new DataLinkInfo(DataLinkType.URL);
			UrlLinkObject urlLinkObject = new UrlLinkObject();
			urlLinkObject.setFullHost(pt.getValue(row, 0));
			urlLinkObject.setPort(pt.getValue(row, 1));
			urlLinkObject.setPath(pt.getValue(row, 2));
			dli.setUrlLinkObject(urlLinkObject);
		}
		return dli;
	}
	
	/**
	 * TODO should merge with getDataLinkInfo()
	 * Get a DataLinkInfo object of current Dataset. A DataLinkInfo object has the information of 
	 * the data source.
	 * only for schema partition table
	 * @return
	 */
	public DataLinkInfo getDataLinkInfoNonFlip() {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return null;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return null;
		DataLinkInfo dli = null;
		//check if it is url or database
		String conStr = pt.getValue(row, 0);
		if(conStr.indexOf("jdbc")>=0) { //database 
			int col0 = PartitionUtils.CONNECTION;
			int col1 = PartitionUtils.MARTDATABASE;
			int col2 = PartitionUtils.MARTSCHEMA;
			int col3 = PartitionUtils.MARTUSERNAME;
			int col4 = PartitionUtils.MARTPASSWORD;
			if(McUtils.isStringEmpty(pt.getValue(row, col1))) {
				col1 = PartitionUtils.DATABASE;
				col2 = PartitionUtils.SCHEMA;
				col3 = PartitionUtils.USERNAME;
				col4 = PartitionUtils.PASSWORD;
			}

			JdbcType type = JdbcType.valueLike(pt.getValue(row, col0));	
			DataLinkType dtype = DataLinkType.TARGET;
			//check if it is source and non-materialize
			if(pt.getValue(row, PartitionUtils.DATABASE).equals(pt.getValue(row,PartitionUtils.MARTDATABASE)) &&
					pt.getValue(row, PartitionUtils.SCHEMA).equals(pt.getValue(row,PartitionUtils.MARTSCHEMA)) )
				dtype = DataLinkType.SOURCE;
			dli = new DataLinkInfo(dtype);
			JdbcLinkObject jdbcLinkObject = new JdbcLinkObject(pt.getValue(row, col0),
					pt.getValue(row, col1),pt.getValue(row, col2),pt.getValue(row, col3),
					pt.getValue(row, col4),type,"","","0".equals(pt.getValue(row,PartitionUtils.KEYGUESSING))?false:true);
			dli.setJdbcLinkObject(jdbcLinkObject);
		}else {
			dli = new DataLinkInfo(DataLinkType.URL);
			UrlLinkObject urlLinkObject = new UrlLinkObject();
			urlLinkObject.setFullHost(pt.getValue(row, 0));
			urlLinkObject.setPort(pt.getValue(row, 1));
			urlLinkObject.setPath(pt.getValue(row, 2));
			dli.setUrlLinkObject(urlLinkObject);
		}
		return dli;
	}

	public DataLinkType getDataLinkType() {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return null;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return null;
		String col1 = pt.getValue(row, 0);
		if(col1.indexOf("jdbc")>=0)  //database 
			return DataLinkType.TARGET;
		else
			return DataLinkType.URL;
	}
	
	@Override
	public Element generateXml() throws FunctionalException {
		Element element = new Element(XMLElements.DATASET.toString());
		return element;
	}


	public String getVersion() {
//		if(this.getDataLinkType() == DataLinkType.TARGET)
//			return Resources.BIOMART_VERSION;
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return null;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return null;
		return pt.getValue(row, PartitionUtils.VERSION);
	}

	@Override
	public void synchronizedFromXML() {

	}

	@Override
	public List<MartConfiguratorObject> getChildren() {
		List<MartConfiguratorObject> children = new ArrayList<MartConfiguratorObject>();
		return children;
	}

	@Override
	public int compareTo(Dataset obj) {
		Dataset ds = (Dataset)obj;
		return this.getDisplayName().compareTo(ds.getDisplayName());
	}

	public boolean hideOnMaster() {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(this.getName());
		String value = pt.getValue(row, PartitionUtils.HIDE);
		return Boolean.parseBoolean(value);
	}
	
	/*
	 * For a given access point, its visibility is depending on both the 'datasethidevalue' of its own and col6 
	 * in partition table. when either of them sets hide=true, the dataset is hidden for this access point; 
	 * when both of them set hide=false, then the dataset is unhidden for this access point
	 */
	public boolean hideOnConfig(Config config) {
		String hideStr = config.getPropertyValue(XMLElements.DATASETHIDEVALUE);
		int col0 = PartitionUtils.HIDE;
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		int row = pt.getRowNumberByDatasetName(this.getName());
		String value = pt.getValue(row, col0);
		if(Boolean.parseBoolean(value))
			return true;
		else if(!McUtils.isStringEmpty(hideStr)) {
			col0 = McUtils.getPartitionColumnValue(hideStr);
			value = pt.getValue(row, col0);
		}
		return Boolean.parseBoolean(value);
	}
	/**
	 * check if a dataset is materialized
	 * it compares the databasename, schemaname in partitiontable, if they are the same or one is empty, 
	 * return false, else return true
	 * @return
	 */
	public boolean isMaterialized() {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return false;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return false;
		String dbName1 = pt.getValue(row, PartitionUtils.MARTDATABASE);
		String schemaName1 = pt.getValue(row, PartitionUtils.MARTSCHEMA);
		/*
		 * from relational mart
		 */
		if(McUtils.isStringEmpty(dbName1) || McUtils.isStringEmpty(schemaName1)) {
			return true;
		}
		if(pt.getValue(row, PartitionUtils.DATABASE).equals(dbName1) && 
				pt.getValue(row, PartitionUtils.SCHEMA).equals(schemaName1))
			return false;
		else
			return true;
		
	}
	
	public DataLinkInfo getDataLinkInfoForSource() {
		return this.getDataLinkInfo(false);
	}
	
	public DataLinkInfo getDataLinkInfoForTarget() {
		return this.getDataLinkInfo(true);
	}
	
	private DataLinkInfo getDataLinkInfo(boolean target) {
		PartitionTable pt = this.getParentMart().getSchemaPartitionTable();
		if(pt == null)
			return null;
		int row = pt.getRowNumberByDatasetName(this.getName());
		if(row<0)
			return null;
		DataLinkInfo dli = null;
		//check if it is url or database
		String conStr = pt.getValue(row, 0);
		if(conStr.indexOf("jdbc")>=0) { //database 
			int col0 = PartitionUtils.CONNECTION;
			int col1 = target?PartitionUtils.DATABASE:PartitionUtils.MARTDATABASE;
			int col2 = target?PartitionUtils.SCHEMA:PartitionUtils.MARTSCHEMA;
			int col3 = target?PartitionUtils.USERNAME:PartitionUtils.MARTUSERNAME;
			int col4 = target?PartitionUtils.PASSWORD:PartitionUtils.MARTPASSWORD;
			JdbcType type = JdbcType.valueLike(pt.getValue(row, col0));			
			dli = new DataLinkInfo(DataLinkType.TARGET);
			JdbcLinkObject jdbcLinkObject = new JdbcLinkObject(pt.getValue(row, col0),
					pt.getValue(row, col1),pt.getValue(row, col2),pt.getValue(row, col3),
					pt.getValue(row, col4),type,"","","0".equals(pt.getValue(row, PartitionUtils.KEYGUESSING))?false:true);
			dli.setJdbcLinkObject(jdbcLinkObject);
		}else {
			dli = new DataLinkInfo(DataLinkType.URL);
			UrlLinkObject urlLinkObject = new UrlLinkObject();
			urlLinkObject.setFullHost(pt.getValue(row, 0));
			urlLinkObject.setPort(pt.getValue(row, 1));
			urlLinkObject.setPath(pt.getValue(row, 2));
			dli.setUrlLinkObject(urlLinkObject);
		}
		return dli;
	}

	public String getValueForColumn(int col) {
		int row = this.getParentMart().getDatasetRowNumber(this);
		return this.getParentMart().getSchemaPartitionTable().getValue(row, col);
	}
}
