(function($) {
/* Minimizer
 * Apply this to a an element to have toggle show/hide for the .next() element
 */
$.widget('ui.minimizer', {
    options: {
          duration: 'fast',
          state: 'show'
    },

    _classNames: ['ui-icon-triangle-1-e', 'ui-icon-triangle-1-s'],

    _create: function() {
        var self = this,
            element = this.element,
            icon = this._icon = element.children('span.ui-icon'),
            text = this._text = element.children('span.text'),
            div = this._div = element.next();

        self._originalText = text.text();
        self._originalTitle = element.attr('title');

        element
            .attr('title', 'Click to expand')
            .bind('click.minimizer', function() {
                self.toggle.apply(self);
            });

        div.wrap('<div style="overflow: hidden"/>');
        div.data('minimizer', self);

        // if (this[this.options.state]) this[this.options.state]();
    },

    toggle: function() {
        if (this._icon.hasClass(this._classNames[0])) {
            this.show();
        } else {
            this.hide();
        }
    },

    hide: function() {
        var self = this;

        // Already closed
        if (self._icon.hasClass(self._classNames[0])) {
            return;
        }

        self._icon
            .addClass(self._classNames[0])
            .removeClass(self._classNames[1]);

        self._div.hide('drop', {
            direction: 'up',
            duration: self.options.duration,
            complete: function () {
                self.element.trigger('hide');
                if (self.options.close) self.options.close.apply(self.element);
            }
        });
    },

    show: function() {
        var self = this;

        // Already opened
        if (self._icon.hasClass(self._classNames[1])) {
            return;
        }

        self._icon
            .addClass(self._classNames[1])
            .removeClass(self._classNames[0]);

        self._div.show('drop', {
            direction: 'up',
            duration: self.options.duration,
            complete: function () {
                self.element.trigger('show');
                if (self.options.open) self.options.open.apply(self.element);
            }
        });
    },

    text: function(str) {
        this._text.text(str);
    },

    revert_text: function() {
          this._text.text(this._originalText);
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        this.element
            .attr('title', this._originalTitle)
            .unbind('click.minimizer');
        this._div
            .data('minimizer', null)
            .unwrap();
    }
});

$.fn.uploadify = function() {
    return this.each(function() {
        var input = $(this).hide(),
            upload = $(['<a href="#">', _('upload file'), '</a>'].join('')).insertAfter(input).addClass('uploader'),
            subgroup = input.closest('.subgroup'),
            container = subgroup.closest('.filter-container'),
            textarea = subgroup.children('textarea'),
            queue = new biomart.Queue(subgroup, 'upload');

        new AjaxUpload(upload, {
                action: BIOMART_CONFIG.service.url + 'file/wrap?callback=biomart.temp',
                autoSubmit: true,
                onSubmit : function(file, ext){
                    subgroup.block({
                        message: 'uploading ' + file + '&hellip;',
                        css: {
                            backgroundColor: 'transparent',
                            width: '100%',
                            color: '#fff',
                            border: 'none'
                        }
                    });
                },
                onComplete: function(file, response){
                    biomart.temp = function(text) {
                        textarea.val(text).trigger('change');
                    };
                    try {
                        eval(response);
                    } catch (e) {
                        textarea.text('Error parsing file');
                    }
                    subgroup.unblock();
                }
            }); // Upload
    });
};

/*
 * Prettybox
 *
 * Converts a select box into a combo box with autocomplete
 */
$.widget('ui.prettybox', {
    _create: function() {
        var self = this,
            options = self.options,
            select = this.element.hide(),
            multipleText = ['-- ', _('select', biomart.CAPITALIZE), ' --'].join(''),
            multiple = select.multiple,
            combo = $('<div class="ui-prettybox ui-state-default ui-corner-all clearfix"/>').insertAfter(select),
            input = $('<input/>').appendTo(combo),
            button = $('<a></a>').insertAfter(input),
            selected = $('<ul class="selected-items"/>').hide().insertAfter(combo),
            ac_menu;

        input
            .autocomplete({
                source: function(items, request, response) {
                    var matcher,
                        matched,
                        n = items.length;

                    if (!request.term) {
                        matched = items;
                    } else {
                        matcher  = new RegExp(request.term.replace(/\(/g,'\\(').replace(/\)/g,'\\)'), 'i')
                        matched = [];
                        for (var i=0; i<n; i++) {
                            var item = items[i];
                            if (matcher.test(item.text))
                                matched.push({
                                    id: item.id,
                                    label: item.text.replace(new RegExp('(?![^&;]+;)(?!<[^<>]*)(' + request.term.replace(/([\^\$\(\)\[\]\{\}\*\.\+\?\|\\])/gi, '\\$1') + ')(?![^<>]*>)(?![^&;]+;)', 'gi'), '<strong>$1</strong>'),
                                    value: item.text
                                });
                        }
                    }

                    response(matched);
                }.partial(select.children('option').map(function() {
                    return {
                        id: $(this).val(),
                        label: $(this).text(),
                        text: $(this).text()
                    };
                })),
                delay: 10,
                close: function(e, ui) {
                    var $this = $(this);
                    $this.data('justClosed', true);
                    setTimeout(function() {
                        $this.data('justClosed', false);
                    }, 500);
                },
                select: function(e, ui) {
                    var $this = $(this);
                    $this.data('justSelected', true);

                    // remove 'hover' class name on menu so we can close properly
                    $this.data('autocomplete').menu.element.removeClass('hover');

                    if (!ui.item) {
                        var old = $(this).data('old') || '';
                        $this.data('valid', false).val(old);
                        return false;
                    }
                    $this.data('old', null);
                    if (!multiple) {
                        $this.data('valid', true);
                        select.val(ui.item.id).trigger('change');
                    } else {
                        $this.val(multipleText);

                        $('<li class="' + ui.item.id + '">' + ui.item.value + '</li>')
                            .addClass('ui-corner-all ui-state-default')
                            .hover(function() { $(this).addClass('ui-state-hover') },
                                function() { $(this).removeClass('ui-state-hover') })
                            .appendTo(selected);

                        if (selected.is(':hidden')) selected.animate({opacity: 'show'});

                        return false; // don't select yet
                    }
                },
                minLength: 0
            })
            .bind('focus.prettybox', function() {
                var $this = $(this),
                    old = $this.val(),
                    valid = $this.data('valid'),
                    $ac_menu = $this.data('autocomplete').menu.element;

                if (valid && old) {
                    $this.data('old', old);
                }

                $this
                    .data('valid', false)
                    .data('justSelected', false)
                    .addClass('ui-state-focus')
                    .val('');

                if (!$ac_menu.is(':visible')) {
                    $this.autocomplete('search', '');
                }
            })
            .unbind('blur.autocomplete')
            .bind('blur.autocomplete', function() {
                var $this = $(this),
                    $ac_menu = $this.data('autocomplete').menu.element,
                    old = $this.data('old');
                $this.removeClass('ui-state-focus');

                if (!$ac_menu.hasClass('hover')) {
                    $this.autocomplete('close');
                }

                if (old)
                    $this.val(old);
            })
            .addClass('ui-state-default ui-widget ui-corner-left'); // <input/>

        button
            .button({
                icons: {
                    primary: 'ui-icon-triangle-1-s'
                },
                text: false
            }).removeClass('ui-corner-all')
            .addClass('ui-corner-right ui-button-icon')
            .click(function() {
                var input = $(this).siblings('input'),
                    shown = input.data('autocomplete').menu.element.is(':visible');
                // close if already visible
                if (input.data('justClosed')) {
                    return;
                }
                // pass empty string as value to search for, displaying all results
                input.focus();
            }); // <button/>

        ac_menu = input.data('autocomplete').menu.element
            .bind('mouseover.prettybox', function(e) {
                $(this).addClass('hover');
                if (e.target.tagName != 'UL' ) return false;
            })
            .bind('mouseout.prettybox', function(me, e) {
                if (!me.data('justSelected')) {
                    me.data('autocomplete').menu.element.removeClass('hover');
                    if (e.target.tagName != 'UL' ) return false;
                    me.data('valid', false).focus();
                }
            }.partial(input))
            .addClass(select.attr('id'));

        $('.ui-menu-item', ac_menu)
            .live('mouseover', function(menu, ev) {
                menu.addClass('hover');
            }.partial(ac_menu))
            .addClass(select.attr('id'));

        if (!multiple) {
            input.val(select.children().eq(select[0].selectedIndex).text()).data('valid', true);
        } else {
            input.val(multipleText);
        }

        self.element.addClass('ui-prettybox');
    },

    destroy: function() {
        var self = this,
            select = this.element,
            element = select.next('.ui-prettybox'),
            input = element.find('input.ui-autocomplete-input')
                    .unbind('focus.prettybox')
                    .unbind('blur.autocomplete');

        $.Widget.prototype.destroy.apply(this, arguments);

        element.find('a.ui-button').button('destroy').remove();
        element.find('div.ui-prettybox').remove();
        element.find('ul.selected-items').remove();

        ac_menu = input.data('autocomplete').menu.element;

        ac_menu.unbind('mouseover.prettybox');
        ac_menu.unbind('mouseout.prettybox');

        $('.ui-menu-item', ac_menu).die('mouseover');

        input.autocomplete('destroy').remove();
        element.remove();
        select.show();
    }
}); // prettybox

/* Simpler filters and attributes */
$.widget('ui.simplerfilter', {
    options: {
        chooseText: true
    },

    _create: function() {
        var self = this,
            options = self.options,
            element = self.element.addClass('simplerfilter'),
            div = $('<div class="remove"/>').appendTo(element),
            type = element.attr('filter-type'),
            item = element.data('item'),
            name = item.name;

        self._filterName = name;

        self._validateDefaultValue(item);

        // Add an invalid "Choose" option to select boxes
        var select = element.find('select:not([multiple])');
        if (options.chooseText) {
        	if(type != 'singleSelectUpload')
        		select.prepend(['<option value="">-- ', _('select', biomart.CAPITALIZE), ' --</option>'].join('')).val(options.defaultValue || '');
        }
        select.prettybox();

        var label = element.find('.item-name');
        label.append(':').bind('click.simplerfilter', function() {
                // prevent checkbox from being checked/unchecked
                return false;
            });

        element.find('.field.text')
            .val(options.defaultValue || '')
            .addClass('ui-state-default ui-corner-all')
            .bind('focus.simplerfilter', function() {$(this).select();});

        element.find('.upload').uploadify();

        $(['<span class="ui-icon ui-icon-circle-close" title="', _('remove', biomart.CAPITALIZE), '"></span>'].join(''))
            .bind('click.simplerfilter', function() {
                 $(this)
                     .data('value', null)
                     .trigger('removefilter', [item]);
                 $.publish('filterchange.' + name, name, null);
            })
            .appendTo(div);

        element
            .delegate([
                '.field:input[type=checkbox]',
                'select.filter-list[type=checkbox]',
                '.field:input[type=radio]',
                'select'
                ].join(','), 'click.simplerfilter', function() {
                    var $this = $(this),
                        value = $this.val();
                    if (value) {
                        $this
                            .data('value', value)
                            .trigger('addfilter', [item, biomart.validator.filter(element)]);
                        $.publish('filterchange.' + name, name, value);
                    } else {
                        $this
                            .data('value', null)
                            .trigger('removefilter', [item]);
                        $.publish('filterchange.' + name, name, null);
                    }
                })
            .delegate([
                '.field:input:not([type=checkbox],[type=radio])',
                'select'
                ].join(','), 'change.simplerfilter', function() {
                    var $this = $(this),
                        value = $this.val();
                    if (value) {
                        $this
                            .data('value', value)
                            .trigger('addfilter', [item, biomart.validator.filter(element)]);
                        $.publish('filterchange.' + name, name, value);
                    } else {
                        $this
                            .data('value', null)
                            .trigger('removefilter', [item]);
                        $.publish('filterchange.' + name, name, null);
                    }
                });


        if (self.options.defaultValue) {
             self.value(self.options.defaultValue);
             element.data('value', self.options.defaultValue);
        }

        var depends, other, oVal;
        if (depends = element.attr('data-depends')) {
            self._depends = depends;
            self._defaults = item.values;
            other = $('.filter-container.' + biomart.renderer.makeClassName(depends));
            oVal = biomart.validator.filter(other);
            if (oVal) {
                self.getOptions(other.attr('filter-name'), oVal);
            }
            $.subscribe(['filterchange', depends, self._name].join('.'), self, 'getOptions');
        }
    },

    _validateDefaultValue: function(item) {
        var dval = this.options.defaultValue;
        // Validate defaultValue
        if (!dval) return;

        if (item.values.length) {
            for (var i=0, value; value=item.values[i]; i++) {
                if (dval == value.name) {
                    break;
                }
            }
            if (i == item.values.length) {
                delete this.options.defaultValue;
            }
        }
    },

    getOptions: function(filter, value) {
        var self = this,
            element = self.element,
            select = element.find('select'),
            mart = biomart._state.mart || biomart._state.queryMart,
            datasets = biomart._state.datasets || biomart._state.queryMart.datasets,
            arr = [];

        if ($.isArray(mart)) {
            mart = mart[0].config;
        } else if (typeof mart == 'object') {
            mart = mart.config;
        }

        if ($.isArray(datasets)) {
            datasets = datasets.join(',');
        }

        // Convert array to comma-separated string
        if ($.isArray(value)) {
            value = value.join(',');
        }

        if (value) {
            biomart.resource.load('filter_values', function(data) {
                for (var i=0, item; item=data[i]; i++) {
                    arr.push(['<option value="', item.name, '">', item.displayName, '</option>'].join(''));
                }

                select.html(arr.join(''));

                if (!select[0].multiple) {
                    select
                        .prettybox('destroy')
                        .prepend(['<option value="">-- ', _('select', biomart.CAPITALIZE), ' --</option>'].join(''))
                        .val(self.options.defaultValue)
                        .prettybox();
                }

                select.val('').trigger('change');

            }, {datasets: datasets, config: mart, filter: self._filterName, value: value});
        } else if (self._defaults && self._defaults.length) {
            for (var i=0, item; item=self._defaults[i]; i++) {
                arr.push(['<option value="', item.name, '">', item.displayName, '</option>'].join(''));
            }

            select.html(arr.join(''));

            if (!select[0].multiple) {
                select
                    .prettybox('destroy')
                    .prepend(['<option value="">-- ', _('select', biomart.CAPITALIZE), ' --</option>'].join(''))
                    .val(self.options.defaultValue || '')
                    .prettybox();
            }

            select.val('').trigger('change');
        }
    },

    value: function(val) {
          var self = this,
            element = self.element;

        if ($.isArray(val)) {
            var select = element.find('select'),
                name = val[0],
                value = val[1],
                option = select.children('option[value=' + name + ']');

            if (option.length) {
                select.val(name);
                select.siblings('.ui-prettybox').children('.ui-autocomplete-input').val(option.text());
                element.find('.field')
                    .filter('textarea,:text').val(value).end()
                    .filter('[type=radio]').filter('[value=' + value + ']').attr('checked', true).end();
            }
        } else {
            self.element.find('input:text').val(val);
        }
    },

    refresh: function() {
        this.element.find('select:not([multiple])').each(function() {
            var $this = $(this),
                $box = $this.siblings('.ui-prettybox')
                value = $this.children('option:selected').text() || ['-- ', _('select', biomart.CAPITALIZE), ' --'].join('');
            $box.children('.ui-autocomplete-input').val(value);
        });
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);

        this.element.find('.item-name')
            .unbind('click.simplerfilter');

        this.element
            .unbind('click.simplerfilter')
            .unbind('focus.simplerfilter')
            .removeClass('simplerfilter');
    }
});

$.widget('ui.simplerattribute', {
    options:  {
        radio: true
    },
    _create: function() {
        var self = this,
            element = self.element.addClass('simplerattribute'),
            name = element.data('item').name;

        if (self.options.radio) {
            var checkbox = element.find('input.checkbox'),
                radio = $('<input type="radio" name="attribute"/>')
                    .attr('id', checkbox.attr('id'))
                    .addClass(checkbox.get(0).className);
            if (checkbox[0].checked) radio.attr('checked', true);

            checkbox.remove();
            radio.insertBefore(element.find('label.item-name'));
        }

        element.delegate('input[type=checkbox],input[type=radio]', 'click.simplerattribute', function() {
            if (this.checked) {
                $(this).trigger('addattribute');
            } else {
                $(this).trigger('removeattribute');
            }
        });
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);

        this.element.removeClass('simplerattribute');
    }
});

