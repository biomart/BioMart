(function($) {
    var results = biomart.renderer.results;
    
    /* Marrie map  */
    results.marriemap = Object.create(results.chart);
    results.marriemap.tagName = 'div';
    results.marriemap._keyMap = {};
    results.marriemap._lines = [];
    results.marriemap._lineIndices = [1];
    results.marriemap._labels = [];
    results.marriemap._max = 20;
    results.marriemap._header = null;
    results.marriemap.initExport = function(url) {};
    results.marriemap._doExport = function(form) {};
    results.marriemap.printHeader = function(header, writee) {
        this._header = header;
    };
    results.marriemap.parse = function(rows, writee) {
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
    results.marriemap._attachEvents = function() {
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
   
    results.marriemap.draw = function() {
    	new CanvasXpress(this._element,
                {y: {vars:  ['Gene1',  'Gene2', 'Gene3',  'Gene4', 'Gene5',  'Gene6'],
                     smps:  ['Smp1',   'Smp2',  'Smp3',   'Smp4',  'Smp5',   'Smp6',  'Smp7',   'Smp8'],
                     desc:  ['Intensity'],
                     data: [[10, 12, 3, 4, 100, 73, 42, 64],
                            [12, 4, 60, 5, 24, 14, 32, 13],
                            [7, 12, 20, 13, 49, 52, 42, 92],
                            [21, 10, 30, 8, 65, 166, 47, 58],
                            [15, 14, 100, 5, 34, 30, 82, 51],
                            [100, 82, 73, 4, 3, 4, 5, 2]]}},
                {graphType: 'Correlation',
                 title: 'Correlation Plot',
                 gradient: true,
                 scaleLegendFontFactor: 0.5,
                 bins: 10,
                 indicatorWidth: 2});
        
        if (this._xaxisLabel) {
            $(['<p class="plot-label">', this._xaxisLabel, '</p>'].join(''))
                .width(this._plot.width())
                .appendTo(this._element);
        }
    };
    
    results.marriemap.clear = function() {
        this._lines = [];
        this._labels = [];
        this._donorIds = [];
        this._keyMap = {};
    };

})(jQuery);