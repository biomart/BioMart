package org.biomart.configurator.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.biomart.api.enums.Operation;
import org.biomart.backwardCompatibility.BackwardCompatibility;
import org.biomart.backwardCompatibility.DatasetFromUrl;
import org.biomart.backwardCompatibility.MartInVirtualSchema;
import org.biomart.common.exceptions.AssociationException;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.exceptions.ValidationException;
import org.biomart.common.resources.Log;
import org.biomart.common.resources.Resources;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.McEventBus;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.ConnectionPool;
import org.biomart.configurator.utils.FileLinkObject;
import org.biomart.configurator.utils.JdbcLinkObject;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.UrlLinkObject;
import org.biomart.configurator.utils.Validation;
import org.biomart.configurator.utils.type.ComponentStatus;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.configurator.utils.type.DatasetTableType;
import org.biomart.configurator.utils.type.IdwViewType;
import org.biomart.configurator.utils.type.McEventProperty;
import org.biomart.configurator.utils.type.PartitionType;
import org.biomart.configurator.utils.type.ValidationStatus;
import org.biomart.configurator.view.gui.dialogs.CreateLinkIndexDialog;
import org.biomart.configurator.view.idwViews.McViewPortal;
import org.biomart.configurator.view.idwViews.McViews;
import org.biomart.objects.enums.FilterType;
import org.biomart.objects.objects.Attribute;
import org.biomart.objects.objects.Column;
import org.biomart.objects.objects.Config;
import org.biomart.objects.objects.Container;
import org.biomart.objects.objects.DatasetColumn;
import org.biomart.objects.objects.DatasetTable;
import org.biomart.objects.objects.ElementList;
import org.biomart.objects.objects.Exportable;
import org.biomart.objects.objects.Filter;
import org.biomart.objects.objects.ForeignKey;
import org.biomart.objects.objects.Importable;
import org.biomart.objects.objects.Key;
import org.biomart.objects.objects.Link;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.MartConfiguratorObject;
import org.biomart.objects.objects.MartRegistry;
import org.biomart.objects.objects.Options;
import org.biomart.objects.objects.PartitionTable;
import org.biomart.objects.objects.PrimaryKey;
import org.biomart.objects.objects.RDFClass;
import org.biomart.objects.objects.Relation;
import org.biomart.objects.objects.RelationSource;
import org.biomart.objects.objects.RelationTarget;
import org.biomart.objects.objects.SourceColumn;
import org.biomart.objects.objects.SourceTable;
import org.biomart.objects.portal.GuiContainer;
import org.biomart.objects.portal.MartPointer;
import org.biomart.objects.portal.UserGroup;
import org.biomart.processors.Processor;
import org.biomart.processors.ProcessorGroup;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ObjectController {

    public void initMarts(DataLinkInfo dlinkInfo, UserGroup user, String group) throws MartBuilderException {
        /*
         * when the sourcegroup in dlinkInfo is true, it will override group
         */
        boolean sourcegroup = dlinkInfo.isSourceGrouped();
        boolean addDefaultPortal = dlinkInfo.isIncludePortal();
        MartController.getInstance().setChanged(true);

        if (dlinkInfo.getDataLinkType().equals(DataLinkType.SOURCE)) {
            MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
            Collection<Mart> dss = MartController.getInstance().requestCreateMartsFromSource(registry, dlinkInfo, null);
            if (dss.size() == 0)
                return;
            // do naive
            List<Config> configList = new ArrayList<Config>();
            List<Mart> martList = new ArrayList<Mart>();
            for (Mart mc : dss) {
                martList.add(mc);
                mc.setVirtual(true);
                String groupName = sourcegroup
                        ? MartController.getInstance().getFirstDBNameFromMart(mc) : group;
                mc.setGroupName(groupName);
                if (null != registry.getMartByName(mc.getName())) {
                    String name = McGuiUtils.INSTANCE.getUniqueMartName(registry, mc.getName());
                    mc.setName(name);
                    mc.setInternalName(name);
                }
                registry.addMart(mc);
            }
            this.doNaiveMasterConfig(martList);
            for (Mart mc : martList) {
                configList.add(mc.getDefaultConfig());
            }
            if (addDefaultPortal) {
                this.addMartPointers(martList, user, sourcegroup);
            }
        } else if (dlinkInfo.getDataLinkType().equals(DataLinkType.TARGET)) {
            if (dlinkInfo.getJdbcLinkObject().getDsInfoMap().keySet().iterator().next().isUseBCForDB()) {
                this.createMartFromBC_db(dlinkInfo, null, user, group);
            } else {
                MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
                Collection<Mart> dss = MartController.getInstance().requestCreateMartsFromTarget(registry, dlinkInfo);
                // do naive
                List<Mart> martList = new ArrayList<Mart>();
                List<Config> configList = new ArrayList<Config>();
                for (Mart mc : dss) {
                    martList.add(mc);
                    String groupName = sourcegroup
                            ? MartController.getInstance().getFirstDBNameFromMart(mc) : group;
                    mc.setGroupName(groupName);
                    registry.addMart(mc);
                }

                this.doNaiveMasterConfig(martList);
                MartController.getInstance().fixObjects(registry);
                for (Mart mc : martList) {
                    configList.add(mc.getDefaultConfig());
                }
                if (addDefaultPortal) {
                    this.addMartPointers(martList, user, sourcegroup);
                }
            }
        } else if (dlinkInfo.getDataLinkType().equals(DataLinkType.URL)) {
            if (dlinkInfo.getUrlLinkObject().isVersion8())
                createMartFromUrl8(dlinkInfo, user, group);
            else
                createMartFromBC_Url(dlinkInfo, user, group);
        } else if (dlinkInfo.getDataLinkType().equals(DataLinkType.FILE)) {
            FileLinkObject flinkObject = dlinkInfo.getFileLinkObject();
            for (Map.Entry<MartInVirtualSchema, List<DatasetFromUrl>> entry : flinkObject.getDsInfoMap().entrySet()) {
                MartInVirtualSchema martV = entry.getKey();
                if (martV.isURLMart()) {
                    createMartFromBC_Url(dlinkInfo, martV, user, group);
                } else {
                    DataLinkInfo fakeDLinkInfo = new DataLinkInfo(DataLinkType.TARGET);
                    JdbcLinkObject fakeJdbcLinkObj = martV.getJdbcLinkObject();
                    fakeDLinkInfo.setJdbcLinkObject(fakeJdbcLinkObj);
                    this.createMartFromBC_db(fakeDLinkInfo, fakeJdbcLinkObj.getSchemaName(), user, group);
                }
            }
        }
    }

    /**
     * 
     * @param dlinkInfo
     * @param schemaName
     *            for non-mysql dbs
     * @param user
     * @param group
     * @param create
     */
    private void createMartFromBC_db(DataLinkInfo dlinkInfo, String schemaName, UserGroup user, String group) {
        boolean sourcegroup = dlinkInfo.isSourceGrouped();
        boolean addDefaultPortal = dlinkInfo.isIncludePortal();
        // ProgressDialog dialog = ProgressDialog.getInstance();
        // dialog.setStatus("upgrading configuration to 0.8 ... ");
        if (schemaName == null)
            schemaName = dlinkInfo.getJdbcLinkObject().getDsInfoMap().keySet().iterator().next().getSchema();
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        BackwardCompatibility bc = new BackwardCompatibility();
        JdbcLinkObject linkObject = new JdbcLinkObject(dlinkInfo.getJdbcLinkObject().getConnectionBase(), dlinkInfo
                .getJdbcLinkObject().useSchema()
                ? dlinkInfo.getJdbcLinkObject().getDatabaseName() : schemaName, schemaName, dlinkInfo
                .getJdbcLinkObject().getUserName(), dlinkInfo.getJdbcLinkObject().getPassword(), dlinkInfo
                .getJdbcLinkObject().getJdbcType(), dlinkInfo.getJdbcLinkObject().getPartitionRegex(), dlinkInfo
                .getJdbcLinkObject().getPtNameExpression(), dlinkInfo.getJdbcLinkObject().isKeyGuessing());
        // assume only one database selected for now
        // update jdbcLinkObject
        dlinkInfo.setJdbcLinkObject(linkObject);
        bc.setMartRegistry(registry);
        bc.setConnectionObject(ConnectionPool.Instance.getConnection(linkObject));
        bc.setDataLinkInfoObject(dlinkInfo);
        List<Mart> martList = bc.parseOldTemplates();
        ConnectionPool.Instance.releaseConnection(linkObject);
        // dialog.setStatus("creating mart ...");
        // this.validateMarts(martList);

        // this.addMartNodes(martList);
        List<Config> configList = new ArrayList<Config>();
        for (Mart mart : martList) {
            String groupName = sourcegroup
                    ? MartController.getInstance().getFirstDBNameFromMart(mart) : group;
            mart.setGroupName(groupName);
            registry.addMart(mart);
        }
        MartController.getInstance().fixObjects(registry);
        for (Mart mc : martList) {
            Config config = mc.getDefaultConfig();
            config.setMaster(true);
            configList.add(config);
        }
        this.addOptions(bc.getOptions(), martList);
        this.afterBackwardCompatibility(dlinkInfo.isRebuildLink());

        if (addDefaultPortal) {
            this.addMartPointers(martList, user, sourcegroup);
        }
    }

    private void createMartFromBC_Url(DataLinkInfo dlinkInfo, UserGroup user, String group) {
        // ProgressDialog dialog = ProgressDialog.getInstance();
        // dialog.setStatus(Resources.get("UPGRADINGMESSAGE"));
        // when sourcegroup is true, it will override group
        boolean sourcegroup = dlinkInfo.isSourceGrouped();
        Map<String, Set<String>> martDsList = new HashMap<String, Set<String>>();
        Map<String, List<Mart>> martMap = new LinkedHashMap<String, List<Mart>>();

        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        BackwardCompatibility bc = new BackwardCompatibility();
        bc.setDataLinkInfoObject(dlinkInfo);
        bc.setMartRegistry(registry);

        UrlLinkObject conObj = dlinkInfo.getUrlLinkObject();
        Map<MartInVirtualSchema, List<DatasetFromUrl>> dsInfoMap = conObj.getDsInfoMap();
        List<DatasetFromUrl> dsList = new ArrayList<DatasetFromUrl>();
        for (Map.Entry<MartInVirtualSchema, List<DatasetFromUrl>> entry : dsInfoMap.entrySet()) {
            MartInVirtualSchema ms = entry.getKey();
            if (!ms.isVisible()) {
                for (DatasetFromUrl ds : entry.getValue()) {
                    ds.setVisible(false);
                }
            }
            String commonName = ms.getName();
            Set<String> dsNameList = martDsList.get(commonName);
            if (dsNameList == null) {
                dsNameList = new HashSet<String>();
                martDsList.put(commonName, dsNameList);
            }

            dsList.clear();
            if (dlinkInfo.isBCPartitioned()) {
                dsList.addAll(entry.getValue());
                bc.setDatasetsForUrl(dsList);
                List<Mart> result = bc.parseOldTemplates();
                if (!result.isEmpty()) {
                    List<Mart> mlist = martMap.get(commonName);
                    if (null == mlist) {
                        mlist = new ArrayList<Mart>();
                        martMap.put(commonName, mlist);
                    }
                    mlist.addAll(result);
                    for (Mart mart : result) {
                        dsNameList.add(mart.getName());
                    }
                }
            } else {
                for (DatasetFromUrl tmpDs : entry.getValue()) {
                    dsList.add(tmpDs);
                    bc.setDatasetsForUrl(dsList);
                    List<Mart> result = bc.parseOldTemplates();
                    dsList.clear();
                    if (!result.isEmpty()) {
                        List<Mart> mlist = martMap.get(commonName);
                        if (null == mlist) {
                            mlist = new ArrayList<Mart>();
                            martMap.put(commonName, mlist);
                        }
                        mlist.addAll(result);
                        for (Mart mart : result) {
                            dsNameList.add(mart.getName());
                        }
                    }
                }
            }

        }

        if (martMap.size() == 0)
            return;
        // add to option element
        // dialog.setStatus("creating mart ...");

        List<Config> configList = new ArrayList<Config>();
        for (Map.Entry<String, List<Mart>> entry : martMap.entrySet()) {
            String groupName = group;
            if (sourcegroup)
                groupName = entry.getKey();
            for (Mart mart : entry.getValue()) {
                String martName = registry.getNextMartName(mart.getName());
                if (!martName.equals(mart.getName())) {
                    mart.setName(martName);
                    mart.setDisplayName(martName);
                }
                registry.addMart(mart);
                mart.setGroupName(groupName);
            }

        }

        MartController.getInstance().fixObjects(registry);
        List<Mart> allMarts = new ArrayList<Mart>();

        for (Map.Entry<String, List<Mart>> entry : martMap.entrySet()) {
            for (Mart mc : entry.getValue()) {
                allMarts.add(mc);
                Config config = mc.getDefaultConfig();
                config.setMaster(true);
                configList.add(config);
            }
        }
        this.addOptions(bc.getOptions(), allMarts);
        // rebuild link
        this.afterBackwardCompatibility(dlinkInfo.isRebuildLink());
        if (dlinkInfo.isIncludePortal()) {
            this.addMartPointers(allMarts, user, sourcegroup);
        }

    }

    private void createMartFromUrl8(DataLinkInfo dlinkInfo, UserGroup user, String group) {
        ProgressDialog dialog = ProgressDialog.getInstance();
        dialog.setStatus("getting data from url ... ");
        UrlLinkObject conObj = dlinkInfo.getUrlLinkObject();
        Map<String, List<String>> mpList = conObj.getMpList();
        StringBuffer baseStr = new StringBuffer(conObj.getFullHost());
        if (!McUtils.isStringEmpty(conObj.getPort())) {
            baseStr.append(":" + conObj.getPort());
        }
        baseStr.append("/martservice/xml/configs/");
        Map<String, Mart> martMap = new HashMap<String, Mart>();
        List<MartPointer> newMpList = new ArrayList<MartPointer>();
        for (Map.Entry<String, List<String>> entry : mpList.entrySet()) {
            for (String mpName : entry.getValue()) {
                String url = baseStr.toString() + mpName;
                String key = dlinkInfo.getUrlLinkObject().getKeys();
                Document registryDocument = null;
                if (McUtils.isStringEmpty(key))
                    registryDocument = McUtils.getDocumentFromUrl(url, dlinkInfo.getUrlLinkObject().getUserName(),
                            dlinkInfo.getUrlLinkObject().getPassword());
                else
                    registryDocument = McUtils.buildDocument(McUtils.getUrlContentFromOAuth(url, key));
                if (registryDocument == null) {
                    return;
                }
                Element rootElement = registryDocument.getRootElement();
                Element martElement = rootElement.getChild(XMLElements.MART.toString());
                Element mpElement = rootElement.getChild(XMLElements.MARTPOINTER.toString());
                Mart mart = new Mart(martElement);
                // modify the partitiontable
                PartitionTable pt = mart.getSchemaPartitionTable();
                // replace the first 6 columns
                pt.setColumnValue(PartitionUtils.CONNECTION, conObj.getFullHost());
                pt.setColumnValue(PartitionUtils.DATABASE, conObj.getPort());
                pt.setColumnValue(PartitionUtils.SCHEMA, conObj.getPath());
                pt.setColumnValue(PartitionUtils.USERNAME, conObj.getUserName());
                pt.setColumnValue(PartitionUtils.PASSWORD, conObj.getPassword());
                pt.setColumnValue(PartitionUtils.VERSION, "0.8");
                pt.setColumnValue(PartitionUtils.KEY, key);
                // mart.synchronizedFromXML();
                Mart oldMart = martMap.get(mart.getName());
                if (oldMart == null)
                    martMap.put(mart.getName(), mart);
                else {
                    // get the config and add to the oldmart
                    oldMart.addConfig(mart.getDefaultConfig());
                }
                MartPointer mp = new MartPointer(mpElement);
                // mp.synchronizedFromXML();
                newMpList.add(mp);
            }
        }
        if (newMpList.isEmpty())
            return;
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        // add to option element
        dialog.setStatus("creating mart ...");

        // keep track of the mart name change
        Map<String, String> changedNameMap = new HashMap<String, String>();
        for (Mart mart : martMap.values()) {
            if (registry.getMartByName(mart.getName()) != null) {
                // rename
                String oldName = mart.getName();
                String martName = McGuiUtils.INSTANCE.getUniqueMartName(registry, oldName);
                mart.setName(martName);
                changedNameMap.put(oldName, martName);
            }
            registry.addMart(mart);
            mart.setGroupName(group);
        }
        // add martpointer
        GuiContainer gc = ((McViewPortal) (McViews.getInstance().getView(IdwViewType.PORTAL)))
                .getSelectedGuiContainer();
        for (MartPointer mp : newMpList) {
            if (gc.getMartPointerByName(mp.getName()) != null) {
                // rename
                String mpName = McGuiUtils.INSTANCE.getUniqueMartPointerName(gc.getRootGuiContainer(), mp.getName());
                mp.setName(mpName);
            }
            if (changedNameMap.get(mp.getPropertyValue(XMLElements.MART)) != null)
                mp.setProperty(XMLElements.MART, changedNameMap.get(mp.getPropertyValue(XMLElements.MART)));

            gc.addMartPointer(mp);
            mp.synchronizedFromXML();
        }
        registry.synchronizedFromXML();
        MartController.getInstance().fixObjects(registry);
        // create master config
        for (Mart mart : martMap.values()) {
            if (mart.getMasterConfig() == null) {
                Config config = mart.getDefaultConfig();
                // copy config
                Element configElement = config.generateXml();
                Config newConfig = new Config(configElement);
                newConfig.setName(mart.getName());
                newConfig.setMaster(true);
                mart.addConfig(newConfig);
                newConfig.synchronizedFromXML();
            }
        }
    }

    /**
     * for adding marts from a file, the different with other method is that, the connection parameters are from martV
     * instead of dlinkInfo
     * 
     * @param dlinkInfo
     * @param martV
     * @param user
     * @param group
     */
    private void createMartFromBC_Url(DataLinkInfo dlinkInfo, MartInVirtualSchema martV, UserGroup user, String group) {
        // ProgressDialog dialog = ProgressDialog.getInstance();
        // dialog.setStatus(Resources.get("UPGRADINGMESSAGE"));
        // when sourcegroup is true, it will override group
        boolean sourcegroup = dlinkInfo.isSourceGrouped();
        Map<String, Set<String>> martDsList = new HashMap<String, Set<String>>();
        FileLinkObject conObj = dlinkInfo.getFileLinkObject();
        Map<MartInVirtualSchema, List<DatasetFromUrl>> dsInfoMap = conObj.getDsInfoMap();
        List<DatasetFromUrl> dsList = dsInfoMap.get(martV);

        String commonName = martV.getName();

        Set<String> dsNameList = new HashSet<String>();
        martDsList.put(commonName, dsNameList);

        for (DatasetFromUrl tmpDs : dsList) {
            dsNameList.add(tmpDs.getName());
        }

        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        BackwardCompatibility bc = new BackwardCompatibility();
        /*
         * create a fake datalinkinfo for BC, because BC only takes URL or DB
         */
        DataLinkInfo fakeDLinkInfo = new DataLinkInfo(DataLinkType.URL);
        UrlLinkObject fakeUrlLinkObj = new UrlLinkObject();
        fakeUrlLinkObj.setHost(martV.getHost());
        String fullHost = "http://" + martV.getHost();
        fakeUrlLinkObj.setFullHost(fullHost);
        fakeUrlLinkObj.setPort(martV.getPort());
        fakeUrlLinkObj.setPath(martV.getPath());
        fakeUrlLinkObj.setVersion8(false);
        fakeDLinkInfo.setUrlLinkObject(fakeUrlLinkObj);
        bc.setDataLinkInfoObject(fakeDLinkInfo);
        bc.setMartRegistry(registry);
        bc.setDatasetsForUrl(dsList);

        List<Mart> martList = bc.parseOldTemplates();
        if (martList.isEmpty())
            return;
        // add to option element
        // dialog.setStatus("creating mart ...");

        List<Config> configList = new ArrayList<Config>();
        for (Mart mart : martList) {
            String groupName = group;
            if (sourcegroup) {
                for (Map.Entry<String, Set<String>> entry : martDsList.entrySet()) {
                    if (entry.getValue().contains(mart.getName())) {
                        groupName = entry.getKey();
                        break;
                    }
                }
            }
            mart.setGroupName(groupName);
            registry.addMart(mart);
        }
        MartController.getInstance().fixObjects(registry);
        for (Mart mc : martList) {
            Config config = mc.getDefaultConfig();
            config.setMaster(true);
            configList.add(config);
        }
        this.addOptions(bc.getOptions(), martList);
        // rebuild link
        this.afterBackwardCompatibility(dlinkInfo.isRebuildLink());
        if (dlinkInfo.isIncludePortal()) {
            this.addMartPointers(martList, user, sourcegroup);
        }

    }

    public void generateAttributeRDF(Attribute attribute, Config config) {
        // Require that a user runs 'Generate Semantics' again...
        return;

        /*
         * if (attribute.isPseudoAttribute()) return; attribute.setRDF(createDefaultRDF(config.getName(),
         * attribute.getName()));
         */
    }

    public void generateFilterRDF(Filter filter, Config config) {
        // Require that a user runs 'Generate Semantics' again...
        return;

        /*
         * if (filter.isFilterList()) return; filter.setRDF(createDefaultRDF(config.getName(), filter.getName()));
         */
    }

    public void generateRDF(Attribute mainAttribute, Config config, boolean pseudoExposed) {
        String uidAttributeName;

        // No primary key present in the main table.
        if (mainAttribute == null)
            uidAttributeName = "";
        else
            uidAttributeName = mainAttribute.getName();

        /*
         * for (RDFClass rdfClass : config.getRDFClasses()) config.removeRDF(rdfClass);
         */
        config.removeAllRDF();

        final String configName = config.getName();
        final String martClassName = getDefaultRDFClassname(configName);

        final RDFClass rdfClass = new RDFClass(martClassName);
        rdfClass.setUID(uidAttributeName);

        config.addRDF(rdfClass);

        Collection<Attribute> attributes = config.getAllAttributes();

        Map<String, String> seenAttributes = new HashMap<String, String>();

        Collection<Filter> filters = config.getAllFilters();
        for (Filter filter : filters) {
            if (Validation.validate(filter, false) != ValidationStatus.VALID)
                continue;

            // Will be useful later, but needs dialog whether the user
            // really wants to overwrite the existing value:
            // if (existingRDF != null && !existingRDF.isEmpty())
            // continue;

            // Filterlists are not needed for SPARQL. Limit filters
            // work anyway.
            if (filter.isFilterList()) {
                filter.setRDF(""); // Ignore this kind of filter.
                continue;
            }
            /*
             * filter is a pointer and pointing to a filterlist
             */
            if (null == filter.getAttribute()) {
                filter.setRDF(""); // Ignore this kind of filter.
                continue;
            }

            filter.setRDF(createDefaultRDF(configName, filter.getName()));

            // Record which filter this attribute was last seen in.
            seenAttributes.put(filter.getAttribute().getName(), filter.getName());
        }

        for (Attribute attribute : attributes) {
            if (Validation.validate(attribute, false) != ValidationStatus.VALID)
                continue;

            if (!pseudoExposed && attribute.isPseudoAttribute())
                continue;

            if (seenAttributes.containsKey(attribute.getName())
                    && seenAttributes.get(attribute.getName()).equals(attribute.getName())) {
                // RDF-info defined in the filter. However, if the names of the
                // attribute and filter are different, then we would not be able
                // to autogenerate a SPARQL-query. So, in cases where
                // attribute name != filter name
                // it is necessary to have the attribute defined as an RDF-property.
                attribute.setRDF("");
                continue;
            }

            attribute.setRDF(createDefaultRDF(configName, attribute.getName()));
        }

        McEventBus.getInstance().fire(McEventProperty.REFRESH_TARGET.toString(), null);
    }

    private static String getDefaultRDFClassname(String name) {
        // NCName has to start with a letter or an underscore.
        // This regexp has to be identical to the regexp in query.js
        if (!Pattern.matches("^[a-zA-Z_].*", name))
            name = "_" + name;

        return "class:" + name;

        // return "objects:" + toCamelCase(name);
    }

    private String createDefaultRDF(String configName, String propertyName) {
        StringBuilder rdf = new StringBuilder();

        // NCName has to start with a letter or an underscore.
        // This regexp has to be identical to the regexp in query.js
        if (!Pattern.matches("^[a-zA-Z_].*", propertyName))
            propertyName = "_" + propertyName;

        // FORMAT:
        // rdf-property ::= property [ "|" property ]
        // property ::= class-name ";" property-name ";" property-class ";" uri-attributes
        // class-name The RDF-class to which the RDF-property of the attribute belongs to.
        // property-name The RDF-property name under which the attribute can be queried.
        // property-class The datatype of the attribute's values. This is the property's "range".

        rdf.append(getDefaultRDFClassname(configName)); // class-name
        rdf.append(";");
        rdf.append("attribute:");
        rdf.append(propertyName); // property-name
        rdf.append(";");
        rdf.append("rdf:PlainLiteral"); // property-class

        return rdf.toString();
    }

    private void doNaiveMasterConfig(List<Mart> marts) {
        for (Mart mart : marts) {
            // create config object and element
            Config config = this.createNaiveConfig(mart, mart.getName());
            config.setMaster(true);
            mart.addConfig(config);
        }
    }

    public Config createNaiveConfig(Mart mart, String configName) {
        Config config = new Config(configName);
        mart.addConfig(config);
        config.setProperty(XMLElements.DATASETDISPLAYNAME, "(p0c" + PartitionUtils.DISPLAYNAME + ")");
        config.setProperty(XMLElements.DATASETHIDEVALUE, "(p0c" + PartitionUtils.HIDE + ")");
        // martController.addConfig(configObject);
        // TODO should combine the creation of two
        // add root container object
        Container tmpContainer = new Container("root");
        config.addRootContainer(tmpContainer);
        // add attribute container object
        Container attributeRootContainer = new Container(XMLElements.ATTRIBUTE.toString());
        tmpContainer.addContainer(attributeRootContainer);
        // add filter container object
        Container filterRootContainer = new Container(XMLElements.FILTER.toString());
        tmpContainer.addContainer(filterRootContainer);
        // need to order the DataSetTable
        List<DatasetTable> mainList = new ArrayList<DatasetTable>();
        List<DatasetTable> subList = new ArrayList<DatasetTable>();
        List<DatasetTable> dmList = new ArrayList<DatasetTable>();
        for (Iterator<DatasetTable> it = mart.getDatasetTables().iterator(); it.hasNext();) {
            DatasetTable dsTable = (DatasetTable) it.next();
            if (dsTable.getType().equals(DatasetTableType.MAIN))
                mainList.add(dsTable);
            else if (dsTable.getType().equals(DatasetTableType.MAIN_SUBCLASS))
                subList.add(dsTable);
            else
                dmList.add(dsTable);
        }

        // order sublist
        while (subList.size() > 0) {
            DatasetTable lastDst = mainList.get(mainList.size() - 1);
            // make sure the loop ends
            boolean found = false;
            for (DatasetTable dst : subList) {
                if (lastDst.equals(dst.getParentMainTable())) {
                    mainList.add(dst);
                    subList.remove(dst);
                    found = true;
                    break;
                }
            }
            if (!found)
                break;
            // should not go here, hack TODO check
            // subList.remove(lastDst);
        }
        // only keep the last sub main, since it includes all columns
        while (mainList.size() > 1)
            mainList.remove(0);
        mainList.addAll(dmList);

        // now mainList should have all tables with main tables in the begining
        for (DatasetTable dsTable : mainList) {
            // add filter and attribute container for each dataset table
            String tbName = dsTable.getName();

            Container attributeContainer = new Container(tbName + Resources.get("ATTRIBUTESUFFIX"));
            attributeRootContainer.addContainer(attributeContainer);

            Container filterContainer = new Container(tbName + Resources.get("FILTERSUFFIX"));
            filterRootContainer.addContainer(filterContainer);

            this.doNaiveTable(config, dsTable, attributeContainer, filterContainer);
        }
        return config;
    }

    private void doNaiveTable(Config mart, DatasetTable dst, Container attributeContainer, Container filterContainer) {
        for (final Iterator<Column> ci = dst.getColumnList().iterator(); ci.hasNext();) {
            DatasetColumn col = (DatasetColumn) ci.next();
            // check name
            String attName = McUtils.getUniqueAttributeName(mart,
                    dst.getName() + Resources.get("tablenameSep") + col.getName());
            Attribute attribute = new Attribute(col, attName);
            // use the column name as display name and capitalize first letter
            String displayname = col.getName();
            displayname = (displayname.substring(0, 1).toUpperCase() + displayname.substring(1)).replace("_", " ");
            attribute.setDisplayName(displayname);
            String filterName = McGuiUtils.INSTANCE.getUniqueFilterName(mart, attName);
            Filter filter = new Filter(attribute, filterName);
            filter.setDisplayName(displayname);
            attributeContainer.addAttribute(attribute);
            filterContainer.addFilter(filter);
        }
    }

    private GuiContainer doNaivePortal(List<Config> configs, UserGroup user, GuiContainer gc) {
        List<MartPointer> mpList = new ArrayList<MartPointer>();
        // get admin user
        for (Config config : configs) {
            MartPointer mp = new MartPointer(config, config.getName());
            mp.setOperation(Operation.SINGLESELECT);
            mp.setConfig(config);
            mp.addUser(user);

            // mp.orderDatasetList(new DisplayNameComparator());
            gc.addMartPointer(mp);
            Container rootc = config.getRootContainer();
            // add default processorgroups
            ProcessorGroup gridPg = new ProcessorGroup(mp, XMLElements.TABPROCESSORGROUP.toString());
            Processor htmlProcessor = new Processor(gridPg, XMLElements.HTMLPROCESSOR.toString());
            htmlProcessor.setContainer(rootc);
            htmlProcessor.addExportFormatter(XMLElements.HTMLPROCESSOR.toString());
            htmlProcessor.addExportFormatter(XMLElements.XLSPROCESSOR.toString());
            gridPg.setDefaultProcessor(htmlProcessor);

            Processor csvProcessor = new Processor(gridPg, XMLElements.CSVPROCESSOR.toString());
            csvProcessor.setContainer(rootc);
            csvProcessor.addExportFormatter(XMLElements.CSVPROCESSOR.toString());
            csvProcessor.addExportFormatter(XMLElements.XLSPROCESSOR.toString());

            Processor tsvProcessor = new Processor(gridPg, XMLElements.TSVPROCESSOR.toString());
            tsvProcessor.setContainer(rootc);
            tsvProcessor.addExportFormatter(XMLElements.TSVPROCESSOR.toString());
            tsvProcessor.addExportFormatter(XMLElements.XLSPROCESSOR.toString());
            mp.addProcessorGroup(gridPg);

            ProcessorGroup sequencesPg = new ProcessorGroup(mp, XMLElements.SEQUENCESPROCESSORGROUP.toString());
            Processor fastaProcessor = new Processor(sequencesPg, XMLElements.FASTAPROCESSOR.toString());
            fastaProcessor.setContainer(rootc);
            fastaProcessor.addExportFormatter(XMLElements.FASTAPROCESSOR.toString());
            fastaProcessor.addExportFormatter(XMLElements.TSVPROCESSOR.toString());
            sequencesPg.setDefaultProcessor(fastaProcessor);

            Processor gffProcessor = new Processor(sequencesPg, XMLElements.GFFPROCESSOR.toString());
            gffProcessor.setContainer(rootc);
            gffProcessor.addExportFormatter(XMLElements.GFFPROCESSOR.toString());
            gffProcessor.addExportFormatter(XMLElements.CSVPROCESSOR.toString());
            mp.addProcessorGroup(sequencesPg);

            ProcessorGroup graphsPg = new ProcessorGroup(mp, XMLElements.GRAPHSPROCESSORGROUP.toString());
            Processor kaplaProcessor = new Processor(graphsPg, XMLElements.KAPLAPROCESSOR.toString());
            kaplaProcessor.setContainer(rootc);
            kaplaProcessor.addExportFormatter(XMLElements.KAPLAPROCESSOR.toString());
            mp.addProcessorGroup(graphsPg);
            graphsPg.setDefaultProcessor(kaplaProcessor);

            mpList.add(mp);

        }

        // add links
        // this.createLinks(gc,mpList);
        // this.createLinks(registry.getPortal().getRootGuiContainer().getMartPointerList());
        // add them to the guicontainer
        return gc;
    }

    public List<Mart> groupMarts(String martName, List<Mart> martList) {
        List<Mart> newMartList = new ArrayList<Mart>();
        if (martList.isEmpty())
            return newMartList;

        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        DatasetTable centralTable = martList.get(0).getMainTable();
        String centralTableName = centralTable.getName();
        Mart newMart = new Mart(registry, martName, null);
        boolean isNewMartVisible = false;
        // create a new schema partition table
        PartitionTable ptTable = new PartitionTable(newMart, PartitionType.SCHEMA);
        // each existing mart as a partition row
        for (Mart mart : martList) {
            if (!mart.isHidden())
                isNewMartVisible = true;
            else
                Log.debug("mart is hidden " + mart.getName());
            // handle partition table
            PartitionTable existingPt = mart.getSchemaPartitionTable();
            // assume only one row for now TODO
            String partitionValue = existingPt.getTable().get(0).get(PartitionUtils.DATASETNAME);
            for (List<String> rowList : existingPt.getTable()) {
                // add the visible, display name to it
                // String visible = mart.isHidden()? XMLElements.FALSE_VALUE: XMLElements.TRUE_VALUE;
                // rowList.add(rowList.size()-1,visible);
                // rowList.add(rowList.size()-1,mart.getDisplayName());
                ptTable.addNewRow(rowList);
            }

            // construct dataset table
            for (DatasetTable dst : mart.getDatasetTables()) {
                // find if the datasetTable already exist
                DatasetTable datasetTable = newMart.getTableByName(dst.getName());
                if (datasetTable == null) {
                    datasetTable = new DatasetTable(newMart, dst.getName(), dst.getType());
                    newMart.addTable(datasetTable);
                }
                datasetTable.addInPartitions(partitionValue);

                // construct dataset column
                for (Column column : dst.getColumnList()) {
                    DatasetColumn newColumn = datasetTable.getColumnByName(column.getName());
                    if (newColumn == null) {
                        newColumn = new DatasetColumn(datasetTable, column.getName());
                        datasetTable.addColumn(newColumn);
                    }
                    newColumn.addInPartitions(partitionValue);
                }

                // pk, fk TODO pk,fk equals
                if (datasetTable.getPrimaryKey() == null && dst.getPrimaryKey() != null) {
                    PrimaryKey srcpk = dst.getPrimaryKey();
                    List<Column> srcColumns = srcpk.getColumns();
                    List<Column> dscs = new ArrayList<Column>();
                    for (Column col : srcColumns) {
                        dscs.add(datasetTable.getColumnByName(col.getName()));
                    }
                    PrimaryKey pk = new PrimaryKey(dscs);
                    datasetTable.setPrimaryKey(pk);
                }

                for (ForeignKey srcFk : dst.getForeignKeys()) {
                    List<Column> srcColumns = srcFk.getColumns();
                    List<Column> dscs = new ArrayList<Column>();
                    for (Column col : srcColumns) {
                        Column tmp = datasetTable.getColumnByName(col.getName());
                        if (tmp == null)
                            Log.debug("could not find column");
                        else
                            dscs.add(tmp);
                    }
                    if (dscs.size() == 0)
                        continue;
                    ForeignKey fk = new ForeignKey(dscs);
                    // check if fk exist
                    boolean fkAlreadyExists = false;
                    for (ForeignKey candidateFk : datasetTable.getForeignKeys()) {
                        if (candidateFk.equals(fk)) {
                            fkAlreadyExists = true;
                            break;
                        }
                    }
                    if (!fkAlreadyExists) {
                        datasetTable.getForeignKeys().add(fk);
                    }
                }
            }
            // relations
            final List<DatasetTable> mainTables = new ArrayList<DatasetTable>();
            mainTables.add(mart.getMainTable());
            for (int i = 0; i < mainTables.size(); i++) {
                final DatasetTable sourceMtable = mainTables.get(i);
                final DatasetTable targetTable = (DatasetTable) newMart.getTableByName(sourceMtable.getName());
                if (sourceMtable.getPrimaryKey() != null) {
                    for (final Relation relation : sourceMtable.getPrimaryKey().getRelations()) {
                        final DatasetTable target = (DatasetTable) relation.getManyKey().getTable();
                        // find the same keys in the target dataset

                        Key targetfk = null;
                        Key targetsk = null;
                        Key fk = relation.getFirstKey();
                        Key sk = relation.getSecondKey();
                        for (Key tfk : targetTable.getKeys()) {
                            if (tfk.isKeyEquals(fk)) {
                                targetfk = tfk;
                                break;
                            }
                        }
                        final DatasetTable secondTable = (DatasetTable) newMart.getTableByName(relation.getSecondKey()
                                .getTable().getName());
                        for (Key tfk : secondTable.getKeys()) {
                            if (tfk.isKeyEquals(sk)) {
                                targetsk = tfk;
                                break;
                            }
                        }

                        if (targetfk != null && targetsk != null && !Relation.isRelationExist(targetfk, targetsk)) {

                            try {
                                Relation relObj = new RelationTarget(targetfk, targetsk, relation.getCardinality());

                                relObj.setOriginalCardinality(relation.getOriginalCardinality());
                                // targetfk.addRelation(relObj);
                                // targetsk.addRelation(relObj);
                                relObj.setStatus(ComponentStatus.INFERRED);
                                if (target.getType().equals(DatasetTableType.MAIN_SUBCLASS)) {
                                    mainTables.add(target);
                                    relObj.setSubclassRelation(true, Relation.DATASET_WIDE);
                                }
                            } catch (AssociationException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (ValidationException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else if (target.getType().equals(DatasetTableType.MAIN_SUBCLASS)) {
                            // even we don't add the relation, still need to put the table in loop
                            mainTables.add(target);
                        }

                    }
                }
            }
            // add config
            for (Config config : mart.getConfigList()) {
                if (newMart.getConfigList().isEmpty()) {
                    Container oldRootContainer = config.getRootContainer();
                    Config newConfig = new Config(newMart.getName());
                    newMart.addConfig(newConfig);
                    String configName = oldRootContainer.getName();
                    // if the configName has partition info, remove it
                    String[] tmpName = configName.split("_");
                    StringBuffer sb = new StringBuffer(tmpName[0]);
                    for (int k = 1; k < tmpName.length - 1; k++) {
                        sb.append("_");
                        sb.append(tmpName[k]);
                    }
                    // create root container and merge root container
                    Container rootContainer = new Container(sb.toString());
                    newConfig.addRootContainer(rootContainer);
                    rootContainer.mergeContainer(oldRootContainer);
                    // merge importable/exportable
                    for (ElementList imps : config.getImportableList()) {
                        ElementList newImp = new Importable(newConfig, imps.getName());
                        newImp.addLinkVersion(mart.getName(), imps.getLinkVersion());
                        for (Filter filter : imps.getFilterList()) {
                            Filter newFilter = newConfig.getRootContainer().getFilterRecursively(filter.getName());
                            if (newFilter == null) {
                                Log.debug("merge importable: could not find filter " + filter.getName());
                                continue;
                            }
                            newImp.addFilter(newFilter);
                        }
                        newImp.setProperty(XMLElements.TYPE, imps.getPropertyValue(XMLElements.TYPE));
                        newConfig.addElementList(newImp);
                    }
                    for (ElementList exps : config.getExportableList()) {
                        ElementList newExp = new Exportable(newConfig, exps.getName());
                        newExp.addLinkVersion(mart.getName(), exps.getLinkVersion());
                        for (Attribute attribute : exps.getAttributeList()) {
                            Attribute newAttribute = newConfig.getRootContainer().getAttributeRecursively(
                                    attribute.getName());
                            if (newAttribute == null) {
                                Log.debug("merge exportable: could not find attribute " + attribute.getName());
                                continue;
                            }
                            newExp.addAttribute(newAttribute);
                        }
                        newExp.setDefaultState(exps.isDefault());
                        newExp.setProperty(XMLElements.TYPE, exps.getPropertyValue(XMLElements.TYPE));
                        newConfig.addElementList(newExp);
                    }
                } else {
                    // merge config
                    Config newConfig = newMart.getConfigList().get(0);
                    // merge rootcontainer
                    Container newRootContainer = newConfig.getRootContainer();
                    Container oldRootContainer = config.getRootContainer();
                    newRootContainer.mergeContainer(oldRootContainer);
                    // merge importable/exportable
                    for (ElementList imps : config.getImportableList()) {
                        ElementList newImp = newConfig.getImportableByInternalName(imps.getName());
                        if (newImp == null) {
                            newImp = new Importable(newConfig, imps.getName());
                            for (Filter filter : imps.getFilterList()) {
                                Filter newFilter = newConfig.getRootContainer().getFilterRecursively(filter.getName());
                                if (newFilter == null) {
                                    Log.debug("merge importable: could not find filter " + filter.getName());
                                    continue;
                                }
                                newImp.addFilter(newFilter);
                            }
                            newConfig.addElementList(newImp);
                        }
                        newImp.addLinkVersion(mart.getName(), imps.getLinkVersion());
                    }
                    for (ElementList exps : config.getExportableList()) {
                        ElementList newExp = newConfig.getExportableByInternalName(exps.getName());
                        if (newExp == null) {
                            newExp = new Exportable(newConfig, exps.getName());
                            for (Attribute attribute : exps.getAttributeList()) {
                                Attribute newAttribute = newConfig.getRootContainer().getAttributeRecursively(
                                        attribute.getName());
                                if (newAttribute == null) {
                                    Log.debug("merge exportable: could not find attribute " + attribute.getName());
                                    continue;
                                }
                                newExp.addAttribute(newAttribute);
                            }
                            newConfig.addElementList(newExp);
                        }
                        newExp.addLinkVersion(mart.getName(), exps.getLinkVersion());
                    }
                }
            }
            // remove the old mart objects
            registry.getMartList().remove(mart);
        }
        if (!isNewMartVisible)
            newMart.setHideValue(true);
        else
            newMart.setHideValue(false);
        newMart.setCentralTable(newMart.getTableByName(centralTableName));
        newMart.addPartitionTable(ptTable);
        newMartList.add(newMart);

        // add tree nodes
        // this.addMartNodes(newMartList);
        // List<MartPointer> mpList = this.doNaivePortal(newMartList);
        // this.addPortalNodes(mpList);

        return newMartList;
    }

    public Config addEmptyConfig(Mart mart, String configName, UserGroup user, GuiContainer gc) {
        Config newConfig = new Config(configName);
        newConfig.setMaster(false);
        newConfig.setHideValue(false);

        mart.addConfig(newConfig);
        List<Config> configList = new ArrayList<Config>();
        configList.add(newConfig);
        this.doNaivePortal(configList, user, gc);
        return newConfig;
    }

    /**
     * create a new config based on the master config, then add the new config in mart, and create a martpointer for the
     * new config.
     */

    public Config addConfigFromMaster(Mart mart, String configName, UserGroup user, boolean generateRDF, GuiContainer gc) {
        Config masterConfig = mart.getMasterConfig();
        Element configElement = masterConfig.generateXml(false);
        Config newConfig = new Config(configElement);
        newConfig.setName(configName);
        newConfig.setDisplayName(configName);
        newConfig.setMaster(false);
        // newConfig.setHideValue(false);
        // remove all links from derived configs
        newConfig.clearLink();

        mart.addConfig(newConfig);
        newConfig.synchronizedFromXML();
        List<Config> configList = new ArrayList<Config>();
        configList.add(newConfig);
        this.doNaivePortal(configList, user, gc);
        // Always auto-generate RDF:
        // if(generateRDF)
        this.generateRDF(MartController.getInstance().getMainAttribute(mart), newConfig, false);

        return newConfig;
    }

    public Config addReportConfigFromMaster(Mart mart, String configName, UserGroup user) {
        Config masterConfig = mart.getMasterConfig();
        List<Attribute> attributes = ObjectController.getAttributesInMain(masterConfig);
        Config newConfig = new Config(configName);
        Container reportCon = new Container("report");
        Container attrCon = new Container("Attributes");
        reportCon.addContainer(attrCon);
        newConfig.getRootContainer().addContainer(reportCon);
        for (Attribute attr : attributes) {
            attrCon.addAttribute(attr.cloneMyself(false));
        }
        newConfig.setMaster(false);
        // add link to report config from master
        for (Link link : masterConfig.getLinkList())
            newConfig.addLink(link);
        mart.addConfig(newConfig);
        List<Config> configList = new ArrayList<Config>();
        configList.add(newConfig);
        GuiContainer gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getRootGuiContainer()
                .getReportGuiContainer();
        /*
         * GuiContainer gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().
         * getRootGuiContainer().getGCByNameRecursively("report");
         */
        this.doNaivePortal(configList, user, gc);
        return newConfig;
    }

    public void initReportConfig(Config config, Attribute selectedAtt, GuiContainer gc) {
        Container attrCon = config.getContainerByName("Attributes");

        // move the selected attribute to the first col
        List<Attribute> attributes = attrCon.getAttributeList();
        for (Attribute a : attributes) {
            if (a.getName().equals(selectedAtt.getName())) {
                Collections.swap(attributes, 0, attributes.indexOf(a));
            }
        }

        // add filter
        Filter newfilter = null;
        // if no filter, create a filter in master and copy to report config under root container
        if (selectedAtt.getReferenceFilters().isEmpty()) {
            if (config.getMart().isURLbased()) {
                JOptionPane.showMessageDialog(null,
                        "The chosen attribute can not be used as a filtering criterion for the report");
                return;
            } else {
                String name = McGuiUtils.INSTANCE.getUniqueFilterName(config, selectedAtt.getName());
                newfilter = new Filter(selectedAtt, name);
                Container reportCon = config.getRootContainer().getContainerByNameResursively("report");
                if (reportCon != null)
                    reportCon.addFilter(newfilter);
            }
        }
        // if has filter found, copy it to report config
        else {
            for (Filter filter : selectedAtt.getReferenceFilters()) {
                if (filter.getFilterType().equals(FilterType.TEXT))
                    newfilter = new Filter(filter.generateXml());
            }
            if (newfilter == null && !selectedAtt.getReferenceFilters().isEmpty()) {
                newfilter = new Filter(selectedAtt.getReferenceFilters().get(0).generateXml());
            }
            // newConfig.getRootContainer().addFilter(newfilter);
            Container reportCon = config.getRootContainer().getContainerByNameResursively("report");
            if (reportCon != null)
                reportCon.addFilter(newfilter);

            newfilter.synchronizedFromXML();
        }
        if (newfilter == null)
            return;
        else if (newfilter.isHidden())
            newfilter.setHideValue(false);
        // create meta info for report config
        /*
         * { "layout": { "gene_info":{"rendering":"list","options":{"breakAt":2}},
         * "ped":{"rendering":"heatmap","options":{"heatColumn":0,"displayColumns":[2,3,4],"fallbackColumn":1}},
         * "prot_domain_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "prot_family_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "prot_interpro_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "tm_and_signal_1_gene_report":{"rendering":"list"},
         * "go_biological_process_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "go_cellular_component_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "go_molecular_function_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "xrefs_1_gene_report":{"rendering":"list","options":{"grouped":false}},
         * "microarray_1_gene_report":{"rendering":"list","options":{"grouped":false}} } }
         */

        if (attrCon != null) {
            StringBuilder metaInfo = new StringBuilder();
            metaInfo.append("{" + '"' + "layout" + '"' + ":");
            metaInfo.append("{" + '"' + attrCon.getName() + '"' + ":");
            metaInfo.append("{" + '"' + "rendering" + '"' + ":" + '"' + "list" + '"');
            metaInfo.append("}");
            metaInfo.append("}");
            metaInfo.append("}");
            config.setProperty(XMLElements.METAINFO, metaInfo.toString());
        }
        // create linkout url for that chosen attribute in all other configs
        StringBuilder linkOutURL = new StringBuilder();
        linkOutURL.append("/martreport/?report=");
        linkOutURL.append(gc.getName());
        linkOutURL.append("&");
        linkOutURL.append("mart=");
        linkOutURL.append(config.getName());
        linkOutURL.append("&");
        linkOutURL.append(newfilter.getName());
        linkOutURL.append("=%s%");
        linkOutURL.append("&datasets=%dataset%");

        ObjectController.addLinkURLtoAttribute(selectedAtt, linkOutURL.toString());
        // create a container and copy all main table attributes from master to this container
        Container container = new Container("mainTableAttributes");
        for (Attribute attribute : ObjectController.getAttributesInMain(config.getMart().getMasterConfig())) {
            org.jdom.Element elem = attribute.generateXml();
            Attribute newAtt = new Attribute(elem);
            container.addAttribute(newAtt);
        }
        // create a datasets for link out url for all other configs
        for (Config con : config.getMart().getConfigList()) {
            Attribute datasetAttr = new Attribute("dataset", "Dataset");
            datasetAttr.setConfig(config);
            datasetAttr.setHideValue(true);
            datasetAttr.setProperty(XMLElements.VALUE, "(p0c5)");
            con.getRootContainer().addAttribute(datasetAttr);
        }
    }

    public void importConfig(Mart mart) {
        File loadFile = null;
        final String currentDir = Settings.getProperty("currentOpenDir");
        final JFileChooser xmlFileChooser = new JFileChooser();
        xmlFileChooser.setCurrentDirectory(currentDir == null
                ? new File(".") : new File(currentDir));
        if (xmlFileChooser.showOpenDialog(McViews.getInstance().getView(IdwViewType.PORTAL)) == JFileChooser.APPROVE_OPTION) {
            // Update the load dialog.
            Settings.setProperty("currentOpenDir", xmlFileChooser.getCurrentDirectory().getPath());

            loadFile = xmlFileChooser.getSelectedFile();
        }
        if (loadFile == null)
            return;

        Document document = null;
        try {
            SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
            document = saxBuilder.build(loadFile);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, e.getMessage());
            return;
        }

        Element configElement = document.getRootElement();
        Config config = new Config(configElement);
        Config oldConfig = mart.getConfigByName(config.getName());
        if (null != oldConfig) {
            int n = JOptionPane.showConfirmDialog(null, "Replace the existing config?", "Question",
                    JOptionPane.YES_NO_OPTION);
            if (n == 0) {
                mart.removeConfig(oldConfig);
            }
        }
        mart.addConfig(config);
        config.synchronizedFromXML();
    }

    public Collection<MartConfiguratorObject> getPartitionReferences(PartitionTable ptTable, int col) {
        Set<MartConfiguratorObject> mcObjSet = new HashSet<MartConfiguratorObject>();
        Mart mart = ptTable.getMart();
        return mcObjSet;
    }

    public void setHide(MartConfiguratorObject mcObj, boolean b) {
        if (mcObj instanceof DatasetTable) {
            DatasetTable dst = (DatasetTable) mcObj;
            dst.setHideValue(b);
            Collection<Column> dsclist = dst.getColumnList();
            for (Column col : dsclist) {
                this.setColumnHide((DatasetColumn) col, b);
            }
        } else if (mcObj instanceof DatasetColumn) {
            DatasetColumn dsc = (DatasetColumn) mcObj;
            this.setColumnHide(dsc, b);
        } else if (mcObj instanceof SourceTable) {
            mcObj.setHideValue(b);
        } else if (mcObj instanceof RelationSource) {
            mcObj.setHideValue(b);
        } else if (mcObj instanceof SourceColumn) {
            mcObj.setHideValue(b);
        }
    }

    /*
     * only hide filter, not attributelist, elementlist
     */
    private void setColumnHide(DatasetColumn dsc, boolean b) {
        dsc.setHideValue(b);
        List<Attribute> attList = dsc.getReferences();
        for (Attribute att : attList) {
            att.setHideValue(b);
            List<MartConfiguratorObject> filList = att.getAllReferences();
            for (MartConfiguratorObject fil : filList) {
                if (fil instanceof Filter)
                    fil.setHideValue(b);
            }
        }

    }

    public int getNextPartitionIntName(Mart mart) {
        if (mart.getPartitionTableList().isEmpty())
            return 0;
        int x = 0;
        for (PartitionTable pt : mart.getPartitionTableList()) {
            x = Math.max(x, pt.getNameInteger());
        }
        return x + 1;
    }

    /**
     * list the intersection of all attributes from main table in all configs that are based on the same source
     * 
     * @return
     */
    public static List<Attribute> getAttributesInMain(Mart mart) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        List<DatasetTable> mainTables = mart.getOrderedMainTableList();
        // DatasetTable mainTable = mainTables.get(mainTables.size()-1);
        for (DatasetTable mainTable : mainTables) {
            for (Column column : mainTable.getColumnList()) {
                Attribute foundAtt = null;
                for (Config con : mart.getConfigList()) {
                    boolean foundInConfig = false;
                    for (Attribute att : con.getAllAttributes()) {
                        if (null == att.getDatasetTable())
                            continue;
                        if (att.getPropertyValue(XMLElements.COLUMN).equals(column.getName())
                                && att.getDatasetTable().getType() != DatasetTableType.DIMENSION) {
                            foundInConfig = true;
                            foundAtt = att;
                            break;
                        }
                    }
                    if (!foundInConfig) {
                        foundAtt = null;
                        break;
                    }
                }
                if (foundAtt != null && !attributes.contains(foundAtt))
                    attributes.add(foundAtt);

            }
        }
        return attributes;
    }

    public void requestCreateLinkIndex(Mart mart) {
        CreateLinkIndexDialog cld = new CreateLinkIndexDialog(mart);
    }

    /**
     * remove all the files for mart under ./registry/linkindices
     * 
     * @param mart
     */
    public void requestRemoveLinkIndex(Mart mart) {
        // TODO also check ../registry/linkindices
        File dir = null;
        try {
            dir = new File(McGuiUtils.INSTANCE.getDistIndicesDirectory().getCanonicalPath());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String[] children = dir.list();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (String fileName : children) {
                if (fileName.indexOf(mart.getName() + "_") == 0) {
                    // remove file
                    try {
                        if (!(new File(dir.getCanonicalPath() + "/" + fileName)).delete()) {
                            Log.error("cannot remove file");
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public static List<Attribute> getAttributesInMain(Config config) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        Mart mart = config.getMart();
        List<DatasetTable> mainTables = mart.getOrderedMainTableList();
        // DatasetTable mainTable = mainTables.get(mainTables.size()-1);
        for (DatasetTable mainTable : mainTables) {
            for (Column column : mainTable.getColumnList()) {
                for (Attribute att : config.getAllAttributes()) {
                    if (att.getPropertyValue(XMLElements.TABLE).equals(mainTable.getName())
                            && att.getPropertyValue(XMLElements.COLUMN).equals(column.getName())) {

                        attributes.add(att);
                        break;
                    }
                }
            }
        }

        return attributes;
    }

    public static List<Config> getConfigsFromAttributes(Attribute att) {
        List<Config> configs = new ArrayList<Config>();
        Mart mart = att.getParentConfig().getMart();

        for (Config con : mart.getConfigList()) {

            Attribute foundAtt = con.getAttributeByName(att.getName(), null);
            if (foundAtt != null)
                configs.add(con);
        }

        return configs;
    }

    public static void addLinkURLtoAttribute(Attribute att, String url) {
        Mart mart = att.getParentConfig().getMart();
        for (Config con : mart.getConfigList()) {
            Attribute foundAtt = con.getAttributeByName(att.getName(), null);
            if (foundAtt != null) {
                foundAtt.setLinkOutUrl(url);
            }
        }
    }

    private void afterBackwardCompatibility(boolean rebuildlink) {
        if (rebuildlink)
            this.rebuildBrokenLink();
        this.rebuildBrokenPointer();
    }

    /**
     * when a new mart is added from BC (either relational mart or url), scan the whole registry for the exsiting
     * exportable/importable (they are kept only for this reason), if matching pair found, restore a link (two
     * direction) in master config. Before creating a link, pop up a message dialog (with don't want me anymore) for the
     * user. For now, we recheck all mart combinations, to be more efficient, we can use the marts from bc only.
     * 
     * @param bc
     */
    private void rebuildBrokenLink() {
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        // check all matched exp/imp in master config
        for (Mart mart : registry.getMartList()) {
            Config master = mart.getMasterConfig();
            for (ElementList exp : master.getExportableList()) {
                if (!exp.getPropertyValue(XMLElements.TYPE).equals("link"))
                    continue;
                for (Mart otherMart : registry.getMartList()) {
                    if (otherMart == mart) {
                        continue;
                    }
                    Config otherMaster = otherMart.getMasterConfig();
                    for (ElementList imp : otherMaster.getImportableList()) {
                        if (!imp.getPropertyValue(XMLElements.TYPE).equals("link"))
                            continue;

                        if (exp.getName().equals(imp.getName())) {
                            // is there a link between these two configs? will consider multiple links later
                            if (McUtils.getLink(master, otherMaster) != null)
                                continue;
                            // create for both direction or none
                            boolean valid = true;
                            // create link for master config for now
                            String linkName1 = McUtils.getLinkName(master, otherMaster);
                            String linkName2 = McUtils.getLinkName(otherMaster, master);
                            Link link1 = new Link(linkName1);
                            link1.setPointerMart(otherMart);
                            link1.setPointedConfig(otherMaster);
                            link1.setPointedDataset(McUtils.getLinkedDatasetsForConfigs(otherMaster, master));

                            Link link2 = new Link(linkName2);
                            link2.setPointerMart(mart);
                            link2.setPointedConfig(master);
                            link2.setPointedDataset(McUtils.getLinkedDatasetsForConfigs(master, otherMaster));

                            List<Filter> otherFilters = new ArrayList<Filter>();
                            List<Attribute> otherAttributes = new ArrayList<Attribute>();
                            for (Attribute att : exp.getAttributeList()) {
                                link1.addAttribute(att);
                                Filter fil = master.getFirstFilterByAttributeName(att.getName());
                                if (fil == null) {
                                    fil = new Filter(att, att.getName());
                                    fil.setHideValue(true);
                                    att.getParentContainer().addFilter(fil);
                                }
                                otherFilters.add(fil);
                            }
                            if (valid)
                                for (Filter filter : imp.getFilterList()) {
                                    link1.addFilter(filter);
                                    Attribute att = otherMaster.getAttributeByName(filter.getAttribute().getName(),
                                            new ArrayList<String>());
                                    if (att == null) {
                                        valid = false;
                                        break;
                                    }
                                    otherAttributes.add(att);
                                }

                            if (valid) {
                                boolean create = true;
                                if (create) {
                                    master.addLink(link1);
                                    for (Filter fil : otherFilters) {
                                        link2.addFilter(fil);
                                    }
                                    for (Attribute att : otherAttributes) {
                                        link2.addAttribute(att);
                                    }
                                    otherMaster.addLink(link2);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    private void rebuildBrokenPointer() {
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        Set<String> missingDatasets = new HashSet<String>();
        // fix all broken pointer
        for (Mart mart : registry.getMartList()) {
            for (Config config : mart.getConfigList()) {
                for (Attribute attribute : config.getAllAttributes()) {
                    if (attribute.isPointer() && attribute.getObjectStatus() != ValidationStatus.VALID) {
                        // only handle the one from backwardcompatibility, which means pointedmart and pointedconfig are
                        // empty
                        if (McUtils.isStringEmpty(attribute.getPropertyValue(XMLElements.POINTEDMART))
                                && McUtils.isStringEmpty(attribute.getPropertyValue(XMLElements.POINTEDCONFIG))
                                && !McUtils.isStringEmpty(attribute.getPropertyValue(XMLElements.POINTEDATTRIBUTE))) {
                            boolean fixed = false;
                            String pointedDs = attribute.getPropertyValue(XMLElements.POINTEDDATASET);
                            if (McUtils.isStringEmpty(pointedDs))
                                continue;
                            String realDsName = pointedDs;
                            if (McUtils.hasPartitionBinding(pointedDs)) {
                                // get the first one that match
                                PartitionTable pt = mart.getSchemaPartitionTable();
                                for (int i = 0; i < pt.getTotalRows(); i++) {
                                    realDsName = McUtils.getRealName(pt, i, pointedDs);
                                    Mart otherMart = McGuiUtils.INSTANCE.getMartFromDataset(realDsName);
                                    if (otherMart != null) {
                                        attribute.setPointedMartName(otherMart.getName());
                                        attribute.setPointedConfigName(otherMart.getMasterConfig().getName());
                                        fixed = true;
                                        break;
                                    }
                                }
                            } else {
                                Mart otherMart = McGuiUtils.INSTANCE.getMartFromDataset(realDsName);
                                if (otherMart != null) {
                                    attribute.setPointedMartName(otherMart.getName());
                                    attribute.setPointedConfigName(otherMart.getMasterConfig().getName());
                                    fixed = true;
                                }
                            }
                            if (!fixed) {
                                // add the last dataset if partitioned
                                missingDatasets.add(realDsName);
                            }
                        }
                    }
                }

                for (Filter filter : config.getAllFilters()) {
                    if (filter.isPointer() && filter.getObjectStatus() != ValidationStatus.VALID) {
                        // only handle the one from backwardcompatibility, which means pointedmart and pointedconfig are
                        // empty
                        if (McUtils.isStringEmpty(filter.getPropertyValue(XMLElements.POINTEDMART))
                                && McUtils.isStringEmpty(filter.getPropertyValue(XMLElements.POINTEDCONFIG))
                                && !McUtils.isStringEmpty(filter.getPropertyValue(XMLElements.POINTEDFILTER))) {
                            String pointedDs = filter.getPropertyValue(XMLElements.POINTEDDATASET);
                            boolean fixed = false;
                            if (McUtils.isStringEmpty(pointedDs))
                                continue;
                            String realDsName = pointedDs;
                            if (McUtils.hasPartitionBinding(pointedDs)) {
                                // get the first one that match
                                PartitionTable pt = mart.getSchemaPartitionTable();
                                for (int i = 0; i < pt.getTotalRows(); i++) {
                                    realDsName = McUtils.getRealName(pt, i, pointedDs);
                                    Mart otherMart = McGuiUtils.INSTANCE.getMartFromDataset(realDsName);
                                    if (otherMart != null) {
                                        filter.setPointedMartName(otherMart.getName());
                                        filter.setPointedConfigName(otherMart.getMasterConfig().getName());
                                        fixed = true;
                                        break;
                                    }
                                }
                            } else {
                                Mart otherMart = McGuiUtils.INSTANCE.getMartFromDataset(realDsName);
                                if (otherMart != null) {
                                    filter.setPointedMartName(otherMart.getName());
                                    filter.setPointedConfigName(otherMart.getMasterConfig().getName());
                                    fixed = true;
                                }
                            }
                            if (!fixed) {
                                // add the last dataset if partitioned
                                missingDatasets.add(realDsName);
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<String, Set<String>> getFilterDependents(org.jdom.Element martOption) {
        Map<String, Set<String>> filterDependentMap = new HashMap<String, Set<String>>();
        Element configElement = martOption.getChild(XMLElements.CONFIG.toString());
        @SuppressWarnings("unchecked")
        List<Element> filterElements = configElement.getChildren();
        for (Element filterElement : filterElements) {
            Element parentElement = filterElement;
            @SuppressWarnings("unchecked")
            List<Element> dsElements = filterElement.getChildren(XMLElements.DATASET.toString());
            // need to check all dataset since it may include mixed options
            for (Element dsElement : dsElements) {
                // for now, check if it has 3 level childrens
                @SuppressWarnings("unchecked")
                List<Element> rowElements = dsElement.getChildren();
                if (McUtils.isCollectionEmpty(rowElements))
                    continue;

                for (Element rowElement : rowElements) { // could be row
                    @SuppressWarnings("unchecked")
                    List<Element> subFilterElements = rowElement.getChildren();
                    if (McUtils.isCollectionEmpty(subFilterElements))
                        continue;
                    for (Element subFilter : subFilterElements) {
                        @SuppressWarnings("unchecked")
                        List<Element> subRows = subFilter.getChildren();
                        if (subRows.size() > 0) { // yes, it is the push action filter

                            String key = filterElement.getAttributeValue(XMLElements.NAME.toString());
                            Set<String> subFilters = filterDependentMap.get(key);
                            if (subFilters == null) {
                                subFilters = new HashSet<String>();
                                filterDependentMap.put(key, subFilters);
                            }
                            subFilters.add(subFilter.getAttributeValue(XMLElements.NAME.toString()));
                        }
                    }
                }
            }
        }
        return filterDependentMap;
    }

    private void addOptions(org.jdom.Element bcoptionElement, List<Mart> martList) {
        // add to option element
        org.jdom.Element optionElement = Options.getInstance().getOptionRootElement();

        if (bcoptionElement != null) {
            bcoptionElement.detach();
            if (optionElement == null)
                Options.getInstance().setOptions(bcoptionElement);
            else {
                @SuppressWarnings("unchecked")
                List<org.jdom.Element> el = bcoptionElement.getChildren();
                for (org.jdom.Element e : el) {
                    // find if the mart exist
                    String dsName = e.getAttributeValue(XMLElements.NAME.toString());
                    boolean dsExist = false;
                    @SuppressWarnings("unchecked")
                    List<org.jdom.Element> dsElementList = optionElement.getChildren();
                    for (org.jdom.Element dsElement : dsElementList) {
                        if (dsElement.getAttributeValue(XMLElements.NAME.toString()).equals(dsName)) {
                            dsExist = true;
                            break;
                        }
                    }
                    if (!dsExist) {
                        org.jdom.Element cloneE = (org.jdom.Element) e.clone();
                        cloneE.detach();
                        optionElement.addContent(cloneE);
                        // check push action
                        Map<String, Set<String>> filterMap = this.getFilterDependents(cloneE);
                        if (!filterMap.isEmpty()) {
                            // get the mart
                            Mart mart = null;
                            for (Mart m : martList) {
                                if (m.getName().equals(cloneE.getAttributeValue(XMLElements.NAME.toString()))) {
                                    mart = m;
                                    break;
                                }
                            }
                            Config config = mart.getMasterConfig();
                            for (Map.Entry<String, Set<String>> entry : filterMap.entrySet()) {
                                for (String subfilterStr : entry.getValue()) {
                                    Filter subFilter = config.getFilterByName(subfilterStr, new ArrayList<String>());
                                    if (subFilter != null) {
                                        subFilter.setProperty(XMLElements.DEPENDSON, entry.getKey());
                                    }
                                }
                            }
                        }
                    }
                }
                Options.getInstance().synchronizeOptionObjects(false);
            }// end of else
        }
    }

    public void reloadOptions() {
        org.jdom.Element optionElement = Options.getInstance().getOptionRootElement();
        MartRegistry registry = McGuiUtils.INSTANCE.getRegistryObject();
        List<org.jdom.Element> martElements = optionElement.getChildren();
        for (Element martElement : martElements) {
            Map<String, Set<String>> filterMap = this.getFilterDependents(martElement);
            if (!filterMap.isEmpty()) {
                String martName = martElement.getAttributeValue(XMLElements.NAME.toString());
                Mart mart = registry.getMartByName(martName);
                for (Config config : mart.getConfigList()) {
                    for (Map.Entry<String, Set<String>> entry : filterMap.entrySet()) {
                        for (String subfilterStr : entry.getValue()) {
                            Filter subFilter = config.getFilterByName(subfilterStr, new ArrayList<String>());
                            if (subFilter != null) {
                                subFilter.setProperty(XMLElements.DEPENDSON, entry.getKey());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * if hasGroup, a guicontainer will be created and the marts will be added to the guicontainer else, use default
     * guicontainer
     * 
     * @param marts
     * @param hasGroup
     */
    private void addMartPointers(List<Mart> martList, UserGroup user, boolean hasGroup) {
        boolean hasMulGroups = false;
        if (McUtils.isCollectionEmpty(martList))
            return;
        GuiContainer mainGc = null;
        MartRegistry registry = martList.get(0).getMartRegistry();
        if (hasGroup) {
            String firstGroup = martList.get(0).getGroupName();
            for (Mart mart : martList) {
                if (!firstGroup.equals(mart.getGroupName())) {
                    hasMulGroups = true;
                    break;
                }
            }
        }
        if (hasMulGroups) {
            // create one main tab
            String basename = McGuiUtils.INSTANCE.getNextGuiContainerName(Resources.get("NEWGCPREFIX"));
            mainGc = new GuiContainer(basename);
            mainGc.addUser(user);
            registry.getPortal().getRootGuiContainer().addGuiContainer(mainGc);
        } else
            mainGc = registry.getPortal().getRootGuiContainer();

        for (Mart mart : martList) {
            String configName = McGuiUtils.INSTANCE.getUniqueConfigName(mart,
                    mart.getName() + Resources.get("CONFIGSUFFIX"));
            GuiContainer gc = null;
            if (hasGroup) {
                gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getRootGuiContainer()
                        .getGCByNameRecursively(mart.getGroupName());
                if (gc == null) {
                    gc = new GuiContainer(mart.getGroupName());
                    gc.addUser(user);
                    mainGc.addGuiContainer(gc);
                }
            } else
                gc = McGuiUtils.INSTANCE.getRegistryObject().getPortal().getRootGuiContainer()
                        .getGCByNameRecursively(XMLElements.DEFAULT.toString());
            this.addConfigFromMaster(mart, configName, user, false, gc);
        }
    }

    public List<MartInVirtualSchema> getURLMartFromFile(File file) throws JDOMException, IOException {
        List<MartInVirtualSchema> result = new ArrayList<MartInVirtualSchema>();
        Document document = null;

        SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
        document = saxBuilder.build(file);

        Element rootElement = document.getRootElement();
        // has virtualschemas?
        // get all real mart list
        List<Element> martElementList = new ArrayList<Element>();
        @SuppressWarnings("unchecked")
        List<Element> virtuals = rootElement.getChildren();
        for (Element vss : virtuals) {
            // virtual schema?
            if (vss.getName().equals("virtualSchema")) {
                List<Element> subElements = vss.getChildren();
                for (Element subE : subElements) {
                    martElementList.add(subE);
                }
            } else if (vss.getName().equals("MartURLLocation") || vss.getName().equals("MartDBLocation")) {
                martElementList.add(vss);
            }
        }

        for (Element virtualSchema : martElementList) {
            // db or url
            if (McUtils.isStringEmpty(virtualSchema.getAttributeValue("databaseType"))) {
                MartInVirtualSchema mart = new MartInVirtualSchema.URLBuilder()
                        .database(virtualSchema.getAttributeValue("database"))
                        .defaultValue(virtualSchema.getAttributeValue("default"))
                        .displayName(virtualSchema.getAttributeValue("displayName"))
                        .host(virtualSchema.getAttributeValue("host"))
                        .includeDatasets(virtualSchema.getAttributeValue("includeDatasets"))
                        .martUser(virtualSchema.getAttributeValue("martUser"))
                        .name(virtualSchema.getAttributeValue("name")).path(virtualSchema.getAttributeValue("path"))
                        .port(virtualSchema.getAttributeValue("port"))
                        .serverVirtualSchema(virtualSchema.getAttributeValue("serverVirtualSchema"))
                        .visible(virtualSchema.getAttributeValue("visible")).build();
                result.add(mart);
            } else {
                // db
                MartInVirtualSchema mart = new MartInVirtualSchema.DBBuilder()
                        .database(virtualSchema.getAttributeValue("database"))
                        .defaultValue(virtualSchema.getAttributeValue("default"))
                        .displayName(virtualSchema.getAttributeValue("displayName"))
                        .host(virtualSchema.getAttributeValue("host"))
                        .includeDatasets(virtualSchema.getAttributeValue("includeDatasets"))
                        .martUser(virtualSchema.getAttributeValue("martUser"))
                        .name(virtualSchema.getAttributeValue("name")).port(virtualSchema.getAttributeValue("port"))
                        .visible(virtualSchema.getAttributeValue("visible"))
                        .schema(virtualSchema.getAttributeValue("schema"))
                        .username(virtualSchema.getAttributeValue("user"))
                        .password(virtualSchema.getAttributeValue("password"))
                        .type(virtualSchema.getAttributeValue("databaseType")).build();
                result.add(mart);
            }
        }

        return result;
    }
}
