package org.biomart.queryEngine.v2.base;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author jhsu
 */
public class Dataset {
    public final String name;
    public final String config;
    public final List<Attribute> attributes;
    public final List<Filter> filters;

    public Dataset(String name, String config) {
        this.name = name;
        this.config = config;
        this.attributes = new ArrayList<Attribute>();
        this.filters = new ArrayList<Filter>();
    }

    /*
     * Returns XML string
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("<Dataset name=\"").append(name).append("\"");

        if (!isNullOrEmpty(config)) {
            builder.append(" config=\"").append(config).append("\"");
        }

        builder.append(">");

        for (Filter filter : filters) {
            builder.append("<Filter name=\"").append(filter.name).append("\"")
                    .append(" value=\"").append(filter.value).append("\"/>");
        }

        for (Attribute attribute : attributes) {
            builder.append("<Attribute name=\"").append(attribute.name).append("\"/>");
        }

        builder.append("</Dataset>");

        return builder.toString();
    }
}
