/*
 * Renders for mart config objects and results
 */
(function($) {
$.namespace('biomart.renderer', function(self) {
    /*
     * Config objects
     *
     * The types of filters here should match the ones in validator.js
     */

    var CLASS_NAME_REGEX = /[^a-zA-Z0-9_-]/g; // for function makeClassName

    // Whether to return invalid items
    // If set to true, invalid items will return as false
    self.renderInvalid = false;

    // Strips out any invalid character for class names
    self.makeClassName = function(s) {
        return s.replace(CLASS_NAME_REGEX, '');
    };

    // Generate unique ID for filters and attributes
    self.filter = (function() {
        var renderers = {
            'text': function(item, value) {
                var s = self.makeClassName(item.name);
                return ['<div class="value group clearfix">',
                        '<input class="field text" type="text" name="', s, '" value="',
                        value || '', '"/>',
                    '</div>'].join('');
            },

            'boolean': function(item, value) {
                var s = self.makeClassName(item.name);
                return ['<div class="value group clearfix">',
                        '<input class="field" value="only" type="radio" ',
                                value=='only' ? 'checked' : '', ' id="', 
                                s, '_only" name="', s, '"/>',
                        '<label for="', s, '_only">Only</label>',
                        '<input class="field" value="excluded" type="radio" ',
                                value=='excluded' ? 'checked' : '', ' id="', 
                                s, '_excluded" name="', s, '"/>',
                        '<label for="', s, '_excluded">Excluded</label>',
                    '</div>'].join('');
            },

            'upload': function(item, value) {
                return ['<div class="value group clearfix">', 
                        '<span class="subgroup upload-subgroup">',
                            '<textarea class="list field upload-field">', value || '', '</textarea>',
                            '<input type="file" class="upload"/>',
                        '</span>',
                    '</div>'].join('');
            },

            'singleSelectBoolean': function(item, value) {
                var html = [],
                    s = self.makeClassName(item.name),
                    select = createDropdown(item, false, value ? value[0] : null, item.filters, 'filter-list');

                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);

                return ['<div class="value group clearfix', (!select[1] ? ' invalid' : ''), '">', 
                        html.join(''),
                    '</div>'].join('');
            },

            'singleSelect': function(item, value) {
                var html = [],
                    select = createDropdown(item, false, value ? value[1] : null, item.values, 'field');
                
                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);

                return ['<div class="value group clearfix', (!select[1] ? ' invalid' : ''), '">', 
                    html.join(''), '</div>'].join('');

            },

            'singleSelectUpload': function(item, value) {
                var html = [],
                    select = createDropdown(item, false, value ? value[0] : null, item.filters, 'filter-list');

                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);
                
                return ['<div class="value group clearfix', (!select[1] ? ' invalid' : ''), '">', 
                        html.join(''),
                        '<span class="subgroup upload-subgroup">',
                            '<textarea class="list field upload-field">', value ? value[1] : '', '</textarea>',
                            '<input type="file" class="upload"/>',
                        '</span>',
                    '</div>'].join('');
            },

            'multiSelect' : function(item, value) {
                var html = [],
                    select = createDropdown(item, true, value ? value.split(',') : [], item.values, 'field');

                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);

                return '<div class="value group clearfix' + (!select[1] ? ' invalid' : '') + '">' + html.join('') + '</div>';
            },

            'multiSelectBoolean' : function(item, value) {
                var html = [],
                    s = self.makeClassName(item.name),
                    select = createDropdown(item, true, value ? value[0] : null, item.filters, 'filter-list');

                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);

                return ['<div class="value group clearfix', (!select[1] ? ' invalid' : ''), '">', 
                        html.join(''),
                    '</div>'].join('');
            },

            'multiSelectUpload': function(item, value) {
                var html = [],
                    select = createDropdown(item, true, value ? value[0] : null, item.filters, 'filter-list');

                if (!select[1] && !biomart.renderer.renderInvalid) return false;

                html.push(select[0]);

                return ['<div class="value group clearfix', (!select[1] ? ' invalid' : ''), '">', 
                        html.join(''),
                        '<span class="subgroup upload-subgroup">',
                            '<textarea class="list field upload-field">', value ? value[1] : '', '</textarea>',
                            '<input type="file" class="upload"/>',
                        '</span>',
                    '</div>'].join('');
            }



        };

        function createDropdown(filter, multiple, value, list, className) {
            var items, html = [], valid = true;

            if (list && list.length) {
                items = list;
            } else if (!biomart.renderer.renderInvalid) {
                 return false; // invalid
            } else {
                items = [];
                valid = false;
            }
            
            html.push(['<select class="', className, '"',
                multiple ? ' multiple="true"' : '', '>'
            ].join('')); 
            for (var i=0, item, selected; item=items[i]; i++) {
                if (multiple) {
                    selected = value ? $.inArray(item.name, value) != -1 : !!item.isSelected;
                } else {
                    selected = value ? value == item.name : !!item.isSelected;
                }
                html.push('<option value="' + item.name + '" '  +
                        (selected ? 'selected' : '') + '>' + item.displayName + '</option>');
            }
            html.push('</select>');

            return [html.join(''), valid];
        }

        return function(tagName, item, value, extraAttr) {
            var type = item.type || 'text',
                id = biomart.uuid(),
                fn = renderers[type] || renderers['text'],
                value,
                html = [],
                formatted = fn(item, value),
                s = self.makeClassName(item.name);

            if (!formatted) return '';

            html.push([
                '<', tagName, ' class="clearfix item filter-container ', s, ' ', type,
                (value ? ' ui-active' : ''), (item.required ? ' ui-required' : ''),
                '" container="', item['parent'], 
                '" filter-type="', type, '"', 'filter-name="', s, '"',
                item.dependsOn ? (' data-depends="' +  item.dependsOn + '"') : ''
            ].join(''));

            if (extraAttr) {
                 for (var k in extraAttr) {
                     if (extraAttr.hasOwnProperty(k)) {
                        html.push(' ' + k + '="' + extraAttr[k] + '"');
                    }
                }
            }

            html.push('>');

            html.push('<input type="checkbox" class="checkbox ' + s + '"' +
                    (value ? ' checked="checked"' : '') + ' id="' + id + '"/>' +
                    '<label for="' + id + '" class="item-name">' + item.displayName + 
                    (item.required ? ' (' + _('required')  + ')' : "") + '</label> ');
            html.push(formatted);
            html.push('</' + tagName + '>');

            return $(html.join('')).data('item', item);
        }
    })();

    self.attribute = function(tagName, item, checked, extraAttr) {
        var type = item.type || 'text',
            id = biomart.uuid(),
            html = [],
            s = self.makeClassName(item.name);

        if (item.selected) {
            checked = true;
        }

        html.push('<' + tagName + ' class="clearfix item attribute-container ' + s +  
                (checked ? ' ui-active' : '') + '" container="' + item['parent'] + 
                '" attribute-name="' + s + '"');

        if (extraAttr) {
            for (var k in extraAttr) {
                if (extraAttr.hasOwnProperty(k)) {
                    html.push(' ' + k + '="' + extraAttr[k] + '"');
                }
            }
        }

        html.push('>');

        html.push('<input type="checkbox" class="checkbox ' + s + 
                '" name="' + s+ '" ' +
                (checked ? 'checked="checked"' : '') + ' id="' + id + '"/>' +
                 '<label for="' + id + '" class="item-name">');
        html.push(item.displayName);
        html.push('</label>' + '</' + tagName + '>');

        return $(html.join('')).data('item', item);
    };

    self.NONE = 0;
    self.FILTERS = 1;
    self.ATTRIBUTES = 2;
    self.BOTH = 3;

    // mode is a flag for bitwise AND
    self.container = function(o) {
        var tagName = o.tagName || 'div',
            headerTagName = o.headerTagName || 'h3',
            headerClassName = o.headerClassName || 'ui-widget-header',
            item = o.item,
            mode = o.mode || 0,
            level = o.level || 1,
            id = biomart.uuid(),
            element = $(['<', tagName, ' id="', id, '" class="container level-', level, ' ', 
                self.makeClassName(item.name), ' gradient-grey-reverse"/>'].join(''));
        
        if (!item) {
            alert('Container object cannot be null');
            return;
        }

        if (o.extraClassNames) element.addClass(o.extraClassNames);

        element.data('item', item);

        $(['<', headerTagName, ' class="', headerClassName, '">',
                item.displayName,
            '</', headerTagName, '>',].join('')).appendTo(element);
        

        if (o.appendTo) element.appendTo(o.appendTo);
        
        // Draw filters
        if (mode & self.FILTERS) {
            if (item.filters.length) {
                list = $('<ul class="items filters clearfix"/>').appendTo(element);

                for (var i=0, f, formatted, checked, value; f=item.filters[i]; i++) {
                    checked = o.selectedFilters && o.selectedFilters[f.name];
                    if (checked) {
                        value = o.selectedFilters ? o.selectedFilters[f.name].value || o.selectedFilters[f.name] : biomart.params[f.name];
                        if (!biomart.utils.filterValueExists(f, value)) {
                            value = null;
                        }
                    } else {
                        value = null;
                    }
                    if (formatted = $(biomart.renderer.filter('li', f, value))) {
                        formatted
                            .appendTo(list)
                            .simplerfilter({defaultValue: value});
                        if (formatted.children('div.invalid').length) {
                            biomart.disableFilter(formatted);
                        }
                    }
                    if (value && o.onFilterSelect) o.onFilterSelect(f, value);
                }
            }
        }
        
        if (mode & self.ATTRIBUTES) {
            // Draw attributes
            if (item.attributes.length) {
            	element.delegate('a.select-all', 'click', function(ev) {
            		// check all the checkbox under the container
            		$(ev.target)
            			.closest('.container')
            			.find('.attribute-container input:checkbox')
            			.each(function() {
            				var $attributeContainer = $(this).closest('.attribute-container');
            				this.checked = true;
                			$(this).trigger('click.martwizard');
                			this.checked = true;
                			$attributeContainer.addClass('ui-active');
                			$attributeContainer.trigger('addattribute');
            			});
            		
            	});
            	element.delegate('a.unselect-all', 'click', function(ev) {
            		// check all the checkbox under the container
            		$(ev.target)
            			.closest('.container')
            			.find('.attribute-container input:checkbox')
            			.each(function() {
            				var $attributeContainer = $(this).closest('.attribute-container');
            				this.checked = false;
                			$(this).trigger('click.martwizard');
                			this.checked = false;
                			$attributeContainer.removeClass('ui-active');
                			$attributeContainer.trigger('removeattribute');
            			});
            		
            	});
            	// add select all and unselect all links to Container (Attributes only) 
            	$('<div align="right" class="clearfix"><a class="select-all" href="javascript:;">select all</a> | <a href="javascript:;" class="unselect-all">select none</a></div>').appendTo(element);
            	
                var list = $('<ul class="items attributes clearfix"/>').appendTo(element);
                for (var i=0, a, formatted, checked; a=item.attributes[i]; i++) {
                    // Either a default selected attribute, or it's selected from URL query param
                    checked = a.selected || (o.selectedAttributes && $.inArray(a.name, o.selectedAttributes) != -1);

                    formatted = $(biomart.renderer.attribute('li', a, checked));
                    formatted
                        .simplerattribute({radio:false})
                        .appendTo(list);
                    if (formatted.children('div.invalid').length) {
                        biomart.disableFilter(formatted);
                    }
                    if (checked) {
                        element.addClass('hasDefaults');
                        if (o.onAttributeSelect) o.onAttributeSelect(a);
                    }
                }
            }
        }

        // Draw subcontainers
        if (item.containers) {
            for (var i=0, c; c=item.containers[i]; i++) {
                self.container($.extend({}, o, {
                    item: c,
                    level: level+1, 
                    extraClassNames: item.maxContainers ? 'isMaxContainer' : '',
                    appendTo: element
                }));
            }

            // Use tabs
            if (item.maxContainers) {
                var defaults = element.addClass('hasMaxContainers').find('.hasDefaults'),
                    index = 0;

                if (defaults.length) {
                    index = defaults.closest('.isMaxContainer').index()-1;
                }

                element.singlecontainer({ activeIndex: index });
            }
        } else {
            element.addClass('leaf-container');
        }

        if (o.extras) o.extras(item, element);

        return element;
    };

    /*
     * RESULTS
     *
     * These are actual object instances, so you shouldn't parse more than one resultset
     * at a time. This is beneficial for performance reasons, but if need be the code
     * should be refactored to allow multiple instances to parse.
     *
     * Each format extends upon the `plain` format through Object.create function.
     */
    self.get = function(format) {
        if (format in self.results) 
            return self.results[format];
        alert(format + ' is an invalid renderer!');
        return null
    };

    self.results = {};

    /* PLAIN */
    self.results.plain = {
        tagName: 'pre',
        parse: function(rows, writee) {
            var n = rows.length,
                tmp = [];
            while (n--)
                tmp[n] = rows[n].join('\t');
            writee.append(tmp.join('\n') + '\n');
        },
        getElement: function() {
            this._id = biomart.uuid();
            this._hasError = false;
            return $(['<', this.tagName, ' id="', this._id, '"/>'].join(''));
        },
        printHeader: function(header, writee) { 
            writee.append(header.join('\t') + '\n');
        },
        printCaption: function(info, writee) {
            writee.append(info);
        },
        option: function(name, value) {
            this['_' + name] = value;
        },
        draw: function() {},
        error: function(writee, reason) {
            this._hasError = true;
            switch (reason) {
                case 'timeout':
                    writee.html([
                        '<p class="error">', _('error_timeout_msg'), '</p>'
                    ].join('')).addClass('error');
                    break;
                default:
                    writee.html(_('error has occurred', biomart.CAPITALIZE)).addClass('error');
            }
        },
        clear: function() {},
        destroy: function() {}
    };
});
})(jQuery);

