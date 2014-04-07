/*
 * Partial application for functions in JavaScript
 *
 * Usage: foo.partial(arg) will bind arg to first argument of foo and return resulting function
 */
Function.prototype.partial= function() {
    var curryArgs = Array.prototype.slice.call(arguments, 0),
        func = this;

    return (function() {
        var normalizedArgs = Array.prototype.slice.call(arguments, 0);
        return func.apply(null, curryArgs.concat(normalizedArgs));
    });
};

/*
 * Curry functions in JavaScript; Probably not very useful, just for fun
 *
 */
Function.prototype.curry = function() {
    var curryArgs = [], n = this.length, func = this;

    return (function(x) {
        curryArgs.push(x);
        if (curryArgs.length < n) return arguments.callee;
        else return func.apply(null, curryArgs);
    });
};

// Array Remove - By John Resig (MIT Licensed)
Array.prototype.remove = function(from, to) {
    var rest = this.slice((to || from) + 1 || this.length);
    this.length = from < 0 ? this.length + from : from;
    return this.push.apply(this, rest);
};

if (typeof Object.create !== 'function') {
    Object.create = function (o) {
        function F() {}
        F.prototype = o;
        return new F();
    };
}

(function($) {
/*
 * Declaring a namespace.
 * Will extend any existing namespaces.
 *
 * Usages: 
 *     $.namespace('foo.bar', {faz: 1});
 *  $.namespace('faz.baz.test', function(self) { self.a = 1; self.b = 2 });
 */
$.namespace = function(ns, obj) {
    ns = ns.split('.'), p = window;
    for(var i=0, curr; curr=ns[i]; i++) 
        p = p[curr] = p[curr] || {};
    if (obj) 
        $.extend(p, $.isFunction(obj) ? obj(p) : obj);
};

/*
 * Implements the subscriber part of the Pub/Sub pattern
 */
$.subscribe = function(signal, scope, fnName) {
    var curryArgs = Array.prototype.slice.call(arguments, 3);
    $(document.body).bind(signal, function () {
        var normalizedArgs = Array.prototype.slice.call(arguments, 1);
        scope[fnName].apply(scope, curryArgs.concat(normalizedArgs));
    });
    return $;
};

$.unsubscribeAll = function(signal) {
    $(document.body).unbind(signal);
    return $;
};

/*
 * Implements the publisher part of the Pub/Sub pattern
 */
$.publish = function(signal) {
    $(document.body).trigger(signal, Array.prototype.slice.call(arguments, 1));
    return $;
};

/*
 * BioMart
 */
$.namespace('biomart', function(self) {
    var GUID = 1;

    self.NAME = 'BioMart';
    self.VERSION = '0.8';
    self.DEFAULT_PAGE_SIZE = 20;
    self.errorRegex = /error=true/;
    self.htmlRegex = /<[A-Z0-9="':\/\s\.&\?_\-#]+>/ig;

    self.OPERATION = {
        MULTI_SELECT: 'MULTISELECT',
        SINGLE_SELECT: 'SINGLESELECT'
    };

    self.NON_FILTER_PARAMS = ['datasets', 'attributes', 'preview'];

    self.MAX_CONCURRENT_REQUESTS = 4;

    // Used by apps to maintain UI state
    self.params = [];
    self.state = [];

    // i18n function
    self.CAPITALIZE = 1;
    self.PLURAL = 2;
    self.getLabel  = function(name, o) {
        var key = name.replace(/\s+/g, '_'),
            str = BIOMART_CONFIG.labels[key] || name,
            plural;
        if (o & self.PLURAL) {
            plural = BIOMART_CONFIG.labels[key+'__plural'];
            if (plural) {
                str = plural;
            } else {
                str = str + 's';
            }
        }
        if (o & self.CAPITALIZE) {
            str = str.charAt(0).toUpperCase() + str.slice(1);
        }
        return str;
    };

    self._defaultErrorMsg = self.getLabel('error_generic_msg');
    self.error = function(msg) {
        msg = msg || self._defaultErrorMsg;
        $('#biomart-loading').empty().css('background-color', '#999');
        self.errorDialog
            .find('.msg').html(msg)
            .end()
            .dialog('open');
    };

    self.message = function(msg) {
        $('#biomart-message-dialog')
            .html(msg)
            .dialog({
                dialogClass: 'message',
                autoOpen: true,
                resizable: false,
                draggable: false,
                modal: true,
                width: 400,
                height: 200,
                buttons: {
                    'OK': function() {
                        $(this).dialog('close')
                    }
                },
                close: function() {
                    $(this).dialog('destroy');
                }
            });
    };

    self.stripHtml = function(str) {
        return str.replace(self.htmlRegex, '');
    };

    self.getHomeLinks = function() {
         return $('#biomart-header a[rel=biomart-home]');
    }

    // HTML templates we can clone() from
    self.templates = {
        loader: $('<span class="loading"/>'),
        filterList: $('<ul class="filters"/>')
    };

    // Queue to help with executing async functions in order
    self.Queue = function(element, queueName, namespace) {
        this.element = element;
        this.queueName = queueName;
        this.namespace = namespace;
    };
    self.Queue.prototype.queue = function(fn) {
        this.element.queue(this.queueName, fn);
    };
    self.Queue.prototype.dequeue = function(n) {
        if (n === 0 || n < 0) return;
        var length = this.element.queue(this.queueName).length;
        if (length) {
            this.element.dequeue(this.queueName);
            if (--n > 0 && n <= length) {
                this.dequeue(n-1);
            }
        } else {
            if (this.namespace) 
                this.element.trigger('queue.done.' + this.queueName);
            else 
                this.element.trigger('queue.done');
        }
    };
    self.Queue.prototype.clear = function() {
        this.element.queue(this.queueName, []);
    };

    self.notify = function(msg, options) {
        if (typeof $.jGrowl == 'undefined') {
            $.growlUI(msg);
        } else {
            $.jGrowl(msg, options);
        }    
    };

    self.errorDialog = $('#biomart-error-dialog')
        .dialog({
            dialogClass: 'error',
            autoOpen: false,
            modal: true,
            width: 400,
            height: 200,
            draggable: false,
            resizable: false
        })
        .delegate('button', 'click', function() {
            location = '/';
        });

    // TODO: clearing of filters should be handled elsewhere, similar to validators returning values
    self.clearFilter = function(element, cb) {
        var name = element.attr('filter-name');
        var type = element.attr('filter-type');
        element.removeClass('ui-active');
        element.find(':input').each(function() {
            var $this = $(this);
            if ($this.is('select,.ui-autocomplete-input')) {
            	if(type === 'singleSelectUpload')
            		$this.val(element.data('item').filters[0].displayName);
            	else
            		$this.val(['-- ', _('select', biomart.CAPITALIZE), '--'].join(''));
            } else if ($this.is(':text,textarea')) {
                $this.val('');
            } else if ($this.is('[type=radio]')) {
                $this[0].checked = false;
            }
        });
        if (cb) cb(element.data('item'));
    };

    self.escapeXml = function(str) {
        var tmp = $('<div/>');
        tmp.text(str);
        return tmp[0].innerHTML.replace(/"/g, '\\"');
    };


    (function() {
    var $blockElement = $('<div style="z-index: 100; border: none; margin: 0; padding: 0; width: 100%; height: 100%; top: 0; left: 0; background-color: #fff; cursor: default; position: absolute;"/>').animate({opacity: .7}),
        $msgElement = $('<div style="z-index: 101; padding: 10px 20px; margin: 0px; position: absolute; top: 0; left: 0; height: 100%; width: 100%; text-align: center; color: #555; font-size: 13px; border: none; background-color: #fff" class="blockUI blockMsg blockElement"/>').animate({opacity: .2}).disableSelection();

    // Can be overlayed on top of another element to block it
    self.getBlockElement = function(tooltip) {
        var el = $blockElement.clone();
        if (tooltip) el.attr('title', tooltip);
        return el;
    };

    self.getBlockMsgElement = function(msg) {
        return $msgElement.clone().text(msg);
    }

    })();

    self.number = {
        separateThousands: function (s, n) {
            var sign = '';
            var decimals = null;
            if (typeof n != 'string') {
                n = '' + n;
            }
            if (parseFloat(n) < 0) {
                sign = '-';
            }
            if (n.indexOf('.') != -1) {
                var arr = n.split('.');
                n = arr[0];
                decimals = arr[1];
            }
            n = parseFloat(n);
            n = '' + Math.abs(n);
            return sign + (function _(s, n, i, j) {
                if (i < 0) {
                    return n.substring(0, j);
                }
                return _(s, n, i - 3, j - 3) + (i > 0 ? s : '') + n.substring(i, j);
            })(s, n, n.length - 3, n.length) + (decimals ? '.' + decimals : '');
        },
        roundDecimals: function (n, d) {
            var N = parseFloat(n);
            N = Math.round(N*Math.pow(10,d));
            N /= Math.pow(10,d);
            return N.toFixed(d);
        },
        format: function (n, o) {
            if (isNaN(n) || n === 0 || n == '0') {
                return '--';
            }
            if (o.zeroIsNull === true && n === 0) {
                return '--';
            }
            if (o.decimals !== undefined) {
                n = self.number.roundDecimals(n, o.decimals);
            }
            if (o.separateThousands === true) {
                n = self.number.separateThousands(',', n);
            }
            var prefix = o.prefix || '';
            var suffix = o.suffix || '';
            return prefix + n + suffix;
        }
    };

    self.uuid = function() {
        return [self.NAME, GUID++].join('_'); 
    };

    self.disableFilter = function(element) {
        element
            .css({
                'opacity': .3,
                'position': 'relative'
            })
            .append(biomart.getBlockElement(self.getLabel('option_unavailable', self.CAPITALIZE)).css('opacity', 0))
            .find('.ui-autocomplete-input').attr('tabIndex', -1).val('No data');
    };

    self.makeMartSelect = function(marts, options) {
        var select = $(['<select id="', biomart.uuid(), '"/>'].join('')),
            groups = {};
        options = options || {};
        for (var i=0, mart, option, selected; mart=marts[i]; i++) {
            selected = false;
            if (!mart.isHidden) {
                if (mart.group) {
                    var option = groups[mart.group];
                    if (!option) {
                        selected = (!options.selected && !i) || (options.selected == mart.group);
                        option = groups[mart.group] = $(['<option value="', mart.name, '">', mart.group, '</option>'].join(''))
                                .data('mart', [mart])
                                .data('group', mart.group)
                                .appendTo(select);
                        if (selected) option.attr('selected', true); // mark active
                        if (options.each) options.each(option.data('mart'), selected);
                    } else {
                        var value = option.val(),
                            items = option.data('mart');
                        option.val(value + ',' + mart.name);
                        items.push(mart);
                        option.data('mart', items);
                        if (selected) option.attr('selected', true); // mark active
                        if (options.each) options.each(option.data('mart'), selected);
                    }
                } else {
                    selected = (!options.selected && !i) || (options.selected == mart.name);
                    option = $(['<option value="', mart.name, '">', mart.displayName, '</option>'].join(''))
                        .data('mart', mart)
                        .appendTo(select);
                    if (selected) option.attr('selected', true); // mark active
                    if (options.each) options.each(mart, selected);
                }
            }
        }
        return select;
    };

    self.escapeHTML = function(str) {
         return str.replace(/&(?!\w+;)/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    };

    self.checkRequiredFilters = function(filterElements) {
        for (var i=0; i<filterElements.length; i++) {
            var filterElement = filterElements.eq(i),
                item = filterElement.data('item'),
                isActive = filterElement.hasClass('ui-active');
            if (item.required && !isActive) {
                var message = _('This filter is required'),
                    element = $('<div class="tipsy tipsy-east"><div class="tipsy-arrow"></div><div class="tipsy-inner">' 
                        + message + '</div></div>'),
                    position = filterElement.offset();
                filterElement.scrollTo();
                element.css('position', 'absolute').appendTo(document.body);
                element.css({
                        top: position.top + 'px',
                        left: (position.left - element.width() - 10) + 'px'
                    });
                return element;
            }
        }
    };

    $('#biomart-locations').bind('change.location', function() {
        location = $(this).val();
    });

    $('#biomart-flash-message').delegate('.ui-icon-close', 'click', function() {
        $('#biomart-flash-message').fadeAndRemove();
    });
});

$.fn.stopIframeLoading = function() {
    return this.each(function() {
        if ($.browser.msie) $(this).contents()[0].execCommand('Stop');
        else $(this)[0].contentWindow.stop();
    });
};

$.stop = function() {
    if ($.browser.msie) document.execCommand('Stop');
    else window.stop();
};

$.fn.fadeAndRemove = function(t) {
    t = t || 100;
      return this.each(function() {
        var $this = $(this);
        $this.animate({opacity: 'hide'}, {
            duration: t,
            complete: function() { $this.remove() }
        });
    });
};

$.fn.inView = function(buffer) {
    var $w = $(window),
        y1 = $w.scrollTop(),
        y2 = y1 + $w.height(),
        me = this.eq(0),
        y3 = me.offset().top,
        y4 = y3 + me.height();
    buffer = buffer || 0;
    return y4 >= y1 - buffer && y3 <= y2 + buffer;
};

$.fn.scrollTo = function(arg1, arg2) {
    var me = this.eq(0),
        isFunc = $.isFunction(arg1),
        offsetTop = arg1 && isFunc ? arg1 : 0;
        callback = arg1 && isFunc ? arg1 : arg2;
    $('html,body').stop(true, true).animate({scrollTop: me.offset().top - offsetTop}, {
        duration: 250,
        complete: function() {
            if (callback) callback();
        }
    });
    return this;
};

if ($.fn.delegate) {
    $(document.body).delegate('a[rel=view-more]', 'click.biomart', function() {
        var href = this.href,
            params,
            url,
            form;
        if (href.length > 2048) {
            form = $('<form/>').appendTo(document.body);
            params = href.split('?');
            url = params.slice(0,params.length-1).join('');
            params = params[params.length-1];
            form.attr('action', url).attr('method', 'POST').attr('target', '_blank');
            $(['<input type="hidden" name="qs" value="', params, '"/>'].join('')).appendTo(form);
            form.submit();
            return false;
        }
    });
}

})(jQuery);

_ = biomart.getLabel;

