(function($) {
    var results = biomart.renderer.results;

    /* HEATMAP */
    results.heatmap = Object.create(results.plain);
    results.heatmap.tagName = 'div';
    results.heatmap._heatColumn = 0;
    results.heatmap._max = 5.0;
    results.heatmap._min = -5.0;
    results.heatmap._getColor = function(val) {
        var min = this._min,
            max = this._max,
            mid = (max + min) / 2;

        if (val > max) return 'rgb(255,0,0)';
        if (val < min) return 'rgb(0,0,255)';

        var r = this._getRed(val, min, mid, max),
            b = this._getBlue(val, min, mid, max),
            g = this._getGreen(val, min, mid, max);

        return ['rgb(', r, ',', g, ',', b, ')'].join('')
    };
    results.heatmap._getBlue = function(val, min, mid, max) {
        if (val >= 0) return 0;
        var range = Math.abs(min - mid);
        val = Math.abs(val - mid);
        return parseInt(val / range * 255);
    };
    results.heatmap._getGreen = function(val, min, mid, max) {
        if (val <= 0) return 0;
        var mid2 = (max + mid) / 2,
        val2 = Math.abs(mid2 - val);
        return 180 - parseInt(val2 / mid2 * 180);
    };
    results.heatmap._getRed = function(val, min, mid, max) {
        if (val <= 0) return 0;
        var mid2 = (max + mid) / 2;
        if (val >= mid2) return 255;
        return parseInt(val / mid2 * 255);
    };
    results.heatmap.parse = function(rows, writee) {
        var n = rows.length,
            arr = [],
            currVal;

        row_loop:
        while (n--) {
            var m = rows[n].length,
                row = [];

            var curr = [null, []];

            for (var j=0, currVal, extraClass, title; j<m; j++) {
                currVal = rows[n][j];

                if (j == this._heatColumn) {
                    currVal = parseFloat(currVal);
                    curr[2] = currVal || -Infinity;
                    if (currVal) {
                        currVal = biomart.number.format(currVal, {decimals:2});
                        extraClass = '';
                        title = this._header[j];
                    } else if (typeof this._fallbackColumn != 'undefined' &&
                            (currVal = rows[n][this._fallbackColumn])) {
                        curr[0] = ['<span class="fallback" title="', this._header[this._fallbackColumn], 
                                '">', currVal, '</span>'].join('');
                        extraClass = 'none';
                        title = [this._header[j], ' ', _('not_reported'), ', ', _('displaying'), ' ', 
                                this._header[this._fallbackColumn]].join('');
                    } else {
                        continue row_loop;
                    }
                    currVal = [
                        '<div class="heat-box ', extraClass, '" ', 
                                'style="background-color: ', this._getColor(currVal), '" title="', title, '">',
                            '<span class="value">', currVal, '</span>',
                        '</div>'
                    ].join('');
                    curr[0] = currVal;
                } else if (!this._displayColumns || $.inArray(j, this._displayColumns) != -1) {
                    curr[1].push(currVal || _('empty_value'));
                }
            }

            this._arr.push(curr);
        }
    };
    results.heatmap.setHighlightColumn = function(i) { this._highlight = i };
    results.heatmap.printHeader = function(header, writee) {
        var arr = [null, []];
        writee.addClass('clearfix');
        this._header = header;
        for (var i=0, n=header.length-1; i<n; i++) {
            if (i == this._heatColumn) {
                arr[0] = ['<td class="primary">', header[i], '</td>'].join('');
            } else if (!this._displayColumns || $.inArray(i, this._displayColumns) != -1) {
                arr[1].push(['<td>', header[i], '</td>'].join(''));
            }
        }
        this._table = $(['<table><thead><tr>', arr[0], arr[1].join(''), '</tr></thead></table>'].join('')).appendTo(writee);
        this._tbody = $('<tbody/>').appendTo(this._table);
        this._arr = [];
    };
    results.heatmap.option = function(name, value) {
        this['_' + name] = value;
    };
    results.heatmap.draw = function(writee) {
        if (this._hasError) return;

        if (!this._arr || !this._arr.length) {
            writee.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
            return;
        }

        var n = this._arr.length,
            arr = this._arr;

        arr.sort(function(left, right) {
            var a = right[2],
                b = left[2];
            if (a > b) return 1;
            if (a < b) return -1;
            return 0;
        });

        while (n--) {
            if (arr[n]) arr[n] = ['<tr><td class="primary">', arr[n][0], '</td><td>', arr[n][1].join('</td><td>'), '</td></tr>'].join('');
        }

        this._tbody.append(arr.join(''));

        if (!arr.length) {
            writee.parent().parent().html(['<p class="empty">', _('no_results'), '</p>'].join(''));
            return;
        }

        this._tbody.find('div.heat-box').tipsy({
            fade: true,
            gravity: 'w',
            opacity: .9
        });

        // Use canvas to draw the legend
        var legend,
            canvas = $('<canvas/>'),
            ctx,
            grad,
            x1,
            y1,
            x2,
            x2,
            mid = (this._min+this._max) /2
            color1 = this._getColor(this._min);
            color2 = this._getColor(mid);
            color3 = this._getColor((mid+this._max)/2);
            color4 = this._getColor(this._max);
            grad,
            heading = this._header[this._heatColumn];

        writee
            .parent().addClass('clearfix')
            .find('div.heat-box') 
            .hover(function() {
                $(this).children('span.value').fadeIn(300);
            }, function() {
                $(this).children('span.value').fadeOut(300);
            });

        legend = $('<div class="heat-legend"/>')
            .append(canvas)
            .append(['<div class="max">', this._max, '</div>'].join(''))
            .append(['<div class="mid">', (this._max+this._min)/2, '</div>'].join(''))
            .append(['<div class="min">', this._min, '</div>'].join(''))
            .append(['<p>', heading, '</p>'].join(''))
            .disableSelection();

        $('<div class="heat-legend-wrap"/>')
            .insertAfter(this._table)
            .append(legend);

        canvas = canvas.get(0);
        x1 = 0; y1 = 0; x2 = 200; y2 = 20;
        canvas.width = x2;
        canvas.height = y2;

        if (typeof G_vmlCanvasManager != 'undefined')
            canvas = G_vmlCanvasManager.initElement(canvas);

        if (canvas.getContext('2d')) {
            ctx = canvas.getContext('2d');
            grad = ctx.createLinearGradient(x1, y1, x2, y1);
            grad.addColorStop(0, color1);
            grad.addColorStop(.5, color2);
            grad.addColorStop(.75, color3);
            grad.addColorStop(1, color4);
            ctx.fillStyle = grad;
            ctx.fillRect(x1, y1, x2, y2);
        }
    };
})(jQuery);
