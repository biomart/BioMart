_('BM.query.models').namespace(function(self) {
    /*
     * A QueryElement contains a list of datasets + config, a list of attributes, 
     * and a list of filters. Equal to the <Dataset/> element in query XML.
     *
     * Can compile down to XML, Java, and SPARQL queries.
     *
     * TODO: Implement Java and SPARQL compilation
     */
    self.QueryElement = Backbone.Model.extend({
        sync: function(method, model, success, error) {
            // no service for this right now
            // TODO: move this to localStorage?
        },

        url: '/queryelement', // This is meaningless right now due to lack of sync()

        initialize: function(options) {
            log('QueryElement: initialize', options.config)

            _.bindAll(this, '_propagateAttributeEvent', '_propagateFilterEvent')

            this.set({ id: options.config })
            this.datasetList = new BM.models.DatasetList( this.get('datasets') ),
            this.filterList = new BM.models.FilterList,
            this.attributeList = new BM.models.AttributeList

            this.filterList.bind('all', this._propagateFilterEvent)
            this.attributeList.bind('all', this._propagateAttributeEvent)
        },

        _propagateAttributeEvent: function(eventName, model) {
            this.trigger('attribute:' + eventName, model)
        },
        _propagateFilterEvent: function(eventName, model) {
            this.trigger('filter:' + eventName, model)
        },

        compile: function(format) {
            var fn = this._compileFunctions[format];
            if (fn) return fn.apply(this)
            else throw 'Could not find compile function for format: ' + format
        },

        /*
         * Compiles the query into formats matched by key
         */
        _compileFunctions: {
            xml: function() {
                var arr = [],
                    datasets = this.datasetList.toString()

                arr.push([
                    '<Dataset name="', datasets, '"',
                        ' config="' + this.escape('config')  + '"',
                    '>'
                ].join(''))

                this.filterList.each(function(filter) {
                    arr.push([
                        '<Filter name="', filter.escape('name'), '"',
                            ' value="', filter.escape('value'), '"/>'
                    ].join(''))
                })

                this.attributeList.each(function(attribute) {
                    arr.push([
                        '<Attribute name="', attribute.escape('name'), '"/>'
                    ].join(''))
                })

                arr.push('</Dataset>')

                return arr.join('')
            },
            java: function() {
                var arr = [],
                    datasets = this.datasetList.toString()

                arr.push(['\n        Query.Dataset ds = query.addDataset("', datasets, '", ',  this.escape('config') ?
                            ('"' + this.escape('config') + '"') : 'null', ');'].join(''));

                this.filterList.each(function(filter) {
                    arr.push(['        ds.addFilter("', filter.escape('name'), '", "', filter.escape('value'), '");'].join(''));
                })

                this.attributeList.each(function(attribute) {
                    arr.push(['        ds.addAttribute("', attribute.escape('name'), '");'].join(''));
                })

                return arr.join('\n');
            },
            sparql: function() {
                var arr = [ ],
                    config = this.escape('config')
                    datasets = this.datasetList.toString()

                arr.push('PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n');
                arr.push('PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n');
                arr.push('PREFIX owl: <http://www.w3.org/2002/07/owl#>\n');
                arr.push('\n');
                arr.push('PREFIX accesspoint: <' + BIOMART_CONFIG.siteURL + 'martsemantics/' + config + '/ontology#>\n');
                arr.push('PREFIX class: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/class#>\n');
                arr.push('PREFIX dataset: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/dataset#>\n');
                arr.push('PREFIX attribute: <' + site2reference(BIOMART_CONFIG.siteURL) + 'martsemantics/' + config + '/ontology/attribute#>\n\n');

                arr.push('SELECT ');
                this.attributeList.each(function(attribute) {
                    var name = identifier2SPARQL(attribute.escape('name'));
                    arr.push('?' + name + ' ');
                })
                arr.push('\n');
                this.datasetList.each(function(dataset) {
                    arr.push('FROM dataset:' + dataset.escape('name') + '\n');
                })
                arr.push('WHERE {\n');
                this.filterList.each(function(filter) {
                    name = identifier2SPARQL(filter.escape('name'));
                    arr.push('  ?x attribute:' + name + ' \"' + filter.escape('value') + '\" .\n');
                })

                var i = 0,
                    that = this
                this.attributeList.each(function(attribute) {
                    name = identifier2SPARQL(attribute.escape('name'))
                    arr.push('  ?x attribute:' + name + ' ?' + name)
                    if (++i < that.attributeList.length) {
                        arr.push(' .\n')
                    } else {
                        arr.push('\n') 
                    }
                })

                arr.push('}\n');

                return arr.join('');

                /*
                 * Helper functions for SPARQL
                 */
                function site2reference(siteURL) {
                    refURL = siteURL.replace(/^https:/, 'biomart:');
                    refURL = refURL.replace(/^http:/, 'biomart:');
                    return refURL;
                }
                function identifier2SPARQL(id) {
                    // This regexp has to be identical to the regexp in ObjectController.createDefaultRDF
                    if (!id.match(/^[a-zA-Z_].*/)) {
                        id = '_' + id;
                    }
                    return id;
                }
            }
        }
    })

    /*
     * Collection of QueryElements
     */
    self.QueryElementList = Backbone.Collection.extend({ model: BM.query.models.QueryElement })

    /*
     * Represents the entire query; Contains a list of QueryElements.
     *
     * Can compile to XML, Java, and SPARQL.
     * TODO: Implement Java and SPARQL compilation
     *
     * Propagates events from QueryElementList:
     *  - add : new QueryElement added
     *  - remove : QueryElement removed
     *  - attribute:add : new Attribute added
     *  - attribute:remove : Attribute removed
     *  - filter:add : new Filter added
     *  - filter:remove : Filter removed
     */
    self.Query = Backbone.Model.extend({
        defaults: {
            processor: 'TSV',
            limit: -1,
            header: true,
            client: 'webbrowser'
        },
        initialize: function() {
            _.bindAll(this, '_propagateEvent')
            this.queryElements = new self.QueryElementList
            this.queryElements.bind('all', this._propagateEvent)
        },
        _propagateEvent: function(eventName) {
            log('Query._propagateEvent', eventName)
            this.trigger.apply( this, Array.prototype.slice.call(arguments, 0) )
        },
        addElement: function(queryElement) {
            log('Query.addElement')
            this.queryElements.add(queryElement)
            return this
        },
        removeElement: function(queryElement) {
            log('Query.removeElement')
            this.queryElements.remove(queryElement)
            return this
        },
        getElement: function(config) {
            return this.queryElements.detect(function(element) {
                return element.get('config') == config
            })
        },

        /*
         * Compiles the Query object into a string. Takes an optional **format**
         * argument -- default is XML.
         */
        compile: function(format) {
            format = format || 'xml'
            return this._compileFunctions[format].call(this)
        },

        /*
         * Compile query for preview (i.e. with a limit)
         */
        compileForPreview: function(format) {
            var oldLimit = this.get('limit'),
                oldProcessor = this.get('processor')

            this.set({ limit: BM.PREVIEW_LIMIT }, { silent: true })

            // So we can see links
            if (oldProcessor == 'TSV') {
                this.set({ processor: 'TSVX' }, { silent: true })
            }

            var compiled = this.compile(format)

            this.set({ limit: oldLimit, processor: oldProcessor }, {silent: true })

            return compiled
        },

        _compileFunctions: {
            xml: function() {
                var arr = []

                arr.push([
                    '<Query processor="', this.escape('processor'), '" header="', this.escape('header'), '" limit="', 
                            this.escape('limit'), '"  client="', this.escape('client'), '">'
                ].join(''))

                this.queryElements.each(function(queryElement) {
                    arr.push(queryElement.compile('xml'))
                })

                arr.push('</Query>')

                return arr.join('')
            },
            java: function() {
                var arr = []

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
                arr.push(['        query.setProcessor("', this.escape('processor'), '");'].join(''));
                arr.push(['        query.setClient("', this.escape('client'), '");'].join(''));
                arr.push(['        query.setLimit(', this.escape('limit'), ');'].join(''));
                arr.push(['        query.setHeader(', this.escape('header'), ');'].join(''));

                this.queryElements.each(function(queryElement) {
                    arr.push(queryElement.compile('java'))
                })

                arr.push('\n        // Print to System.out, but you can pass in any java.io.OutputStream');
                arr.push('        query.getResults(System.out);');

                arr.push(['\n        System.exit(0);'].join(''));

                arr.push('    }');
                arr.push('}');

                return arr.join('\n')
            },
            sparql: function() {
                var arr = [];

                this.queryElements.each(function(queryElement) {
                    arr.push(queryElement.compile('sparql'))
                })

                if (this.get('limit') > 0) {
                    arr.push('LIMIT ' + this.get('limit') + '\n');
                }

                return arr.join('');
            }
        }
    })
})

