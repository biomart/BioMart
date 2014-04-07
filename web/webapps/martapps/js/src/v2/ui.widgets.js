/*
 * jQuery UI widgets for BioMart
 */
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

        element.bind('click.minimizer', function() {
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
        this.element.unbind('click.minimizer');
        this._div
            .data('minimizer', null)
            .unwrap();
    }
});

/*
 * Uploadify
 *
 * Turns a file upload form element into a upload link that will automatically send the file to server.
 */
$.fn.uploadify = function() {
    return this.each(function() {
        var input = $(this).hide(),
            upload = $(['<a href="#">', _('upload file').i18n(), '</a>'].join('')).insertAfter(input).addClass('uploader'),
            subgroup = input.closest('.filter-field-upload'),
            container = subgroup.closest('.model-filter'),
            textarea = subgroup.children('textarea');

        new AjaxUpload(upload, {
                action: BIOMART_CONFIG.service.url + 'file/wrap?callback=BM.upload.temp', 
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
                    _('BM.upload').namespace({
                        temp: function(text) {
                            textarea.val(text).trigger('change');;
                        }
                    })
                    eval(response);
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
            multipleText = ['-- ', _('select').i18n(BM.i18n.CAPITALIZE), ' --'].join(''),
            multiple = select.multiple,
            combo = $('<div class="ui-prettybox ui-state-default ui-corner-all clearfix"/>').insertAfter(select),
            input = $('<input/>').appendTo(combo),
            button = $('<a></a>').insertAfter(input),
            selected = $('<ul class="selected-items"/>').hide().insertAfter(combo),
            ac_menu;

        input
            .autocomplete({
                source: (function() {
                   var items = select.children('option').map(function() {
                        return { id: $(this).val(), label: $(this).text(), text: $(this).text() }
                    }) 

                    return function(request, response) {
                        var matcher,
                            matched,
                            n = items.length;

                        if (!request.term) {
                            matched = items;
                        } else {
                            matcher  = new RegExp(request.term.replace(/\(/g,'\\(').replace(/\)/g,'\\)'), 'i')
                            matched = [];
                            for (var i = 0; i < n; i++) {
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
                    }
                })(),
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
            .bind('mouseout.prettybox', _.partial(function(me, e) {
                if (!me.data('justSelected')) {
                    me.data('autocomplete').menu.element.removeClass('hover');
                    if (e.target.tagName != 'UL' ) return false;
                    me.data('valid', false).focus();
                }
            }, input))
            .addClass(select.attr('id'));

        $('.ui-menu-item', ac_menu)
            .live('mouseover', _.partial(function(menu, ev) {
                menu.addClass('hover');
            }, ac_menu))
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

})(jQuery);
