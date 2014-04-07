(function($) {
    var results = biomart.renderer.results;

    /* TABLE */
    results.table = Object.create(results.plain);
    results.table.tagName = 'table';
    results.table._highlight = null;
    results.table.canPaginate = true;
    results.table.parse = function(rows, writee) {
        var n = rows.length,
            arr = [],
            currVal,
            className;
        
        while (n--) {
            var m = writee.data('numCols') || rows[n].length;
            arr[n] = [];
            while (m--) {
                currVal = rows[n][m];
                if (currVal) {
                    className = '';
                } else {
                    currVal = _('empty_value')
                    className = 'empty';
                }
                arr[n][m] = [
                    '<td', this._highlight==m? ' class="highlight"' : '', '>',
                        '<p class="', className ,'">', currVal, '</p>',
                    '</td>'
                ].join('');
            }

            arr[n] = ['<tr>', arr[n].join(''), '</tr>'].join('');
        }
        writee.append(arr.join(''));
    };
    results.table.setHighlightColumn = function(i) { this._highlight = i };
    results.table.printHeader = function(header, writee) {
        var thead = ['<thead><tr>'],
            numCols = 0;
        for (var i=0, col; col=header[i]; i++) {
            numCols++;
            thead.push([
                '<td', this._highlight==i ? ' class="highlight"' : '', '>',
                    '<p>', header[i], '</p>',
                '</td>'
            ].join(''));
        }
        thead.push('</tr></thead>');
        $(thead.join('')).disableSelection().appendTo(writee);
        writee.data('numCols', numCols);
    };
    results.table.printCaption = function(caption, writee) {
        caption = caption.split('\n').join('<br/>');
        $(['<caption>', caption, '</caption>'].join('')).appendTo(writee);
    };
})(jQuery);

