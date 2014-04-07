(function($) {
$.namespace('biomart.url', function(self) {
    self.SEPARATOR = '#!/';

    var regex = new RegExp([
        '(https?:\/\/[^\/]+\/?)?([^#\\?]+)?(\\?([^#]+))?(', self.SEPARATOR, '(.+))?'
    ].join(''));

    self.jsonify = function(url) {
        url = url || location.href;
        var match = url.match(regex);

        if (!match) return false;

        return {
            host: match[1],
            path: unescape(match[2]) || '',
            query: match[4] || '',
            fragment: match[6] || ''
        };
    };

    self.stringify = function(hash) {
        var arr = [hash.host, hash.path];
        if (hash.query) arr.push(['?', hash.query].join(''));
        if (hash.fragment) arr.push(['#!/', hash.fragment].join(''));
        
        return arr.join('');
    };

    // Very simple but very fast query param parsing.
    // If multiple entries exist for a param, the value of the first occurrence is used.
    // This is not normal behaviour, but is not allowed in our application anyway.
    self.simpleQueryParams = function(s) {
        if (!s) return null;
        var arr = s.split('&'), n = arr.length, dict = {}; 
        while(n--) { var me = arr[n]; me = me.split('='); dict[me[0]] = decodeURIComponent(me[1]).replace(/\+/g, ' ') }
        return dict;
    };

});
})(jQuery);

