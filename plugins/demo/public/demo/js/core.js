// usage: log('inside coolFunc', this, arguments);
// paulirish.com/2009/log-a-lightweight-wrapper-for-consolelog/
window.log = function(){
  log.history = log.history || [];   // store logs to an array for reference
  log.history.push(arguments);
  arguments.callee = arguments.callee.caller; 
  if(this.console) console.log( Array.prototype.slice.call(arguments) );
};
// make it safe to use console.log always
(function(b){function c(){}for(var d="assert,count,debug,dir,dirxml,error,exception,group,groupCollapsed,groupEnd,info, log,markTimeline,profile,profileEnd,time,timeEnd,trace,warn".split(","),a;a=d.pop();)b[a]=b[a]||c})(window.console=window.console||{});

_('BM').namespace(function(self) {
    self.NAME = 'BioMart'
    self.SEPARATOR = '#!/'

    var guid = 1,
        regex = new RegExp([
            '(https?:\/\/[^\/]+\/?)?([a-zA-Z0-9._\\-/ %]+)?(\\?([^#]+))?(', self.SEPARATOR, '(.+))?'
        ].join(''))

    self.i18n = {CAPITALIZE: 1, PLURAL: 2}

    self.CLASS_NAME_REGEX = /[^a-zA-Z0-9_-]/g;

    self.conf = {
        url: '/martservice'
    }

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
