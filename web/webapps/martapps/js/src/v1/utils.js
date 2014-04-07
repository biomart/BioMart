(function($) {
$.namespace('biomart.utils', function(self) {
    // Recurses through gui containers, and return list of marts
    // Optional: can filter by gui type
    self.getMartsFromGuiContainer = (function() {
        return function(container, type) {
            var arr = [];
            recurse(container, arr, type);
            return arr;
        };

        function recurse(c, a, t) {
            if (!c.guiContainers.length) {
                if (t && c.guiType != t) return;

                for (var i=0, m; m=c.marts[i]; i++) {
                    if (!m.isHidden) a.push(m);
                }
                return;
            }

            for (var i=0, gc; gc=c.guiContainers[i]; i++) {
                recurse(gc, a, t);
            }
        }
    })();
    self.getFlatContainerList = (function() {
        return function(container) {
            var arr = [];
            recurse(container, arr);
            return arr;
        };

        function recurse(c, a) {
            if (!c.containers) {
                if (!c.isHidden) a.push(c);
                return;
            }

            for (var i=0, ci; ci=c.containers[i]; i++) {
                recurse(ci, a);
            }
        }
    })();
    self.reloadPage = function() {
        location = location.href;
    };

    // Object to handle metainfo field of mart
    // See end of function for example
    self.MetaInfo = function(mart) {
        var exports = this,
            DEFAULTS = {
                rendering: 'table',
                aggregation: 'none',
                options: {
                    paginateBy: 20
                },
                replace: {},
                dynamicAttribute: false
            };
        /*
         * Sample martconfig metainfo
         * {
         *    // Global config
         *    global: {
         *        rendering: 'table',
         *        aggregation: 'none'
         *    },
         *    // Container level config (overrides global)
         *    layout: {
         *       mutation_frequencies: {
         *          rendering: 'chart',
         *          aggregation: 'summary',
         *          options: {
         *             detailsUrl: '/martform/#!/hsapiens_gene_ensembl/hsapiens_gene_ensembl?ds=${datasets}&id_list_limit_filters[]=ensembl_gene_id&id_list_limit_filters[]=${ids} &id_list_limit_filters[]=Ensembl+Gene+ID(s)&preview=true',
         *             xaxisLabel: 'Number of affected donors',
         *             useRaw: true,
         *             lineIndices: [
         *                1,
         *                2,
         *                3
         *             ],
         *             extraLabel: true,
         *             includeTotal: true
         *          },
         *          replace: {
         *             title: 'datasets'
         *          }
         *       }
         *    }
         * }
         */

        exports._globalConfig = $.extend({}, DEFAULTS);
        exports._containerConfigs = {};

        if (mart.meta) {
            try {
                var meta = $.parseJSON(mart.meta);
                $.extend(exports._globalConfig, meta.global);

                if (meta.group) {
                    mart.group = meta.group;
                }

                if (meta.layout) {
                    for (var k in meta.layout) {
                        exports._containerConfigs[k] = $.extend({}, DEFAULTS, meta.layout[k]);
                    }
                }
            } catch (e) {
                if (console && console.error) console.error('Error parsing JSON', mart.meta);
            }
        }

        this.getConfig = function(containerName) {
            return exports._containerConfigs[containerName] || exports._globalConfig;
        };
    };

    /*
     * Recursively goes through container data and returns the proper jstree structure.
     * Also caches the containers as a flat map (to retrieve container for later use).
     *
     * param container: object from web service representing a container node
     */
    self.processContainer = function(container, firstLeafState) {
        var node = {
            data: container.displayName,
            attr: {
                'container': container.name,
                'class': biomart.renderer.makeClassName(container.name)
            }
        };

        // base case
        if (!container.containers.length) {
            if (container.attributes.length || container.filters.length) {
                return node;
            }
            else return null;
        }

        //recursive case
        var children = [],
            containers = container.containers;

        for (var i=0, n=containers.length; i<n; i++) {
            var childContainer = containers[i],
                childNode = self.processContainer(childContainer, firstLeafState);

            if (!childNode) continue;

            // Open the first non-leaf node encountered
            if (i===0 && childContainer.containers.length) childNode.state = firstLeafState || 'open';

            children.push(childNode);
        }

        node.children = children;

        if (!node.children.length) return null;
        return node;
    };

    self.filterValueExists = function(item, value) {
        if (item.type == 'singleSelect' || item.type == 'multiSelect') {
            for (var i=0, val; val=item.values[i]; i++) {
                if (value == val.name) {
                    break;
                }
            }
            if (i == item.values.length) {
                return false;
            }
        }
        return true;
    };

    self.hasGroupedMarts = function(martObj) {
        martObj = martObj || biomart._state.mart;
        return $.isArray(martObj);
    };
});
})(jQuery);

