(function($) {

/*
 * Author: jhsu
 *
 * This file + required modules contains all the JavaScript to run MartExplorer.
 *
 * The workflow starts from the biomart.martexplorer.init function, and ends with _stepCallback function.
 *
 * The _stepCallback function has an array (bound by closure) that maps to each of the four steps.
 * Each step callback contains its own specific logic (e.g. Results requires >= 1 attributes, or else
 * goes back to Output step)
 *
 * _elements hold all jQuery DOM elements that are used multiple times
 * _state holds current state of the explorer application
 *
 */

$.namespace('biomart.martexplorer', function(self) {
    var QUERY_CLIENT = 'webbrowser',
        QUERY_LIMIT = 1000,
        ANIMATION_TIME = 100,
        BLOCK_OPTIONS  = {
            message: '<span class="loading" style="margin: 3px auto"></span>',
            css: { padding: '5px', borderColor: '#2e6e9e' },
            overlayCSS:  { backgroundColor: '#fff' }
        },
        QUERY_RESULTS_OPTIONS = {
            paginateBy: 20,
            showProgress: true,
            animationTime: ANIMATION_TIME,
            showLoading: false
        },
        SORTABLE_OPTIONS = {
            helper: 'clone',
            revert: 250,
            cursor: 'move',
            update: function(ev, ui) {
                $.publish('biomart.change');
                if (!ui.item.hasClass('empty')) {
                }
            }
        },

        _urlHash,
        _guiContainer, // required (see MartService API document)
        _elements = {}, // cache
        _dimensions = {},

        // URL query parameters that are not filters (need to know this later)
        _nonFilterParams = ['mart', 'step', 'datasets', 'attributes', '_ac', '_fc'],

        // callbacks when user navigates to a step
        _stepCallback = (function() {
            var fn = [
                // Datasets
                function(element) {
                    _elements.filterTree.addClass('hidden');
                    _elements.navPrev.addClass('ui-state-disabled');
                    _elements.navNext.show();
                    _elements.navResults.hide();
                    if (biomart._state.queryMart.datasets && biomart._state.queryMart.datasets.length) {
                        _elements.navNext.removeClass('ui-state-disabled');
                    } else {
                        _elements.navNext.addClass('ui-state-disabled');
                    }
                    if (!_elements.steps.eq(0).data('loaded')) {
                    } else {
                        $.publish('biomart.loaded');
                    }
                },
                // Filters
                function(element) {
                    _elements.filterTree.removeClass('hidden');
                    _elements.navPrev.removeClass('ui-state-disabled');
                    _elements.navNext.removeClass('ui-state-disabled').show();
                    _elements.navResults.hide();
                    if (!element.data('loaded')) {
                        loadFilters(function() {
                            element.data('loaded', true);
                        });
                    } else {
                        $.publish('biomart.loaded');
                    }
                },
                // Output
                function(element) {
                    _elements.attributeTree.removeClass('hidden');
                    _elements.navPrev.removeClass('ui-state-disabled');
                    _elements.navNext.hide();
                    _elements.navResults.show();
                    if ($.isEmptyObject(biomart._state.queryMart.attributes)) {
                        _elements.navResults.addClass('ui-state-disabled');
                    } else {
                        _elements.navResults.removeClass('ui-state-disabled');
                    }
                    if (!element.data('loaded')) {
                        loadAttributes(function() {
                            if (!$.isEmptyObject(biomart._state.queryMart.attributes)) {
                                _elements.navResults.removeClass('ui-state-disabled');
                            }
                            element.data('loaded', true);
                        });
                    } else {
                        $.publish('biomart.loaded');
                    }
                },

                function(element) {
                    var title = biomart.utils.hasGroupedMarts() ? biomart._state.mart[0].group : biomart._state.mart.displayName,
                        processor = "TSVX", renderer = "table", limit = QUERY_LIMIT;

                    _elements.contentWrapper.slideUp();
                    _elements.toolbar.slideUp();
                    _elements.results.resultsPanel('run', title,
                        $.extend({
                            queries: getXmlDefault(processor, limit, QUERY_CLIENT),
                            downloadXml: getXmlDefault("TSV", -1, "false"),
                            martObj: biomart._state.queryMart,
                            dataAggregation: 'none',
                            displayType: renderer
                        }, QUERY_RESULTS_OPTIONS));
                    $.publish('biomart.loaded');
                }
            ];

            return function(index, callback) {
                fn[index](_elements.steps.eq(index));
                self.resize();
                if (callback) callback();
            };
        })();

    biomart._state = {
        queryMart: {datasets:null, attributes:{}, filters:{}},
        activeStep: 1
    };

    // Invalid filters will show as disabled controls
    biomart.renderer.renderInvalid = true;

    /*
     * Public functions
     */

    self.init = function() {
        if (!biomart._state.initialized) {
            // Initial page load
            initElements();
            initNav();
        } else {
            // Probably a logout action
            _elements.martSelectBox.empty();
            updateParam('mart', null);
            updateParam('step', 1);
            biomart.resource.cache = {};
        }

        biomart.resource.load('gui', function(json) {
            var martSelect;

            if (!biomart._state.initialized) {
                initDimensions();
            }

            if (!json.marts) {
                biomart.error();
                self.resize();
                return;
            }

            martSelect = biomart.makeMartSelect(json.marts, {
                    each: function(mart, selected) {
                        // If mart not set, default to first
                        if (selected) {
                            biomart._state.mart = mart;
                            $.publish('biomart.change', 'mart', mart);
                        } else if (!biomart._state.mart) biomart._state.mart = mart;
                    },
                    selected: biomart.params.mart
                });

            martSelect
                .addClass('mart')
                .appendTo(_elements.martSelectBox)
                .prettybox();

            initState();

            self.reset(function() {
                if (biomart.params.step) self.step(parseInt(biomart.params.step));
                if (!biomart._state.initialized)
                    attachEvents();
                initData(function() {
                    _stepCallback(biomart._state.activeStep-1);
                    biomart._state.initialized = true;
                    $('#biomart-loading').fadeAndRemove(ANIMATION_TIME);
                });
            });
        }, {name: _guiContainer});
    };

    // Reset everything but mart
    self.reset = function(callback) {
        _elements.steps.each(function() { $(this).data('loaded', false) });
        getDatasets(function() {
            if (biomart._state.mart.config) {
                biomart._state.queryMart.config = biomart._state.mart.config;
            }
            updateDatasets();
            if (callback) callback();
        });
    };

    // Update mart from selection and reset
    self.restart = function() {
        updateMart(biomart._state.mart);
        self.refreshParamCache();
        self.reset();
    };

    self.resize = function() {
        var h, w;

        h = _dimensions.viewportHeight = $(window).height() - 120;
        h -= _dimensions.header;
        h -= _dimensions.footer;
        h -= _dimensions.toolbar;
        h -= _dimensions.contentFooter;

        _elements.filterContent.height(h-2 + 'px');
        _elements.filterTree.height(h-2 + 'px');
        _elements.attributeContent.height(h-2 + 'px');
        _elements.attributeTree.height(h-2 + 'px');
        _elements.summary.height(h + 'px');
        _elements.main.height(h + 'px');

        _elements.mainSteps.height(h + 'px');

        w = _elements.contentWrapper.outerWidth() - 26;
        w -= _dimensions.summaryWidth;

        _elements.main.width(w + 'px');
    };

    self.updateNav = function(type) {
        var num = biomart._state.activeStep;
        _elements.stepInfo.removeClass('ui-active').filter('.step-' + num).addClass('ui-active');
        // step callback is called at end of initialization, so no need to do it here
        if (biomart._state.initialized) {
            _stepCallback(num-1);
        }
    };

    self.updateStepInfo = function() {
        // Datasets valid
        if (biomart._state.queryMart.datasets && biomart._state.queryMart.datasets.length) {
            _elements.stepInfo.eq(1).removeClass('ui-state-disabled');
            _elements.stepInfo.eq(2).removeClass('ui-state-disabled');
        } else {
            _elements.stepInfo.eq(1).addClass('ui-state-disabled');
            _elements.stepInfo.eq(2).addClass('ui-state-disabled');
            _elements.stepInfo.eq(3).addClass('ui-state-disabled');
        }

        // Attributes valid
        if (biomart.params.attributes) {
            _elements.stepInfo.eq(3).removeClass('ui-state-disabled');
        } else {
            _elements.stepInfo.eq(3).addClass('ui-state-disabled');
        }
    };

    self.showSubContainer = function() {
        if (biomart._state.initialized) {
            if (biomart._state.activeStep == 2) {
                var node = _elements.filterTree.find('.jstree-leaf');
                if (biomart.params._fc) {
                    node = node.filter('.' + biomart.params._fc);
                } else {
                    node = node.eq(0);
                }
                node = node.children('a')
                if (!node.hasClass('jstree-clicked')) {
                    node.click()
                }
            } else if (biomart._state.activeStep == 3) {
                var node = _elements.attributeTree.find('.jstree-leaf');
                if (biomart.params._ac) {
                    node = node.filter('.' + biomart.params._ac);
                } else {
                    node = node.eq(0);
                }
                node = node.children('a')
                if (!node.hasClass('jstree-clicked')) {
                    node.click()
                }
            }
        }
    };

    self.block = function() {
        _elements.content.block(BLOCK_OPTIONS);
    };

    self.unblock = function() {
        _elements.content.unblock();
    };

    // Goes to a step in the explorer (between 1 and 4)
    self.step = function(num) {
        if (num < 1 || num > 4) return;
        var index = num - 1,
            curr;
        _elements.steps.eq(biomart._state.activeStep-1).fadeOut({ duration: ANIMATION_TIME });
        curr = _elements.steps.eq(index).fadeIn();
        if (!curr.data('loaded')) self.block();
        biomart._state.activeStep = index + 1;
        _elements.filterContent.scrollTop(0);
        _elements.attributeContent.scrollTop(0);
        $.publish('biomart.change', 'step', num);
    };

    self.invalidateResults = function() {
    };

    self.refreshParamCache = function() {
        // Grab URL info
        _urlHash = biomart.url.jsonify(location.href);
        _urlHash = biomart.url.jsonify(_urlHash.fragment); // only care about fragment
        _guiContainer = _urlHash.path.replace(/\/$/, ''); // clean trailing slash (if exist)
        biomart.params = $.deparam(_urlHash.query);
        if (biomart._state.initialized && biomart.params.step && biomart.params.step != biomart._state.activeStep) {
            self.step(parseInt(biomart.params.step));
        }
    };

    self.updateSummary = function(type) {
        var item, value, containerName;
        switch (type) {
            case 'mart':
                item = arguments[1];
                _elements.summaryMart.html(['<li>', biomart.utils.hasGroupedMarts() ?  item[0].group : item.displayName, '</li>'].join(''));
                break;
            case 'datasets':
                item = arguments[1];
                _elements.summaryDatasets.empty();
                _elements.summaryAttributes
                    .html(['<li class="empty">', _('empty_value'), '</li>'].join(''));
                _elements.summaryFilters
                    .html(['<li class="empty">', _('empty_value'), '</li>'].join(''));

                if (!item.length) {
                    _elements.summaryDatasets
                        .empty()
                        .append(['<li class="empty">', _('empty_value'), '</li>'].join(''));
                } else {
                    for (var i=0, ds; ds=item[i]; i++) {
                        $(['<li class="', ds.name, '">',
                            ds.displayName,
                        '</li>'].join(''))
                            .appendTo(_elements.summaryDatasets)
                            .data('item', ds);
                    }
                }
                break;
            case 'filters':
                item = arguments[1];
                value = arguments[2];
                containerName = item['parent']

                _elements.summaryFilters.children('li.empty').remove();

                if ($.isEmptyObject(biomart._state.queryMart.filters)) {
                    _elements.summaryFilters
                        .empty()
                        .append(['<li class="empty">', _('empty_value'), '</li>'].join(''));
                } else {
                    if (value) {
                        var li = _elements.summaryFilters.children('li.' + biomart.renderer.makeClassName(item.name)),
                            displayName, value;

                        if (!li.length) {
                            li = $(['<li data-container="', containerName, '" class="', biomart.renderer.makeClassName(item.name), '"></li>'].join('')).appendTo(_elements.summaryFilters);
                        }

                        li
                            .data('item', item)
                            .data('value', value);

                        if ($.isArray(value)) {
                             displayName  = value[2];
                             value = value[1];
                        } else {
                             displayName = item.displayName;
                        }

                        // -------->> iwan
                        var len_max = 50;
                        var title = "";
                        if (value.length>len_max) {
                            var items = value.split(",").length;
                            title = value;
                            value = items + " " + (items==1 ? 'item' : 'items') + " in the list";
                        }
                        // <<-------- iwan

                        li
                            .html([
                                '<span class="key">', displayName, '</span>: ',
                                '<span class="value" title="', title, '">', value, '</span>',
                                '<span class="ui-icon ui-icon-circle-close" title="Remove"/>',
                                '<span class="ui-icon ui-icon-circle-arrow-w" title="View in container"/>'
                            ].join(''));
                    } else {
                        _elements.summaryFilters.children('li.' + biomart.renderer.makeClassName(item.name)).addClass('removing').fadeAndRemove(ANIMATION_TIME);
                    }
                }
                break;
            case 'attributes':
                item = arguments[1];
                containerName = item['parent'];

                _elements.summaryAttributes.children('li.empty').remove();

                if ($.isEmptyObject(biomart._state.queryMart.attributes)) {
                    _elements.summaryAttributes.empty();
                    $(['<li class="empty">', _('empty_value'), '</li>'].join(''))
                        .data('item', item)
                        .appendTo(_elements.summaryAttributes);
                } else {
                    if (arguments[2] /* add */) {
                        if (!_elements.summaryAttributes.children('li.' + biomart.renderer.makeClassName(item.name)).length) {
                            $(['<li data-container="', containerName, '" class="', biomart.renderer.makeClassName(item.name), '">',
                                    item.displayName,
                                    '<span class="ui-icon ui-icon-circle-close" title="Remove"/>',
                                    '<span class="ui-icon ui-icon-circle-arrow-w" title="View in container"/>',
                                '</li>'].join(''))
                                .data('item', item)
                                .appendTo(_elements.summaryAttributes);
                        }
                    } else {
                        _elements.summaryAttributes.children('li.' + biomart.renderer.makeClassName(item.name)).addClass('removing').fadeAndRemove(ANIMATION_TIME);
                    }
                }
                break;
        }
    };

    /*
     * Helper functions for initialization
     */

    function initState() {
        for (var k in biomart.params) {
            if ($.inArray(k, _nonFilterParams) == -1) {
                biomart._state.queryMart.filters[k] = {name:k, value:biomart.params[k]};
            }
        }
        for (var k in biomart.params) {
            if ($.inArray(k, _nonFilterParams) == -1) {
                biomart._state.queryMart.filters[k] = {
                    name: k,
                    value: biomart.params[k]
                };
            }
        }
        if (biomart.params.attributes) {
            var attributes = biomart.params.attributes.split(',');
            for (var i=0, a; a=attributes[i]; i++) {
                biomart._state.queryMart.attributes[a] = {name:a};
            }
        }
    }

    function initElements() {
        // Cache elements for use later and set up any UI widgets
        _elements.header = $('#biomart-header');
        _elements.wrapper = $('#biomart-wrapper');
        _elements.filterContent = $('#biomart-filter-content');
        _elements.filterTree = $('#biomart-filter-tree');
        _elements.attributeContent = $('#biomart-attribute-content');
        _elements.attributeTree = $('#biomart-attribute-tree');
        _elements.main = $('#biomart-main');
        _elements.mainSteps = _elements.main.children('div.steps');
        _elements.content = $('#biomart-content');
        _elements.contentWrapper = $('#biomart-content-wrapper');
        _elements.toolbar = $('#biomart-toolbar');
        _elements.stepInfo = $('#biomart-step-info').children();
        _elements.stepsContainer = _elements.content.find('.steps');
        _elements.steps = _elements.content.find('.step');
        _elements.results = $('#biomart-results')
            .resultsPanel()
            .bind('edit', function() {
                _elements.results.resultsPanel('edit');
                _elements.toolbar.slideDown();
                _elements.contentWrapper.slideDown({
                    duration: 'fast',
                    complete: function() {
                        updateParam('step', biomart._state.activeStep-1);
                        self.step(biomart._state.activeStep-1);
                    }
                });
            });
        _elements.contentFooter = $('#biomart-content-footer');
        _elements.nav = $('#biomart-navigation');
        _elements.navPrev = _elements.nav.find('a.prev');
        _elements.navNext = _elements.nav.find('a.next');
        _elements.navResults = _elements.nav.find('a.results');
        _elements.footer = $('#biomart-footer');
        _elements.views = null;
        _elements.exportForm = $('#biomart-export-form');
        _elements.exportLimit = $('#biomart-export-limit');
        _elements.martSelectBox = $('#biomart-select-mart');
        _elements.datasetSelectBox = $('#biomart-select-dataset');
        _elements.summary = $('#biomart-summary');
        _elements.summaryMart = _elements.summary.find('li.mart > ul');
        _elements.summaryDatasets = _elements.summary.find('li.datasets > ul');
        _elements.summaryFilters = _elements.summary.find('li.filters > ul').sortable(SORTABLE_OPTIONS);
        _elements.summaryAttributes = _elements.summary.find('li.attributes > ul').sortable(SORTABLE_OPTIONS);
        _elements.viewXmlLink = _elements.summary.find('div.actions');
        _elements.viewXml = $('#biomart-view-xml').dialog({
            modal: true,
            autoOpen: false,
            width: 600,
            height: 350,
            resizable: false,
            buttons: {
                'Close': function() {
                    $(this).dialog('close');
                },
                'Toggle quote-escape': function() {
                    var limit = parseInt(_elements.exportLimit.val()) || -1,
                        xml = getXmlDefault('TSV', limit, true, ''),
                        $this = $(this);
                    if (!$this.data('escaped')) {
                        xml = xml.replace(/"/g, '\\"');
                        $this.data('escaped', true);
                    } else {
                        $this.data('escaped', false);
                    }
                    _elements.viewXml.children('textarea').val(xml);
                }
            }
        });
        _elements.newButton = $('#biomart-new');

        _elements.noWarningCheckbox = $('#biomart-no-warning');
        _elements.warning = $('#biomart-warning').dialog({
            modal: true,
            autoOpen: false,
            width: 425,
            height: 225,
            resizable: false,
            buttons: {
                'No': function() {
                    $(this).dialog('close');
                },
                'Yes, start new': function() {
                    if (_elements.noWarningCheckbox[0].checked) {
                        $.cookie('no_warning', 1, {path: '/', expires: 365});
                    }
                    self.restart();
                    $(this).dialog('close');
                }
            }
        });
    }

    function initDimensions() {
        _dimensions.header = _elements.header.outerHeight();
        _dimensions.toolbar = _elements.toolbar.outerHeight();
        _dimensions.nav = _elements.nav.outerHeight();
        _dimensions.contentFooter = _elements.contentFooter.outerHeight();
        _dimensions.footer = _elements.footer.outerHeight();
        _dimensions.summaryWidth = _elements.summary.outerWidth();
        $(window).bind('resize.martexplorer', self.resize);
    }

    function initNav() {
        _elements.nav.delegate('a', 'click.martexplorer', function() {
            var $this = $(this),
                index;
            if (!$this.hasClass('ui-state-disabled')) {
                if ($this.hasClass('next')) index = biomart._state.activeStep;
                if ($this.hasClass('prev')) index = biomart._state.activeStep - 2;
                updateParam('step', index+1);
            }
            return false;
        });
        _elements.stepInfo.bind('click.martexplorer', function() {
            var $this = $(this);
            if (!$this.hasClass('ui-state-disabled')) {
                updateParam('step', $this.attr('data-step-number'));
            }
        });
        _elements.newButton.bind('click.martexplorer', function() {
            if ($.cookie('no_warning') == '1') {
                self.restart();
            } else {
                _elements.warning.dialog('open');
            }
            return false;
        });
    }

    function initData(callback) {
        var queue = new biomart.Queue(_elements.content, 'init'),
            hasSetFilters = !$.isEmptyObject(biomart._state.queryMart.filters),
            hasSetAttributes = !$.isEmptyObject(biomart._state.queryMart.attributes);

        _elements.content.one('queue.done', callback);

        if (hasSetFilters) {
            queue.queue(function() {
                var params = {datasets: biomart._state.queryMart.datasets.join(',')};
                if (biomart._state.queryMart.config)
                    params.config = biomart._state.queryMart.config;
                biomart.resource.load('filters', function(json) {
                    for (var i=0, f, v; f=json[i]; i++) {
                        if (v = biomart._state.queryMart.filters[f.name]) {
                            $.publish('biomart.change', 'filters', f, v.value);
                        }
                    }
                    queue.dequeue();
                }, params);
            });
        }

        if (hasSetAttributes) {
            queue.queue(function() {
                var params = {datasets: biomart._state.queryMart.datasets.join(',')};
                if (biomart._state.queryMart.config)
                    params.config = biomart._state.queryMart.config;
                biomart.resource.load('attributes', function(json) {
                    for (var i=0, a; a=json[i]; i++) {
                        if (a.selected || biomart._state.queryMart.attributes[a.name]) {
                            $.publish('biomart.change', 'attributes', a, true);
                        }
                    }
                    queue.dequeue();
                }, params);
            });
        }

        queue.dequeue();
    }

    function attachEvents() {
        // Mart changed
        _elements.steps.eq(0).delegate('select.mart', 'change.martexplorer', function(ev) {
            updateMart($(this).children().eq(this.selectedIndex).data('mart'));
            self.refreshParamCache();
            self.reset();
        });

        // Dataset changed
        _elements.steps.eq(0).delegate('select.datasets', 'change.martexplorer', function(ev) {
            updateDatasets();
        });

        // Filters changed
        _elements.filterContent
            .delegate('.filter-container', 'removefilter', function(ev) {
                 biomart.clearFilter($(this), function(item) {
                    removeQueryFilter(item);
                 });
            })
            .delegate('.filter-container', 'addfilter', function(ev) {
                var $div = $(this),
                    $box = $div.children('input.checkbox'),
                    name = $div.attr('filter-name'),
                    value = biomart.validator.filter($div),
                    valid = false,
                    item = $div.data('item');

                if ($.isArray(value)) valid = !!value[0] && !!value[1];
                else valid = !!value;

                if (valid) {
                    $div.addClass('ui-active');
                    setQueryFilter(item, value);
                } else {
                    $div.removeClass('ui-active');
                    removeQueryFilter(item);
                }
            });

        // attributes changed
        _elements.attributeContent
            .delegate('.attribute-container', 'addattribute', function(ev) {
                var item = $(this).addClass('ui-active').data('item');
                addQueryAttribute(item);
            })
            .delegate('.attribute-container', 'removeattribute', function(ev) {
                var item = $(this).removeClass('ui-active').data('item');
                removeQueryAttribute(item);
            });

        // URL fragment changed
        $(window).bind('hashchange', self.refreshParamCache);

        // View XML function
        _elements.viewXmlLink.bind('click.martexplorer', function() {
            var limit = parseInt(_elements.exportLimit.val()) || -1,
                xml = getXmlDefault('TSV', limit, true, '');
            _elements.viewXml.children('textarea').val(xml);
            _elements.viewXml.data('escaped', false).dialog('open');
        });

        _elements.summary
            .delegate('span.ui-icon-circle-close', 'click.martexplorer', function(ev) {
                var $target = $(ev.target),
                    $li = $target.parent(),
                    item = $li.data('item');

                if (item.type) {
                    // Filter
                    removeQueryFilter(item);
                    (function(div) {
                    if (div.data('loaded')) {
                        div.find('.' + biomart.renderer.makeClassName(item.name))
                            .find('span.ui-icon-circle-close').trigger('click');
                    }
                    })(_elements.steps.eq(1));
                } else {
                    // Attribute
                    removeQueryAttribute(item);
                    (function(div) {
                    if (div.data('loaded')) {
                        div.find('.' + biomart.renderer.makeClassName(item.name))
                            .removeClass('ui-active')
                            .find('input.checkbox').attr('checked', false);
                    }
                    })(_elements.steps.eq(2));
                }
            })
            .delegate('span.ui-icon-circle-arrow-w', 'click.martexplorer', function(ev) {
                var $target = $(ev.target),
                    $li = $target.parent(),
                    item = $li.data('item'),
                    ancestor = $li.parent().closest('li');
                if (ancestor.hasClass('filters')) {
                    showFilterContainer($li.attr('data-container'), biomart.renderer.makeClassName(item.name));
                } else {
                    showAttributeContainer($li.attr('data-container'), biomart.renderer.makeClassName(item.name));
                }
            });
    }

    /*
     * Functions for updating user selections
     */

    function updateParam(name, value) {
        var newUrlHash = biomart.url.jsonify(location.href);
        if (value) biomart.params[name] = value;
        else delete biomart.params[name];
        _urlHash.query = $.param(biomart.params);
        newUrlHash.fragment = biomart.url.stringify(_urlHash);
        location = biomart.url.stringify(newUrlHash);
    }

    function updateMart(item) {
        var name;

        biomart._state.mart = item;

        if ($.isArray(item)) {
            name = item[0].group;
        } else {
            name = item.name;
        }

        // reset all selections
        biomart._state.queryMart = { dataset: null, attributes:{}, filters: {} };
        updateParam('mart', name);
        updateParam('datasets', null);
        updateParam('attributes', null);
        updateParam('filters', null);
        updateParam('step', 1);
        $.publish('biomart.change', 'mart', item);
    }

    function updateDatasets() {
        var selected = [],
            items = [],
            active = _elements.datasetSelect.children('option:selected');

        for (var i=0, n=active.length, curr; i<n; i++) {
            curr = active.eq(i);
            selected.push(curr.attr('value'));
            items.push(curr.data('item'));
        }

        biomart._state.queryMart.datasets = selected;

        if (biomart.utils.hasGroupedMarts) {
            var ds = items[0];
            if (ds && ds.mart.config) {
                biomart._state.queryMart.config = ds.mart.config;
            } else {
                delete biomart._state.queryMart.config;
            }
        }

        // When app has initialized, we need to clear all attributes and filters from URL
        // Otherwise they are present from previous "saved" state
        if (biomart._state.initialized) {
            biomart._state.queryMart.filters = {};
            biomart._state.queryMart.attributes = {};
            biomart._state.activeStep = 1;

            _elements.steps.eq(1).data('loaded', false);
            _elements.steps.eq(2).data('loaded', false);

            _elements.filterTree.jstree('destroy')
            _elements.filterContent.empty();
            _elements.attributeTree.jstree('destroy');
            _elements.attributeContent.empty();

            updateParam('datasets', selected.join(','));
            updateParam('attributes', null);

            for (var k in biomart.params) {
                if ($.inArray(k, _nonFilterParams) == -1) {
                    updateParam(k, null);
                }
            }
        }

        $.publish('biomart.change', 'datasets', items);
    }

    function setQueryFilter(item, value) {
        biomart._state.queryMart.filters[item.name] = {name: item.name, value: value};
        updateParam(item.name, value);
        $.publish('biomart.change', 'filters', item, value);
    }

    function removeQueryFilter(item) {
        delete biomart._state.queryMart.filters[item.name];
        updateParam(item.name, null);
        $.publish('biomart.change', 'filters', item, null);
    }

    function addQueryAttribute(item) {
        biomart._state.queryMart.attributes[item.name] = {name: item.name};
        updateAttributeParam();
        $.publish('biomart.change', 'attributes', item, true);
    }

    function removeQueryAttribute(item) {
        delete biomart._state.queryMart.attributes[item.name];
        updateAttributeParam();
        $.publish('biomart.change', 'attributes', item, false);
    }

    function updateAttributeParam() {
        var newAttributes = [];
        for (var name in biomart._state.queryMart.attributes) {
            newAttributes.push(name);
        }
        updateParam('attributes', newAttributes.join(','));
    }


    /*
     * Functions for fetching configurations and data
     */

    function getDatasets(callback) {
        var martParam,
            resource;

        if (!biomart.utils.hasGroupedMarts()) {
            martParam = biomart._state.mart.name;
            resource = 'datasets';
        } else {
            martParam = $.map(biomart._state.mart, function(m) { return m.name });
            martParam = martParam.join(',');
            resource = 'datasets/mapped';
        }

        biomart.resource.load(resource, function(json) {
            var selected = false,
                dsArr = biomart.params.datasets ? biomart.params.datasets.split(',') : [],
                multiple = false,
                isMapped = biomart.utils.hasGroupedMarts();

            if (!isMapped) {
                multiple = biomart._state.mart.operation == biomart.OPERATION.MULTI_SELECT;
            }

            if (_elements.datasetSelect) {
                _elements.datasetSelect.prettybox('destroy').remove();
            }

            _elements.datasetSelect = $(['<select ', multiple ? 'multiple size="10"' : '', ' class="datasets" id="',
                biomart.uuid() , '"/>'].join('')).appendTo(_elements.datasetSelectBox);

            if (!isMapped) {
                for (var i=0, d; d=json[i]; i++) {
                    if (!d.isHidden) {
                        d.mart = biomart._state.mart;
                        selected = (dsArr.length && $.inArray(d.name, dsArr) != -1) || (!dsArr.length && i==0);
                        $([
                            '<option value="', d.name, '"', selected ? ' selected="selected"' : '', '>',
                                d.displayName,
                            '</option>'
                        ].join('')).data('item', d).appendTo(_elements.datasetSelect);
                    }
                }
            } else {
                var num = 0;

                for (var k in json) {
                    var datasets = json[k],
                        mart;

                    for (var i=0, curr; curr=biomart._state.mart[i]; i++) {
                        if (curr.name == k) {
                            mart = curr;
                            break;
                        }
                    }

                    for (var i=0, ds; ds=datasets[i]; i++) {
                        selected = (dsArr.length && $.inArray(ds.name, dsArr) != -1) || (num==0);
                        ds.mart = mart;
                        $(['<option value="', ds.name, '"', selected ? ' selected="selected"' : '', '>', ds.displayName, '</option>'].join(''))
                            .data('item', ds)
                            .appendTo(_elements.datasetSelect);

                        num++;
                    }
                }
            }


            if (!multiple) _elements.datasetSelect.prettybox();

            _elements.steps.eq(0).data('loaded', true);

            if (callback) callback();
        }, {mart: martParam});
    }

    function findContainer(node, name) {
        if (node.name == name) return node;
        if (node.containers) {
            for (var i=0, curr; curr=node.containers[i]; i++) {
                var result = findContainer(curr, name);
                if (result) return result;
            }
        }
        return null;
    }

    function loadFilters(callback) {
        var params = {
            datasets: biomart._state.queryMart.datasets.join(','),
            withfilters: true,
            withattributes: false
        };

        if (biomart._state.queryMart.config)
            params.config = biomart._state.queryMart.config;

        $.unsubscribeAll('filterchange');
        _elements.steps.eq(1).data('loaded', true);
        biomart.resource.load('containers', function(json) {
            var root = biomart.utils.processContainer(json);

            if (root) {

                biomart._state.filtersRoot = json;

                root.title = biomart._state.mart.displayName,
                root.attr = {
                    'class': 'mart ' + biomart._state.mart.name,
                    mart: biomart._state.mart.name
                };
                root.state = 'open';

                if (!_elements.filterTree.hasClass('jstree')) {
                    _elements.filterTree
                        .bind('select_node.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj),
                                $martNode = $node.closest('li.mart'),
                                martName = $martNode.attr('mart'),
                                name = $node.attr('container'),
                                container;
                            updateParam('_fc', name);
                            container = findContainer(biomart._state.filtersRoot, name);
                            if ($node.hasClass('jstree-leaf') && !$node.hasClass('mart')) {
                                drawFilterContainer(_elements.filterContent.empty(), container);
                            }
                        })
                        .bind('beforechange.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj);
                            if ($node.hasClass('jstree-leaf') && !$node.hasClass('mart')) {
                                return true;
                            } else if (!$node.hasClass('loader')) {
                                $.jstree._focused().open_branch($node);
                            }
                            return false;
                        })
                        .bind('beforeclose.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj);
                            if ($node.find('a.clicked').length) {
                                 return false;
                            }
                            return true;
                        })
                        .jstree({
                            plugins: ['themes', 'json_data', 'ui'],
                            json_data: {
                                data: root.children
                            },
                            themes: {
                                theme: 'default'
                            }
                        });
                }

                // Small hack to use setTimeout because otherwise the proper styles don't get added for some reason
                // Bug in jstree maybe?
                setTimeout(function() {
                    var node = _elements.filterTree.find('.jstree-leaf');
                    if (biomart.params._fc) {
                        node = node.filter('.' + biomart.params._fc);
                    } else {
                        node = node.eq(0);
                    }
                    node.children('a').click();
                }, 100);
            }
            if (callback) callback();
            $.publish('biomart.loaded');
        }, params);
    }

    function drawFilterContainer(element, container, level) {
        biomart.renderer.container({
            tagName:'div',
            item: container,
            mode: biomart.renderer.FILTERS,
            selectedFilters: biomart._state.queryMart.filters,
            appendTo: element
        });
    }

    function loadAttributes(callback) {
        var params = {
            datasets: biomart._state.queryMart.datasets.join(','),
            withfilters: false,
            withattributes: true
        };

        if (biomart._state.queryMart.config)
            params.config = biomart._state.queryMart.config;

        _elements.steps.eq(2).data('loaded', true);
        biomart.resource.load('containers', function(json) {
            var root = biomart.utils.processContainer(json);

            if (root) {

                biomart._state.attributesRoot = json;

                root.title = biomart._state.mart.displayName,
                root.attr = {
                    'class': 'mart ' + biomart._state.mart.name,
                    mart: biomart._state.mart.name
                };
                root.state = 'open';

                if (!_elements.attributeTree.hasClass('jstree')) {
                    _elements.attributeTree
                        .bind('select_node.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj),
                                $martNode = $node.closest('li.mart'),
                                martName = $martNode.attr('mart'),
                                name = $node.attr('container'),
                                container;
                            updateParam('_ac', name);
                            container = findContainer(biomart._state.attributesRoot, name);
                            if ($node.hasClass('jstree-leaf') && !$node.hasClass('mart')) {
                                drawAttributeContainer(_elements.attributeContent.empty(), container);
                            }
                        })
                        .bind('beforechange.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj);
                            if ($node.hasClass('jstree-leaf') && !$node.hasClass('mart')) {
                                return true;
                            } else if (!$node.hasClass('loader')) {
                                $.jstree._focused().open_branch($node);
                            }
                            return false;
                        })
                        .bind('beforeclose.jstree', function(ev, data) {
                            var $node = $(data.rslt.obj);
                            if ($node.find('a.clicked').length) {
                                 return false;
                            }
                            return true;
                        })
                        .jstree({
                            plugins: ['themes', 'json_data', 'ui'],
                            json_data: {
                                data: root.children
                            },
                            themes: {
                                theme: 'default'
                            }
                        });
                    }

                // Small hack to use setTimeout because otherwise the proper styles don't get added for some reason
                // Bug in jstree maybe?
                setTimeout(function() {
                    var node = _elements.attributeTree.find('.jstree-leaf');
                    if (biomart.params._ac) {
                        node = node.filter('.' + biomart.params._ac);
                    } else {
                        node = node.eq(0);
                    }
                    node.children('a').click();
                }, 100);
            }

            $.publish('biomart.loaded');
        }, params);
    }

    function drawAttributeContainer(element, container, level, extraClassNames) {
        biomart.renderer.container({
            tagName:'div',
            item: container,
            mode: biomart.renderer.ATTRIBUTES,
            extraClassNames: extraClassNames || '',
            selectedAttributes: biomart.params.attributes ? biomart.params.attributes.split(',') : [],
            onAttributeSelect: function(item) {
                biomart._state.queryMart.attributes[item.name] = {name:item.name};
                $.publish('biomart.change', 'attributes', item, true);
            },
            extras: function(item, element) {
                if (item.maxContainers) {
                    element
                        .bind('containershow', function(ev, panel) {
                            var list = panel.find('.attribute-container.ui-active'),
                                n = list.length;
                            while (n--) {
                                addQueryAttribute(list.eq(n).data('item'));
                            }
                        })
                        .bind('containerhide', function(ev, panel) {
                            var list = panel.find('.attribute-container.ui-active'),
                                n = list.length;
                            while (n--) {
                                removeQueryAttribute(list.eq(n).data('item'));
                            }
                        });
                }
            },
            appendTo: element
        });
    }

    function getXmlDefault(renderer, limit, client) {
        var mart = {datasets: [], attributes: {}, filters: []};

        mart.config = biomart._state.queryMart.config;

        // Grab selections from summary panel and generate XML
        _elements.summaryDatasets.children().each(function() {
            var item = $(this).data('item');
            if (item)
                mart.datasets.push($(this).data('item').name);
        });
        _elements.summaryFilters.children(':not(.removing)').each(function() {
            var $this = $(this),
                item = $this.data('item'),
                value = $this.data('value'),
                name;

            if (item) {
                if ($.isArray(value)) {
                    name = value[0];
                    value = value[1];
                } else {
                    name = item.name;
                }
                mart.filters.push({
                    name: name,
                    value: value,
                    filterList: item.name
                });
            }
        });
        _elements.summaryAttributes.children(':not(.removing)').each(function() {
            var item = $(this).data('item');
            if (item) {
                mart.attributes[item.name] = {name: item.name};
            }
        });

        return biomart.query.compile('XML', mart, renderer, limit, true, client);
    }

    function getXmlSplitted(renderer, limit, client) {
        var mart = {datasets: [], attributes: {}, filters: {}};

        mart.config = biomart._state.queryMart.config;

        // Grab selections from summary panel and generate XML
        _elements.summaryDatasets.children().each(function() {
            var item = $(this).data('item');
            if (item)
                mart.datasets.push($(this).data('item').name);
        });
        _elements.summaryFilters.children(':not(.removing)').each(function() {
            var $this = $(this),
                item = $this.data('item'),
                value = $this.data('value'),
                name;

            if (item) {
                if ($.isArray(value)) {
                    name = value[0];
                    value = value[1];
                } else {
                    name = item.name;
                }
                mart.filters[name] = {name: name, value: value};
            }
        });
        _elements.summaryAttributes.children(':not(.removing)').each(function() {
            var item = $(this).data('item');
            if (item) {
                mart.attributes[item.name] = {name: item.name};
            }
        });

        var compiledQueries = [],
            newMart = JSON.parse(JSON.stringify(mart))
        // Split this query: one query for each attribute list
        for (var attr in mart.attributes) {
            newMart.attributes = {};
            newMart.attributes[attr] = mart.attributes[attr];
            compiledQueries.push(biomart.query.compile('XML', newMart, renderer, limit, true, client));
        }

        return compiledQueries
    }

    function showFilterContainer(containerName, filterName) {
        updateParam('_fc', containerName);
        updateParam('step', 2);
        _stepCallback(1);
    }

    function showAttributeContainer(containerName, attributeName) {
        updateParam('_ac', containerName);
        updateParam('step', 3);
        _stepCallback(2);
    }
});

/*
 * Bind callbacks to events
 *
 * The ordering here is very important!
 */
$.subscribe('biomart.init', biomart.martexplorer, 'refreshParamCache');
$.subscribe('biomart.init', biomart.martexplorer, 'init');
$.subscribe('biomart.loaded', biomart.martexplorer, 'unblock');
$.subscribe('biomart.loaded', biomart.martexplorer, 'updateStepInfo');
$.subscribe('biomart.loaded', biomart.martexplorer, 'showSubContainer');
$.subscribe('biomart.change', biomart.martexplorer, 'updateSummary');
$.subscribe('biomart.change', biomart.martexplorer, 'invalidateResults');
$.subscribe('biomart.change', biomart.martexplorer, 'updateNav');
$.subscribe('biomart.restart', biomart.martexplorer, 'init');

})(jQuery);

biomart.error = function() {
    $('#biomart-main').html('<p class="error">An error has occurred. You can try refreshing this page or go back to the <a href="../">landing page</a>.');
    $('#biomart-content').unblock();
    $('#biomart-loading').fadeAndRemove();
};
