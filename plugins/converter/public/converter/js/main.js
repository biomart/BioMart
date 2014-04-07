(function($) {
$.namespace('biomart.converter', function(self) {
    var CLIENT = 'webbrowser',
        _urlHash = biomart.url.jsonify(location.href),
        _elements = {},
        _state = {},
        _delimiterRegex = /[,\s\n]/,

        QUERY_RESULTS_OPTIONS = {
            sourceOptions: {
                dataFilter: function(data, type) {
                    var rows = data.split('\n'),
                        inputs = _state.queryMart.filters[0].value.split(_delimiterRegex),
                        hash = {},
                        from, to,
                        processed = [rows[0]], // insert header
                        input;

                    for (var i=1, row; row=rows[i]; i++) {
                        row = row.split('\t');
                        from = row[0].toUpperCase();
                        to = row[1].toUpperCase();

                        // Ensure the array exists
                        hash[from] = hash[from] || [];

                        hash[from].push(to);
                    }

                    for (i=0; input=inputs[i]; i++) {
                        // Use uppercase for case insensitivity
                        var upper = input.toUpperCase();

                        if (upper in hash) {
                            for (var j=0; j<hash[upper].length; j++) {
                                processed.push([input, hash[upper][j]].join('\t'));
                            }
                        } else {
                            processed.push([input, ''].join('\t'));
                        }
                    }

                    return processed.join('\n');
                }
            },
            paginateBy: 10,
            timeout: 10000,
            showProgress: false,
            animationTime: 200,
            iframe: false,
            showLoading: false,
            footer: false
        };

    self.init = function() {
        var fragments = _urlHash.fragment.split('/');

        _state.gui = decodeURIComponent(fragments[0]);
        _state.martName = decodeURIComponent(fragments[1]);

        _state.queryMart = {
            datasets: null,
            filters: [],
            attributes: []
        };

        _elements.dsDiv = $('#biomart-datasets');
        _elements.dsSelect = $('#dataset-list')
            .bind('change', function() {
                var ds = $(this).val();
                datasetChange(ds);
            });
        _elements.content = $('#biomart-content');
        _elements.fromDiv = $('#biomart-from');
        _elements.toDiv = $('#biomart-to');
        _elements.submit = $('#biomart-submit')
            .delegate('button', 'click.converter', function() {
                if (validate()) {
                    runQuery();
                } else {
                    if (!_state.hasError) {
                        _state.hasError = true;
                        var div = $('<div class="error" style="position:absolute; margin-top: 20px; margin-left: 10px; color: #7f0000">Please enter a value</div>')
                            .insertBefore(_elements.inputField);

                            _elements.fromDiv
                                .css({
                                    'border-color': '#7f0000',
                                    'background-color': '#ffcfcf'
                                })
                                .one('mouseover.error', function() {
                                    _state.hasError = false;
                                    _elements.fromDiv.css({
                                        'border-color': '',
                                        'background-color': ''
                                    });
                                    div.fadeAndRemove();
                                });
                    }
                }
                return false;
            });
        _elements.form = $('#biomart-form')
            .attr('action', BIOMART_CONFIG.service.url + 'results');
        _elements.results = $('#biomart-results');
        _elements.resultsWrapper = $('#biomart-results-wrapper').dialog({
            autoOpen: false,
            modal: true,
            width: 600,
            height: 450,
            close: function() {
                _elements.results.queryResults('destroy').empty();
            },
            buttons: {
                'Close': function() {
                    $(this).dialog('close');
                }
            }
        });
        _elements.resultsActions = _elements.resultsWrapper.find('.actions')
            .delegate('a', 'click.converter', function() {
                _elements.form
                    .find('input[name=query]').val(_state.xml)
                    .end()
                    .submit();
                return false;
            });

        // Load all necessary data from ReST API
        biomart.resource.load('gui', function(gui) {
            var title;

            for (var i=0, mart; mart=gui.marts[i]; i++) {
                if (mart.name == _state.martName) {
                    title = mart.displayName;
                    _state.mart = mart;
                    break;
                }
            }

            $('h2').text(title);

            biomart.resource.load('datasets', function(datasets) {
                var options = [];

                _state.datasets = datasets;

                for (var i=0, ds; ds=datasets[i]; i++) {
                    options.push([
                        '<option value="', ds.name, '">', ds.displayName, '</option>'
                    ].join(''))
                }

                _elements.dsSelect
                    .html(options.join(''))
                    .prettybox();


                datasetChange(datasets[0].name);
            }, {config: _state.mart.name});
        }, {name: _state.gui});
    };

    function datasetChange(ds) {
        _state.activeDataset = ds;

        var params = {datasets: _state.activeDataset};

        block();

        if (_state.mart.config)
            params.config = _state.mart.config;

        biomart.resource.load('containers', function(root) {
            if (root.containers.length) {
                containerSetup(root);
                unblock();
            } else if (root.filters.length) {
                singleFilterSetup(root.filters[0]);
                unblock();
            } else {
                biomart.error('Container not properly configured');
            }
            $('#biomart-loading').fadeAndRemove();
        }, params);
    }

    function containerSetup(root) {
        // Expect from and to containers
        var to, from;
        for (var i=0, curr; curr=root.containers[i]; i++) {
            if (curr.name == 'to') {
                to = curr;
            } else if (curr.name == 'from') {
                from = curr;
            }
        }

        var filter = from.filters[0];
        _state.filter = filter;

        if (to.filters.length) {
            if (to.filters[0].type == 'singleSelectUpload') {
                var filters = to.filters[0].filters;
                _state.attributes = [];
                for (i=0, filter; filter=filters[i]; i++) {
                    _state.attributes.push({
                        name: filter.attribute,
                        displayName: filter.displayName
                    });
                }
            }
        } else {
            if (to.attributes.length) {
                _state.attributes = to.attributes;
            }
            // Check for extra attributes
            if (to.containers.length) {
                _state.extraAttributes = [];
                for (i=0; curr=to.containers[i]; i++) {
                    if (curr.attributes.length) {
                        $.merge(_state.extraAttributes, curr.attributes);
                    }
                }
            }
        }
        setup();
        $('#biomart-loading').fadeAndRemove();
    }

    function singleFilterSetup(filter) {
        if (filter.type == 'singleSelectUpload') {
            _state.filter = filter;
            _state.attributes = filter.filters;
            setup();
        } else {
            biomart.error('Filter type "' + filter.type + '" not support for this GUI type');
        }
    }

    function setup() {
        var div = biomart.renderer.filter('div', _state.filter),
            select = ['<select id="biomart-attributes">'];

        // From
        _elements.fromDiv.find('.simplerfilter').simplerfilter('destroy').remove();
        div.appendTo(_elements.fromDiv).simplerfilter({chooseText: false});
        _elements.inputField = _elements.fromDiv.find('textarea.field,input.field,select.field');

        _elements.filterDiv = div;

        // To
        if (_state.attributes) {
            _elements.toDiv.find('select.ui-prettybox').prettybox('destroy').remove();
            for (var i=0, attr; attr=_state.attributes[i]; i++) {
                select.push([
                    '<option value="', attr.name, '">', attr.displayName, '</option>'
                ].join(''));
            }

            select.push('</select>');
            select = $(select.join(''))
                .appendTo(_elements.toDiv)
                .prettybox();
        } else {
            _elements.toDiv.hide();
            _elements.dsDiv.css('margin', '10px auto');
            _elements.fromDiv.css({
                'float': 'none',
                'margin': 'auto'
            });
        }

        // Same height for both boxes
        var h1 = _elements.fromDiv.height(),
            h2 = _elements.toDiv.height();

        if (h1 > h2) {
            _elements.toDiv.height(h1);
        } else {
            _elements.fromDiv.height(h2);
        }
    }

    function block() {
        _elements.content.block({
            message: '<span class="loading"></span>',
            css: {
                backgroundColor: 'transparent',
                width: '100%',
                border: 'none'
            },
            overlayCSS: {
                backgroundColor: '#fff'
            }
        });
    }

    function unblock() {
        _elements.content.unblock();
    }

    function validate() {
        return !!_elements.inputField.val();
    }

    function reset() {
        _state.queryMart.filters = [];
        _state.queryMart.attributes = [];
    }

    function runQuery() {
        var q = _state.queryMart,
            value = biomart.validator.filter(_elements.filterDiv),
            f,
            filter,
            attribute;

        block();

        reset();

        if (_state.mart.config)
            q.config = _state.mart.config;

        q.datasets = _state.activeDataset;

        if ($.isArray(value)) {
            f = {name: value[0], value: value[1]};
            q.attributes.push({name: f.name});
        } else {
            f = {name: _state.filter.name, value: value};
            q.attributes.push({name: _state.filter.attribute});
        }

        q.filters.push(f);

        _elements.toDiv.find('option').each(function() {
            if (this.selected) {
                q.attributes.push({name: this.value});
                return false;
            }
        });

        // If there are extra attributes
        if (_state.extraAttributes && _state.extraAttributes.length) {
            for (var i=0, attr; attr=_state.extraAttributes[i]; i++) {
                q.attributes.push(attr);
            }
        }

        _state.xml = biomart.query.compileSingleMartXML(q, 'TSV', -1, true, CLIENT);

        _elements.results
            .queryResults($.extend({
                queries: _state.xml,
                done: function(total) {
                    if (total) {
                        _elements.resultsActions.show();
                    } else {
                        _elements.resultsActions.hide();
                    }
                    unblock();
                }
            }, QUERY_RESULTS_OPTIONS));

        _elements.resultsWrapper.dialog('open');
    }
});

$.subscribe('biomart.init', biomart.converter, 'init');

})(jQuery);
