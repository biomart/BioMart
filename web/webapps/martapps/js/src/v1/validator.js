(function($) {
/*
 * Validates form values
 */
$.namespace('biomart.validator', {
    // Validate filter based on type, and returns value if valid or null otherwise
    // element is the .field DOM element corresponding to the filter container
    filter: (function() {
        var fn = {
            'default': function(element) {
                return element.find('input.field:text').val() || null;
            },
            'boolean': function(element) {
                var inputs = element.find('input.field'),
                    n = inputs.length,
                    value;
                while (n--) {
                    if (inputs[n].checked) {
                        return inputs[n].value;
                    }
                }
                return null;
            },
            'upload': function(element) {
                var value = element.find('textarea.field').val() || null;
                value = replaceWhitespaces(value);
                return value;
            },
            'singleSelect': function(element) {
                var list = element.find('select');

                // list contains values for filter
                if (list.hasClass('field')) {
                    return list.val() || null;
                }

                // list containers filter names, value from text box
                var name = list.val(),
                    value = element.find('input.field').val() || null,
                    displayName = list.children('option[value=' + name + ']').text();
                return [name, value, displayName];
            },
            'singleSelectBoolean': function(element) {
                var list = element.find('select.filter-list'),
                    item = element.data('item')
                    name = list.val(),
                    value = 'only',
                    displayName = list.children('option[value=' + name + ']').text();
                return [name, value, [item.displayName, ' ', displayName].join('')];
            },
            'singleSelectUpload': function(element) {
                var list = element.find('select.filter-list'),
                    name = list.val(),
                    value = element.find('textarea.field').val() || null,
                    displayName = list.children('option[value=' + name + ']').text();
                value = replaceWhitespaces(value);
                return [name, value, displayName];
            },
            'multiSelect': function(element) {
                var value = element.find('select.field').val();
                if (value) return value.join(',');
                return null;
            },
            'multiSelectBoolean': function(element) {
                var list = element.find('select.filter-list'),
                    item = element.data('item')
                    names = list.val(),
                    value = 'only',
                    displayNames = [];
                if (names) names = names.join(',');
                list.children().each(function() {
                    if (this.selected) {
                        displayNames.push(this.text);
                    }
                });
                displayNames = displayNames.join(', ');
                return [names, value, [item.displayName, ': ', displayNames].join('')];
            },
            'multiSelectUpload': function(element) {
                var list = element.find('select.filter-list'),
                    item = element.data('item')
                    names = list.val(),
                    value = element.find('textarea.field').val() || null,
                    displayNames = [];
                if (names) names = names.join(',');
                list.children().each(function() {
                    if (this.selected) {
                        displayNames.push(this.text);
                    }
                });
                displayNames = displayNames.join(', ');
                value = replaceWhitespaces(value);
                return [names, value, [item.displayName, ': ', displayNames].join('')];
            }
        };

        function replaceWhitespaces(value) {
            return $.trim(value).replace(/\n/g, ',');
        }

        return function(element) {
            var item = element.data('item'),
                validator = fn[item.type] || fn['default'];
            return validator(element);
        };
    })()
});
})(jQuery);