// Needs datacontroller widget to be called on element
$.widget('ui.paginate', {
    options: {
        pageSize: 10,
        animationTime: 200,
        infoElement: false
    },

    _create: function() {
        var self = this,
            options = self.options,
            element = self.element;

        self._skip = 0;

        if (!options.total) { alert('total needs to be set to for pagination!'); return }

        self._width = element.width();

        self._paginator = $('<p class="paginator"/>')
            .insertAfter(element)
            .disableSelection()
            .bind('click.paginate', function(ev) {
                var $origin = $(ev.target);
                if ($origin.hasClass('prev')) {
                    self.previous();
                } else if ($origin.hasClass('next')) {
                    self.next();
                } else if ($origin.hasClass('page')) {
                    var i = parseInt($origin.text());
                    self.page(i);
                }
                return false;
            });

        // Previous
        $('<a href="#"/>')
            .addClass('prev disabled')
            .html(['&laquo; ', _('previous', biomart.CAPITALIZE)].join(''))
            .appendTo(self._paginator);

        // Pages
        self._totalPages = Math.ceil(options.total/options.pageSize);
        for (var i=0; i++<10; ) {
            if (i > self._totalPages) break;
            $('<a href="#"/>')
                .text(i)
                .addClass(i==1 ? 'ui-active page' : 'page')
                .appendTo(self._paginator);
        }

        // Next
        $('<a href="#"/>')
            .addClass('next')
            .html([_('next', biomart.CAPITALIZE), ' &raquo;'].join(''))
            .appendTo(self._paginator);

        if (options.infoElement) {
            var html = [];

            html.push([
                '<span>', _('displaying results', biomart.CAPITALIZE), ' <strong class="start">', options.start,
                '</strong>-<strong class="end">', options.end, '</strong></span>'
            ].join(''));

            html.push([ ' ', _('out_of'), ' <strong>', options.total, '</strong>' ].join(''));

            options.infoElement.empty().append(html.join(''));
        }

        self._parent = self.element.parent();
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        this._paginator.unbind('click.paginate').remove();
        $.unsubscribeAll(['filterchange', self._depends, self._name].join('.'));
    },

    next: function() {
        this._scrollHide(true, function(self) {
            self._skip += self.options.pageSize;
            self._updateNav.apply(self,
                self.element.datacontroller('paginate', self._skip, self.options.pageSize)
            );
            self._scrollShow();
        }.partial(this));
    },

    previous: function() {
        this._scrollHide(false, function(self) {
            self._skip -= self.options.pageSize;
            self._updateNav.apply(self,
                self.element.datacontroller('paginate', self._skip, self.options.pageSize)
            );
            self._scrollShow();
        }.partial(this));
    },

    page: function(i, force) {
        var skip = (i-1) * this.options.pageSize;
        if (!force && skip == this._skip) return;

        if (skip == this._skip) {
            this.element.datacontroller('paginate', this._skip, this.options.pageSize);
        } else {
            this._scrollHide(skip > this._skip, function(self) {
                self._skip = skip;
                self._updateNav.apply(self,
                    self.element.datacontroller('paginate', self._skip, self.options.pageSize)
                );
                self._scrollShow();
            }.partial(this));
        }
    },

    _scrollHide: function(left, callback) {
        this._width = this.element.width();
        this.element
            .css('width', this._width + 'px')
            .animate({marginLeft: (left ? -1 : 1) * (50+this._width) + 'px'}, {
                duration: this.options.animationTime,
                complete: function(self) {
                    self.element.css({
                        'margin-left': (left ? 1 : -1) * (50+self._width) + 'px'
                    });
                    if (callback) callback();
                }.partial(this)
            });
    },

    _scrollShow: function(callback) {
        var self = this;
        this.element
            .animate({'margin-left': 0}, {
                duration: this.options.animationTime,
                complete: function() {
                    self.element.css('width', 'auto');
                    self.element.parent().css('overflow', '');
                    callback || function(){}
                }
            });
    },

    _updateNav: function(total, start, end) {
        var self = this,
            $prev = self._paginator.find('a.prev'),
            $next = self._paginator.find('a.next'),
            html = [],
            page = Math.ceil(start/self.options.pageSize),
            pageOffset = 4,
            pageStart = Math.max(page-pageOffset, 1)
            pageEnd = pageStart + 9;

        if (pageEnd > self._totalPages) {
            pageEnd = self._totalPages;
             pageStart = Math.max(1, pageEnd - 9);
        }

        self._paginator.children('.page').remove();

        for (var i=pageStart; i<=pageEnd; i++ ) {
             $('<a href="#"/>')
                .text(i)
                .addClass(i==page ? 'ui-active page' : 'page')
                .insertBefore($next);
        }

        if (start > 1)
            $prev.removeClass('disabled');
        else
            $prev.addClass('disabled');

        if (end < total)
            $next.removeClass('disabled');
        else
            $next.addClass('disabled');

        if (self.options.infoElement) {
            self.options.infoElement.find('strong.start').text(start);
            self.options.infoElement.find('strong.end').text(end);
        }

        if (self.options.change) self.options.change(page);
    }
});

