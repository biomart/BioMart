(function($) {
$.namespace('biomart.query', function(self) {
    self.createQueryMart = function(mart) {
        var queryMart = {
            filters: {},
            attributes: {},
            datasets: mart.datasets
        };
        if (mart.config) queryMart.config = mart.config;
        return queryMart;
    };

    self.handlers = {
        XML: function(mart, processor, limit, header, client) {
            header = header ? 1 : 0;
            client = client || '';

            var arr = [
                    '<!DOCTYPE Query>',
                    '<Query client="' + client + '" processor="' + processor + '"' + (limit ? ' limit="' + limit + '"' : '-1') + ' header="' + header + '">'
                ];

            if (!mart.datasets) return null;

            mart = self.normalizeQueryMartObject(mart);
            arr.push(mart2xml(mart));

            arr.push('</Query>');

            return arr.join('');
        },
        SPARQL: function(mart, processor, limit, header, client) {
            var arr = [];

            if (!mart.datasets) return null;

            mart = self.normalizeQueryMartObject(mart);

            arr.push(mart2sparql(mart));

            if (limit > 0)
                arr.push('LIMIT ' + limit + '\n');

            return arr.join('');
        },
        Java: function(mart, processor, limit, header, client) {
            var arr = [];

            if (!mart.datasets) return null;

            mart = self.normalizeQueryMartObject(mart);

            arr.push('import org.biomart.api.factory.*;');
            arr.push('import org.biomart.api.Portal;');
            arr.push('import org.biomart.api.Query;\n');

            arr.push('/*');
            arr.push(' * This is a runnable Java class that executes the query.');
            arr.push(' * Please adapt this code as needed, and DON\'T forget to change the xmlPath.');
            arr.push(' */\n');

            arr.push('public class QueryTest {');
            arr.push('    public static void main(String[] args) throws Exception {');

            arr.push('        String xmlPath = "/path/to/registry_xml"; // Needs to be changed\n');
            arr.push('        MartRegistryFactory factory = new XmlMartRegistryFactory(xmlPath, null);');
            arr.push('        Portal portal = new Portal(factory, null);');
            arr.push('\n        Query query = new Query(portal);');
            arr.push(['        query.setProcessor("', processor, '");'].join(''));
            arr.push(['        query.setClient("', client, '");'].join(''));
            arr.push(['        query.setLimit(', limit, ');'].join(''));
            arr.push(['        query.setHeader(', !!header, ');'].join(''));

            arr.push(mart2java(mart));

            arr.push('\n        // Print to System.out, but you can pass in any java.io.OutputStream');
            arr.push('        query.getResults(System.out);');

            arr.push(['\n        System.exit(0);'].join(''));

            arr.push('    }');
            arr.push('}');

            return arr.join('\n');
        }
    };

    self.compile = function(format, mart, processor, limit, header, client) {
        processor = processor || "TSV";
        limit = limit || -1;
        header = typeof header == 'undefined' ? true : header;
        client = client || 'biomartclient';
        return self.handlers[format](mart, processor, limit, header, client);
    };

    self.compileXML = function(processor, limit, header, client) {
        header = header ? 1 : 0;
        client = client || '';

        var arr = [
            '<?xml version="1.0" encoding="UTF-8"?>',
            '<!DOCTYPE Query>',
            '<Query client="' + client + '" processor="' + processor + '"' + (limit ? ' limit="' + limit + '"' : '-1') + ' header="' + header + '">'
        ];

        for (var k in self.data) {
            var mart = self.data[k];
            if (!mart.datasets) continue;
            mart = self.normalizeQueryMartObject(mart);
            arr.push(mart2xml(mart));
        }

        arr.push('</Query>');

        return arr.join('');
    };

    // For making old function calls work
    self.compileSingleMartXML = self.compile.partial('XML');
    self.compileSingleMartSPARQL = self.compile.partial('SPARQL');

    /*
     * Normalized the mart object notation before generating query format
     *
     * Makes datasets an CSV string of dataset names
     * Makes filters an array of filter objects
     * Makes attributes an array of attribute objects
     */
    self.normalizeQueryMartObject = function(obj) {
        var normalized = {};

        if (obj.config) {
            normalized.config = obj.config;
        }

        if ($.isArray(obj.datasets)) {
            normalized.datasets = obj.datasets.join(',');
        } else {
            normalized.datasets = obj.datasets;
        }

        if (!$.isArray(obj.params)) {
            normalized.params = [];
            for (var k in obj.params) {
                normalized.params.push(obj.params[k]);
            }
        } else {
            normalized.params = obj.params;
        }

        if (!$.isArray(obj.filters)) {
            normalized.filters = [];
            for (var k in obj.filters) {
                normalized.filters.push(obj.filters[k]);
            }
        } else {
            normalized.filters = obj.filters;
        }

        if (!$.isArray(obj.attributes)) {
            normalized.attributes = [];
            for (var k in obj.attributes) {
                normalized.attributes.push(obj.attributes[k]);
            }
        } else {
            normalized.attributes = obj.attributes;
        }

        return normalized;
    };

    function mart2xml(mart) {
        var arr = [],
            datasets = mart.datasets,
            params = mart.params,
            filters = mart.filters,
            attributes = mart.attributes;

        if (params && params.length) {
            arr.push('<Processor>');
            for (i=0, item; item=params[i]; i++) {
                arr.push([
                    '<Parameter name="', biomart.escapeHTML(item.name), '" value="', biomart.escapeHTML(item.value), '"/>'
                ].join(''));
            }
            arr.push('</Processor>');
        }

        arr.push([
            '<Dataset name="', datasets, '" ', (mart.config ? 'config="' + mart.config + '"' : ''), '>'
        ].join(''));

        for (var i=0, item; item=filters[i]; i++) {
            var value = item.value, name = item.name,
                filterList = item.filterList;

            if ($.isArray(value)) {
                name = value[0];
                value = value[1];
            }

            if (value !== null && $.trim(value)) {
                arr.push([
                    '<Filter name="', biomart.escapeHTML(name),
                    '" value="', biomart.escapeHTML(value),
                    '" filter_list="',
                        filterList
                            ? biomart.escapeHTML(filterList)
                            : "",
                    '"/>'
                ].join(''));
            }
        }

        for (i=0, item; item=attributes[i]; i++) {
            if (item.attributes && item.attributes.length) {
                var list = item.attributes;
                for (var j=0; j<list.length; j++) {
                    arr.push([
                        '<Attribute name="', biomart.escapeHTML(list[j].name), '"/>'
                    ].join(''));
                }
            } else {
                arr.push([
                    '<Attribute name="', biomart.escapeHTML(item.name), '"/>'
                ].join(''));
            }
        }

        arr.push('</Dataset>');

        return arr.join('');
    }

    function site2reference(siteURL) {
        refURL = siteURL.replace(/^https:/, 'biomart:');
        refURL = refURL.replace(/^http:/, 'biomart:');

        return refURL;
    }

    function identifier2SPARQL(id) {
        // This regexp has to be identical to the regexp in ObjectController.createDefaultRDF
        if (!id.match(/^[a-zA-Z_].*/))
            id = '_' + id;

        return id;
    }

    function mart2sparql(mart) {
        var arr = [ ],
            config = mart.config,
            datasets = mart.datasets.split(','),
            filters = mart.filters,
            attributes = mart.attributes;

        arr.push('PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n');
        arr.push('PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n');
        arr.push('PREFIX owl: <http://www.w3.org/2002/07/owl#>\n');
        arr.push('\n');
        arr.push('PREFIX accesspoint: <' + BIOMART_CONFIG.siteURL + 'martsemantics/' + config + '/ontology#>\n');
        arr.push('PREFIX class: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/class#>\n');
        arr.push('PREFIX dataset: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/dataset#>\n');
        arr.push('PREFIX attribute: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/attribute#>\n\n');

        arr.push('SELECT ');
        for (var i = 0; i < attributes.length; i++) {
            var name = identifier2SPARQL(attributes[i].name);

            arr.push('?' + name + ' ');
        }
        arr.push('\n');
        for (i = 0; i < datasets.length; i++) {
            arr.push('FROM dataset:' + datasets[i] + '\n');
        }
        arr.push('WHERE {\n');

        for (i = 0; i < filters.length; i++) {
            name = identifier2SPARQL(filters[i].name);

            arr.push('  ?x attribute:' + name + ' \"' + filters[i].value + '\" .\n');
        }

        for (i = 0; i < attributes.length; i++) {
            name = identifier2SPARQL(attributes[i].name);

            arr.push('  ?x attribute:' + name + ' ?' + name);
            if (i + 1 < attributes.length) arr.push(' .\n'); else arr.push('\n');
        }

        arr.push('}\n');

        return arr.join('');
    }

    function mart2java(mart) {
        var arr = [],
            datasets = mart.datasets,
            filters = mart.filters,
            attributes = mart.attributes;

        arr.push(['\n        Query.Dataset ds = query.addDataset("', datasets, '", ',  mart.config ?
                    ('"' + mart.config + '"') : 'null', ');'].join(''));

        for (var i=0, filter; filter=filters[i]; i++) {
            arr.push(['        ds.addFilter("', biomart.escapeHTML(filter.name), '", "', biomart.escapeHTML(filter.value), '");'].join(''));
        }

        for (var i=0, attribute; attribute=attributes[i]; i++) {
            arr.push(['        ds.addAttribute("', biomart.escapeHTML(attribute.name), '");'].join(''));
        }

        return arr.join('\n');
    }
});

})(jQuery);

