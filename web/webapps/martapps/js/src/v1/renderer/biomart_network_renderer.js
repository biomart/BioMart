;(function (d3) {
"use strict";
// NETWORK
biomart.networkRendererConfig = {
    graph: {
        nodeClassName: 'network-bubble',
        edgeClassName: 'network-edge',
        radius: function (d) {
            return 20//5 + d.radius
        },
        "id": function (d) {
            return d._id
        }
    },

    force: {
        linkDistance: function(link) {
            // return link.source.weight + link.target.weight > 8 ? 200 : 100
            if (link.source.weight > 4 ^ link.target.weight > 4)
                return 100
            if (link.source.weight > 4 && link.target.weight > 4)
                return 200
            return 50
        },
        charge: -300,
        gravity: 0.01, // 0.175
        threshold: 0.005,
        cluster: {
            padding: 60
        }
    },

    text: {
        className: 'network-text',
        'doubleLayer': { 'className': 'network-shadow' },
        callback: textCallback,
        link: function (d) {
            return d._link
        },
        text: function (d) {
            return d._id
        }
    }
}

// biomart.networkRendererConfig.force.linkDistance = 20
biomart.networkRendererConfig.force.charge = function (d) {
    // return d.isHub ? 10 * d.weight * d.x/1000 : -10 * d.weight * d.x/1000
    return d.isHub ? 4 * d.weight : -2 * d.weigth
}

// ENRICHMENT
biomart.enrichmentRendererConfig = {
    graph: {
        nodeClassName: function (d) {
            return "isHub" in d ? "annotation-bubble" : "network-bubble"
        },
        edgeClassName: 'enrichment-edge',
        radius: function (d) {
            return 5 + d.radius
        },
        'id': function (d) {
            return d._id
        }
    },

    force: {
        linkDistance: 0,
        charge: 0,
        gravity: 0, // 0.175
        threshold: 0.005,
        cluster: {
                padding: 60
        }
    },

    text: {
        className: 'network-text',
        'doubleLayer': { 'className': 'network-shadow' },
        callback: textCallback,
        link: function (d) {
                return d._link
        },
        text: function (d) {
            return 'description' in d ? d.description : d._id
        }
    }
}



function textCallback (_, config) {
    var keys = ['font-family', 'font-size', 'stroke', 'stroke-width', 'text-anchor']
    var attrs = {}
    keys.forEach(function (k) {
        if (k in config)
            this[k] = config[k]
    }, attrs)

    if ('className' in config)
        attrs['class'] = config.className

    // `this` will be the selection this cb is invoked on
    var textGroup = this.append('svg:g')

    // This could be improved returning a different func
    // chosen by the doubleLayer param
    if (config.doubleLayer) {
        textGroup.append('svg:text')
            .attr(attrs)
            .attr('class', config.doubleLayer.className)
            .text(config.text)
    }

    textGroup.append('svg:text')
        .attr(attrs)
        .text(config.text)
}

function hyperlinks (svg, data, config) {
    var update = svg.selectAll('a')
        .data(data)

    var a = update.enter()
        .append('svg:a')
        .attr({
            'xlink:href': config.link,
            target: '_blank'
        })

    if (config.callback)
        a.call(config.callback, config)

    update.exit().remove()

    return a
}

function makeText (svg, nodes, config) {
    return hyperlinks(svg, nodes, config).selectAll('g')
}

// ## Force
//
// *    nodes  - `Array`
// *    edges  - `Array`
// *    config - `Object`
//              *       size
//              *       gravity
//              *       linkDistance
//              *       charge
//              *       tick
//
var Force = (function (d3) {

    "use strict";

    function make (nodes, edges, config) {
        var force = d3.layout.force()
            .nodes(nodes)
            .links(edges)
            .size(config.size)
            .theta(config.theta || 2)
            .gravity(config.gravity)
            .linkDistance(config.linkDistance) // px
            // .linkStrength(cs.linkStrength)
            .charge(config.charge)

        force.on("tick", config.tick)

        return force
    }

    return make

})(d3);

function makeForce (nodes, edges, config) {
    // Create the layout and place the bubbles and links.
    return Force(nodes, edges, config)
}
// ## Graph
//
// *    svg     - `Object` d3 selection of an svg.
// *    nodes   - `Array`  Of objects.
// *    edges   - `Array`  Of objects of the form `{ source: a, target: b }`. Where `a` and `b` ara integers.
//      See [d3.force.links()](https://github.com/mbostock/d3/wiki/Force-Layout#wiki-links).
// *    config  - `Object` Containes the configuration for the graph.
//      *       radius: bubble's radius
//      *       nodeClassName - Optional: class for a bubble
//      *       fill - Optional : color for a bubble
//      *       edgeClassName - Optional: class for a link
//
//
// All the attributes are d3 style: value or callback(d, i).
var Graph = (function (d3) {

    "use strict";

    function makeLines(svg, edges, config) {
        // Update
        var update = svg.selectAll('line')
            .data(edges)

        var attrs = {}

        if ('edgeClassName' in config)
            attrs['class'] = config.edgeClassName

        // Enter
        var lines = update.enter()
            .append('svg:line')
            .attr(attrs)

        lines.each(function (d) {
            var w = 'value' in d ? d.value * 100 : 1
            if (w > 7) w = 7
            if (w < 1) w = 1
            d3.select(this).style('stroke-width', w)
        })

        // Exit
        update.exit()
            .remove()

        return lines
    }

    // A group with a circle and a text for each data.
    function makeBubbles(svg, nodes, config) {
        var update = svg.selectAll('circle')
            .data(nodes)

        update.exit()
            .remove()

        var attrs = { r: config.radius }

        if ('fill' in config)
            attrs.fill = config.fill
        if (config.hasOwnProperty('id'))
            attrs['id'] = config['id']
        if ('nodeClassName' in config)
            attrs['class'] = config.nodeClassName

        var bubbles = update.enter()
            .append('svg:circle')
            .attr(attrs)

        return bubbles
    }

    function graph (svg, nodes, edges, config) {
        var group = svg
        if ('groupId' in config) {
            group = d3.select('#'+config.groupId).empty()
                ? svg.append('svg:g')
                : d3.select('#'+config.groupId)
            group.attr('id', config.groupId)
        }
        return {
            links: makeLines(group, edges, config),
            bubbles: makeBubbles(group, nodes, config)
        }
    }

    return graph

})(d3);
/**
 * @param config - Object configuration for the graph only
**/
function makeGraph (svg, nodes, edges, config) {
    // Draw the graph chart without positioning the elements, and return
    // bubbles and links: { bubbles: ..., links: ... }
    var graphChart = Graph(svg, nodes, edges, config)
    graphChart.bubbles.on('mouseover', function () {
            this.__radius__ || (this.__radius__ = +this.getAttribute('r'))
            d3.select(this)
                .transition()
                .attr('r', this.__radius__ * 2) })
        .on('mouseout', function () {
            d3.select(this)
                .transition()
                .attr('r', this.__radius__)
        })

    return graphChart
}

var maxDomain = 20000
var d3colorScale = null
var hubFromColor = null
var hubNodes = null
var padding = null
var maxRadius = null
var width = null
var height = null
var config = null
var nodes = null
var wk = null

/**
 * Using the BFS visit, find the value of the color property of the first node
 * reachable that has it.
 *
 * @param {BaseNetworkRenderer} renderer - renderer on which do the search.
 * @param {Object} root - node from which starts the search.
 * @return {String|null} the value of the color property or null if no node
 * reachable from root has a color defined.
 */
function searchColor(renderer, root) {
    var queue = [root], q, adj, nidx, anode

    while(queue.length) {
        q = queue.pop()
        q._visited = true
        nidx = renderer.nodes.indexOf(q)
        adj = renderer.getNeighbors(nidx)
        for (var n = 0, len = adj.length; n < len; ++n) if ('color' in (anode = adj[n])) {
                return anode.color
            } else if (!('_visited' in anode) && queue.indexOf(anode) < 0) {
                queue.push(anode)
            }
    }

    // No color found for this node (a hub?)
    return null;
}

function colorScale(n) {
    d3colorScale || (d3colorScale = d3.scale.linear().domain([0, maxDomain]).range(['#ff0000', '#00ffff']))
    return d3colorScale(n)
}

function markHub (node) {
    var color = colorScale(Math.random() * maxDomain)
    node.color = color
    node.isHub = true
    hubFromColor[color] = node
}

// Modified version of http://bl.ocks.org/mbostock/1748247
// Move d to be adjacent to the cluster node.
function clusterHelper(alpha) {
    return function(d) {
        var l, r, x, y, k = 1.5, node = hubFromColor[d.color]

        // For cluster nodes, apply custom gravity.
        if (d === node) {
            node = {x: width / 2, y: height / 2, radius: -d.radius}
            k = wk * Math.sqrt(d.radius)
        }

        // I need them gt zero or they'll have the same value while deciding the bubble position.
        // Same position causes problems with further recomputation of this func.
        // if ((x = d.x - node.x) < 0) x = node.x - d.x
        // if ((y = d.y - node.y) < 0) y = node.y - d.y
        x = d.x - node.x
        y = d.y - node.y
        // distance between this node and the hub
        l = Math.sqrt(x * x + y * y)
        r = 2 * (node.radius + d.radius) //('radius' in node ? node.radius : radius)
        // if distance !== from sum of the two radius, that is, if they aren't touching
        if (l != r) {
            l = (l - r) / l * alpha * k
            // move this node towards the hub of some amount
            d.x -= x *= l
            d.y -= y *= l
            node.x += x
            node.y += y
        }
    }
}

// Resolves collisions between d and all other circles.
function collide(alpha) {
    var quadtree = d3.geom.quadtree(nodes)
    var padding = config.force.cluster.padding
    return function(d) {
        var r = 2 * (d.radius + maxRadius) + padding
        var nx1 = d.x - r
        var nx2 = d.x + r
        var ny1 = d.y - r
        var ny2 = d.y + r
        quadtree.visit(function(quad, x1, y1, x2, y2) {
            if (quad.point && (quad.point !== d)) {
                var x = d.x - quad.point.x
                var y = d.y - quad.point.y
                var l = Math.sqrt(x * x + y * y)
                var r = 2 * (d.radius + quad.point.radius) + (d.color !== quad.point.color) * padding + padding
                if (l < r) {
                    l = (l - r) / l * alpha
                    d.x -= x *= l
                    d.y -= y *= l
                    quad.point.x += x
                    quad.point.y += y
                }
            }
            return x1 > nx2 || x2 < nx1 || y1 > ny2 || y2 < ny1
        })
    }
}

function tick2 (attrs) {
    var renderer = attrs.renderer, bubbles = attrs.graph.bubbles,
        links = attrs.graph.links

    wk = attrs.wk
    config = attrs.config
    nodes = bubbles.data()
    width = config.force.size[0]
    height = config.force.size[1]
    padding = config.force.cluster.padding
    hubFromColor = {}
    hubNodes = attrs.hubs
    maxRadius = typeof config.graph.radius === 'number'
        ? config.graph.radius
        : d3.max(nodes, config.graph.radius)

    hubNodes.forEach(function (h) {
        markHub(h)
    })

    // Give colors to nodes based on clusters
    nodes.forEach(function (node) {
        if ((node.color = searchColor(renderer, node)) === null) {
            // If no color was given, assign random color.
            // With the current hub selection algorithm, this can happen when there
            // are strongly connected components with few edges.
            markHub(node)
            hubNodes.push(node)
        }
    })

    return function(evt) {
        bubbles
            .each(clusterHelper(10 * evt.alpha * evt.alpha))
            .each(collide(0.5))
    }
}

function cluster (attrs) {
    attrs.config.force.tick = tick2(attrs)
}


function hubs(edges, nodes) {
    var freq = [], m, hubs = []
    var degs = function (edge) {
        freq[edge.source] = freq[edge.source] ? freq[edge.source] + 1 : 1
        freq[edge.target] = freq[edge.target] ? freq[edge.target] + 1 : 1
    }

    edges.forEach(degs)
    m = d3.quantile(freq.slice(0).sort(), 0.98)
    freq.forEach(function (f, i) {
        if (f >= m)
            hubs.push(nodes[i])
    })

    return hubs
}



function resize (listener, interval) {
    var resizeTimeout

    window.addEventListener('resize', function() {
        if (! resizeTimeout) {
            resizeTimeout = setTimeout(function() {
                resizeTimeout = null
                listener.apply(null, arguments)
            }, interval || 66)
        }
    })
}

var Table = (function (d3) {
    "use strict"

    var toString = Object.prototype.toString

    function Table (config) {
        this.init(config)
    }

    Table.prototype = {
        init: function (config) {
            this.config = config
            this.tooltip = config.tooltip
            this.numCol = config.numCol
            this.header = config.header
            this.table = this.body = this.tp = this.tooltip = null
            config.className || (config.className = "rtb")
            this._makeTable(config.wrapper)
            if (_isFunction(config.tooltip)) {
                this.tooltip = config.tooltip
                this.makeTooltip()
            }
        },

        _makeTable: function (wrapper) {
            // make table
            var t = this.table = _make("table", null, this.config.className)
            var h

            // append header
            t.appendChild(h = _make("thead"))
            // header is of one row
            h.appendChild(_makeRow(this.header))
            t.appendChild(this.body = _make("tbody"))
            wrapper.appendChild(this.table)
        },

        addRow: function (content) {
            var r = _makeRow(content, this.numCol)
            r.__data__ = content
            this.body.appendChild(r)
            return r
        },

        makeTooltip: function () {
            this.tp = _make("div", "rtb-tooltip", "rtp-tooltip-hidden")
            this.table.parentNode.appendChild(this.tp)
            this.body.addEventListener("mouseover", tpMouseover(this.tooltip))
            this.body.addEventListener("mouseout", tpMouseout)
        },

        clear: function () {
            this.table.removeChild(this.body)
            this.tp && this.tp.parentNode.removeChild(this.tp)
            this.body = this.tp = null
        },

        destroy: function () {
            this.table.parentNode.removeChild(this.table)
            this.tp && this.tp.parentNode.removeChild(this.tp)
            this.table = this.body = this.header = this.config = this.tp = null
        }
    }

    function _isFunction (obj) {
        return toString.call(obj) === "[object Function]"
    }

    function tpMouseout (e) {
        if (e.target.tagName.toLowerCase() !== "td")
            return
        var t = document.getElementById("rtb-tooltip")
        // t.innerHTML = ''
        t.className = 'rtb-tooltip-hidden'
        t.setAttribute("style", "")
    }

    function tpMouseover (cb) {
        return function (e) {
            if (e.target.tagName.toLowerCase() !== "td")
                return
            var t = document.getElementById("rtb-tooltip")
            // data attached to the row
            t.innerHTML = cb(e.target.parentNode.__data__)
            t.className = "rtb-tooltip"
            t.setAttribute("style", "position:fixed;top:"+e.pageY+"px;left:"+e.pageX+"px;")
        }
    }

    function _makeRow (content, c) {
        var i = 0, len = c || content.length, r = _make("tr")
        for (; i < len; ++i) {
            r.appendChild(_makeCol(content[i]))
        }
        return r
    }


    function _makeCol (text) {
        var t = _make("td")
        t.textContent = text
        return t
    }

    function _make (el, idName, className) {
        var e = document.createElement(el)
        if (idName) e.id = idName
        if (className) e.className = className
        return e
    }

    return Table
}) (d3);
function centrePic(self) {
    var n = self.nodes[0], size = self.force.size(),
    m = [size[0] / 2, size[1] / 2]
    self.group.attr("transform", "translate("+(m[0] - n.x)+","+(m[1] - n.y)+")")
}

/**
 * Places elements along a circle perimenter at equal distance from each other.
 *
 * @param {Array.<Object>} nodes - elements on which set coordinates.
 * @param {Number} r - the radius of the circumference.
 * @param {Array.<Number>} centre - the centre of the circle, default the origin.
 */
function placeAlongFullCircle(nodes, r, centre) {
    if (!centre) centre = [0, 0]
    var k = nodes.length, mpi = Math.PI/180, dist = 360/k * mpi, pos = dist,
        offset = 20
    nodes.forEach(function(d, i) {
        d.x = centre[0] + r * Math.cos(pos) + (-offset + Math.random() * 2 * offset)
        d.y = centre[1] + r * Math.sin(pos) + (-offset + Math.random() * 2 * offset)
        pos += dist
    })
}

function placeAlongCircle(nodes, r, centre) {
    if (!centre) centre = [0, 0]
    var k = nodes.length, mpi = Math.PI/180, dist = 360/k * mpi, pos = dist,
        offset = 20
    nodes.forEach(function(d, i) {
        // d.x = centre[0] + r * Math.cos(pos) + (-offset + Math.random() * 2 * offset)
        d.x = centre[0] + r * Math.cos(i / k * Math.PI) //+ (-offset + Math.random() * 2 * offset)
        d.y = centre[1] + r * Math.sin(i / k * Math.PI) //+ (-offset + Math.random() * 2 * offset)
        // pos += dist
    })
}

/**
 * It returns a tick function for the "tick" event of force layout.
 * @param nodes
 * @param {String} selector
 * @return {Function} the tick function.
 */
function fociTick (nodes, circles, lines, text, self) {
    return function (e) {
        // Push nodes toward their designated focus.
        var k = .1 * e.alpha;
        nodes.forEach(function(o, i) {
            if (o.isHub) return;
            self.getNeighbors(i).forEach(function(h) {
                var x = h.x - o.x
                var y = h.y - o.y
                var l = Math.sqrt(x * x + y * y)
                if (l < 100) {
                    o.y += y * k;
                    o.x += x * k;
                }
            })
        })

        circles
            .attr("transform", function (d) {
                return "translate("+d.x+","+d.y+")"
            })
            // .attr("cx", function(d) { return d.x; })
            // .attr("cy", function(d) { return d.y; });

        lines
            .attr({
                x1: function(d) { return d.source.x },
                y1: function(d) { return d.source.y },
                x2: function(d) { return d.target.x },
                y2: function(d) { return d.target.y }
            })

        if (text) {
            text.attr('transform', function (d) {
                return 'translate('+ (d.x + 5) +','+ (d.y + 5) +')'
            })
        }
    }
}

function fociForce(nodes, size) {
    return d3.layout.force()
        .nodes(nodes)
        .links([])
        .gravity(0)
        .size(size)
}

function computeRadius(nodes, header) {
    var score = header[1]
    var max = nodes[nodes.length-1][score]
    var min = nodes[0][score]
    var diff = max - min
    nodes.forEach(function (n) {
        n.radius = (1 - (n[score] - min) / diff) * 50 + 10
    })
}

function fociDraw() {
    var w = $(window).width(), h = $(window).height()
    // this.nodes = this.nodes.filter(function (n) {
    //     return !n.isHub || (hubCount++ < 5)
    // })
    var hubs = this.nodes.filter(function (d) {
            return d.isHub
        }),
        genes = this.nodes.filter(function(d) {
            return !d.isHub
        }),
        colorScale = null

    placeAlongFullCircle(genes, 230, [w/2, h/2])
    placeAlongFullCircle(hubs, 100, [w/2, h/2])
    computeRadius(hubs, this.header)

    // colorScale = d3.scale.linear()
    //     .domain([this.nodes[this.nodes.length-1].radius,
    //             this.nodes[0].radius])
    //     .range(["#169BF9", "#ff0000"])

    colorScale = d3.scale.category20()

    var self = this, g = self.config.graph,
        lines = self.group.append("svg:g").selectAll("line")
            .data(this.edges).enter()
            .append("svg:line")
            .attr({
                x1: function (d) { return d.source.x },
                x2: function (d) { return d.target.x },
                y1: function (d) { return d.source.y },
                y2: function (d) { return d.target.y },
                "class": this.config.graph.edgeClassName
            }),
        circles = self.group.append("svg:g")
            .selectAll("g")
            .data(self.nodes).enter()
            .append("svg:g"),
        tick = null, text = null

        this.force = fociForce(this.nodes, [w, h])

        circles.append("svg:circle")
            .attr({
                "class": g.nodeClassName,
                "r": g.radius,
                "id": g["id"],
                "fill": function (d, i) {
                    return d.color = d.isHub
                        ? colorScale(d.radius)
                        : "#8D6D8D"
                },
                "stroke": "#352118",
                "stroke-width": 2,
                "opacity": 0.8
                // cx: function (d) { return d.x },
                // cy: function (d) { return d.y }
            })
        circles.append("svg:circle")
            .attr({
                "r": 2,
                "fill": "black"
            })

        if ("text" in this.config)
            text = makeText(this.group, this.nodes, this.config.text)
        tick = fociTick(this.nodes, circles, lines, text, this)

    this.force.on("tick", tick).start()
    // loop(this.config.force.threshold, this, function(self) {
    //     self.force.stop()
    //     centrePic(self)
    //     var drag = self.force.drag().on('dragstart', function (d) {
    //         self.force.stop()
    //         d3.event.sourceEvent.stopPropagation()
    //         d.fixed = true
    //     })
    //     var g = self.config.graph
    //     self.group.selectAll("circle")
    //         .data(self.nodes).enter()
    //         .append("svg:circle")
    //         .attr({
    //             "class": g.nodeClassName,
    //             "r": g.radius,
    //             "id": g["id"],
    //             // cx: function (d) { return d.x },
    //             // cy: function (d) { return d.y }
    //         })
    //         // .call(drag)
    // })
}

function loop (thr, self, cb) {
    var t
    if (self.force.alpha() > thr) {
        self.force.tick()
        t = setTimeout(function () { loop(thr, self, cb) }, 1)
        self.addTimer(t)
    } else {
        cb(self)
    }
}

function nwtDraw() {
    "use strict"

    var nodes = this.nodes, edges = this.edges, conf = this.config,
        lines = null, circles = null, group = this.group, text = "text" in conf,
        neighbors = null, force = null, neighbors = null,
        extent = d3.extent(edges, function (e) {
            return e.value
        }),
        draw = {
            force: nwtGetForce,
            newForce: nwtForce,
            draw: nwtDraw,
            lines: nwtLines,
            circles: nwtCircles,
            tick: nwtTickFn,
            computeThickness: nwtComputeThickness,
            filter: nwtFilter,
            setPosition: nwtSetPosition,
            neighbors: nwtNeighbors,
            text: nwtText
        }

    // compute neighbors
    // this.getNeighbors(0)
    // neighbors = this.neighbors

    // function signHubs() {
    //     nodes.forEach(function (n) {

    //     })
    // }

    function nwtFilter(cutoff) {
        var n = []
        edges = edges.filter(function (e) {
            if (e.value >= cutoff) {
                n.push(e.source, e.target)
                return true
            }
            return false
        })

        for (var i = 0; i < n.length; ++i) {
            for (var j = i + 1; j < n.length; ++j) {
                if (n[i] === n[j]) {
                    n.splice(j, 1);
                    --j
                }
            }
            n[i].index = i
        }

        nodes = n

        return draw
    }

    function nwtGetForce() {
        return force
    }

    function nwtForce(size) {
        return d3.layout.force()
            .nodes(nodes)
            .links(edges)
            .gravity(0.01)
            .linkDistance(150)
            .size(size)
    }

    function nwtDraw() {
        var size = [$(window).width(), $(window).height()]
        draw.filter(extent[1] / 5)
        draw.setPosition([size[0]/2, size[1]/2])
        draw.computeThickness()
        force = draw.newForce(size)
        lines = draw.lines()
        circles = draw.circles()
        if (text) {
            text = draw.text()
        }
        force.on("tick", draw.tick()).start()
        return draw
    }

    function nwtLines() {
        var g = conf.graph
        var lines = group.append("svg:g").selectAll("line")
            .data(edges).enter()
            .append("svg:line")
            .attr({
                x1: function (d) { return d.source.x },
                x2: function (d) { return d.target.x },
                y1: function (d) { return d.source.y },
                y2: function (d) { return d.target.y },
                "class": g.edgeClassName,
                "stroke-width": function (d) {
                    return d.value
                },
                "opacity": 0.8
            })
        return lines
    }

    function nwtCircles() {
        var g = conf.graph

        var circles = group.append("svg:g")
            .selectAll("g")
            .data(nodes).enter()
            .append("svg:g")

        circles.append("svg:circle")
            .attr({
                "class": g.nodeClassName,
                "r": g.radius,
                "id": g["id"],
                "stroke": "#352118",
                "stroke-width": 2,
                "opacity": 0.8
                // cx: function (d) { return d.x },
                // cy: function (d) { return d.y }
            })
        circles.append("svg:circle")
            .attr({
                "r": 2,
                "fill": "black"
            })
        return circles
    }

    function nwtText() {
        return makeText(group, nodes, conf.text)
    }

    function nwtComputeThickness() {
        var diff = extent[1] - extent[0]

        edges.forEach(function (e) {
            e.value = (e.value - extent[0]) / diff * 11 + 1
        })

        return draw
    }

    function nwtSetPosition(centre) {
        var inner = 200, outer = 300, r, mpi = Math.PI/180,
            dist = 360/nodes.length * mpi, pos = dist
        nodes.forEach(function (d, i) {
            if (draw.neighbors(i).length > 4) r = inner
            else r = outer
            d.x = centre[0] + r * Math.cos(pos)
            d.y = centre[1] + r * Math.sin(pos)
            pos += dist
        })
    }

    function nwtNeighbors(idx) {
        if (!neighbors) {
            var n = nodes.length
            var m = edges.length
            neighbors = new Array(n)
            for (var j = 0; j < n; ++j) {
                neighbors[j] = []
            }
            for (j = 0; j < m; ++j) {
                var o = edges[j]
                neighbors[o.source.index].push(o.target);
                neighbors[o.target.index].push(o.source);
            }
        }
        return neighbors[idx]
    }

    function nwtTickFn () {
        // 1.. tick with text
        var cb0 = function () {
            circles
                .attr('transform', function (d) {
                    return 'translate(' + d.x + ',' + d.y + ')'
                })

            lines
                .attr({
                    x1: function(d) { return d.source.x },
                    y1: function(d) { return d.source.y },
                    x2: function(d) { return d.target.x },
                    y2: function(d) { return d.target.y }
                })

            text.attr('transform', function (d) {
                return 'translate('+ (d.x + 5) +','+ d.y +')'
            })
        },

        // 1.. tick without text
        cb1 = function () {
            circles
                .attr('transform', function (d) {
                    return 'translate(' + d.x + ',' + d.y + ')'
                })

            lines
                .attr({
                    x1: function(d) { return d.source.x },
                    y1: function(d) { return d.source.y },
                    x2: function(d) { return d.target.x },
                    y2: function(d) { return d.target.y }
                })
        },

        callback = cb1

        circles
            .attr('transform', function (d) {
                // d.fixed = true
                // d3.select(this).style('fill', d3.rgb(d.color).darker(d.weight/3))
                return 'translate(' + d.x + ',' + d.y + ')'
            })

        lines
            .attr({
                x1: function(d) { return d.source.x },
                y1: function(d) { return d.source.y },
                x2: function(d) { return d.target.x },
                y2: function(d) { return d.target.y }
            })

        if (text) {
            text.attr('transform', function (d) {
                return 'translate('+ (d.x + 5) +','+ d.y +')'
            })

            callback = cb0
        }

        return callback
    }


    // function scroll() {
    //     var dispatch = d3.dispatch("scroll"), hLength = 300, min = 0, max = 1
    //         vLength = 16, diff = 1, xPad = 20, yPad = 20, sc = {},
    //         scGroup = group.append("svg:g").attr("id", "scroll")

    //     function scDraw() {
    //         scGroup.selectAll("line.vscroll")
    //             .data([0, hLength]).enter()
    //             .append("svg:line")
    //             .attr({
    //                 x1: function (d) {
    //                     return d + xPad
    //                 },
    //                 y1: yPad - vLength / 2,
    //                 x2: x1: function (d) {
    //                     return d + xPad
    //                 },
    //                 y2: vLength / 2 + yPad,
    //                 "stroke-width": 2,
    //                 "stroke": "#333"
    //             })

    //         scGroup.append("svg:line")
    //             .attr({
    //                 x1: xPad, y1: yPad,
    //                 x2: xPad + hLength, y2: yPad,
    //                 "stroke-width": 5,
    //                 "stroke": "#ff0"
    //             })
    //     }

    //     scGroup
    //         // .data({ x: xPad + hLength/2, y: yPad }).enter()
    //         .append("svg:path")
    //         .attr("transform", function(d) {
    //             return "translate(" + x(d.x) + "," + y(d.y) + ")"
    //         })
    //         .attr("d", d3.svg.symbol())
    // }

    // function x(pos) {
    //     return xPax + currentX
    // }

    // function y(pos) {
    //     return yPad
    // }

    return draw
}
var concat = Array.prototype.push
var slice = Array.prototype.slice
var toString = Object.prototype.toString

function assign(obj) {
    slice.call(arguments, 1).forEach(function(source) {
        if (source) {
            for (var prop in source) {
                obj[prop] = source[prop]
            }
        }
    })
    return obj;
}

function extend (protoProps, staticProps) {
    var parentCtor = this

    function F() {
        parentCtor.apply(this, arguments)
        this.constructor = F
    }

    F.prototype = Object.create(parentCtor.prototype)

    assign(F.prototype, protoProps)
    assign(F, staticProps)

    return F
}
////////////////////////////////////////////////////////////////////////////////
// NOTE!!
//
// Just for now ignore renderInvalid Option!
////////////////////////////////////////////////////////////////////////////////
function BaseNetworkRenderer () {}
BaseNetworkRenderer.extend = extend

BaseNetworkRenderer.prototype = assign({}, biomart.renderer.results.plain, {
    // The wrap container
    tagName: 'div',

    /**
     *
     * @param {String} value
     */
    addProp: function (node, key, value) {
        if (toString.call(value) === "[object String]" &&
            value.indexOf('<a') >= 0) {
            value = $(value)
            node[key] = value.text()
            node._link = value.attr('href')
        } else {
            node[key] = value
        }

        return node
    },

    init: function () {
        this.nodes = []
        this.edges = []
        this.rowBuffer = []
        this.neighbors = []
        this.header = null
    },

    /**
     * Creates and inserts new nodes within BaseNetworkRenderer#nodes. If the
     * nodes that are supposed to be created are already present within
     * BaseNetworkRenderer#nodes, then returns those nodes and the new ones.
     *
     * NOTE: A node must have a _id property for which will be compared for identity
     * with an other node, and an index property representing the its own index
     * within the nodes array.
     *
     * @param {Array.<string>} row - a row
     * @param {Array.<string>} header - the header of incoming data
     * @param {Array.<Object>} - array of new or already present nodes
     */
    insertNodes: function (row, header) {
        throw new Error("BaseNetworkRenderer#insertNodes not implemented")
    },

    /**
     * Creates and insert new edges within BaseNetworkRenderer#edges. Duplicated edges will
     * not be added to the collection.
     *
     * NOTE: An edge must have a _id property for which will be compared for identity
     * with an other edge.
     *
     * @param {Array.<Object>} nodes - nodes returned by the last invokation of
     * BaseNetworkRenderer#insertNodes
     * @param {Array.<string>} row - a row
     * @param {Array.<string>} header - the header of incoming data
     * @return {Array.<Object>} - the inserted edges
     */
    insertEdges: function (nodes, row, header) {
        throw new Error("BaseNetworkRenderer#insertEdges not implemented")
    },

    /**
     * Given a node index returns its adjacency.
     * @param {number} nodeIndex - index of the node within BaseNetworkRenderer#nodes
     * @return the adjacency of the node
     */
    getNeighbors: function (nodeIndex) {
        // throw new Error("BaseNetworkRenderer#neighbors not implemented")
        // From d3's src/layout/force.js
        var ne = this.neighbors, n, m, j, nodes = this.nodes, links = this.edges
        if (!ne.length) {
            n = nodes.length
            m = links.length
            ne = new Array(n)
            for (j = 0; j < n; ++j) {
                ne[j] = []
            }
            for (j = 0; j < m; ++j) {
                var o = links[j]
                ne[o.source.index].push(o.target);
                ne[o.target.index].push(o.source);
            }
        }
        return ne[nodeIndex]
    },

    findIndex: function (collection, cb) {
        for (var i = 0, len = collection.length; i < len; ++i) {
            if (cb(collection[i]))
                return i
        }
        return -1
    },

    /**
     * Creates the proper nodes and edges and populates BaseNetworkRenderer#nodes and
     * BaseNetworkRenderer#edges.
     * @param {Array.<string>} rows - all the rows retrieved
     */
    makeNE: function (rows) {
        var h = this.header, e = this.edges, i = 0, rLen = rows.length, r

        for (; i < rLen && (r = rows[i]); ++i) {
            this.insertEdges(this.insertNodes(r, h), r, h)
        }
    },

    // rows : array of arrays
    parse: function (rows, writee) {
        concat.apply(this.rowBuffer, rows)
    },

    /**
     * Adds a new tab.
     * Initializes the tab widget at the first invokation.
     *
     * @param {jQuery} $container - it's the element on which to invoke the tab widget.
     * @param {jQuery} $tabs - it's the tab headers container, usually a list.
     * @return a jQuery object representing the newly create tab.
     */
    newTab: function($container, $tabs, title) {
        var item, tabNum, svg

        tabNum = $tabs.children().size() + 1
        if (tabNum === 1)
            $container.tabs()

        title || (title = Object.keys(biomart._state.queryMart.attributes)[tabNum-1])

        var itemIdSelector = '#item-'+ tabNum
        // For each attribute list create a tab
        $container.tabs('add', itemIdSelector, title)

        return $(itemIdSelector)
    },

    /**
     * It creates and appends a new svg to the specified container.
     * To the svg it's going to be appended a group.
     *
     * @param {Object} config .
     * @param {DOMElement} config.container - the element where append the new svg.
     * @param {number} config.w - width of the svg.
     * @param {number} config.h - height of the svg.
     * @param {string} [config.idName] - id for the svg.
     * @param {string} [config.className] - class for the svg.
     * @return the group appended to the new svg.
     */
    newSVG: function (config) {
        var w = config.w, h = config.h, container = config.container, group, svg

        // Playground for the new network
        svg = d3.select(container)
            .append('svg:svg')
            .attr({ width: w, height: h })

        if ("idName" in config)
            svg.attr("id", config.idName)
        if ("className" in config)
            svg.attr("class", config.className)

        group = svg.append('g')
            .call(d3.behavior.zoom().scaleExtent([0, 20]).on('zoom', function () {
                group.attr("transform", "translate(" + d3.event.translate + ")scale(" + d3.event.scale + ")") }))
            .append('g')

        group.append("rect")
            .attr('class', 'zoom-container')
            .attr('x', -5e3)
            .attr('y', -3e3)
            .attr("width", 1e4)
            .attr("height", 6e3)

        return group.append("g")
    },

    printHeader: function(header, writee) {
        this.header = header
    },

    draw: function (writee) {

    },

    clear: function () {
        // Now that we're submitting only one query i can safely implement this m.
        this.init()
    },

    destroy: function () {
        this.init()
    }
})




function getTickFn (graph, text) {
    // 1.. tick with text
    var cb0 = function () {
        graph.bubbles
            .attr('transform', function (d) {
                return 'translate(' + d.x + ',' + d.y + ')'
            })

        graph.links
            .attr({
                x1: function(d) { return d.source.x },
                y1: function(d) { return d.source.y },
                x2: function(d) { return d.target.x },
                y2: function(d) { return d.target.y }
            })

        text.attr('transform', function (d) {
            return 'translate('+ (d.x + 5) +','+ d.y +')'
        })
    },

    // 1.. tick without text
    cb1 = function () {
        graph.bubbles
            .attr('transform', function (d) {
                return 'translate(' + d.x + ',' + d.y + ')'
            })

        graph.links
            .attr({
                x1: function(d) { return d.source.x },
                y1: function(d) { return d.source.y },
                x2: function(d) { return d.target.x },
                y2: function(d) { return d.target.y }
            })
    },

    callback = cb1

    graph.bubbles
        .attr('transform', function (d) {
            d.fixed = true
            d3.select(this).style('fill', d3.rgb(d.color).darker(d.weight/3))
            return 'translate(' + d.x + ',' + d.y + ')'
        })

    graph.links
        .attr({
            x1: function(d) { return d.source.x },
            y1: function(d) { return d.source.y },
            x2: function(d) { return d.target.x },
            y2: function(d) { return d.target.y }
        })

    if (text) {
        text.attr('transform', function (d) {
            return 'translate('+ (d.x + 5) +','+ d.y +')'
        })

        callback = cb0
    }

    return callback
}

function showNetwork (struct) {
    setEventHandlers(struct)
    var n = struct.renderer.nodes[0], size = struct.force.size(),
    m = [size[0] / 2, size[1] / 2]
    struct.renderer.group.attr("transform", "translate("+(m[0] - n.x)+","+(m[1] - n.y)+")")
}

function setEventHandlers(struct) {
    var fn = getTickFn(struct.graph, struct.text)
    struct.force.on('tick', function () { fn() })

    var drag = struct.force.drag().on('dragstart', function (d) {
        struct.force.stop()
        d3.event.sourceEvent.stopPropagation()
        d.fixed = true
    })
    struct.graph.bubbles.call(drag)
}

function endSimulation (struct) {
    console.timeEnd('simulation ticks')
    struct.force.stop()
    showNetwork(struct)
}

function initPosition (nodes, width, height) {
    nodes.forEach(function (node) {
        node.x = Math.random() * width
        node.y = Math.random() * height
    })
}

var NetworkRenderer = BaseNetworkRenderer.extend({

    config: biomart.networkRendererConfig,

    init: function () {
        BaseNetworkRenderer.prototype.init.call(this)
        this.tabSelector = '#network-list'
        this.group = null
    },

    getElement: function () {
        // If already present there isn't need to do anything else here
        if ($(this.tabSelector).size())
            return $(this.tabSelector)

        // Create the container
        var $elem = BaseNetworkRenderer.prototype.getElement.call(this)
        // This is the actual tab list
        $elem.append('<ul id="network-list" class="network-tabs"></ul>')
        return $elem
    },

    findElem: function (collection, idName) {
        var c = collection
        for (var n = null, i = 0, len = c.length; i < len; ++i) {
            if (idName === (n = c[i])._id)
                return n
        }
        return null
    },

    insertNodes: function (row, header) {
        var n0 = this.findElem(this.nodes, row[0]),
            n1 = this.findElem(this.nodes, row[1]), index
        if (! n0) {
            index = this.nodes.push(n0 = this.addProp({}, header[0], row[0])) - 1
            this.addProp(n0, 'index', index)
            this.addId(n0, row[0])
        }
        if (! n1) {
            index = this.nodes.push(n1 = this.addProp({}, header[1], row[1])) - 1
            this.addProp(n1, 'index', index)
            this.addId(n1, row[1])
        }
        // n0.radius = 'radius' in n0 ? n0.radius + 3 : 3
        // n1.radius = 'radius' in n1 ? n1.radius + 3 : 3

        return [n0, n1]
    },

    addId: function (o, idValue) {
        this.addProp(o, '_id', idValue)
    },

    makeTable: function (wrapper) {
        // $elem.append('<div id="network-report-table" class="network-report-table"></div>')
        this.table = new Table({
            wrapper: wrapper,
            className: "",
            header: this.header.slice(0, -1),
            numCol: 3,
            tooltip: null
        })
    },

    // I'm assuming there could not be duplicated edges and
    // nodes.length === 2
    insertEdges: function (nodes, row, header) {
        // ids are strings here
        var _id = nodes[0]._id + nodes[1]._id,
            e = this.findElem(this.edges, _id),
            edge

        if (! e) {
            edge = {
                _id: _id,
                source: nodes[0],
                target: nodes[1],
                value: row[2]
            }
            this.edges.push(edge)
        }

        return [edge]
    },

    printHeader: function (header, writee) {
        this.init()
        BaseNetworkRenderer.prototype.printHeader.call(this, header, writee)
    },

    makeNE: function (rows) {
        BaseNetworkRenderer.prototype.makeNE.call(this, rows)
        for (var i = 0, rLen = rows.length, r; i < rLen && (r = rows[i]); ++i) {
            this.table.addRow(this.makeRow(r))
        }
    },

    makeRow: function (row) {
        return row
    },

    draw: function (writee) {
        var t = "", attrs = Object.keys(biomart._state.queryMart.attributes)
        attrs.forEach(function(a) { t += a + " " })
        var domItem = this.newTab(writee, $(this.tabSelector), t)[0]

        this.group = this.newSVG({
            container: domItem,
            w: "100%",
            h: "100%",
            className: "network-wrapper"
        })

        this.makeTable($('<div class="network-report-table">').appendTo($(domItem))[0])

        this.makeNE(this.rowBuffer)
        this.rowBuffer = []
        this.canvas = nwtDraw.call(this).draw()
        // this.drawNetwork(this.config)
        // Reset the status for the next draw (tab)
        // this.init()

        $.publish('network.completed')
    },

    onResize: function (force) {
        var w = $(window).width(), h = $(window).height()
        if (this.group && !this.group.empty()) {
            this.group.attr({
                width: w,
                height: h
            })
            force.size([w, h])
        }
    },

    clustering: function (struct) {
        struct.wk = 0.5
        cluster(struct)
    },

    drawNetwork: function (config) {
        var w = $(window).width(), h = $(window).height(),
            drawText = 'text' in config, self = this,
            struct = { config: config, renderer: this }

        config.force.size = [w, h]

        this.makeNE(this.rowBuffer)
        this.rowBuffer = []
        struct.graph = makeGraph(this.group, this.nodes, this.edges, config.graph)
        struct.hubs = hubs(this.edges, this.nodes)
        initPosition(this.nodes, w, h)

        if (drawText) {
            config.text.text = config.graph['id']
            struct.text = makeText(this.group, this.nodes, config.text)
        }

        // `cluster` defines the right force configuration: e.g. charge, tick
        // TODO: NOTE: This is a temporary solution, find a better one!
        this.clustering(struct)
        // Now we can create the force layout. This actually starts the symulation.
        struct.force = makeForce(this.nodes, this.edges, config.force)
        resize(function () { self.onResize.call(self, struct.force) })

        function loop (thr, iter) {
            var t
            if (iter < 1000 && struct.force.alpha() > thr) {
                struct.force.tick()
                t = setTimeout(function () { loop(thr, ++iter) }, 1)
                self.addTimer(t)
            } else {
                endSimulation(struct)
            }
        }

        // Make the simulation in background and then draw on the screen
        struct.force.start()
        console.time('simulation ticks')
        loop(config.force.threshold, 0)
    },

    clear: function () {
        BaseNetworkRenderer.prototype.clear.call(this)
        this.clearTimers()
        if (this.group) {
            d3.select(this.group.node().nearestViewportElement).remove()
        }
        this.group = null
        this.table && this.table.destroy()
        this.table = null
    },

    destroy: function () {
        this.clear()
    },

    addTimer: function (t) {
        this.timers || (this.timers = [])
        this.timers.push(t)
    },

    clearTimers: function () {
        this.timers && this.timers.forEach(function (t) {
                clearTimeout(t)
        })
        this.timers = []
    }

})

NetworkRenderer.extend = extend

biomart.renderer.results.network = new NetworkRenderer()

var EnrichmentRenderer = NetworkRenderer.extend({

    config: biomart.enrichmentRendererConfig,

    init: function () {
        NetworkRenderer.prototype.init.call(this)
        this.annCount = -1
    },

    makeTable: function (wrapper) {
        // $elem.append('<div id="network-report-table" class="network-report-table"></div>')
        this.table = new Table({
            wrapper: wrapper,
            className: "",
            header: this.header.slice(0, -2).concat([this.header[4]]),
            numCol: 4,
            tooltip: function (data) {
                var i = 0, d = data[4].split(","), len = d.length, b = ""
                for (; i < len; ++i) {
                    b += d[i]+"<br>"
                }
                return b
            }
        })
    },

    draw: function (writee) {
        var domItem = this.newTab(writee, $(this.tabSelector))[0]

        this.group = this.newSVG({
            container: domItem,
            w: "100%",
            h: "100%",
            className: "network-wrapper"
        })

        this.makeTable($('<div class="network-report-table">').appendTo($(domItem))[0])

        this.makeNE(this.rowBuffer)
        this.rowBuffer = []

        fociDraw.call(this)
        $.publish('network.completed')
    },

    // makeNE: function (rows) {
    //     NetworkRenderer.prototype.makeNE.call(this, rows)
    //     for (var i = 0, rLen = rows.length, r; i < rLen && (r = rows[i]); ++i) {
    //         this.table.addRow(this.makeRow(r))
    //     }
    // },

    makeRow: function (row) {
        return row.slice(0, -2).concat([row[4], row[3]])
    },

    insertNodes: function (row, header) {
        var ann, gs, res, index, g, gid, annIdx = 0, genesIdx = 3,
            // p-value index, bonferroni p-value
            pvIdx = 1, bpvIdx = 2, descIdx = 4

        if (++this.annCount >= 5) return []
        //row: [annotation, score, gene list]
        ann = this.findElem(this.nodes, row[annIdx])
        gs = row[genesIdx].split(",")
        res = []

        if (! ann) {
            index = this.nodes.push(ann = this.addProp({}, header[annIdx], row[annIdx])) - 1
            this.addProp(ann, header[pvIdx], row[pvIdx])
            this.addProp(ann, "description", row[descIdx])
            // this.addProp(ann, header[bpvIdx], row[bpvIdx])
            this.addId(ann, row[annIdx])
            ann.index = index
            ann.isHub = true
            // Nodes are sorted by score
            // ann.radius = (row[1] / this.nodes[0][header[1]]) / 700  + 15
        }
        res.push(ann)
        for (var i = 0, len = gs.length; i < len; ++i) {
            g = this.findElem(this.nodes, gid = gs[i])
            if (! g) {
                index = this.nodes.push(g = this.addProp({}, header[genesIdx], gid)) - 1
                this.addId(g, gid)
                g.index = index
                g.radius = 8
            }
            res.push(g)
        }
        return res
    },

    insertEdges: function (nodes, row, header) {
        //nodes: [ann, g0, g1, ...]
        //row/header: [annotation, score, gene list]
        if (!nodes.length) return []
        var ann = nodes[0], annId = ann._id, eid, g, e, res = []
        for (var i = 1, len = nodes.length; i < len; ++i) {
            e = this.findElem(this.edges, eid = annId + (g = nodes[i])._id)
            if (! e) {
                this.edges.push(e = {source: ann, target: g, _id: annId+g._id })
                res.push(e)
            }
        }
        return res
    },

    clustering: function (struct) {
        struct.wk = 7
        cluster(struct)
    },

    clear: function () {
        NetworkRenderer.prototype.clear.call(this)
        // this.table && this.table.destroy()
        // this.table = null
    }
})

biomart.renderer.results.enrichment = new EnrichmentRenderer()

})(d3);