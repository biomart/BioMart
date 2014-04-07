(function($) {
$.namespace('biomart.martform.params', function(self) {
    var hash = biomart.url.jsonify( biomart.url.jsonify().fragment ),
        path = hash.path.split('/'),
        params = $.deparam(biomart.METHOD=='POST' ? biomart.POST.qs : hash.query) || {};

    self.delegate = function() {};

    self.getGuiContainer = function() { return unescape(path[0]) };
    self.getMart = function() { return unescape(path[1]) };
    self.isPreview = function() { return !!params.preview };
    self.getDatasets = function() { return params.ds };
    self.getAttr = function() { return params.attr ? params.attr.split(',') : false };
    self.getPage = function() { return params.page || 1 };
    self.getSort = function() {
        if (params.order && params.col) 
             return [params.order, parseInt(params.col)];
        return false;
    };
    // Return a copy of parameters without datasets or preview
    self.getFilters = function() {
        var filters = $.extend({}, params);
        delete filters.page;
        delete filters.ds;
        delete filters.preview;
        delete filters.attr;
        delete filters.order;
        delete filters.col;
        return filters;
    };

    // returns false if path hasn't changed
    self.setMart = function(m) {
        if (m != path[1]) {
            path[1] = m;
            hash.path = path.join('/');
            hash.query = '';
            params = {};
            updateFragment();
            $.publish('biomart.changed.mart', m);
        }
    };
    
    self.setDatasets = function(ds) {
        if (params.ds != ds) {
            // Clear filters and attributes because they may be invalid
            params = { ds: ds };
            hash.query = $.param(params);
            updateFragment();
            $.publish('biomart.changed.datasets', ds);
        }
    };
    self.setPage = function(i) {
        self.setParam('page', i);
    };

    self.setSort = function(asc, index) {
        self.setParam('page', 1, false);
        self.setParam('order', asc ? 'asc' : 'desc', false);
        self.setParam('col', index, true);
    };

    self.setPreview = function(isPreview) {
          if (isPreview && !params.preview) {
            params.preview = true;
            $.publish('biomart.preview');
        } else if (!isPreview && params.preview) {
            delete params.preview;
            $.publish('biomart.edit');
        } else {
            return;
        }
        hash.query = $.param(params);
        updateFragment();
    };

    self.setParam = function(name, value, update) {
        if (value != 0 && !value) delete params[name];
        else params[name] = value;
        hash.query = $.param(params);
        if (update !== false) updateFragment();
    };

    self.setAttr = function(attr) {
        params.attr = attr;
        hash.query = $.param(params);
        updateFragment();
    };

    self.addAttr = function(name) {
        var arr = params.attr ? params.attr.split(',') : [];
        arr.push(name);
        params.attr = arr.join(',');
        hash.query = $.param(params);
        updateFragment();
    };

    self.removeAttr = function(name) {
        var i = $.inArray(name);
        if (params.attr && i != -1) {
            var arr = params.attr.split(',');
            arr.remove(i);
            params.attr = arr.join(',');
            hash.query = $.param(params);
            updateFragment();
        }
    };

    function updateFragment() {
        location.hash = [biomart.url.SEPARATOR, biomart.url.stringify(hash)].join('');
    }

    function update() {
        var newHash = biomart.url.jsonify( biomart.url.jsonify().fragment ),
            newPath = newHash.path.split('/'),
            newParams = $.deparam(newHash.query) || {};

        if (path[1] != newPath[1]) {
            self.setMart(newPath[1]);
        } else {
            if (params.ds != newParams.ds) {
                if (newParams.ds) self.setDatasets(newParams.ds);
            }
            if (params.preview != newParams.preview) {
                self.setPreview(!!newParams.preview);
            }
            if (newParams.order && newParams.col) {
                if (newParams.order != params.order || newParams.col != params.col) {
                    self.setParam('order', newParams.order, false);
                    self.setParam('col', newParams.col, true);
                    $.publish('biomart.changed.sort', self.getSort());
                }
            } else if (params.order && params.col) {
                self.setParam('order', null, false);
                self.setParam('col', null, true);
                $.publish('biomart.changed.sort', null);
            }
            if (newParams.preview && params.page != newParams.page) {
                self.setPage(newParams.page);
                $.publish('biomart.changed.page', newParams.page || 1);
            }
        }
    }

    $(window).bind('hashchange', update);
});
})(jQuery);
