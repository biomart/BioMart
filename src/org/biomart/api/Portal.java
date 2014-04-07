package org.biomart.api;

import com.google.common.collect.Lists;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Container;
import org.biomart.api.lite.Filter;
import org.biomart.api.lite.Dataset;
import org.biomart.api.lite.GuiContainer;
import org.biomart.api.lite.Mart;
import org.biomart.api.lite.MartRegistry;
import org.biomart.common.resources.Log;
import org.biomart.configurator.model.object.FilterData;
import org.biomart.queryEngine.QueryController;

public class Portal {
    public final MartRegistry _registry;
    public final String _user;

    public Portal(MartRegistryFactory factory) {
        this(factory, null);
    }

    public Portal(MartRegistryFactory factory, @Nullable String username) {
        _user = username;
        _registry = factory.getRegistry(username == null ? "" : username);
    }

    public GuiContainer getRootGuiContainer(@Nullable String type) {
        if (type == null)
            return _registry.getRootGuiContainer();
        return _registry.getRootGuiContainer(type);
    }

    public GuiContainer getGuiContainer(@Nonnull String name) {
        enforceNonnullAndNonEmpty(name);
        return _registry.getGuiContainer(name);
    }

    public List<Mart> getMarts(@Nullable String guiContainerName) {
        List<Mart> list;

        if (guiContainerName == null) {
            list = _registry.getMarts();
        } else {
            list = _registry.getGuiContainer(guiContainerName).getMartList();
        }

        return list;
    }

    public List<Dataset> getDatasets(@Nonnull String martName) {
        enforceNonnullAndNonEmpty(martName);

        List<Dataset> list;
        list = _registry.getMartByName(martName).getDatasets();
        return list;
    }

    public Map<String,List<Dataset>> getDatasetMapByMarts(@Nonnull String martNames) {
        enforceNonnullAndNonEmpty(martNames);

        String[] martNameArray = martNames.split(",");
        Map<String,List<Dataset>> map = new LinkedHashMap<String,List<Dataset>>();

        for (String martName : martNameArray) {
            map.put(martName, _registry.getMartByName(martName).getDatasets());
        }

        return map;
    }

    public List<Filter> getFilters(@Nonnull String datasets,
            @Nullable String config, @Nullable String container) {
        enforceNonnullAndNonEmpty(datasets);

        Mart mart;
        List<Filter> list;

        if (config == null)
            mart = _registry.getMartByDatasets(Lists.newArrayList(datasets.split(",")));
        else
            mart = _registry.getMartByConfigName(config);

        if (container == null)
            list = mart.getFilters(datasets, false, false);
        else
            list = mart
                    .getRootContainer(datasets, false, true,true)
                    .getContainerByName(container).getFilterList();

        return list;
    }

    public List<Attribute> getAttributes(@Nonnull String datasets,
            @Nullable String config, @Nullable String container, @Nullable Boolean allowPartialList /* for attribute lists */) {
        enforceNonnullAndNonEmpty(datasets);

        // Default includes partial attribute lists
        if (allowPartialList == null) {
            allowPartialList = true;
        }

        Mart mart;
        List<Attribute> list;

        if (config == null)
            mart = _registry.getMartByDatasets(Lists.newArrayList(datasets.split(",")));
        else
            mart = _registry.getMartByConfigName(config);

        if (container == null)
            list = mart.getAttributes(datasets, allowPartialList);
        else
            list = mart
                    .getRootContainer(datasets, true, false, allowPartialList)
                    .getContainerByName(container).getAttributeList();

        return list;
    }


    public Map getFilterMapByDataset(@Nonnull String datasets,
            @Nullable String config, @Nullable String container) {
        enforceNonnullAndNonEmpty(datasets);

        Mart mart;
        Map<String,List<Filter>> map;

        if (config == null)
            mart = _registry.getMartByDatasets(Lists.newArrayList(datasets.split(",")));
        else
            mart = _registry.getMartByConfigName(config);

        if (container == null)
            map = getIndependentMartFilters(mart, datasets);
        else
            map = getIndependentContainerFilters(mart, datasets, container);

        return map;
    }

    public Map getAttributeMapByDataset(@Nonnull String datasets,
            @Nullable String config, @Nullable String container) {
        enforceNonnullAndNonEmpty(datasets);

        Mart mart;
        Map<String,List<Attribute>> map;

        if (config == null)
            mart = _registry.getMartByDatasets(Lists.newArrayList(datasets.split(",")));
        else
            mart = _registry.getMartByConfigName(config);

        if (container == null)
            map = getIndependentMartAttributes(mart, datasets);
        else
            map = getIndependentContainerAttributes(mart, datasets, container);

        return map;
    }

