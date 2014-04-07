package org.biomart.objects.objects;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.biomart.common.constants.MartConfiguratorConstants;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Resources;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.configurator.controller.SplitOptimiserColumnDef;
import org.biomart.configurator.model.TransformationUnit;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.type.ValidationStatus;

public class DatasetColumn extends Column {

    private static final long serialVersionUID = 6824928327693517287L;

    private TransformationUnit tu;
    private boolean isColumnIndexed;
    private SplitOptimiserColumnDef splitOptColDef;
    private boolean orphan = false;
    private boolean renamed = false;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((object == null)) {
            return false;
        }
        DatasetColumn col = (DatasetColumn) object;

        return super.getPropertyValue(XMLElements.INTERNALNAME).equalsIgnoreCase(
                col.getPropertyValue(XMLElements.INTERNALNAME))
                && col.getParent().equals(this.getParent());
    }

    @Override
    public int hashCode() {
        int hash = MartConfiguratorConstants.HASH_SEED1;
        hash = MartConfiguratorConstants.HASH_SEED2 * hash + (null == this.getPropertyValue(XMLElements.INTERNALNAME)
                ? 0 : this.getPropertyValue(XMLElements.INTERNALNAME).toLowerCase().hashCode()); // Must specify
                                                                                                 // toLowerCase here
                                                                                                 // TODO move to MCO?
        return hash;
    }

    protected DatasetColumn(String name) {
        super(name);
    }

    public DatasetColumn(DatasetTable table, String name) {
        super(table, name);
    }

    public DatasetColumn(org.jdom.Element element) {
        super(element);
        this.setInternalName(this.getName());
        this.setDisplayName(this.getName());
    }

    public DatasetTable getDatasetTable() {
        return (DatasetTable) this.parent;
    }

    public org.jdom.Element generateXml() {
        org.jdom.Element element = new org.jdom.Element(XMLElements.COLUMN.toString());
        element.setAttribute(XMLElements.NAME.toString(), this.getName());
        if (!this.getRange().isEmpty()) {
            element.setAttribute(XMLElements.INPARTITIONS.toString(), McUtils.StrListToStr(this.getRange(), ","));
        } // if only one partition, set it.
        else if (this.getDatasetTable().getMart().getSchemaPartitionTable().getTotalRows() == 1) {
            String ds = this.getDatasetTable().getMart().getSchemaPartitionTable()
                    .getValue(0, PartitionUtils.DATASETNAME);
            element.setAttribute(XMLElements.INPARTITIONS.toString(), ds);
        } else
            // should not happen
            element.setAttribute(XMLElements.INPARTITIONS.toString(), "");
        return element;
    }

    public boolean isKeyColumn() {
        final Set<Column> cols = new HashSet<Column>();
        for (Key key : this.getTable().getKeys()) {
            cols.addAll(key.getColumns());
        }
        return cols.contains(this);
    }

    /*
     * for override
     */
    public SourceColumn getSourceColumn() {
        return null;
    }

    /**
     * Set the transformation unit causing this column to exist.
     * 
     * @param tu
     *            the unit.
     */
    public void setTransformationUnit(final TransformationUnit tu) {
        this.tu = tu;
    }

    public List<Attribute> getReferences() {
        List<Attribute> attributeList = new ArrayList<Attribute>();
        Mart mart = this.getDatasetTable().getMart();
        String tableName = this.getDatasetTable().getName();
        for (Config con : mart.getConfigList()) {
            for (Attribute attribute : con.getAttributes(new ArrayList<String>(), true, true)) {
                if (attribute.getDataSetColumn() != null
                        && attribute.getDataSetColumn().getName().equals(this.getName())
                        && tableName.equals(attribute.getDatasetTable().getName()))
                    attributeList.add(attribute);
            }
        }
        return attributeList;
    }

    public boolean hasReferences() {
        Mart mart = this.getDatasetTable().getMart();
        String tableName = this.getDatasetTable().getName();
        // search in the master config
        Config con = mart.getMasterConfig();
        for (Attribute attribute : con.getAllAttributes()) {
            // if(attribute.getDataSetColumn()!=null && attribute.getDatasetTable() !=null &&
            // attribute.getDataSetColumn().getName().equals(this.getName())
            // && tableName.equals(attribute.getDatasetTable().getName()))

            if (attribute.getPropertyValue(XMLElements.TABLE).equals(tableName)
                    && attribute.getPropertyValue(XMLElements.COLUMN).equals(this.getName()))
                return true;
        }

        return false;
    }

    @Override
    public void synchronizedFromXML() {
        // set range
        String[] ranges = this.getPropertyValue(XMLElements.INPARTITIONS).split(",");
        for (String range : ranges) {
            this.addInPartitions(range);
        }
        this.setObjectStatus(ValidationStatus.VALID);
    }

    public boolean isColumnIndexed() {
        return this.isColumnIndexed;
    }

    public SplitOptimiserColumnDef getSplitOptimiserColumn() {
        return this.splitOptColDef;
    }

    public void setSplitOptimiserColumnDef(SplitOptimiserColumnDef value) {
        this.splitOptColDef = value;
    }

    public void setOrphan(boolean orphan) {
        this.orphan = orphan;
    }

    public boolean isOrphan() {
        return orphan;
    }

    /**
     * Is this a renamed column?
     * 
     * @return <tt>null</tt> if it is not, otherwise return the new name.
     */
    public boolean isColumnRenamed() {
        return this.renamed;
    }

    /**
     * Rename this column.
     * 
     * @param columnRename
     *            the new name, or <tt>null</tt> to undo it.
     * @param userRequest
     *            <tt>true</tt> if this is a user request, <tt>false</tt> if not.
     * @throws ValidationException
     *             if it could not be done.
     */
    public void setColumnRename(String columnRename, final boolean userRequest) throws ValidationException {
        String oldValue = this.getName();
        if (oldValue.equals(columnRename))
            return;
        // Make the name unique.
        if (columnRename != null) {
            final Set<String> entries = new HashSet<String>();
            // Get renames of siblings.
            for (Column column : this.getTable().getColumnList())
                entries.add(column.getName());
            entries.remove(oldValue);
            // First we need to find out the base name, ie. the bit
            // we append numbers to make it unique, but before any
            // key suffix. If we appended numbers after the key
            // suffix then it would confuse MartEditor.
            String keySuffix = Resources.get("keySuffix");
            String baseName = columnRename;
            if (columnRename.endsWith(keySuffix)) {
                baseName = columnRename.substring(0, columnRename.indexOf(keySuffix));
            }
            if (!this.isKeyCol())
                keySuffix = "";
            columnRename = baseName + keySuffix;
            // Now, if the old name has a partition prefix, and the
            // new one doesn't, reinstate or replace it.
            if (this.getName().indexOf("__") >= 0) {
                if (columnRename.indexOf("__") >= 0)
                    columnRename = columnRename.substring(columnRename.lastIndexOf("__") + 2);
                columnRename = this.getName().substring(0, this.getName().lastIndexOf("__") + 2) + columnRename;
            }
            // Remove numbered prefix if its snuck back in.
            if (columnRename.indexOf(".") >= 0)
                columnRename = columnRename.substring(columnRename.lastIndexOf(".") + 1);
            // Now simply check to see if the name is used, and
            // then add an incrementing number to it until it is unique.
            int suffix = 1;
            while (entries.contains(columnRename))
                columnRename = baseName + '_' + (userRequest
                        ? 'c' : 'r') + suffix++ + keySuffix;
        }
        // Check and change it.
        if (oldValue.equals(columnRename))
            return;

        this.renamed = true;
        this.setName(columnRename);
        this.setInternalName(columnRename);
        this.setDisplayName(columnRename);
        this.setDescription(columnRename);

        // this.pcs.firePropertyChange("columnRename", oldValue, columnRename);
    }

    private boolean isKeyCol() {
        // Are we in our table's PK or FK?
        final Set<Column> cols = new HashSet<Column>();
        for (Key key : this.getDatasetTable().getKeys())
            cols.addAll(key.getColumns());
        return cols.contains(this);
    }

}
