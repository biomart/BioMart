package org.biomart.api.soap;

import org.biomart.api.PortalService;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.biomart.api.Portal;
import org.biomart.api.factory.MartRegistryFactory;
import org.biomart.api.factory.XmlMartRegistryModule;
import org.biomart.api.lite.Attribute;
import org.biomart.api.lite.Container;
import org.biomart.api.lite.Dataset;
import org.biomart.api.lite.Filter;
import org.biomart.api.lite.FilterData;
import org.biomart.api.lite.GuiContainer;
import org.biomart.api.lite.Mart;
import org.jvnet.jax_ws_commons.guicemanaged.GuiceManaged;

/**
 *
 * @author jhsu
 */
@GuiceManaged(module=XmlMartRegistryModule.class)
@WebService(serviceName="BioMartSoapService")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL, parameterStyle = SOAPBinding.ParameterStyle.WRAPPED)
public class PortalServiceImpl implements PortalService {
    private MartRegistryFactory factory;

    @Inject
    public PortalServiceImpl(MartRegistryFactory factory) {
        this. factory = factory;
    }

    @Override
    @WebResult(name="guicontainer")
    public GuiContainer getRootGuiContainer(@WebParam(name="guitype") String guiType) {
        return getPortal().getRootGuiContainer(guiType);
    }

    @Override
    @WebResult(name="mart")
    public List<Mart> getMarts(@WebParam(name="guicontainer") String guiContainerName) {
        return getPortal().getMarts(guiContainerName);
    }

    @Override
    @WebResult(name="dataset")
    public List<Dataset> getDatasets(@WebParam(name="mart") String martName) {
        return getPortal().getDatasets(martName);
    }

    @Override
    @WebResult(name="filter")
    public List<Filter> getFilters(@WebParam(name="datasets") String datasets,
            @WebParam(name="config") String config, @WebParam(name="container") String containerName) {
        return getPortal().getFilters(datasets, config, containerName);
    }

    @Override
    @WebResult(name="filtervalue")

    public List<FilterData> getFilterValues(@WebParam(name="filter") String filterName,
            @WebParam(name="value") String value, @WebParam(name="datasets") String datasets,
            @WebParam(name="config") String config) {
        return getPortal().getFilterData(datasets, filterName, value, config);
    }

    @Override
    @WebResult(name="attribute")
    public List<Attribute> getAttributes(@WebParam(name="datasets") String datasets,
            @WebParam(name="config") String config, @WebParam(name="container") String containerName,
            @WebParam(name="allowPartialList") Boolean allowPartialList) {
        return getPortal().getAttributes(datasets, config, containerName, allowPartialList);
    }

    @Override
    @WebResult(name="container")
    public Container getContainers(@WebParam(name="datasets") String datasets,
            @WebParam(name="config") String config, @WebParam(name="withAttributes") Boolean withAttributes,
            @WebParam(name="withFilters") Boolean withFilters,
        @WebParam(name="allowPartialList") Boolean allowPartialList) {
        return getPortal().getContainers(datasets, config, withAttributes, withFilters, allowPartialList);
    }

    @Override
    @WebResult(name="dataset")
    public List<Dataset> getLinkables(@WebParam(name="datasets") String datasets) {
        return getPortal().getLinkables(datasets);
    }

    @Override
    @WebResult(name="result")
    public String getResults(@WebParam(name="query") String xml) {
        OutputStream out = new ByteArrayOutputStream();
        getPortal().executeQuery(xml, out, false);
        return out.toString();
    }

    private Portal getPortal() {
        return new Portal(factory);
    }
}
