(function($) {

/*
 * Author: jhsu
 *
 * This file + required modules contains all the JavaScript to run MartWizard.
 *
 * The workflow starts from the biomart.martwizard.init function, and ends with _stepCallback function.
 *
 * The _stepCallback function has an array (bound by closure) that maps to each of the four steps.
 * Each step callback contains its own specific logic (e.g. Results requires >= 1 attributes, or else 
 * goes back to Output step)
 *
 * _elements hold all jQuery DOM elements that are used multiple times
 * _state holds current state of the wizard application
 *
 */

$.namespace('biomart.martwizard', function(self) {
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
        _nonFilterParams = ['mart', 'step', 'datasets', 'attributes'],

        // callbacks when user navigates to a step
        _stepCallback = (function() {
            var fn = [
                // Datasets
                function() {
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
                function() {
                    _elements.navPrev.removeClass('ui-state-disabled');
                    _elements.navNext.removeClass('ui-state-disabled').show();
                    _elements.navResults.hide();
                    if (!_elements.steps.eq(1).data('loaded')) {
                        loadFilters();
                    } else {
                        $.publish('biomart.loaded');
                    }
                },
                // Output
                function() {
                    _elements.navPrev.removeClass('ui-state-disabled');
                    _elements.navNext.hide();
                    _elements.navResults.show();
                    if ($.isEmptyObject(biomart._state.queryMart.attributes)) {
                        _elements.navResults.addClass('ui-state-disabled');
                    }
                    if (!_elements.steps.eq(2).data('loaded')) {
                        loadAttributes(function() {
                            if (!$.isEmptyObject(biomart._state.queryMart.attributes)) {
                                _elements.navResults.removeClass('ui-state-disabled');
                            }
                        });
                    } else {
                        if (!$.isEmptyObject(biomart._state.queryMart.attributes)) {
                            _elements.navResults.removeClass('ui-state-disabled');
                        }
                        $.publish('biomart.loaded');
                    }
                },

                function() {
                	var colTypes = [];
                    for(var attr in biomart._state.queryMart.attributes){
                    	var item = biomart._state.queryMart.attributes[attr];
                    	colTypes.push(item.type);
                    }
                    
                    console.log(colTypes);
                    
                    _elements.contentWrapper.slideUp();
                    _elements.toolbar.slideUp();
                    _elements.results.resultsPanel('run', 
                        biomart.utils.hasGroupedMarts() ? biomart._state.mart[0].group : biomart._state.mart.displayName,
                        $.extend({
                            queries: getXml('TSVX', QUERY_LIMIT, true, QUERY_CLIENT),
                            downloadXml: getXml('TSV', -1, true, QUERY_CLIENT),
                            martObj: biomart._state.queryMart,
                            dataAggregation: 'none',
                            displayType: 'table',
                            colDataTypes: colTypes
                        }, QUERY_RESULTS_OPTIONS));
                    $.publish('biomart.loaded');
                }
            ];

            return function(index, callback) {
                fn[index]();
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

            if (!biomart._state.initialized) {
                initDimensions();
            }

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
        var h;

        h = _dimensions.viewportHeight = $(window).height() - 120;
        h -= _dimensions.header;
        h -= _dimensions.footer;
        h -= _dimensions.toolbar;
        h -= _dimensions.contentFooter;

        _elements.summary.height(h + 'px');
        _elements.main.height(h + 'px');

        //h -= _dimensions.nav;

        _elements.mainSteps.height(h + 'px');
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

    self.block = function() {
        _elements.content.block(BLOCK_OPTIONS);
    };

    self.unblock = function() {
        _elements.content.unblock();
    };

    // Goes to a step in the wizard (between 1 and 4)
    self.step = function(num) {
        if (num < 1 || num > 4) return;
        var index = num - 1,
            curr;
        _elements.steps.eq(biomart._state.activeStep-1).fadeOut({ duration: ANIMATION_TIME });
        curr = _elements.steps.eq(index).fadeIn();
        if (!curr.data('loaded')) self.block();
        biomart._state.activeStep = index + 1;
        _elements.stepsContainer.scrollTop(0);
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
        var item, value;
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
                            li = $(['<li class="', biomart.renderer.makeClassName(item.name), '"></li>'].join('')).appendTo(_elements.summaryFilters);
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

                        li
                            .html([
                                '<span class="key">', displayName, '</span>: ',
                                '<span class="value">', value, '</span>',
                                '<span class="ui-icon ui-icon-circle-close" title="Remove"/>'
                            ].join(''));
                    } else {
                        _elements.summaryFilters.children('li.' + biomart.renderer.makeClassName(item.name)).addClass('removing').fadeAndRemove(ANIMATION_TIME);
                    }
                }
                break;
            case 'attributes':
                item = arguments[1];

                _elements.summaryAttributes.children('li.empty').remove();

                if ($.isEmptyObject(biomart._state.queryMart.attributes)) {
                    _elements.summaryAttributes.empty();
                    $(['<li class="empty">', _('empty_value'), '</li>'].join(''))
                        .data('item', item)
                        .appendTo(_elements.summaryAttributes);
                } else {
                    if (arguments[2] /* add */) {
                        if (!_elements.summaryAttributes.children('li.' + biomart.renderer.makeClassName(item.name)).length) {
                            $(['<li class="', biomart.renderer.makeClassName(item.name), '">', item.displayName, 
                                    '<span class="ui-icon ui-icon-circle-close" title="Remove"/></li>'].join(''))
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
        biomart._state.selectedFilters = {};
        for (var k in biomart.params) {
            if ($.inArray(k, _nonFilterParams) == -1) {
                biomart._state.selectedFilters[k] = biomart.params[k];
            }
        }
        biomart._state.selectedAttributes = biomart.params.attributes ? biomart.params.attributes.split(',') : [];
    }

    function initElements() {
        // Cache elements for use later and set up any UI widgets
        _elements.header = $('#biomart-header');
        _elements.wrapper = $('#biomart-wrapper');
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
                        xml = getXml('TSV', limit, true, ''),
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
        $(window).bind('resize.martwizard', self.resize);
        self.resize();
    }

    function initNav() {
        _elements.nav.delegate('a', 'click.martwizard', function() {
            var $this = $(this),
                index;
            if (!$this.hasClass('ui-state-disabled')) {
                if ($this.hasClass('next')) index = biomart._state.activeStep;
                if ($this.hasClass('prev')) index = biomart._state.activeStep - 2;
                updateParam('step', index+1);
            }
            return false;
        });
        _elements.stepInfo.bind('click.martwizard', function() {
            var $this = $(this);
            if (!$this.hasClass('ui-state-disabled')) {
                updateParam('step', $this.attr('data-step-number')); 
            }
        });
        _elements.newButton.bind('click.martwizard', function() {
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
            hasSetFilters = !$.isEmptyObject(biomart._state.selectedFilters),
            hasSetAttributes = biomart._state.selectedAttributes.length;

        _elements.content.one('queue.done', callback);

        if (hasSetFilters) {
            queue.queue(function() {
                var params = {datasets: biomart._state.queryMart.datasets.join(',')};
                if (biomart._state.queryMart.config)
                    params.config = biomart._state.queryMart.config;
                biomart.resource.load('filters', function(json) {
                    for (var i=0, f, v; f=json[i]; i++) {
                        if (v = biomart._state.selectedFilters[f.name]) {
                            biomart._state.queryMart.filters[f.name] = { name: f.name, value: v };
                            $.publish('biomart.change', 'filters', f, v);
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
                        if (a.selected || $.inArray(a.name, biomart._state.selectedAttributes) != -1) {
                            biomart._state.queryMart.attributes = { name: a.name , type: a.dataType};
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
        _elements.steps.eq(0).delegate('select.mart', 'change.martwizard', function(ev) {
            updateMart($(this).children().eq(this.selectedIndex).data('mart'));
            self.refreshParamCache();
            self.reset();
        });

        // Dataset changed
        _elements.steps.eq(0).delegate('select.datasets', 'change.martwizard', function(ev) {
            updateDatasets();
        });

        // Filters changed
        _elements.steps.eq(1)
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
        _elements.steps.eq(2)
            .delegate('input.checkbox', 'click.martwizard', function(ev) {
                var $this = $(this),
                    $parent = $this.parent(),
                    item = $parent.data('item');
                if (this.checked) {
                    addQueryAttribute(item);
                    $parent.addClass('ui-active');
                    debugger;
                } else {
                    removeQueryAttribute(item);
                    $parent.removeClass('ui-active');
                    debugger;
                }
            });

        // URL fragment changed
        $(window).bind('hashchange', self.refreshParamCache);

        // View XML function
        _elements.viewXmlLink.bind('click.martwizard', function() {
            var limit = parseInt(_elements.exportLimit.val()) || -1,
                xml = getXml('TSV', limit, true, '');
            _elements.viewXml.children('textarea').val(xml);
            _elements.viewXml.data('escaped', false).dialog('open');
        });

        _elements.summary.delegate('span.ui-icon-circle-close', 'click.martwizard', function(ev) {
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

            _elements.steps.eq(1).data('loaded', false).empty();
            _elements.steps.eq(2).data('loaded', false).empty();

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
        biomart._state.queryMart.attributes[item.name] = {name: item.name, type: item.dataType};
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

    function loadFilters() {
        var params = {
            datasets: biomart._state.queryMart.datasets.join(','), 
            withfilters: true, 
            withattributes: false
        };

        if (biomart._state.queryMart.config) 
            params.config = biomart._state.queryMart.config;

        _elements.steps.eq(1).data('loaded', true);
        biomart.resource.load('containers', function(json) {
            for (var i=0, c; c=json.containers[i]; i++) {
                drawFilterContainer(_elements.steps.eq(1), c);
            }
            $.publish('biomart.loaded');
        }, params);
    }

    function drawFilterContainer(element, container, level) {
        biomart.renderer.container({
            tagName:'div', 
            item: container, 
            mode: biomart.renderer.FILTERS,
            selectedFilters: biomart._state.selectedFilters,
            onFilterSelect: function(item, value) {
                biomart._state.queryMart.filters[item.name] = {name:item.name, value:value};
                $.publish('biomart.change', 'filters', item, value);
            },
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
            var list = $('<ul/>').appendTo(_elements.steps.eq(2));
            for (var i=0, c; c=json.containers[i]; i++) {
                drawAttributeContainer(list, c);
            }
            if (callback) callback();
            $.publish('biomart.loaded');
        }, params);
    }

    function drawAttributeContainer(element, container, level, extraClassNames) {
        biomart.renderer.container({
            tagName:'div', 
            item: container, 
            mode: biomart.renderer.ATTRIBUTES,
            extraClassNames: extraClassNames || '',
            selectedAttributes: biomart._state.selectedAttributes,
            onAttributeSelect: function(item) {
                biomart._state.queryMart.attributes[item.name] = {name:item.name, type:item.dataType};
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

    function getXml(renderer, limit, client) {
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
            if (item)
                mart.attributes[item.name] = {name: item.name};
        });

        return biomart.query.compile('XML', mart, renderer, limit, true, client);
    }
});

/*
 * Bind callbacks to events
 *
 * The ordering here is very important!
 */
$.subscribe('biomart.init', biomart.martwizard, 'refreshParamCache');
$.subscribe('biomart.init', biomart.martwizard, 'init');
$.subscribe('biomart.loaded', biomart.martwizard, 'unblock');
$.subscribe('biomart.loaded', biomart.martwizard, 'updateStepInfo');
$.subscribe('biomart.change', biomart.martwizard, 'updateSummary');
$.subscribe('biomart.change', biomart.martwizard, 'invalidateResults');
$.subscribe('biomart.change', biomart.martwizard, 'updateNav');
$.subscribe('biomart.restart', biomart.martwizard, 'init');

})(jQuery);

biomart.error = function() {
    $('#biomart-main').html('<p class="error">An error has occurred. You can try refreshing this page or go back to the <a href="../">landing page</a>.');
    $('#biomart-content').unblock();
    $('#biomart-loading').fadeAndRemove();
};