$.widget('ui.singleselect', {
    options: {
    },

    _create: function() {
        var self = this,
            element = self.element,
            options = self.options;
        element
            .children()
                .addClass('ui-selectee')
            .end()
            .addClass('ui-singleselect')
            .delegate('.ui-selectee', 'click.singleselect', function(ev) {
                var $this = $(this);
                $this.addClass('ui-selected').siblings().removeClass('ui-selected');
                if (options.selected) options.selected.apply(element);
            });
        this.element.disableSelection();
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        this.element.removeClass('ui-singleselect').enableSelection();
    }
});

$.widget('ui.singlecontainer', {
    options: {
        activeIndex: 0
    },

    _create: function() {
        var self = this,
            element = self.element.addClass('ui-singlecontainer'),
            children = element.children(),
            items = children.slice(1),
            first = items.eq(self.options.activeIndex);

        self._wrap = $(['<div id="', biomart.uuid(), '" class="ui-singlecontainer-wrap"/>'].join(''))
            .appendTo(element);
        self._menu = $('<ul class="menu"/>').appendTo(self._wrap);
        self._items = items;

        items.each(function(i, e) {
            var $e = $(e).addClass('ui-singlecontainer-item').appendTo(self._wrap),
                item = $e.data('item');
            $([
                '<li>',
                    '<a href="#', e.id, '" title="', item.description || item.displayName, '">',
                        item.displayName,
                    '</a>',
                '</li>'
            ].join('')).appendTo(self._menu);

            $e.children('.ui-widget-header').hide();
        });

        self._wrap
            .tabs({
                selected: self.options.activeIndex
            })
            .bind('tabsshow', function(ev, ui) {
                var panel = $(ui.panel);
                self.element.trigger('containershow', [panel]);
                self.element.trigger('containerhide', [self._prevpanel]);
                self._prevpanel = panel;
            });

        self._prevpanel = self._wrap.children('.ui-tabs-panel').eq(self.options.activeIndex);
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        var self = this;
        self._wrap.tabs('destoy');
        self._items.each(function(i, e) {
            var $e = $(e).removeClass('ui-singlecontainer-item');
            $e.children('.ui-widget-header').show();
            $e.appendTo(self.element);
        });
        self._list.appendTo(self.element);
        self._wrap.remove();
    }
});

