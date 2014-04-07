(function($) {
    var results = biomart.renderer.results;

    /* LIST */
    results.list = Object.create(results.plain);
    results.list.tagName = 'ul';
    results.list._list = {};
    results.list._breakAt = 1;
    results.list._grouped = true;
    results.list._template = null;
    results.list.parse = function(rows, writee) {
        if (this._squish && this._oneColumn) {
            var $ul = this._list.main,
                $li;
            if (!$ul) {
                $ul = this._list.main = this._template.clone();
                $('<li class="group clearfix"/>').append($ul).appendTo(writee);
            }
            $li = $ul.children('li');

            for (var i=0, row; row=rows[i]; i++) {
                var $value = $li.children('span.value'),
                    value = row[0],
                    arr = $value.data('values');

                if (!value) continue;
                if (!arr) {
                    arr = [];
                    $value.data('values', arr);
                }
                if ($.inArray(value, arr) == -1) arr.push(value);
            }
        } else if (!this._grouped) {
            var $items = this._list.main;
            if (!$items) {
                $items = this._list.main = this._template.clone();
                $('<li class="group clearfix"/>').append($items).appendTo(writee);
            }
            var children = $items.children('li');
            for (var i=0, row; row=rows[i]; i++) {
                for (var j=0; j<row.length; j++) {
                    var value = row[j],
                        $value = children.eq(j).children('span.value'),
                        arr = $value.data('values');

                    if (!arr) {
                        arr = [];
                        $value.data('values', arr);
                    }

                    if (value) {
                        if ($.inArray(value, arr) == -1) {
                            arr.push(value);
                        }
                    }
                }
            }
        } else {
            for (var i=0, row; row=rows[i]; i++) {
                var pk = row[0], // first column is primary key
                    $items = this._list[pk],
                    children;

                if (!pk) continue;

                if (!$items) {
                    $items = this._list[pk] = this._template.clone();
                    $('<li class="group clearfix"/>').attr('pk', biomart.stripHtml(pk)).append($items).appendTo(writee);
                }

                children = $items.children('li');

                for (var j=0; j<row.length; j++) {
                    var value = row[j],
                        $value = children.eq(j).children('span.value'),
                        arr = $value.data('values');

                    if (!arr) {
                        arr = [];
                        $value.data('values', arr);
                    }

                    if (value) {
                        if ($.inArray(value, arr) == -1) {
                            arr.push(value);
                        }
                    }
                }
            }
        }
    };
    results.list.printHeader = function(header, writee) {
        var n = header.length;
        this._oneColumn = n == 1;

        this._template = $('<ul class="clearfix"/>');
        for (var i=0, classNames; i<n; i++) {
            classNames = [
                i==0 ? this._grouped ? 'primary' : '' : (i==n-1 ? 'last' : ''),
                i%2==0 ? 'even' : 'odd'
            ];

            if (this._grouped && i==this._breakAt) {
                classNames.push('break');
            } else if (i>0 && i<this._breakAt) {
                classNames.push('meta');
            }

            this._template.append([
                    '<li class="', classNames.join(' '), ' col-', i, '">',
                        '<span class="key heading">', header[i], ':</span>', 
                        '<span class="value"></span>', 
                    '</li>'
                ].join(''));
        }
        if (n==1) this._template.addClass('only-one');
    };
    results.list.draw = function(writee) {
        if (this._hasError) return;

        if ($.isEmptyObject(this._list)) {
            writee.append(['<p class="empty">', _('no_results'), '</p>'].join(''));
        }

        writee.hover(function() { writee.addClass('hover') }, function() { writee.removeClass('hover') });

        if (!this._grouped) {
            this._list.main.children('li').each(function(i) {
                var $this = $(this),
                    $value = $this.children('span.value'),
                    values = $value.data('values'),
                    title = $this.children('span.key').text();

                if (!values || !values.length) {
                    values = _('empty_value');
                } else if (values.length && values.length > 2) {
                    for (var i=0, n=values.length; i<n; i++) {
                        values[i] = [
                            '<span class="joined ', i%2==0 ? 'even' : 'odd', '">',
                                values[i],
                            '</span>'
                        ].join('');
                    }
                    values = values.join(', ');
                } else {
                    values = values[0];
                }

                // Because IE 6/7 don't handle pseudo CSS :after selector 
                if ($.browser.msie && /^6|7/.test($.browser.version)) 
                    if (i==1) values = ['[', values, ']'].join('');

                $value
                    .attr('title', title.substr(0, title.length-1)) // Remove colon (':') at the end
                    .html(values);
            });
        } else {
            for (var k in this._list) {
                var curr = this._list[k];
                curr.children('li').each(function(i) {
                    var $this = $(this),
                        $value = $this.children('span.value'),
                        values = $value.data('values'),
                        title = $this.children('span.key').text();

                    if (!values || !values.length) {
                        values = _('empty_value');
                    } else if (values.length && values.length > 2) {
                        for (var i=0, n=values.length; i<n; i++) {
                            values[i] = [
                                '<span class="joined ', i%2==0 ? 'even' : 'odd', '">',
                                    values[i],
                                '</span>'
                            ].join('');
                        }
                        values = values.join(', ');
                    } else {
                        values = values[0];
                    }

                    // Because IE 6/7 don't handle pseudo CSS :after selector 
                    if ($.browser.msie && /^6|7/.test($.browser.version)) 
                        if (i==1) values = ['[', values, ']'].join('');

                    $value
                        .attr('title', title.substr(0, title.length-1)) // Remove colon (':') at the end
                        .html(values);
                });
            }
        }

        // reset
        this._list = {};
        this._template = null;
        this._breakAt = 1;
        this._grouped = true;
    };
})(jQuery);