    public Container getContainers(@Nonnull String datasets, @Nullable String config,
            Boolean withattributes, Boolean withfilters, Boolean allowPartialList /* for attribute lists */) {

        enforceNonnullAndNonEmpty(datasets);

        // Default allows partial attribute lists
        if (allowPartialList == null) {
            allowPartialList = true;
        }

        Mart mart;

        if (config == null) {
            mart = _registry.getMartByDatasets(Lists.newArrayList(datasets.split(",")));
        } else {
            mart = _registry.getMartByConfigName(config);
        }

        return mart.getRootContainer(datasets, withattributes, withfilters, allowPartialList);
    }

    public List<Dataset> getLinkables(@Nonnull String datasets) {
        enforceNonnullAndNonEmpty(datasets);
        List<String> from = Lists.newArrayList(datasets.split(","));
        List<Dataset> list = _registry.getMartByDatasets(from).getLinkableDatasets(from);
        return list;
    }

    public List<org.biomart.api.lite.FilterData> getFilterData(@Nonnull String datasets, @Nonnull String filterName,
            @Nonnull String value, @Nullable String config) {
        List<String> ds = Lists.newArrayList(datasets.split(","));
        Mart mart;
        if (config == null) {
            mart = _registry.getMartByDatasets(ds);
        } else {
            mart = _registry.getMartByConfigName(config);
        }
        List<FilterData> list = mart.getFilterDataList(value, ds, filterName);
        List<org.biomart.api.lite.FilterData> rlist = new ArrayList<org.biomart.api.lite.FilterData>();
        for (FilterData fd : list) {
            rlist.add(new org.biomart.api.lite.FilterData(fd));
        }
        return rlist;
    }

    public void executeQuery(String xml, OutputStream out, boolean isCountQuery) {
        try {
            new QueryController(xml, _registry.getFullRegistry(),
                    _user == null ? "" : _user, isCountQuery).runQuery(out);
        } catch (Exception e) {
            Log.error("Error during querying", e);
            throw new BioMartApiException(e.getMessage());
        }
    }

    public void executeSPARQLQuery(String url, String sparql, OutputStream out) {
        try {
            URL service = new URL(url);

            URLConnection conn = service.openConnection();
            conn.setDoOutput(true);

            conn.getOutputStream().write(("query=" + URLEncoder.encode(sparql, "UTF-8")).getBytes());
            conn.getOutputStream().close();

            int b;
            InputStream in = conn.getInputStream();
            while ((b = in.read()) != -1)
                out.write(b);
        } catch (Exception e) {
            Log.error("Error during querying", e);
            throw new BioMartApiException(e.getMessage());
        }
    }

    public String getUser() {
        return _user;
    }

    private void enforceNonnullAndNonEmpty(Object... args) {
        for (Object obj : args) {
            if (obj == null || "".equals(obj)) throw new BioMartApiException("Nonnull parameter was passed as null");
        }
    }
    private Map<String,List<Attribute>> getIndependentMartAttributes(Mart mart, String datasets) {
        String[] ds = datasets.split(",");
        Map<String,List<Attribute>> map = new LinkedHashMap<String,List<Attribute>>();
        for (String d : ds) {
            map.put(d, mart.getAttributes(d,true));
        }
        return map;
    }
    private Map<String,List<Filter>> getIndependentMartFilters(Mart mart, String datasets) {
        String[] ds = datasets.split(",");
        Map<String,List<Filter>> map = new LinkedHashMap<String,List<Filter>>();
        for (String d : ds) {
            map.put(d, mart.getFilters(d, false,false));
        }
        return map;
    }
    private Map<String,List<Attribute>> getIndependentContainerAttributes(Mart mart, String datasets, String containerName) {
        String[] ds = datasets.split(",");
        Map<String,List<Attribute>> map = new LinkedHashMap<String,List<Attribute>>();
        for (String d : ds) {
            Container container = mart.getRootContainer(d, true, false,true).getContainerByName(containerName);
            map.put(d, container.getAttributeList());
        }
        return map;
    }
    private Map<String,List<Filter>> getIndependentContainerFilters(Mart mart, String datasets, String containerName) {
        String[] ds = datasets.split(",");
        Map<String,List<Filter>> map = new LinkedHashMap<String,List<Filter>>();
        for (String d : ds) {
            Container container = mart.getRootContainer(d, false, true,true).getContainerByName(containerName);
            map.put(d, container.getFilterList());
        }
        return map;
    }
}