$.widget('ui.multiselect', {
    options: {
        minSelected: 0
    },

    _create: function() {
        var self = this,
            element = self.element,
            options = self.options;
        element
            .children()
                .addClass('ui-selectee')
            .end()
            .addClass('ui-multiselect')
            .delegate('.ui-selectee', 'click.multiselect', function(ev) {
                var $this = $(this);
                if ($this.hasClass('ui-selected')) {
                    var num = element.children('.ui-selected').length;
                    if (num > options.minSelected) {
                        $this.removeClass('ui-selected');
                    }
                } else {
                    $this.addClass('ui-selected');
                }
                if (options.selected) options.selected.apply(element);
            });
        this.element.disableSelection();
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        this.element.removeClass('ui-multiselect').enableSelection();
    }
});

$.widget('ui.resultsPanel', {
    options: {
        wrapperId: 'biomart-top-wrapper'
    },

    _create: function() {
        var self = this,
            element = self.element.addClass('ui-resultsPanel'),
            options = self.options;

        self._wrapper = $('#' + options.wrapperId);
        self._actions = element.children('.actions');
        self._data = element.find('.data');
        self._info = element.find('.info');
        self._edit = element.find('.edit')
            .bind('click.resultsPanel', function() {
                element.trigger('edit');
                return false;
            });
        self._content = element.find('.content');
        self._exportForm = $('#biomart-export-form');
        self._export = element.find('.export')
            .bind('click.resultsPanel', function() {
                element.trigger('download');
                self._exportForm.children('input[name=query]').val(self._downloadXml);
                biomart.notify('Your download will start soon', {header: 'Export Results'});
                self._exportForm.submit();
                return false;
            });
        // Support for Galaxy export function
        self._galaxy = element.find('.galaxy')
            .hide()
            .bind('click', function() {
                var url = self._galaxyUrl;
                if (url) {
                    var $form = $('<form style="height: 1; visibility: hidden" action="' + url + '">').appendTo(document.body);
                    // Replace invalid Galaxy name character
                    var title = self._title.replace('&raquo;', '-');
                    $.cookie('GALAXY_URL', null, { path: '/' });
                    $form
                        .append( $('<input type="hidden" name="URL" value="' + location.protocol + '//' + location.host + BIOMART_CONFIG.service.url + 'results/?query=' +
                                encodeURIComponent(self._downloadXml) + '"/>') )
                        .append( $('<input type="hidden" name="name" value="' + title + '"/>') )
                        .append( $('<input type="hidden" name="type" value="text"/>') )
                        .submit();
                } else {
                    alert('Galaxy URL not found. Try restarting again from the Galaxy server.');
                }
                return false;
            });
        if (self._galaxyUrl = $.cookie('GALAXY_URL')) {
            self._galaxy.show();
        }
        self._explain = element
            .delegate('.explain', 'click.resultsPanel', function() {
                self._xmlViewer
                    .find('textarea').text(self._downloadXml)
                    .end()
                    .dialog('open');
                return false;
            })
            .delegate('.explain-sparql', 'click.resultsPanel', function() {
                var martObj = self._martObj;
                // Default to first mart found since we don't support more than one mart objects for SPARQL yet
                if (!martObj) {
                    martObj = biomart._state.queryMart;
                }
                self._sparqlViewer
                    .find('textarea').text(biomart.query.compile('SPARQL', martObj)).end()
                    .dialog('open');
                return false;
            })
            .delegate('.explain-java', 'click.resultsPanel', function() {
                var martObj = self._martObj;
                if (!martObj) {
                    martObj = biomart._state.queryMart;
                }
                self._javaViewer
                    .find('textarea').text(biomart.query.compile('Java', martObj)).end()
                    .dialog('open');
                return false;
            })
            .delegate('.bookmark', 'click.resultsPanel', function() {
                self._bookmarkViewer
                    .find(':text').val(location.href).end()
                    .dialog('open');
                return false;
            });

        self._xmlViewer = $('#biomart-view-xml').dialog({
            autoOpen: false,
            width: 800,
            height: 500,
            modal: true,
            show: 'fast',
            hide: 'fast',
            buttons: {
                'Close': function() { $(this).dialog('close') },
                'Toggle quote-escape': function() {
                    var xml = self._downloadXml,
                        $this = $(this);
                    if (!$this.data('escaped')) {
                        xml = xml.replace(/"/g, '\\"');
                        $this.data('escaped', true);
                    } else {
                        $this.data('escaped', false);
                    }
                    self._xmlViewer.children('textarea').text(xml);
                }
            }
        });

        self._sparqlViewer = $('#biomart-view-sparql').dialog({
            autoOpen: false,
            width: 800,
            height: 500,
            modal: true,
            show: 'fast',
            hide: 'fast',
            buttons: {
                'Close': function() { $(this).dialog('close') }
            }
        });

        self._javaViewer = $('#biomart-view-java').dialog({
            autoOpen: false,
            width: 800,
            height: 500,
            modal: true,
            show: 'fast',
            hide: 'fast',
            buttons: {
                'Close': function() { $(this).dialog('close') }
            }
        });

        self._bookmarkViewer = $('#biomart-view-bookmark').dialog({
            autoOpen: false,
            open: function() {
                $(this).find(':text').focus();
            },
            width: 500,
            height: 125,
            modal: true,
            show: 'fast',
            hide: 'fast',
            resizable: false,
            buttons: {
                'Close': function() { $(this).dialog('close') }
            }
        }).find(':text').bind('focus.resultsPanel', function() {
            $(this).select();
        }).end();

        // self._countEstimate = $('#biomart-count-estimate').tipsy({gravity: 's'})
    },

    downloadXml: function() {
        return this._downloadXml;
     },

    run: function(title, options) {
        var self = this;
        self._title = title;
        self._content.removeClass('hidden');
        self._wrapper.addClass('wide');
        self._content.slideDown()
        self.element.trigger('show');
        self.element.find('h3').html(title);

        self._info.hide();

        self._downloadXml = options.downloadXml;
        self._martObj = options.martObj;

        // self._updateCounts(options.downloadXml);

        self._data.queryResults($.extend({
            animationTime: 50,
            header: true,
            footer: true,
            iframe: self.element.find('iframe.streaming')
        }, options));
    },

    edit: function() {
        var self = this;
        self._content.addClass('hidden');
        self._data.queryResults('destroy').empty();
        self._wrapper.removeClass('wide', 50, function() {
            self._content.slideUp({
                duration: 50,
                complete: function() {
                    self.element.trigger('hide');
                }
            });
        });
        // self._countEstimate
        //     .find('.biomart-count-filtered')
        //         .text('-')
        //     .end()
        //     .find('.biomart-count-total')
        //         .text('-');
    },

    explain: function() {
    },

    destroy: function() {
        $.Widget.prototype.destroy.apply(this, arguments);
        this.element.removeClass('ui-resultsPanel');
    }

    // _updateCounts: function(xml) {
    //     var that = this;

    //     $.ajax({
    //         url: BIOMART_CONFIG.service.url + 'results/count.json',
    //         type: 'GET',
    //         data: {
    //             query: xml
    //         },
    //         success: function(json) {
    //             that._countEstimate.find('.biomart-count-filtered').text(json.entries);
    //         },
    //         error: function(json) {
    //             that._countEstimate.find('.biomart-count-filtered').text('-');
    //         }
    //     });

    //     var noFiltersXml = xml.replace( (/<Filter.*?>/), '');

    //     $.ajax({
    //         url: BIOMART_CONFIG.service.url + 'results/count.json',
    //         type: 'GET',
    //         data: {
    //             query: noFiltersXml
    //         },
    //         success: function(json) {
    //             that._countEstimate.find('.biomart-count-total').text(json.entries);
    //         },
    //         error: function(json) {
    //             that._countEstimate.find('.biomart-count-total').text('-');
    //         }
    //     });
    // }
});

})(jQuery);
