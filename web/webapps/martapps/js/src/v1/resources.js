(function($) {
$.namespace('biomart.resource', function(self) {
    self.cache = {};
    self.params = {};

    self.load = function(resource, callback, params) {
        var data = $.extend({}, self.params, params),
            dataType = BIOMART_CONFIG.service.type,
            url = [resource, '.', dataType, '?', $.param(data)].join('');

         if (self.cache[url]) {
             callback(self.cache[url]);
        } else {
            $.ajax({
                url: BIOMART_CONFIG.service.url + url,
                dataType: dataType,
                success: function(json) {
                    var type = resource.split('/')[0];
                    self.cache[url] = json;
                    callback(json);
                    $.publish(type+'load', json);
                }, 
                error: function() {
                    biomart.error();
                }
            });
        }
    }
});
})(jQuery);

