/*
 * The MIT License

Copyright (c) 2010 by Long Yao

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
/*
Flot plugin for boxplot data sets

  series: {
    boxplot: { active: false,
             show: false,
             barHeight: 2
    }
  }
data: [

  $.plot($("#placeholder"), [{ data: [ ... ], boxplot:{show: true} }])

*/

(function ($) {
    var options = {
        series: {
                        boxplot: {active: false
                                , show: false
                                , showOutliers: true
                                , lineWidth: 2
                                , showMean: false
								, connectSteps: { show: false, lineWidth:2, color:"rgb(0,0,0)" }
                                , barHeight: 30
								, highlight: { opacity: 0.5 }
								, drawstep: drawStepDefault
                        }
                }
    };
    var  data = null, canvas = null, target = null, axes = null, offset = null, highlights = [];
 	function drawStepDefault(ctx,series,data,x,min,q1,mid,q2,max,color, isHighlight)
	{
        ctx.beginPath();
        ctx.strokeStyle = color;
        ctx.fillStyle = "rgba(255,255,255,0.9)";
        ctx.lineWidth = series.boxplot.lineWidth;
        
        ctx.moveTo(x-series.boxplot.barHeight /2, min);
        ctx.lineTo(x+series.boxplot.barHeight /2, min);
        ctx.stroke();
        
		ctx.moveTo(x, min);
        ctx.lineTo(x, q1);
        ctx.stroke();
        
        ctx.moveTo(x, q2);
        ctx.lineTo(x, max);
        ctx.stroke();
        
        ctx.moveTo(x-series.boxplot.barHeight /2, max);
        ctx.lineTo(x+series.boxplot.barHeight /2, max);
        ctx.stroke();
        
        ctx.fillRect(x-series.boxplot.barHeight/2, q1, series.boxplot.barHeight, mid-q1);
        ctx.fillRect(x-series.boxplot.barHeight/2, mid, series.boxplot.barHeight, q2-mid);
        ctx.strokeRect(x-series.boxplot.barHeight/2, q1, series.boxplot.barHeight, mid-q1);
        ctx.strokeRect(x-series.boxplot.barHeight/2, mid, series.boxplot.barHeight, q2-mid);
	}
    function init(plot) 
	{	plot.hooks.processOptions.push(processOptions);
        function processOptions(plot,options)
        {   if (options.series.boxplot.active)
            {   plot.hooks.draw.push(draw);
                plot.hooks.bindEvents.push(bindEvents);
                plot.hooks.drawOverlay.push(drawOverlay);
            }
        }
       	function draw(plot, ctx)
        {   var series;
            canvas = plot.getCanvas();
            target = $(canvas).parent();
            axes = plot.getAxes();           
            offset = plot.getPlotOffset();   
            data = plot.getData();
            for (var i = 0; i < data.length; i++)
            {	
            	series = data[i];
				if (series.boxplot.show) 
				{	for (var j = 0; j < series.data.length; j++)
					{
						drawData(ctx,series, series.data[j], series.color,false);
						if(series.boxplot.showOutliers){
							drawOutliers(ctx,series,series.data[j], series.outliers[j], series.color,false);
						}
						if(series.boxplot.showMean) {
							drawMeans(ctx,series,series.data[j],series.means[j],series.color,false);
						}
					}
					if(series.boxplot.connectSteps.show)
					{	
						drawConnections(ctx,series);
					}
                }
            }
     	}
        function drawData(ctx,series,data,color,isHighlight)
        {	
        	var min,q1,mid,q2,max;
        	min = offset.top + axes.yaxis.p2c(data[1]);
        	q1 = offset.top + axes.yaxis.p2c(data[2]);
        	mid = offset.top + axes.yaxis.p2c(data[3]);
        	q2 = offset.top + axes.yaxis.p2c(data[4]);
        	max = offset.top + axes.yaxis.p2c(data[5]);
        	
            x = offset.left + axes.xaxis.p2c(data[0]);
			
			series.boxplot.drawstep(ctx,series,data,x,min,q1,mid,q2,max,color,isHighlight);
        }
        function drawOutliers(ctx,series,data,outliers,color,isHighlight)
        {
        	var radius = series.points.radius;
        	var x = offset.left + axes.xaxis.p2c(data[0]);
        	for(var i = 0; i <outliers.length; i++){
        		var y = offset.top + axes.yaxis.p2c(outliers[i]);
        		
        		ctx.beginPath();
        		ctx.strokeStyle = color;
                ctx.lineWidth = series.boxplot.lineWidth;
        		ctx.arc(x,y,radius, 0, Math.PI*2,true);
        		ctx.closePath();
        		
        		ctx.stroke();
        	}
        }
        function drawMeans(ctx,series,data,means,color,isHighlight)
        {
        	var radius = series.points.radius*2;
        	var x = offset.left + axes.xaxis.p2c(data[0]);
        	for(var i=0; i<means.length; i++){
        		var y = offset.top + axes.yaxis.p2c(means[i]);
        		
        		ctx.beginPath();
        		ctx.strokeStyle = color;
        		ctx.moveTo(x - radius, y - radius);
        		ctx.lineTo(x + radius, y + radius);
        		ctx.moveTo(x - radius, y + radius);
        		ctx.lineTo(x + radius, y - radius);
        		ctx.closePath();
        		
        		ctx.stroke();
        	}
        }
		function drawConnections(ctx,series)
		{	for(var i = 0; i < series.length; i++)
			{	
				
				var x = offset.left + axes.xaxis.p2c(series.data[i][0])
			       ,min = axes.yaxis.p2c(series.data[i][1])
				   ,q1 = axes.yaxis.p2c(series.data[i][2])
				   ,q2 = axes.yaxis.p2c(series.data[i][4])
				   ,max = axes.yaxis.p2c(series.data[i][5]);
				drawConnection(ctx,x,min,q1,series.boxplot.connectSteps.lineWidth,series.color);
				drawConnection(ctx,x,q2,max,series.boxplot.connectSteps.lineWidth,series.color);
			
			
			}
		}
		function drawConnection(ctx,x,y,y2,lineWidth,color)
		{	ctx.beginPath();
			ctx.lineWidth = lineWidth;
			ctx.strokeStyle = color;
			ctx.moveTo(x, y);
			ctx.lineTo(x, y2);
			ctx.stroke();
		}
        function bindEvents(plot, eventHolder)
        {   var r = null;
            var options = plot.getOptions();
            var hl = new HighLighting(plot, eventHolder, findNearby, options.series.boxplot.active,highlights)
        }
        function findNearby(plot,mousex, mousey)
		{	var series, r;
            axes = plot.getAxes();
            for (var i = 0; i < data.length; i++) 
			{	series = data[i];
                if (series.boxplot.show)
				{	for (var j = 0; j < series.data.length; j++) 
					{	var dataitem = series.data[j];
                        var dx = axes.xaxis.p2c(dataitem[0])
						  , dx2 = axes.xaxis.p2c(dataitem[2])
                          , dy = Math.abs(axes.yaxis.p2c(dataitem[1]) - mousey);
                        if (dy <= series.boxplot.barheight / 2) 
						{	if (mousex >= dx && mousex <= dx2)
							{	r = { i: i, j: j }; }
						}
					}
            	}
        	}
         	return r;
    	}
        function drawOverlay(plot, octx)
        { 	octx.save();
            octx.clearRect(0, 0, target.width, target.height);
            for (i = 0; i < highlights.length; ++i) 
			{	drawHighlight(highlights[i]);}
            octx.restore();
            function drawHighlight(s)
			{	var c = "rgba(255, 255, 255, " + s.series.boxplot.highlight.opacity + ")";
               	drawData(octx, s.series, s.point, c,true);
         	}
      	}
    }
    $.plot.plugins.push({
        init: init,
        options: options,
        name: 'boxplot',
        version: '0.1'
    });
})(jQuery);
