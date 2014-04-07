(function($) {
    var results = biomart.renderer.results;

    /* CHART */
    results.chart = Object.create(results.plain);
    results.chart.tagName = 'div';
    results.chart._keyMap = {};
    results.chart._lines = [];
    results.chart._lineIndices = [1];
    results.chart._labels = [];
    results.chart._max = 20;
    results.chart._header = null;
    results.chart.initExport = function(url) {
        var div = $([
                '<div class="chart-export clearfix">',
                    '<h6 class="chart-export-title">', _('export chart', biomart.CAPITALIZE), '<span class="ui-icon ui-icon-disk"/></h6>',
                '</div>'
            ].join('')),

            dialog,
            results = ['PNG', 'JPEG', 'TIFF', 'PDF', 'EPS', 'SVG'],
            arr = [],
            self = this,
            id = biomart.uuid(),
            id2 = biomart.uuid(),
            id3 = biomart.uuid();

        for (var i=0, f; f=results[i]; i++) {
            arr.push(['<option value="', f, '">', f, '</option>'].join(''));
        }

        dialog = $([
            '<div class="chart-export-dialog gradient-grey-reverse" style="display:none">',
                '<form target="_blank" method="GET" action="', BIOMART_CONFIG.service.url, 'chart/', url,'">',
                    '<label for="', id, '">Format: </label>',
                    '<select id="', id, '" name="f">',
                        arr.join(''),
                    '</select><br/>',
                    '<label for="', id2, '">Width: </label>',
                    '<input size="4" type="text" id="', id2, '" name="w" value="800"/>pixels<br/>',
                    '<label for="', id3, '">Height: </label>',
                    '<input size="4" type="text" id="', id3, '" name="h" value="', this._max*40, '"/>pixels<br/>',
                    '<input type="submit" value="', _('go', biomart.CAPITALIZE), '"/>',
                '</form>',
                '<p class="info">Select a format and press <em>Go</em> to generate the chart in a new window.</p>',
            '</div>'
        ].join(''));

        dialog.find('form').bind('submit.chart', function() {
            return self._doExport($(this));
        });

        div
            .hoverIntent({
                over: function() {
                    dialog.fadeIn(200);
                },
                out: function() {
                    dialog.fadeOut(200);
                },
                timeout: 500
            })
            .insertBefore(this._element)
            .append(dialog);

        this._exportDiv = div;
        this._exportDialog = dialog;
    };
    results.chart._doExport = function(form) {
        var data = [],
            lines = this._lines,
            n = Math.min(this._max, this._lines.length),
            ck = [],
            sl = this._header[0],
            sk = [];

        while (n--) {
            for (var j=0, m=lines[n].values.length; j<m; j++) {
                if (!data[j]) data[j] = [];
                data[j][n] = lines[n].values[j];
            }
            ck[n] = lines[n].key;
        }

        for (var i=1; i<this._header.length; i++) {
            sk.push(this._header[i]);
        }

        while (m--) {
            data[m] = data[m].join(',');
        }
        data = data.join('|');
        form.prepend(['<input type="hidden" name="d" value="', data, '"/>'].join(''));
        //form.prepend(['<input type="hidden" name="ck" value="', ck.join(','), '"/>'].join(''));
        form.prepend(['<input type="hidden" name="ck" value="', ck.join(String.fromCharCode(29)), '"/>'].join(''));
        form.prepend(['<input type="hidden" name="sl" value="', sl, '"/>'].join(''));
        form.prepend(['<input type="hidden" name="cl" value="', this._xaxisLabel, '"/>'].join(''));
        form.prepend(['<input type="hidden" name="sk" value="', sk.join(','), '"/>'].join(''));

        return true;
    };
    results.chart.clear = function() {
        this._lines = [];
        this._labels = [];
        this._keyMap = {};
    };
    results.chart.destroy = function() {
        // cleanup
        this.clear();
        this.tagName = 'div';
        this._lineIndices = [1];
        this._max = 20;
        this._header = null;
        if (this._exportDialog) this._exportDialog.remove();
        if (this._exportDiv) this._exportDiv.remove();
    };
    results.chart.printHeader = function(header, writee) {
        this._header = header;
        // Not all columns are returned, truncate our indices array
        if (header.length-1 < this._lineIndices.length) {
            this._lineIndices = this._lineIndices.slice(0 ,header.length-1);
        }
    };
    results.chart._prevGetElement = results.chart.getElement;
    results.chart.getElement = function() {
        this._element = this._prevGetElement();
        this._tooltip = $('<div class="chart-tooltip"/>').hide().appendTo(document.body);
        return this._element;
    };
    results.chart.parse = function(rows, writee) {
        if (!rows.length) return;
        for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
            row = rows[i];
            rawKey = row[0],
            cleanedKey = typeof rawKey == 'string' ? biomart.stripHtml(rawKey) : rawKey,
            index = this._keyMap[cleanedKey];

            if (typeof index == 'undefined') {
                index = this._keyMap[cleanedKey] = this._lines.length;
                this._lines[index] = {
                    key: cleanedKey,
                    raw: rawKey,
                    values: [],
                    totals: []
                };
            }

            for (var j=0, value, m=this._lineIndices.length; j<m; j++) {
                var prevVal, prevTotal;
                value = row[this._lineIndices[j]];
                if (typeof value == 'object') {
                    prevVal = this._lines[index].values[j] || 0;
                    prevTotal = this._lines[index].totals[j] || 0;
                    this._lines[index].values[j] = prevVal + (parseInt(value.count) || 0);
                    this._lines[index].totals[j] = prevTotal + (parseInt(value.total) || 0);
                } else {
                    prevVal = this._lines[index].values[j] || 0;
                    this._lines[index].values[j] = prevVal + (parseInt(value) || 0);
                }
            }
        }
    };
    results.chart._sort = function(index) {
        var self = this;
        this._lines.sort(function(left, right) {
            var total_r = 0,
                total_l = 0;
            for (var i=0, n=self._lineIndices.length; i<n; i++) {
                total_r += right.values[i ]|| 0;
                total_l += left.values[i] || 0;
            }
            if (total_r > total_l) return 1;
            else if (total_r < total_l) return -1;

            if (right.key > left.key) return 1;
            if (right.key < left.key) return -1;

            return 0;
        });
    };
    results.chart.draw = function() {
        if (!this._lines.length || this._hasError) return;
        this.initExport('bar/stacked');

        // sort by total
        this._sort(false);

        var topRows = this._lines.slice(0, this._max),
            x_options = this._getXOptions(topRows, false),
            chartLines = [],
            chartLabels = [];
        
        this._element.css('height', (topRows.length * 40 + 155) + 'px');
        this._attachEvents();

        for (var i=0, n=topRows.length, item; item=topRows[i]; i++) {
            for (var j=0, m=this._lineIndices.length; j<m; j++) {
                if (!chartLines[j]) chartLines[j] = { data: [], label: this._header[j+1] };
                chartLines[j].data[i] = [item.values[j] || 0, n-i];
            }
            chartLabels.push([n-i, item.raw]);
        }

        this._plot = $.plot(this._element, chartLines, {
            series: {
                stack: true,
                bars: {
                    align: 'center',
                    show: true,
                    horizontal: true,
                    lineWidth: 0,
                    fill: true,
                    barWidth: .6
                }
            },
            xaxis: x_options,
            yaxis: {
                min: 0,
                max: topRows.length + 3,
                ticks: chartLabels
            },
            grid: {
                clickable: true,
                hoverable: true,
                autoHighlight: true
            },
            legend: {
                margin: [5, 5],
                backgroundOpacity: .6,
                  show: true,
                position: 'ne'
            }
        });

        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    results.chart['export'] = function() {
    };
    results.chart._getXOptions = function(rows, index) {
        var largestValue = -Infinity,
            interval,
            x_ticks = [];

        for (var i=0, n=rows.length, item, value; item=rows[i]; i++) {
            if (index) {
                value = item.values[index];
            } else {
                value = 0;
                for (var j=0, m=this._lineIndices.length; j<m; j++) {
                    value += item.values[j];
                }
            }
            if (value > largestValue) largestValue = value;
        }

        largestValue = Math.max(5, largestValue);

        if (largestValue < 20) interval = 1;
        else if (largestValue < 50) interval = 5;
        else if (largestValue < 100) interval = 10;
        else interval = ~~(largestValue / 100) * 10;

        for (var i=0; i<=largestValue; i+=interval) {
            x_ticks.push([i,i]);
        }
        
        return {ticks: x_ticks, min: 0, max: largestValue+1};
    };
    results.chart._attachEvents = function() {
        var self = this;
        this._element.bind('plothover', function (ev, pos, item) {
            var total, content;
            if (item) {
                clearTimeout(self._t);
                if (this._prevPt != item.datapoint) {
                    this._prevPt = item.datapoint;

                    total = self._lines[item.dataIndex].totals[item.seriesIndex];

                    if (total) {
                        content = [
                            item.series.data[item.dataIndex][0], '/', 
                            self._lines[item.dataIndex].totals[item.seriesIndex]
                        ].join('');
                    } else {
                        content = item.series.data[item.dataIndex][0];
                    }

                    self._showTooltip(item.pageX, item.pageY, content);

                    self._t = setTimeout(function() {
                        self._tooltip.fadeOut(100);
                    }, 3000);
                }
            } else {
                self._tooltip.fadeOut(100);
            }
        });
    };
    results.chart._showTooltip = function(x, y, contents) {
        var left = x - this._tooltip.width() - 5,
            w = this._element.width(),
            pw = this._plot.width();
            diff = w - pw + this._element.offset().left;

        this._tooltip
            .html(contents)
            .css({
                'top': y - 6,
                left: diff < left ? left : x + 5
            })
            .fadeIn(100);
    };

    /* HISTOGRAM */
    results.histogram = Object.create(results.chart);
    results.histogram._counts  = [];
    results.histogram._ids = [];
    results.histogram._countCol = 2;
    results.histogram._totalCol = 3;
    results.histogram._idCol = 1;
    results.histogram._detailsUrl = null;
    results.histogram._lines = [];
    results.histogram.parse = function(rows, writee) {
        var max = Math.min(rows.length, this._max);
        for (var i=0, row, count, total; (row=rows[i]); i++) {
            if (i == max) break;
            count = parseInt(row[this._countCol]);
            total = parseInt(row[this._totalCol]);
            this._lines.push([count, max-i]);
            this._labels[max-i-1] = [max-i, row[0]];
            this._counts[max-i-1] = [ 
                count, '/', total
            ].join(''); // because jqplot will sort y values but not associated count labels
            this._ids[max-i-1] = row[this._idCol];
        }
    };
    results.histogram.printHeader = function(header, writee) {
        this._xLabel = ['Number of ', header[this._idCol]].join('');
        this._yLabel = header[0];
    };
    results.histogram._doExport = function(form) {
        var data = [],
            lines = this._lines,
            n = this._lines.length,
            ck = [];

        while (n--) {
            data[n] = this._lines[n][0];
            ck[n] = biomart.stripHtml(this._labels[n][1]);
        }

        data = data.join(',');
        form.prepend(['<input type="hidden" name="d" value="', data, '"/>'].join(''));
        form.prepend(['<input type="hidden" name="sl" value="', this._yLabel, '"/>'].join(''));
        form.prepend(['<input type="hidden" name="cl" value="', this._xaxisLabel, '"/>'].join(''));
        //form.prepend(['<input type="hidden" name="ck" value="', ck.join(','), '"/>'].join(''));
        //using group separator to separate ck values
        form.prepend(['<input type="hidden" name="ck" value="', ck.join(String.fromCharCode(29)), '"/>'].join(''));
    };
    results.histogram.draw = function(writee) {
        if (!this._lines.length) {
            writee.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
            return;
        }
        if (this._hasError) return;

        this.initExport('bar');
        var x_options = this._getXOptions();

        this._attachEvents();

        this._element.css('height', (this._lines.length * 40 + 55) + 'px');

        this._plot = $.plot(this._element, [
            {
                data: this._lines,
                stack: null,
                bars: {
                    lineWidth: 0,
                    align: 'center',
                    show: true,
                    barWidth: .7,
                    horizontal: true
                }
            }
        ], {
            xaxis: x_options,
            yaxis: {
                min: 0,
                ticks: this._labels
            },
            grid: {
                clickable: true,
                hoverable: true,
                autoHighlight: true
            }
        });

        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    results.histogram._getXOptions = function() {
        var largestValue = this._lines[0][0],
            interval,
            x_ticks = [];

        if (largestValue < 20) interval = 1;
        else if (largestValue < 50) interval = 5;
        else if (largestValue < 100) interval = 10;
        else interval = ~~(largestValue / 100) * 10;

        for (var i=0; i<=largestValue; i+=interval) {
            x_ticks.push([i,i]);
        }
        
        return {ticks: x_ticks, min: 0, max: largestValue +1};
    };
    results.histogram._prevPt = null;
    results.histogram._t = null;
    results.histogram._attachEvents = function() {
        var self = this;
        this._element.bind('plothover', function (ev, pos, item) {
            if (item) {
                clearTimeout(self._t);
                if (this._prevPt != item.datapoint) {
                    this._prevPt = item.datapoint;

                    self._showTooltip(item.pageX, item.pageY, 
                            self._counts[self._counts.length-item.dataIndex-1], item.dataIndex);

                    self._t = setTimeout(function() {
                        self._tooltip.fadeOut(100);
                    }, 3000);
                }
            } else {
                self._tooltip.fadeOut(100);
            }
        });

        this._element.bind('plotclick', function (ev, pos, item) {
            if (item) {
                self._showDetails(self._labels[self._labels.length-item.dataIndex-1][1], 
                    self._ids[self._ids.length-item.dataIndex-1],
                    self._counts[self._counts.length-item.dataIndex-1], item.dataIndex);
            }
        });

        this._tooltip.bind('click.histogram', function() {
            var index = $(this).data('index');
            if (typeof index != 'undefined') {
                self._showDetails(self._labels[self._labels.length-index-1][1], 
                    self._ids[self._ids.length-index-1],
                    self._counts[self._counts.length-index-1], index);
            }
        });
    };
    results.histogram._showTooltip = function(x, y, contents, dataIndex) {
        var left = x - this._tooltip.width() - 5,
            w = this._element.width(),
            pw = this._plot.width();
            diff = w - pw + this._element.offset().left;

        this._tooltip
            .data('index', dataIndex)
            .text(contents + ' (click for details)')
            .css({
                'top': y - 6,
                left: diff < left ? left : x + 5
            })
            .fadeIn(100);
    };
    results.histogram._attrRegex = /\${.+?}/g;
    results.histogram._attrRegex2 = /\${(.+)}/;
    results.histogram._urlReplace = function(index) {
        var url = this._detailsUrl,
            matches = url.match(this._attrRegex),
            n = matches.length,
            match,
            actual;
        while (n--) {
            match = matches[n].match(this._attrRegex2)[1];
            if (match == 'dataset' ) {
                actual = this._rawData[index].dataset;
            } else {
                actual = biomart.stripHtml(this['_' + match][this['_' + match].length-index-1]);
            }
            url = url.replace(matches[n], actual);
        }
        return url.replace(/ /g, '');
    };
    results.histogram._showDetails = function(title, contents, counts, index) {
        var arr = [];
        if (this._detailsUrl) {
            arr.push(['<p style="float:right"><a rel="view-more" target="_blank" href="', this._urlReplace(index), '">', _('view_more_details', biomart.CAPITALIZE), '</a></p>'].join(''));
        }
        arr.push(['<p><strong>IDs returned for this dataset (', counts, '):</strong></p>'].join(''));
        arr.push(['<p class="ids">', contents, '</p>'].join(''));

        $('<div class="chart-details"/>').appendTo(document.body)
            .html(arr.join(''))
            .dialog({
                title: title,
                autoOpen: true,
                width: 500,
                height: 200,
                close: function() { $(this).dialog('destroy').remove() }
            });
    };
    
    /* SCATTER PLOT */
    results.scatterplot = Object.create(results.chart);
    results.scatterplot.tagName = 'div';
    results.scatterplot._keyMap = {};
    results.scatterplot._lines = [];
    results.scatterplot._lineIndices = [1];
    results.scatterplot._labels = [];
    results.scatterplot._max = 20;
    results.scatterplot._header = null;
    results.scatterplot.initExport = function(url) {};
    results.scatterplot._doExport = function(form) {};
    results.scatterplot.printHeader = function(header, writee) {
        this._header = header;
    };
    results.scatterplot.parse = function(rows, writee) {
    	if (!rows.length) return;
    	
    	// hard coded col value for now
    	var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	this._xaxisLabel = this._header[rowGeneID] + " " + rows[0][rowGeneID];
        for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
            row = rows[i];
            rawKey = row[rowCancerType],
            cleanedKey = typeof rawKey == 'string' ? biomart.stripHtml(rawKey) : rawKey;
            
            /*this._lines.push({
                key: cleanedKey,
                raw: rawKey,
                values: [],
                totals: []
            });*/
            
            index = this._lines.length - 1;
           
            var value1 = row[rowValue1],
            	value2 = row[rowValue2],
            	valueX = row[rowX],
            	valueID = row[rowID];
            if(value1 == "" || value2 == "" || valueX == "")
            	continue;
            var avg = (parseFloat(value1) + parseFloat(value2))/2;
            
            if(rawKey in this._lines){
            	
            }else{
            	this._lines[rawKey] = [];
            }
            this._lines[rawKey].push( [parseInt(valueX) , avg , valueID] );
        }
	
    };
    results.scatterplot._attachEvents = function() {
    	var self = this;
        this._element.bind('plothover', function (ev, pos, item) {
        	if (item) {
                if (previousPoint != item.dataIndex) {
                    previousPoint = item.dataIndex;
                    clearTimeout(self._t);
                    //$("#tooltip").remove();
                    var x = item.datapoint[0].toFixed(2),
                        y = item.datapoint[1].toFixed(2),
                        tooltip = '';
                    
                    for( var index = 0; index< item.series.data.length; index++){
                    	var d = item.series.data[index];
                    	if(item.datapoint[0] === d[0] && item.datapoint[1] === d[1]){
                    		tooltip = d[2];
                    		break;
                    	}                    		
                    }
                    
                    self._showTooltip(item.pageX, item.pageY,
                                tooltip + " ("+x+","+y+")");
                    
                    self._t = setTimeout(function() {
                        self._tooltip.fadeOut(100);
                    }, 3000);
                }
            }
            else {
            	self._tooltip.fadeOut(100);
                //$("#tooltip").remove();
                previousPoint = null;            
            }
        
        });
    };
   
    results.scatterplot.draw = function() {
    	if (this._hasError) return;
        this.initExport('plot');

        // sort by total
        //this._sort(false);

        //var topRows = this._lines.slice(0, this._max);
        var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	
		this._attachEvents();
		//set height for scatter plot render
		this._element.css('height', ( 200 + 155) + 'px');
		
        var chartLines = [];
        for (var key in this._lines) {
        	if(this._lines.hasOwnProperty(key)){
	        	chartLines.push({
	        		data : this._lines[key],
	        		label : key
	        	});
        	}
            //if (!chartLines[j]) chartLines[j] = { data: [] , label:''};
            //chartLines[0].data[j] = line.values;
            //chartLines[0].label = line.rawKey;
        }
        
        this._plot = $.plot(this._element, chartLines, {
            series: {
                points: { show: true }
            },
            xaxis: {},
            yaxis: {},
            grid: {
                clickable: true,
                hoverable: true,
                autoHighlight: true
            },
            legend: {
                margin: [5, 5],
                backgroundOpacity: .6,
                  show: true,
                position: 'ne'
            }
        });
        
        //var series = this._plot.getData();
        
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    
    /* BOX PLOT */
    results.boxplot = Object.create(results.chart);
    results.boxplot.tagName = 'div';
    results.boxplot._keyMap = {};
    results.boxplot._lines = [];
    results.boxplot._lineIndices = [1];
    results.boxplot._labels = [];
    results.boxplot._max = 20;
    results.boxplot._miny = 100;
    results.boxplot._maxy = 0;
    results.boxplot._header = null;
    results.boxplot.initExport = function(url) {};
    results.boxplot._doExport = function(form) {};
    results.boxplot.printHeader = function(header, writee) {
        this._header = header;
    };
    results.boxplot.parse = function(rows, writee) {
    	if (!rows.length) return;
    	
    	// hard coded col value for now
    	var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	this._xaxisLabel = this._header[rowGeneID] + " " + rows[0][rowGeneID];
		for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
			row = rows[i];
			rawKey = row[rowCancerType];
			
			index = i;

            var value1 = row[rowValue1],
        	value2 = row[rowValue2],
        	valueX = row[rowX];
            if(value1 == "" || value2 == "" || valueX == "")
            	continue;
            var avg = (parseFloat(value1) + parseFloat(value2))/2;
            
            if(rawKey in this._lines){
            	if(valueX in this._lines[rawKey]){
            		
            	}else{
            		this._lines[rawKey][valueX] = {
                			Group : [],
                			boxValue : [],
                			outliers : [],
                			means: []
                	};
            	}
            }else{
            	this._lines[rawKey] = new Array();
            	this._lines[rawKey][valueX] = {
            			Group : [],
            			boxValue : [],
            			outliers : [],
            			means : []
            	};
            }

            this._lines[rawKey][valueX].Group.push(avg);
            
		}
		
    };
   
    results.boxplot.draw = function() {
    	if (this._hasError) return;
        this.initExport('plot');

        // sort by total
        //this._sort(false);
      //calculate all values for box plot
		function sortNumber(a,b)
		{
			return a - b;
		}
		var index = 0;
		for( var key in this._lines){
			if(!this._lines.hasOwnProperty(key)){
				continue;
			}
			for( var xkey in this._lines[key]){
				if(!this._lines[key].hasOwnProperty(xkey)){
					continue;
				}
				this._lines[key][xkey].Group.sort(sortNumber);
	    		var size = this._lines[key][xkey].Group.length;
	    		
	    		if(this._lines[key][xkey].Group[0] < this._miny)
	    			this._miny = this._lines[key][xkey].Group[0];
	    		if(this._lines[key][xkey].Group[size-1] > this._maxy)
	    			this._maxy = this._lines[key][xkey].Group[size-1];
	    		
	    		index ++;
	    		//calculate means
	    		var mean = 0;
	    		for(var i=0;i<size;i++){
	    			mean += this._lines[key][xkey].Group[i];
	    		}
	    		mean = mean / size;
	    		this._lines[key][xkey].means.push(mean);
	    		//calculate outliers
	    		var Q1 = this._lines[key][xkey].Group[Math.floor(size/4)];
	    		var Q3 = this._lines[key][xkey].Group[Math.floor(size*3/4)];
	    		var IQR = Q3 - Q1;
	    		var lowerBound = 0;
	    		var upperBound = size - 1;
	    		for(var i =0; i< size/4; i++){
	    			if(this._lines[key][xkey].Group[i] < Q1 - 1.5 * IQR){
	    				if(this._lines[key][xkey].outliers.indexOf(this._lines[key][xkey].Group[i]) == -1){
	    					this._lines[key][xkey].outliers.push(this._lines[key][xkey].Group[i]);
	    				}
	    				lowerBound = i + 1;
	    			}else{
	    				break;
	    			}	    				
	    		}
	    		for(var i = size - 1; i> size*3/4; i--){
	    			if(this._lines[key][xkey].Group[i] > Q3 + 1.5 * IQR){
	    				if(this._lines[key][xkey].outliers.indexOf(this._lines[key][xkey].Group[i]) == -1){
		    				this._lines[key][xkey].outliers.push(this._lines[key][xkey].Group[i]);
	    				}
	    				upperBound = i - 1;
	    			}else{
	    				break;
	    			}
	    		}
	    		//calculate box values
	    		if(size == 0){
	    			this._lines[key][xkey].boxValue = [index, 0, 0 , 0, 0, 0];
	    		}else{
	    			this._lines[key][xkey].boxValue = [index,
	    			                                   this._lines[key][xkey].Group[lowerBound],
	    			                                   this._lines[key][xkey].Group[Math.floor(size/4)],
	    			                                   this._lines[key][xkey].Group[Math.floor(size/2)],
	    			                                   this._lines[key][xkey].Group[Math.floor(size*3/4)],
	    			                                   this._lines[key][xkey].Group[upperBound]];
	    		}
	    		
			}
    		
		}
        //var topRows = this._lines.slice(0, this._max);
        var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	
		//set height for box plot render
		this._element.css('height', 500 + 'px');
		var chartLines = [];
		var xTicks = [];
		var index = 0;
		for( var key in this._lines){
			if(this._lines.hasOwnProperty(key)){
				var chartLine = {
		        		data : [],
		        		label : key,
		        		outliers : [],
		        		means : []
		        };
				for(var xkey in this._lines[key]){
					if(this._lines[key].hasOwnProperty(xkey)){
						chartLine.data.push(this._lines[key][xkey].boxValue);
						chartLine.outliers.push(this._lines[key][xkey].outliers);
						chartLine.means.push(this._lines[key][xkey].means);
						index ++;
						xTicks.push([index,xkey + '(' + this._lines[key][xkey].Group.length + ')']);
					}
				}
    			chartLines.push(chartLine);
			}
		}

        this._plot = $.plot(this._element, chartLines, {
        	series : {
        		boxplot: {active : true, show : true, showOutliers : true, showMean : true}
        	},
            xaxis: {min: 0, max: index+1 , ticks: xTicks},
            yaxis: {min: this._miny - 1, max: this._maxy + 1, ticks : 20},
            grid: {
                clickable: true,
                hoverable: true,
                autoHighlight: true
            },
            legend: {
                margin: [5, 5],
                backgroundOpacity: .6,
                  show: true,
                position: 'ne'
            }
        });
        
	
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
        //reset min y and max y
        results.boxplot._miny = 100;
        results.boxplot._maxy = 0;
    };
    
    /* DOT PLOT */
    results.dotplot = Object.create(results.chart);
    results.dotplot.tagName = 'div';
    results.dotplot._keyMap = {};
    results.dotplot._lines = [];
    results.dotplot._lineIndices = [1];
    results.dotplot._labels = [];
    results.dotplot._max = 20;
    results.dotplot._header = null;
    results.dotplot.initExport = function(url) {};
    results.dotplot._doExport = function(form) {};
    results.dotplot.printHeader = function(header, writee) {
        this._header = header;
    };
    results.dotplot.parse = function(rows, writee) {
    	if (!rows.length) return;
    	
    	// hard coded col value for now
    	var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	this._xaxisLabel = this._header[rowGeneID] + " " + rows[0][rowGeneID];
		for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
			row = rows[i];
			rawKey = row[rowCancerType];
			
			index = i;

            var value1 = row[rowValue1],
        	value2 = row[rowValue2],
        	valueX = row[rowX],
            valueID = row[rowID];
            if(value1 == "" || value2 == "" || valueX == "")
            	continue;
            var avg = (parseFloat(value1) + parseFloat(value2))/2;
            
            if(rawKey in this._lines){
            	if(valueX in this._lines[rawKey]){
            		
            	}else{
            		this._lines[rawKey][valueX] = {
                			Group : [],
                			boxValue : []
                	};
            	}
            }else{
            	this._lines[rawKey] = new Array();
            	this._lines[rawKey][valueX] = {
            			Group : [],
            			boxValue : []
            	};
            }

            this._lines[rawKey][valueX].Group.push([avg, valueID]);
		}
	
    };
    results.dotplot._attachEvents = function() {
    	var self = this;
        this._element.bind('plothover', function (ev, pos, item) {
        	if (item) {
                if (previousPoint != item.dataIndex) {
                    previousPoint = item.dataIndex;
                    clearTimeout(self._t);
                    //$("#tooltip").remove();
                    var x = item.datapoint[0].toFixed(2),
                        y = item.datapoint[1].toFixed(2),
                        tooltip = '';
                    
                    for( var index = 0; index< item.series.data.length; index++){
                    	var d = item.series.data[index];
                    	if(item.datapoint[0] === d[0] && item.datapoint[1] === d[1]){
                    		tooltip = d[2];
                    		break;
                    	}                    		
                    }
                    
                    self._showTooltip(item.pageX, item.pageY,
                                tooltip + " ("+x+","+y+")");
                    
                    self._t = setTimeout(function() {
                        self._tooltip.fadeOut(100);
                    }, 3000);
                }
            }
            else {
            	self._tooltip.fadeOut(100);
                //$("#tooltip").remove();
                previousPoint = null;            
            }
        
        });
    };
    
    results.dotplot.draw = function() {
    	if (this._hasError) return;
        this.initExport('plot');

        // sort by total
        //this._sort(false);
        this._attachEvents();
        //var topRows = this._lines.slice(0, this._max);
        var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	
		//set height for box plot render
		this._element.css('height', 500 + 'px');
		var chartLines = [];
		var xTicks = [];
		var index = 0;
		for( var key in this._lines){
			if(this._lines.hasOwnProperty(key)){
				var chartLine = {
		        		data : [],
		        		label : key
		        };
				for(var xkey in this._lines[key]){
					if(this._lines[key].hasOwnProperty(xkey)){
						index ++;
						for(var i = 0; i<this._lines[key][xkey].Group.length; i++){
							chartLine.data.push([index, this._lines[key][xkey].Group[i][0], this._lines[key][xkey].Group[i][1]]);
						}
						
						xTicks.push([index,xkey]);
					}
				}
    			chartLines.push(chartLine);
			}
		}

        this._plot = $.plot(this._element, chartLines, {
        	series: {
                points: { show: true }
            },
            xaxis: {min: 0, max: index+1 , ticks: xTicks},
            yaxis: {},
            grid: {
                clickable: true,
                hoverable: true,
                autoHighlight: true
            },
            legend: {
                margin: [5, 5],
                backgroundOpacity: .6,
                  show: true,
                position: 'ne'
            }
        });
        
	
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    
    /* Bar chart */
    results.barchart = Object.create(results.chart);
    results.barchart.tagName = 'div';
    results.barchart._keyMap = {};
    results.barchart._lines = [];
    results.barchart._lineIndices = [1];
    results.barchart._labels = [];
    results.barchart._max = 20;
    results.barchart._header = null;
    results.barchart.initExport = function(url) {};
    results.barchart._doExport = function(form) {};
    results.barchart.printHeader = function(header, writee) {
        this._header = header;
    };
    results.barchart.parse = function(rows, writee) {
    	if (!rows.length) return;
    	
    	// hard coded col value for now
    	var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	this._xaxisLabel = this._header[rowGeneID] + " " + rows[0][rowGeneID];
        for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
            row = rows[i];
            rawKey = row[rowCancerType],
            cleanedKey = typeof rawKey == 'string' ? biomart.stripHtml(rawKey) : rawKey;
            
            index = this._lines.length - 1;
           
            var value1 = row[rowValue1],
            	value2 = row[rowValue2],
            	valueX = row[rowX],
            	valueID = row[rowID];
            if(value1 == "" || value2 == "" || valueX == "")
            	continue;
            var avg = (parseFloat(value1) + parseFloat(value2))/2;
            
            if(rawKey in this._lines){
            	if(valueX in this._lines[rawKey]){
            		
            	}else{
            		this._lines[rawKey][valueX] = {
                			Group : [],
                			boxValue : []
                	};
            	}
            }else{
            	this._lines[rawKey] = new Array();
            	this._lines[rawKey][valueX] = {
            			Group : [],
            			boxValue : []
            	};
            }

            this._lines[rawKey][valueX].Group.push([avg, valueID]);
        }
	
    };
    results.barchart._attachEvents = function() {
    	var self = this;
        this._element.bind('plothover', function (ev, pos, item) {
        	if (item) {
                if (previousPoint != item.dataIndex) {
                    previousPoint = item.dataIndex;
                    clearTimeout(self._t);
                    //$("#tooltip").remove();
                    var x = item.datapoint[0].toFixed(2),
                        y = item.datapoint[1].toFixed(2),
                        tooltip = '';
                    
                    for( var index = 0; index< item.series.data.length; index++){
                    	var d = item.series.data[index];
                    	if(item.datapoint[0] === d[0] && item.datapoint[1] === d[1]){
                    		tooltip = d[2];
                    		break;
                    	}                    		
                    }
                    
                    self._showTooltip(item.pageX, item.pageY,
                                tooltip + " ("+x+","+y+")");
                    
                    self._t = setTimeout(function() {
                        self._tooltip.fadeOut(100);
                    }, 3000);
                }
            }
            else {
            	self._tooltip.fadeOut(100);
                //$("#tooltip").remove();
                previousPoint = null;            
            }
        
        });
    };
   
    results.barchart.draw = function() {
    	if (this._hasError) return;
        this.initExport('plot');

        // sort by total
        //this._sort(false);

        //var topRows = this._lines.slice(0, this._max);
        var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5;
    	
		this._attachEvents();
		//set height for scatter plot render
		this._element.css('height', ( 200 + 155) + 'px');
		
        var chartLines = [];
		var xTicks = [];
		var isDigit = false;
		var categories = [];
		var index = 0;
		for( var key in this._lines){
			if(this._lines.hasOwnProperty(key)){
				var chartLine = {
		        		data : [],
		        		label : key
		        };
				
				for(var xkey in this._lines[key]){
					if(this._lines[key].hasOwnProperty(xkey)){
						var y=parseInt(xkey);
						isDigit = !isNaN(y);
						if (isNaN(y)){
							if(xkey in categories){
								
							}else{
								index ++;
								categories[xkey] = index;
							}
							for(var i = 0; i<this._lines[key][xkey].Group.length; i++){
								chartLine.data.push([categories[xkey], this._lines[key][xkey].Group[i][0],this._lines[key][xkey].Group[i][1]]);
							}
							xTicks.push([categories[xkey],xkey]);
						} else{
							for(var i = 0; i<this._lines[key][xkey].Group.length; i++){
								chartLine.data.push([y, this._lines[key][xkey].Group[i][0],this._lines[key][xkey].Group[i][1]]);
							}
						}
					}
				}
    			chartLines.push(chartLine);
			}
		}
		if(isDigit){
			this._plot = $.plot(this._element, chartLines, {
	            series: {
	            	stack: 0,
	                lines: {show: false, steps: false },
	                bars: {show: true, barWidth: 0.5, align: 'center'}
	            },
	            xaxis: {},
	            yaxis: {},
	            grid: {
	                clickable: true,
	                hoverable: true,
	                autoHighlight: true
	            },
	            legend: {
	                margin: [5, 5],
	                backgroundOpacity: .6,
	                  show: true,
	                position: 'ne'
	            }
	        });
		}else {
	        this._plot = $.plot(this._element, chartLines, {
	            series: {
	            	stack: 0,
	                lines: {show: false, steps: false },
	                bars: {show: true, barWidth: 0.5, align: 'center'}
	            },
	            xaxis: {min: 0, max: index+2 , ticks: xTicks},
	            yaxis: {},
	            grid: {
	                clickable: true,
	                hoverable: true,
	                autoHighlight: true
	            },
	            legend: {
	                margin: [5, 5],
	                backgroundOpacity: .6,
	                  show: true,
	                position: 'ne'
	            }
	        });
		}
        
        //var series = this._plot.getData();
        
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    
    results.barchart.clear = function() {
        this._lines = [];
        this._labels = [];
        this._donorIds = [];
        this._keyMap = {};
    };
    
    /* BIOHEATMAP */
    results.bioheatmap = Object.create(results.chart);
    results.bioheatmap.tagName = 'div';
    results.bioheatmap._max = 9;
    results.bioheatmap._min = 0;
    results.bioheatmap._keyMap = {};
    results.bioheatmap._maxXY = [];
    results.bioheatmap._lines = [];
    results.bioheatmap._xlabels = [];
    results.bioheatmap._ylabels = [];
    results.bioheatmap.stages = [];
    results.bioheatmap.patientStatus =[];
    results.bioheatmap.tissueType = [];
    results.bioheatmap._header = null;
    results.bioheatmap.initExport = function(url) {};
    results.bioheatmap._doExport = function(form) {};
    results.bioheatmap.printHeader = function(header, writee) {
        this._header = header;
    };
    results.bioheatmap._getColor = function(val) {
        var min = this._min,
            max = this._max,
            mid = (max + min) / 2;

        if(isNaN(val)) return 'rgb(255,255,255)';
        if (val > max) return 'rgb(255,0,0)';
        if (val < min) return 'rgb(0,0,255)';

        var r = this._getRed(val, min, mid, max),
            b = this._getBlue(val, min, mid, max),
            g = this._getGreen(val, min, mid, max);

        return ['rgb(', r, ',', g, ',', b, ')'].join('')
    };
    results.bioheatmap._getBlue = function(val, min, mid, max) {
        if (val >= 0) return 0;
        var range = Math.abs(min - mid);
        val = Math.abs(val - mid);
        return parseInt(val / range * 255);
    };
    results.bioheatmap._getGreen = function(val, min, mid, max) {
        if (val <= 0) return 0;
        var mid2 = (max + mid) / 2,
        val2 = Math.abs(mid2 - val);
        return 180 - parseInt(val2 / mid2 * 180);
    };
    results.bioheatmap._getRed = function(val, min, mid, max) {
        if (val <= 0) return 0;
        var mid2 = (max + mid) / 2;
        if (val >= mid2) return 255;
        return parseInt(val / mid2 * 255);
    };
    results.bioheatmap.clear = function() {
        this._lines = [];
        this._xlabels = [];
        this._ylabels = [];
        this._keyMap = {};
        this._maxXY = [];
        this.stages = [];
        this.patientStatus =[];
        this.tissueType = [];
    };
    results.bioheatmap.parse = function(rows, writee) {
	    var n = rows.length,
	        arr = [],
	        currVal;
	    if (!rows.length) return;
		
		// hard coded col value for now
		var rowCancerType = 0, rowValue1 = 1, rowValue2 = 2, rowX = 3, rowID = 4, rowGeneID = 5, tmaName=6;
		var stageCol = 8, outcomeCol = 9, patientStatusCol = 10, tissueTypeCol = 11, popcureCol=12;
		this._xaxisLabel = this._header[tmaName] + " " + rows[0][tmaName];
		
		for (var i=0, row, rawKey, cleanedKey, index, n=rows.length; i<n; i++) {
			row = rows[i];
			rawKey = row[rowCancerType];
			
			index = i;
	
	        var value1 = row[rowValue1],
	    	value2 = row[rowValue2],
	    	valueX = parseFloat(row[rowX]),
	        valueID = parseFloat(row[rowID]),
	        tooltipID = row[rowGeneID],
	        stageID = row[stageCol],
	        outcomeID = row[outcomeCol],
	        patientStatusID = row[patientStatusCol],
	        tissueTypeID = row[tissueTypeCol],
	        popcureID = row[popcureCol],
	        avg = (parseFloat(value1) + parseFloat(value2))/2;
	        
	        if(rawKey in this._lines){
	        }else{
	        	this._lines[rawKey] = new Array();
	        }
	        if(rawKey in this._maxXY){
	        	if(valueX > this._maxXY[rawKey][0]){
	        		this._maxXY[rawKey][0] = valueX;
	            }
	            if(avg > this._maxXY[rawKey][1]){
	            	this._maxXY[rawKey][1] = avg;
	            }
	        }else{
	        	this._maxXY[rawKey] = [valueX, avg];
	        }
	        
	        if(valueID > this._max)
	        	this._max = valueID;
	        if(valueID < this._min)
	        	this._min = valueID;
	        
	        if(rawKey in this.stages){
	        }else{
	        	this.stages[rawKey] = new Array();
	        }
	        if(rawKey in this.patientStatus){
	        }else{
	        	this.patientStatus[rawKey] = new Array();
	        }
	        if(rawKey in this.tissueType){
	        }else{
	        	this.tissueType[rawKey] = new Array();
	        }
	        this.stages[rawKey][valueX] = stageID;
	        this.patientStatus[rawKey][valueX] = patientStatusID;
	        this.tissueType[rawKey][valueX] = tissueTypeID;
	
	        this._lines[rawKey].push({
	        	x: valueX,
	        	y: avg,
	        	value: valueID,
	        	tooltip : tooltipID,
	        	stage : stageID,
	        	outcome : outcomeID,
	        	patientStatus : patientStatusID,
	        	tissueType : tissueTypeID,
	        	popcure : popcureID
	        });
	        this._xlabels[valueX] = tooltipID;
	        this._ylabels[avg] = stageID;
		}
	
    };
    results.bioheatmap._showTooltip = function(x, y, contents) {
        var left = x - 10,
            w = this._element.width(),
            pw = this._plot.width();
            diff = w - pw + this._element.offset().left;

        this._tooltip
            .html(contents)
            .css({
                'top': y - 6,
                left: diff < left ? left : x + 5
            })
            .fadeIn(100);
    };
    results.bioheatmap.onMouseMove = function(event) {
    	
    	var a = event.layerX;
    	var b = event.layerY;
    	var tooltipx = event.pageX;
    	var tooltipy = event.pageY;
    	var rectH = 20;
    	var rectW = 30;
    	var gap = 1;
    	var scale = 40;
    	var numCat = 0;
    	var preX = 0;
    	var preY = 0;
	
    	for(var category in results.bioheatmap._lines){
        	if(results.bioheatmap._lines.hasOwnProperty(category)){
        		
        		for(var data in results.bioheatmap._lines[category]){
                	if(results.bioheatmap._lines[category].hasOwnProperty(data)){
                		var x = results.bioheatmap._lines[category][data].x * (rectW+gap) + (preX+gap)*rectW*numCat  - rectW-gap;
                		var y = (results.bioheatmap._lines[category][data].y + 3) * (rectH+gap) ;
                		var stageY =  1 * (rectH+gap) ;
                		var patientStatusY =  2 * (rectH+gap) ;
                		var tissueTypeY = 3 * (rectH+gap) ;
                		
                		if(a > x && a < x + rectW 
                				&& b > stageY && b < stageY + rectH){
                			var content = results.bioheatmap._lines[category][data].stage;
                			results.bioheatmap._showTooltip(tooltipx,tooltipy,content);
                			return;
                		}
                		if(a > x && a < x + rectW 
                				&& b > patientStatusY && b < patientStatusY + rectH){
                			var content = results.bioheatmap._lines[category][data].patientStatus;
                			results.bioheatmap._showTooltip(tooltipx,tooltipy,content);
                			return;
                		}
                		if(a > x && a < x + rectW 
                				&& b > tissueTypeY && b < tissueTypeY + rectH){
                			var content = results.bioheatmap._lines[category][data].tissueType;
                			results.bioheatmap._showTooltip(tooltipx,tooltipy,content);
                			return;
                		}
                		if(a > x && a < x + rectW 
                				&& b > y && b < y + rectH){
                			var content = results.bioheatmap._lines[category][data].value;
                			results.bioheatmap._showTooltip(tooltipx,tooltipy,content);
                			return;
                		}
                	}
        		}
        		numCat ++;
        		preX = results.bioheatmap._maxXY[category][0];
				preY = results.bioheatmap._maxXY[category][1] + 3;
        	}
    	}
    	results.bioheatmap._tooltip.fadeOut(100);
    };
    results.bioheatmap.draw = function(writee) {
    	if (this._hasError) return;

        if (!this._lines || !this._lines.length) {
            writee.html(['<p class="empty">', _('no_results'), '</p>'].join(''));
            return;
        }

        if (!this._lines.length) {
            writee.parent().parent().html(['<p class="empty">', _('no_results'), '</p>'].join(''));
            return;
        }
        
        results.bioheatmap._max = 9;
        results.bioheatmap._min = 0;

        writee.find('div.heat-box').tipsy({
            fade: true,
            gravity: 'w',
            opacity: .9
        });
        //define color map used for drawing heatmap covariates
        var colorMap = [];
        colorMap['Tumor'] = "Red";
        colorMap['Control'] = "Orange";
        colorMap['Adenoma'] = "Yellow";
        colorMap['Metastasis'] = "Grey";
        colorMap['Tumor Edge'] = "Pink";
        colorMap['Metastasis Edge'] = "Blue";
        colorMap['Normal'] = "Green";
        colorMap['I'] = this._getColor(0.2 * this._max);
        colorMap['II'] = this._getColor(0.4 * this._max);
        colorMap['III'] = this._getColor(0.6 * this._max);
        colorMap['IV'] = this._getColor(0.8 * this._max);
        colorMap['99'] = this._getColor(this._max);
        colorMap['Dead of disease'] = "Red";
        colorMap['Alive-No evidence of this tumour'] = "Green";
        colorMap['Dead unknown cause'] = "Grey";
        colorMap['Dead of other causes'] = "Brown";
        colorMap['Alive with disease'] = "Orange";
        colorMap['Unknown'] = "Black";
        colorMap['Alive-Evidence of this tumour'] = "Blue";
        colorMap['Dead - unknown cause'] = "Grey";
        // Use canvas to draw the legend
        var legend,
        	tmamap,
        	tmacanvas = $('<canvas id="tmacanvas"/>'),
            canvas = $('<canvas id="legend"/>'),
            ctx,
            grad,
            x1,
            y1,
            x2,
            x2,
            color1 = this._getColor(this._min),
            color2 = this._getColor(0.5 * (this._min+this._max)),
            color3 = this._getColor(0.75 * (this._min+this._max)),
            color4 = this._getColor(this._max),
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

        tmamap = $('<div class="bioheatmap"/>')
        	.append(tmacanvas)
        	.disableSelection()
        	.appendTo(writee);
        
        //draw heat map
        var preX = 0;
    	var preY = 0;
    	var maxX = 0;
    	var maxY = 0;
    	var rectH = 20;
    	var rectW = 30;
    	for(var category in this._lines){
        	if(this._lines.hasOwnProperty(category)){
        		maxX += this._maxXY[category][0];
        		maxY = this._maxXY[category][1] + 3;
        	}
    	}
        
        this._plot = tmamap;
        tmacanvas = tmacanvas.get(0);
        x1=0; y1=0; x2=300 * this._lines.length; y2=500;
        tmacanvas.width = x2;
        tmacanvas.height = y2;
        this._element.css('width', x2 + 'px');
        this._element.css('height', y2 + 'px');
        
        this._tooltip.css('background-color',"white");
        
        if (typeof G_vmlCanvasManager != 'undefined')
        	tmacanvas = G_vmlCanvasManager.initElement(tmacanvas);
 
        tmacanvas.onmousemove = this.onMouseMove;
    	
    	
    	var gap = 1;
    	var scale = 40;
    	var numCat = 0;
    	var shift = 15;
    	var labelX = [];
    	var labelY = ["","Stage","Patient status", "Tissue type"];
    	var context = tmacanvas.getContext('2d');
        for(var category in this._lines){
        	if(this._lines.hasOwnProperty(category)){
        		
        		for(var data in this._lines[category]){
                	if(this._lines[category].hasOwnProperty(data)){
                		//draw stage, patient status and tissue type chart on top
                		var style = colorMap[this._lines[category][data].stage];
                 		context.fillStyle = style;
            			context.fillRect(this._lines[category][data].x*(rectW+gap) + (preX+gap)*rectW*numCat - rectW-gap,
            					1 * (rectH+gap),rectW,rectH);
            			style = colorMap[this._lines[category][data].patientStatus];
            			context.fillStyle = style;
            			context.fillRect(this._lines[category][data].x*(rectW+gap) + (preX+gap)*rectW*numCat - rectW-gap,
            					2 * (rectH+gap),rectW,rectH);
            			style = colorMap[this._lines[category][data].tissueType];
            			context.fillStyle = style;
            			context.fillRect(this._lines[category][data].x*(rectW+gap) + (preX+gap)*rectW*numCat - rectW-gap,
            					3 * (rectH+gap),rectW,rectH);
            			
                		var x = this._lines[category][data].x * (rectW+gap) + (preX+gap)*rectW*numCat - rectW-gap;
                		var y = (this._lines[category][data].y + 3) * (rectH+gap) ;
            			labelX[this._lines[category][data].x] = this._lines[category][data].tooltip;
            			labelY[this._lines[category][data].y+3] = this._lines[category][data].outcome;
            			value = this._lines[category][data].value;
            			// draw the heatmap rect            			
            			context.fillStyle = this._getColor(value);
            			context.fillRect(x,y,rectW,rectH);
            			context.fill();
                	}
        		}
        		numCat ++;
        		preX = this._maxXY[category][0];
				preY = this._maxXY[category][1] + 3;
        	}
        }
    
        //draw x and y labels
        context.font = "10px sans-serif";
        context.textBaseline = "middle";
        context.fillStyle = "Black";
        
        for(var i=1;i <= maxY; i++){
        	context.fillText(labelY[i], maxX*(rectW+gap)+ gap*rectW*(numCat-1)  - rectW-gap, (i+0.5)*(rectH+gap));
        }
        numCat = 0;
        for(var category in this._lines){
        	if(this._lines.hasOwnProperty(category)){
        		var curX = this._maxXY[category][0];
        		for(var i=1; i <= curX; i++){
        			context.save();
                	context.translate(i*(rectW+gap)+ rectW/2 + (preX+gap)*rectW*numCat  - rectW-gap, (maxY+1)*(rectH+gap));
                    context.rotate(Math.PI/2);
                	context.fillText(this._lines[category][i-1].tooltip, 0, 0);
                	context.restore();
        		}
        		preX = curX;
        		numCat ++;
        		
        	}
        }
        /*
        for(var i=1;i <= maxX; i++){
        	context.save();
        	context.translate(i*(rectW+gap)+ rectW/2, (maxY+1)*(rectH+gap));
            context.rotate(Math.PI/2);
        	context.fillText(labelX[i], 0, 0);
        	context.restore();
        }*/

        
        legend = $('<div class="heat-legend"/>')
            .append(canvas)
            .append(['<div class="max">', this._max, '</div>'].join(''))
            .append(['<div class="mid">', this._mid, '</div>'].join(''))
            .append(['<div class="min">', this._min, '</div>'].join(''))
            .append(['<p>', heading, '</p>'].join(''))
            .disableSelection();

        $('<div class="heat-legend-wrap"/>')
            .insertAfter(writee)
            .append(legend);        
        

        canvas = canvas.get(0);
        x1 = 0; y1 = 0; x2 = 200; y2 = 20;
        canvas.width = x2;
        canvas.height = y2;

        if (typeof G_vmlCanvasManager != 'undefined')
            canvas = G_vmlCanvasManager.initElement(canvas);

        
        if (canvas.getContext('2d')) {
            ctx = canvas.getContext('2d');
            //create gradient color bar
            grad = ctx.createLinearGradient(x1, y1, x2, y1);
            grad.addColorStop(0, color1);
            grad.addColorStop(.5, color2);
            grad.addColorStop(.75, color3);
            grad.addColorStop(1, color4);
            ctx.fillStyle = grad;
            ctx.fillRect(x1, y1, x2, y2);
        }
        
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
})(jQuery);
