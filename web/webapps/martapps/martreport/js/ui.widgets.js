(function($) {
$.widget('ui.limitedfilter', {
    options: {
        value: null
    },

       _create: function() {
         var self = this,
            element = self.element.addClass('limitedfilter'),
            type = element.attr('filter-type'),
            textarea = element.find('textarea.field.list');

        if (self.options.value) {
             self.value(self.options.value);
        }

        element.find('select')
            .prettybox()
            .change(function() { 
                element.find('.ui-autocomplete-input').blur();
                element.find('.field.text.list').focus();
            });

        if (textarea.length) {
            textarea
                .after(
                    $('<input class="field text list limited" type="text"/>')
                    .val(textarea.val())
                    .change(function() { $(this).siblings('textarea').val($(this).val()) })
                )
                .hide();
        }

        element.find('input:text')
            .addClass('ui-state-default ui-corner-all')
            .bind('focus.limitedfilter', function() {$(this).addClass('ui-state-focus').select()})
            .bind('blur.limitedfilter', function() {$(this).removeClass('ui-state-focus')});

        element.find('.upload').hide();

        if (self.options.value) this.value(self.options.value);
    },

    value: function(val) {
          var self = this,
            element = self.element;

        if ($.isArray(val)) {
            var select = element.find('select'),
                name = val[0],
                value = val[1];

            if (select.children('option[value=' + name + ']').length) {
                select.val(name);
                element.find('.field').val(value);
            }
        } else {
            self.element.find('input:text').val(val);
        }
    },

    destroy: function() {
         var self = this,
            element = self.element.addClass('limitedfilter'),
            type = element.attr('filter-type')
            ;

        element.find('textarea.field.list')
            .next($('input.field.text.list"')).remove().end()
            .show()
            ;

        element.find('input:text')
            .removeClass('ui-state-default ui-corner-all')
            .unbind('focus.limitedfilter')
            .unbind('blur.limitedfilter')
            ;

        element.find('select').prettybox('destroy');

        element.find('.upload').show();
    }
});
})(jQuery);

