// Custom utility functions
_.mixin({
    // Create namespace
    namespace: function(str, obj) {
        var p = window, ns = str.split('.')
        for(var i=0, curr; curr=ns[i]; i++) 
            p = p[curr] = p[curr] || {}
        if (obj) 
            _.extend(p, _.isFunction(obj) ? obj(p) : obj)
    },

    // Partial application
    partial: function(fn) {
        var curryArgs = Array.prototype.slice.call(arguments, 1)
        return (function() {
            var normalizedArgs = Array.prototype.slice.call(arguments, 0)
            return fn.apply(null, curryArgs.concat(normalizedArgs))
        })
    },

    // i18n labels
    i18n: function(label, o) {
        var key = label.replace(/\s+/g, '_'),
            str = BM.conf.labels[key] || label,
            plural
        if (o & BM.i18n.PLURAL) {
            plural = BM.conf.labels[key+'__plural']
            if (plural) {
                str = plural
            } else {
                str = str + 's'
            }
        }
        if (o & BM.i18n.CAPITALIZE) {
            str = str.charAt(0).toUpperCase() + str.slice(1)
        }
        return str
    },

    // Strips out any invalid character for class names
    slugify: function(str) {
        return str.replace(BM.CLASS_NAME_REGEX, '');
    },

    // Escapes XML entities
    escapeXML: function(str) {
        var el = $('<span/>')
        el.text(str)
        return el.html()
    }
})



