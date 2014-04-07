/*
 * Set log() function to whatever is available
 */
window.log = function() {
    if (window.console)  {
        window.console.log(arguments)
    }
}

/*
 * Helper function for calling __super__ functions for Backbone's 
 * View and Model objects.
 */
Backbone.Model.prototype.__super__ = function(funcName) {
    return this.constructor.__super__[funcName].apply(this, _.rest(arguments))
}
Backbone.View.prototype.__super__ = function(funcName) {
    return this.constructor.__super__[funcName].apply(this, _.rest(arguments))
}

/*
 * Core BioMart functions
 */
_('BM').namespace(function(self) {
    self.NAME = 'BioMart'
    self.SEPARATOR = '#!/'
    self.PREVIEW_LIMIT = 1000

    var guid = 1,
        regex = new RegExp([
            '(https?:\/\/[^\/]+\/?)?([a-zA-Z0-9._\\-/ %]+)?(\\?([^#]+))?(', self.SEPARATOR, '(.+))?'
        ].join(''))

    self.i18n = {CAPITALIZE: 1, PLURAL: 2}

    self.CLASS_NAME_REGEX = /[^a-zA-Z0-9_-]/g;

    self.conf = $.extend({}, BIOMART_CONFIG)

    self.jsonify = function(url) {
        url = url || location.href
        var match = url.match(regex)

        if (!match) return false;

        return {
            host: match[1],
            path: match[2] || '',
            query: match[4] || '',
            fragment: match[6] || ''
        }
    }

    self.stringify = function(hash) {
        var arr = [hash.host, hash.path]
        if (hash.query) arr.push(['?', hash.query].join(''))
        if (hash.fragment) arr.push(['#!/', hash.fragment].join(''))
        
        return arr.join('')
    }

    // Very simple but very fast query param parsing.
    // If multiple entries exist for a param, the value of the first occurrence is used.
    // This is not normal behaviour, but is not allowed in our application anyway.
    self.simpleQueryParams = function(s) {
        if (!s) return null;
        var arr = s.split('&'), n = arr.length, dict = {}; 
        while(n--) { var me = arr[n]; me = me.split('='); dict[me[0]] = decodeURIComponent(me[1]).replace(/\+/g, ' ') }
        return dict;
    }
})
