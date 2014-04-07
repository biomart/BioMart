(function($) {
$.namespace('biomart.signals', function(exports) {
    var _elements = {
        crumbs: $('#biomart-breadcrumbs')
    };

    exports.updateBreadcrumbs = function(gui) {
        var arr = []
        if (gui.parentDisplayName && gui.parentDisplayName != 'root') {
            arr.push(['<li>', gui.parentDisplayName, '</li>'].join(''));
        }
        arr.push(['<li>', gui.displayName, '</li>'].join(''));
        $(arr.join('')).appendTo(_elements.crumbs);
    };
});

$.subscribe('guiload', biomart.signals, 'updateBreadcrumbs');
})(jQuery);
